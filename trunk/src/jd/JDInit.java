//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.JSonWrapper;
import jd.event.ControlEvent;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.events.EDTEventQueue;
import jd.gui.swing.laf.LookAndFeelController;
import jd.http.Browser;
import jd.http.ext.security.JSPermissionRestricter;
import jd.nutils.OSDetector;
import jd.nutils.encoding.Encoding;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

import org.appwork.storage.config.JsonConfig;
import org.appwork.update.updateclient.UpdaterConstants;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;
import org.lobobrowser.util.OS;

/**
 * @author JD-Team
 */

public class JDInit {
    static {
        try {
            JSPermissionRestricter.init();
        } catch (final Throwable e) {
            e.printStackTrace();
        }

    }

    private static final boolean TEST_INSTALLER = false;

    private static final Logger  LOG            = JDLogger.getLogger();

    public JDInit() {
    }

    public void checkUpdate() {

        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
            final String old = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION, "");
            if (!old.equals(JDUtilities.getRevision())) {
                JDInit.LOG.info("Detected that JD just got updated");

                final ConfirmDialog dialog = new ConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, _JDT._.system_update_message_title(JDUtilities.getRevision()), _JDT._.system_update_message(), null, null, null);
                dialog.setLeftActions(new AbstractAction(_JDT._.system_update_showchangelogv2()) {

                    private static final long serialVersionUID = 1L;

                    public void actionPerformed(final ActionEvent e) {
                        try {
                            OS.launchBrowser("http://jdownloader.org/changes/index");
                        } catch (final IOException e1) {
                            e1.printStackTrace();
                        }
                    }

                });
                try {
                    Dialog.getInstance().showDialog(dialog);
                } catch (DialogClosedException e1) {

                } catch (DialogCanceledException e1) {

                }
            }
        }
        this.submitVersion();
    }

    public void init() {
        this.initBrowser();

    }

    public void initBrowser() {
        Browser.setGlobalLogger(JDLogger.getLogger());

        /* init default global Timeouts */
        Browser.setGlobalReadTimeout(JsonConfig.create(GeneralSettings.class).getHttpReadTimeout());
        Browser.setGlobalConnectTimeout(JsonConfig.create(GeneralSettings.class).getHttpConnectTimeout());

        if (JSonWrapper.get("DOWNLOAD").getBooleanProperty(UpdaterConstants.USE_PROXY, false)) {
            final String host = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.PROXY_HOST, "");
            final int port = JSonWrapper.get("DOWNLOAD").getIntegerProperty(UpdaterConstants.PROXY_PORT, 8080);
            final String user = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.PROXY_USER, "");
            final String pass = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.PROXY_PASS, "");
            if ("".equals(host.trim())) {
                JDInit.LOG.warning("Proxy disabled. No host");
                JSonWrapper.get("DOWNLOAD").setProperty(UpdaterConstants.USE_PROXY, false);
                return;
            }

            final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.HTTP, host, port);
            if (user != null && user.trim().length() > 0) {
                pr.setUser(user);
            }
            if (pass != null && pass.trim().length() > 0) {
                pr.setPass(pass);
            }
            Browser.setGlobalProxy(pr);
        }
        if (JSonWrapper.get("DOWNLOAD").getBooleanProperty(UpdaterConstants.USE_SOCKS, false)) {
            final String user = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.PROXY_USER_SOCKS, "");
            final String pass = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.PROXY_PASS_SOCKS, "");
            final String host = JSonWrapper.get("DOWNLOAD").getStringProperty(UpdaterConstants.SOCKS_HOST, "");
            final int port = JSonWrapper.get("DOWNLOAD").getIntegerProperty(UpdaterConstants.SOCKS_PORT, 1080);
            if ("".equals(host.trim())) {
                JDInit.LOG.warning("Socks Proxy disabled. No host");
                JSonWrapper.get("DOWNLOAD").setProperty(UpdaterConstants.USE_SOCKS, false);
                return;
            }

            final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.SOCKS5, host, port);
            if (user != null && user.trim().length() > 0) {
                pr.setUser(user);
            }
            if (pass != null && pass.trim().length() > 0) {
                pr.setPass(pass);
            }
            Browser.setGlobalProxy(pr);
        }

    }

    public void initControllers() {

    }

    public void initGUI(final JDController controller) {

        EDTEventQueue.initEventQueue();
        ActionController.initActions();
        SwingGui.setInstance(JDGui.getInstance());
        UserIF.setInstance(SwingGui.getInstance());
        controller.addControlListener(SwingGui.getInstance());
        Log.L.info("GUIDONE->" + (System.currentTimeMillis() - Main.startup));
        controller.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_GUI_COMPLETE, null));
    }

    public void initPlugins() {

    }

    public Configuration loadConfiguration() {
        final Object obj = JDUtilities.getDatabaseConnector().getData(Configuration.NAME);

        if (obj == null) {
            JDInit.LOG.finest("Fresh install?");
        }
        try {
            if (!JDInit.TEST_INSTALLER && obj != null && (((Configuration) obj).getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) != null || JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder() != null)) {
                final Configuration configuration = (Configuration) obj;
                JDUtilities.setConfiguration(configuration);
                JDInit.LOG.setLevel(configuration.getGenericProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));

            } else {
                final File cfg = JDUtilities.getResourceFile("config");
                if (!cfg.exists()) {
                    if (!cfg.mkdirs()) {
                        System.err.println("Could not create configdir");
                        return null;
                    }
                    if (!cfg.canWrite()) {
                        System.err.println("Cannot write to configdir");
                        return null;
                    }
                }
                final Configuration configuration = new Configuration();
                JDUtilities.setConfiguration(configuration);
                JDInit.LOG.setLevel(configuration.getGenericProperty(Configuration.PARAM_LOGGER_LEVEL, Level.WARNING));

                JDUtilities.getDatabaseConnector().saveConfiguration(Configuration.NAME, JDUtilities.getConfiguration());

                LookAndFeelController.getInstance().setUIManager();
                final Installer inst = new Installer();

                if (!inst.isAborted()) {
                    final File home = JDUtilities.getResourceFile(".");
                    if (!home.canWrite()) {
                        JDInit.LOG.severe("INSTALL abgebrochen");
                        UserIO.getInstance().requestMessageDialog(_JDT._.installer_error_noWriteRights());
                        JDIO.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                        System.exit(1);
                    }
                } else {
                    JDInit.LOG.severe("INSTALL abgebrochen2");
                    UserIO.getInstance().requestMessageDialog(_JDT._.installer_abortInstallation());
                    JDIO.removeDirectoryOrFile(JDUtilities.getResourceFile("config"));
                    System.exit(0);
                }
            }
            return JDUtilities.getConfiguration();
        } finally {

            convert();
        }
    }

    private void convert() {
        String ddl = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, null);
        if (JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder() == null) {
            JsonConfig.create(GeneralSettings.class).setDefaultDownloadFolder(ddl);
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, Property.NULL);
        }
        JDUtilities.getConfiguration().save();
    }

    private void submitVersion() {
        new Thread(new Runnable() {
            public void run() {
                if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
                    String os = "unk";
                    if (OSDetector.isLinux()) {
                        os = "lin";
                    } else if (OSDetector.isMac()) {
                        os = "mac";
                    } else if (OSDetector.isWindows()) {
                        os = "win";
                    }
                    String tz = System.getProperty("user.timezone");
                    if (tz == null) {
                        tz = "unknown";
                    }
                    final Browser br = new Browser();
                    br.setConnectTimeout(15000);
                    if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION, "").equals(JDUtilities.getRevision())) {
                        try {
                            final String prev = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_UPDATE_VERSION, "");
                            br.postPage("http://service.jdownloader.org/tools/s.php", "v=" + JDUtilities.getRevision().replaceAll(",|\\.", "") + "&p=" + prev + "&os=" + os + "&tz=" + Encoding.urlEncode(tz));
                            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_UPDATE_VERSION, JDUtilities.getRevision());
                            JDUtilities.getConfiguration().save();
                        } catch (final Exception e) {
                        }
                    }
                }
            }
        }).start();
    }

}