//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import jd.Main;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class JDLightTray extends PluginOptional implements MouseListener, MouseMotionListener, WindowListener {

    private SubConfiguration subConfig = null;

    private static final String PROPERTY_START_MINIMIZED = "PROPERTY_START_MINIMIZED";

    private static final String PROPERTY_MINIMIZE_TO_TRAY = "PROPERTY_MINIMIZE_TO_TRAY";

    private static final String PROPERTY_SINGLE_CLICK = "PROPERTY_SINGLE_CLICK";

    private TrayIconPopup trayIconPopup;

    private TrayIcon trayIcon;

    private JFrame guiFrame;
    private boolean iconfied = false;
    private long lastDeIconifiedEvent = System.currentTimeMillis() - 1000;

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    public JDLightTray(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = JDUtilities.getSubConfig("ADDONS_JDLIGHTTRAY");
        initConfig();
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.JDLightTray.name", "JDLightTrayIcon");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.6+";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public boolean initAddon() {
        if (JDUtilities.getJavaVersion() < 1.6) {
            logger.severe("Error initializing SystemTray: Tray is supported since Java 1.6. your Version: " + JDUtilities.getJavaVersion());
            return false;
        }
        if (!SystemTray.isSupported()) {
            logger.severe("Error initializing SystemTray: Tray isn't supported jet");
            return false;
        }
        try {
            JDUtilities.getController().addControlListener(this);
            if (SimpleGUI.CURRENTGUI != null && SimpleGUI.CURRENTGUI.getFrame() != null) {
                guiFrame = SimpleGUI.CURRENTGUI.getFrame();
                guiFrame.addWindowListener(this);
            }
            logger.info("Systemtray OK");
            initGUI();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void initConfig() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_MINIMIZE_TO_TRAY, JDLocale.L("plugins.optional.JDLightTray.minimizetotray", "Minimize to tray")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_START_MINIMIZED, JDLocale.L("plugins.optional.JDLightTray.startMinimized", "Start minimized")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_SINGLE_CLICK, JDLocale.L("plugins.optional.JDLightTray.singleClick", "Toggle window status with single click")).setDefaultValue(false));
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            logger.info("JDLightTrayIcon Init complete");
            guiFrame = SimpleGUI.CURRENTGUI.getFrame();
            if (subConfig.getBooleanProperty(PROPERTY_START_MINIMIZED, false)) {
                guiFrame.setState(JFrame.ICONIFIED);
            }
            guiFrame.addWindowListener(this);
            return;
        }
        super.controlEvent(event);
    }

    private void initGUI() {
        trayIcon = new TrayIcon(JDImage.getImage(JDTheme.V("gui.images.jd_logo")));
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(this);
        trayIcon.addMouseListener(this);
        trayIcon.addMouseMotionListener(this);

        SystemTray systemTray = SystemTray.getSystemTray();
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.getSource() instanceof TrayIcon) {
            if (!OSDetector.isMac()) {
                if (e.getClickCount() >= (subConfig.getBooleanProperty(PROPERTY_SINGLE_CLICK, false) ? 1 : 2) && !SwingUtilities.isRightMouseButton(e)) {
                    iconfied = !iconfied;
                    miniIt();
                } else {
                    if (trayIconPopup != null && trayIconPopup.isShowing()) {
                        trayIconPopup.dispose();
                        trayIconPopup = null;
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        trayIconPopup = new TrayIconPopup();
                        calcLocation(trayIconPopup, e.getPoint());
                        trayIconPopup.setVisible(true);
                    }
                }
            } else {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() >= (subConfig.getBooleanProperty(PROPERTY_SINGLE_CLICK, false) ? 1 : 2) && !SwingUtilities.isLeftMouseButton(e)) {
                        iconfied = !iconfied;
                        miniIt();
                    } else {
                        if (trayIconPopup != null && trayIconPopup.isShowing()) {
                            trayIconPopup.dispose();
                            trayIconPopup = null;
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            trayIconPopup = new TrayIconPopup();
                            Point pointOnScreen = e.getLocationOnScreen();
                            if (e.getX() > 0) pointOnScreen.x -= e.getPoint().x;
                            calcLocation(trayIconPopup, pointOnScreen);
                            trayIconPopup.setVisible(true);
                        }
                    }
                }
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        if (trayIconPopup != null && trayIconPopup.isVisible()) return;
        trayIcon.setToolTip(createInfoString());
    }

    @Override
    public void onExit() {
        if (trayIcon != null) SystemTray.getSystemTray().remove(trayIcon);
        JDUtilities.getController().removeControlListener(this);
        if (guiFrame != null) guiFrame.removeWindowListener(this);
    }

    private void calcLocation(final JWindow window, final Point p) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int limitX = (int) screenSize.getWidth() / 2;
                int limitY = (int) screenSize.getHeight() / 2;
                if (!OSDetector.isMac()) {
                    if (p.x <= limitX) {
                        if (p.y <= limitY) {
                            // top left
                            window.setLocation(p.x, p.y);
                        } else {
                            // bottom left
                            window.setLocation(p.x, p.y - window.getHeight());
                        }
                    } else {
                        if (p.y <= limitY) {
                            // top right
                            window.setLocation(p.x - window.getWidth(), p.y);
                        } else {
                            // bottom right
                            window.setLocation(p.x - window.getWidth(), p.y - window.getHeight());
                        }
                    }
                } else {
                    if (p.getX() <= (screenSize.getWidth() - window.getWidth())) {
                        window.setLocation((int) p.getX(), 22);
                    } else {
                        window.setLocation(p.x - window.getWidth(), 22);
                    }
                }
            }
        });
    }

    private void miniIt() {
        if (System.currentTimeMillis() > this.lastDeIconifiedEvent + 750) {
            this.lastDeIconifiedEvent = System.currentTimeMillis();
            if (guiFrame.isVisible()) {
                guiFrame.setVisible(false);
            } else {
                if (OSDetector.isGnome() && iconfied) {
                    guiFrame.setState(JFrame.NORMAL);
                    guiFrame.setVisible(true);
                    guiFrame.setState(JFrame.ICONIFIED);
                    guiFrame.setVisible(false);
                    guiFrame.setState(JFrame.NORMAL);
                    guiFrame.setVisible(true);
                } else {
                    guiFrame.setState(JFrame.NORMAL);
                    guiFrame.setVisible(true);
                }
                iconfied = false;
            }

        }

    }

    private String createInfoString() {
        StringBuilder creater = new StringBuilder();
        creater.append(JDUtilities.getJDTitle() + "\n");
        int downloads = JDUtilities.getController().getRunningDownloadNum();
        if (downloads == 0) {
            creater.append(JDLocale.L("plugins.optional.trayIcon.nodownload", "No Download in progress"));
        } else {
            creater.append(JDLocale.L("plugins.optional.trayIcon.downloads", "Downloads:") + " " + downloads + " @ " + JDUtilities.formatKbReadable(JDUtilities.getController().getSpeedMeter() / 1024) + "/s");
        }
        return creater.toString();
    }

    public void windowActivated(WindowEvent arg0) {
    }

    public void windowClosed(WindowEvent arg0) {
    }

    public void windowClosing(WindowEvent arg0) {
    }

    public void windowDeactivated(WindowEvent arg0) {
    }

    public void windowDeiconified(WindowEvent arg0) {
        windowIconified(arg0);
    }

    public void windowIconified(WindowEvent arg0) {
        if (subConfig.getBooleanProperty(PROPERTY_MINIMIZE_TO_TRAY, true)) {
            miniIt();
        }
    }

    public void windowOpened(WindowEvent arg0) {
    }
}