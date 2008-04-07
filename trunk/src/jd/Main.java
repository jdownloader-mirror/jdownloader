package jd;

//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.

import java.awt.Graphics;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JWindow;

import jd.captcha.JACController;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.controlling.interaction.Unrar;
import jd.event.UIEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Plugin;
import jd.unrar.JUnrar;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * @author astaldo/JD-Team
 */

public class Main {

	private static Logger logger = JDUtilities.getLogger();

	public static void main(String args[]) {
		
		logger.info(args.toString());
		
		Boolean newInstance = false;
		
		// Mac specific //
		if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
			
			logger.info("apple.laf.useScreenMenuBar=true");
			logger.info("com.apple.mrj.application.growbox.intrudes=false");
			logger.info("com.apple.mrj.application.apple.menu.about.name=jDownloader");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "jDownloader");
			System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			
		}
		
		JDLocale.setLocale("english");
		logger.info("Class path: "+System.getProperty("java.class.path"));
		
		// pre start parameters //
		for (String currentArg : args) {
			
			if (currentArg.equals("--new-instance") || currentArg.equals("-n")) {

				if (Runtime.getRuntime().maxMemory() < 100000000) {
					JDUtilities.restartJD(args);
				}
				
				logger.info(currentArg + " parameter");
				
				newInstance = true;
				break;

			} else if (currentArg.equals("--help") || currentArg.equals("-h")) {
				
				Server.showCmdHelp();
				System.exit(0);
				
			}

		}
		
		JDTheme.setTheme("default");
		
		boolean stop = false;
		boolean extractSwitch = false;
		Vector<String> paths = new Vector<String>();
		long extractTime = 0;
		
		if ( !newInstance ) {
			
			if ( tryConnectToServer(args) ) {
				
				if ( args.length > 0 ) {
					
					logger.info("Send parameters to existing jD instance and exit");
					System.exit(0);
					
				} else {
					
					logger.info("There is already a running jD instance");
					tryConnectToServer(new String[]{"--minimized"});
					System.exit(0);
					
				}
	
			} else {
				
				for (String currentArg : args) {
					
					if (currentArg.equals("--show") || currentArg.equals("-s")) {
						
						JACController.showDialog(false);
						extractSwitch = false;
						stop = true;
						
					} else if (currentArg.equals("--train") || currentArg.equals("-t")) {
						
						JACController.showDialog(false);
						extractSwitch = false;
						stop = true;
						
					} else if (currentArg.equals("--extract") || currentArg.equals("-e")) {
						
						extractSwitch = true;
						stop = true;
						
					} else if (extractSwitch) {
						
						if (currentArg.equals("--rotate") || currentArg.equals("-r")) {
							
							extractTime = -1;
							
						} else if (extractTime == -1) {
							
							if (currentArg.matches("[\\d]+")) {
								extractTime = (int) Integer.parseInt(currentArg);
							} else extractTime = 0;
							
						} else if (!currentArg.matches("[\\s]*")) {
							
							paths.add(currentArg);
							
						}
						
					} else {
						
						extractSwitch = false;
						
					}
					
				}
				
				if (extractSwitch) {
					
					logger.info("Extract: ["+paths.toString()+" | "+extractTime+"]");
					Server.extract(paths, extractTime, true);
					
				}
				
				if ( !stop && Runtime.getRuntime().maxMemory() < 100000000 ) {
					JDUtilities.restartJD(args);
				}
				
				try {
	
					// listen for command line arguments from new jD instances //
					Server server = new Server();
					server.go();
	
					if (!stop) {
	
						Main main = new Main();
						main.go();
	
						// post start parameters //
						server.processParameters(args);
	
					}
					
				} catch (RemoteException e) {
					
					logger.severe("Server could not be started - ignore parameters");
					e.printStackTrace();
	
					if (!stop) {
	
						Main main = new Main();
						main.go();
	
					}
					
				}
				
			}
			
		}

	}

	@SuppressWarnings("unchecked")
	private void go() {

		JDInit init = new JDInit();
		logger.info("Register plugins");
		init.init();
		init.loadImages();
		
		JWindow window = new JWindow() {
			
			private static final long serialVersionUID = 1L;

			public void paint(Graphics g) {
				Image splashImage = JDUtilities.getImage("jd_logo_large");
				g.drawImage(splashImage, 0, 0, this);
			}
			
		};

		window.setSize(450, 100);
		window.setLocationRelativeTo(null);

		if ( JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME)
				.getBooleanProperty(SimpleGUI.PARAM_SHOW_SPLASH, true) ) {
			window.setVisible(true);
		}

		init.loadConfiguration();
		
		/*
		 * Übergangsfix. Die Interactions wurden in eine subconfig verlegt.
		 * dieser teil kopiert bestehende events in die neue configfile
		 */

		if ( JDUtilities.getConfiguration().getInteractions().size() > 0
				&& JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS)
						.getProperty(Configuration.PARAM_INTERACTIONS, null) == null ) {
			
			JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS)
					.setProperty(Configuration.PARAM_INTERACTIONS,
							JDUtilities.getConfiguration().getInteractions());
			JDUtilities.getConfiguration().setInteractions(
					new Vector<Interaction>());
			JDUtilities.saveConfig();
			
		}
		
		final JDController controller = init.initController();
		
		if (init.installerWasVisible()) {
			
			init.doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(
					Configuration.CID, -1), true);
			
		} else {
			
			init.initGUI(controller);
			init.initPlugins();
			init.loadDownloadQueue();
			init.loadModules();
			init.checkUpdate();

			if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
				init.doWebupdate(JDUtilities.getConfiguration()
						.getIntegerProperty(Configuration.CID, -1), false);
			}
			
		}
		
		controller.setInitStatus(JDController.INIT_STATUS_COMPLETE);
		// init.createQueueBackup();
		window.dispose();
		controller.getUiInterface().onJDInitComplete();
		Properties pr = System.getProperties();
		TreeSet propKeys = new TreeSet(pr.keySet());
		
		for (Iterator it = propKeys.iterator(); it.hasNext();) {
			String key = (String) it.next();
			logger.finer("" + key + "=" + pr.get(key));
		}
		
		logger.info("Revision: " + JDUtilities.getJDTitle());
		logger.info("Runtype: " + JDUtilities.getRunType());
		logger.info("Last author: " + JDUtilities.getLastChangeAuthor());
		logger.info("Application directory: " + JDUtilities.getCurrentWorkingDirectory(null));

		new PackageManager().interact(this);
		
		try {
			
			logger.info(Plugin.headRequest(new URL(
					"http://share.gulli.com/files/989404514/Indi.part11.rar.html"), null, null, false).getHtmlCode());
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// org.apache.log4j.Logger lg = org.apache.log4j.Logger.getLogger("jd");
		// BasicConfigurator.configure();
		// lg.error("hallo Welt");
		// lg.setLevel(org.apache.log4j.Level.ALL);
		
	}

	private static Boolean tryConnectToServer(String args[]) {

		String url = "//127.0.0.1/jDownloader";
		
		try {
			
			ServerInterface server = (ServerInterface) Naming.lookup(url);
			server.processParameters(args);
			return true;
			
		} catch (Exception ex) {
			
			return false;
			
		}

	}

}