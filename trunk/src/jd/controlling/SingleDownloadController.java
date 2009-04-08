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

package jd.controlling;

import java.io.File;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.gui.skins.simple.AgbDialog;
import jd.gui.skins.simple.GuiRunnable;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * In dieser Klasse wird der Download parallel zum Hauptthread gestartet
 * 
 * @author astaldo/JD-Team
 */
public class SingleDownloadController extends Thread {
    public static final String WAIT_TIME_ON_CONNECTION_LOSS = "WAIT_TIME_ON_CONNECTION_LOSS";

    private boolean aborted;

    /**
     * Das übergeordnete Fenster
     */
    private JDController controller;

    /**
     * Das Plugin, das den aktuellen Download steuert
     */
    private PluginForHost currentPlugin;

    private DownloadLink downloadLink;

    private LinkStatus linkStatus;

    /**
     * Wurde der Download abgebrochen?
     */
    // private boolean aborted = false;
    /**
     * Der Logger
     */
    private Logger logger = jd.controlling.JDLogger.getLogger();

    /**
     * Erstellt einen Thread zum Start des Downloadvorganges
     * 
     * @param controller
     *            Controller
     * @param dlink
     *            Link, der heruntergeladen werden soll
     */
    public SingleDownloadController(JDController controller, DownloadLink dlink) {
        super("JD-StartDownloads");
        downloadLink = dlink;
        linkStatus = downloadLink.getLinkStatus();
        this.controller = controller;
        setPriority(Thread.MIN_PRIORITY);

        downloadLink.setDownloadLinkController(this);
    }

    /**
     * Bricht den Downloadvorgang ab.
     */
    public SingleDownloadController abortDownload() {
        aborted = true;
        interrupt();

        return this;
    }

    private void fireControlEvent(ControlEvent controlEvent) {
        JDUtilities.getController().fireControlEvent(controlEvent);

    }

    private void fireControlEvent(int controlID, Object param) {
        JDUtilities.getController().fireControlEvent(controlID, param);

    }

    public PluginForHost getCurrentPlugin() {
        return currentPlugin;
    }

    public DownloadLink getDownloadLink() {
        return downloadLink;
    }

    private void handlePlugin() {
        try {
            if (downloadLink.getDownloadURL() == null) {

                downloadLink.getLinkStatus().setStatusText(JDLocale.L("controller.status.containererror", "Container Error"));
                downloadLink.getLinkStatus().setErrorMessage(JDLocale.L("controller.status.containererror", "Container Error"));

                downloadLink.setEnabled(false);
                // linkStatus.addStatus(LinkStatus.ERROR_SECURITY);
                DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);
                Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_FAILED, this);

                return;

            }

            linkStatus.setStatusText(JDLocale.L("gui.download.create_connection", "Connecting..."));

            fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, currentPlugin);
            DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);
            currentPlugin.init();
            try {
                currentPlugin.handle(downloadLink);
            } catch (UnknownHostException e) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.nointernetconn", "No Internet connection?"));
                linkStatus.setValue(5 * 60 * 1000l);
            } catch (SocketTimeoutException e) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.hosteroffline", "Hoster offline?"));
                linkStatus.setValue(10 * 60 * 1000l);
            } catch (SocketException e) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.disconnect", "Disconnect?"));
                linkStatus.setValue(5 * 60 * 1000l);
            } catch (InterruptedException e) {
                logger.severe("Hoster Plugin Version: " + downloadLink.getPlugin().getVersion());

                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));
            } catch (NullPointerException e) {
                logger.severe("Hoster Plugin Version: " + downloadLink.getPlugin().getVersion());
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));
            } catch (Exception e) {
                logger.severe("Hoster Plugin Version: " + downloadLink.getPlugin().getVersion());
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));
            }

            if (isAborted()) {

                logger.warning("Thread aborted");
                linkStatus.setStatus(LinkStatus.TODO);
                return;
            }
            if (linkStatus.isFailed()) {
                logger.severe("\r\nError occured- " + downloadLink.getLinkStatus());
            }
            if (aborted) {
                linkStatus.setErrorMessage(null);
            }
            switch (linkStatus.getLatestStatus()) {
            case LinkStatus.ERROR_LOCAL_IO:
                onErrorLocalIO(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_IP_BLOCKED:
                onErrorWaittime(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
                onErrorTemporarilyUnavailable(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_AGB_NOT_SIGNED:
                onErrorAGBNotSigned(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_FILE_NOT_FOUND:
                onErrorFileNotFound(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_LINK_IN_PROGRESS:
                onErrorLinkBlock(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_FATAL:
                onErrorFatal(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_CAPTCHA:
                onErrorCaptcha(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_PREMIUM:
                onErrorPremium(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
                onErrorIncomplete(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_ALREADYEXISTS:
                onErrorFileExists(downloadLink, currentPlugin);
                break;

            case LinkStatus.ERROR_DOWNLOAD_FAILED:
                onErrorChunkloadFailed(downloadLink, currentPlugin);
                break;

            case LinkStatus.ERROR_PLUGIN_DEFEKT:
                onErrorPluginDefect(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_NO_CONNECTION:
            case LinkStatus.ERROR_TIMEOUT_REACHED:
                onErrorNoConnection(downloadLink, currentPlugin);
                break;

            default:
                if (linkStatus.hasStatus(LinkStatus.FINISHED)) {
                    logger.severe("\r\nFinished- " + downloadLink.getLinkStatus());
                    onDownloadFinishedSuccessFull(downloadLink);
                } else {
                    retry(downloadLink, currentPlugin);
                }
            }

        } catch (Exception e) {
            logger.severe("Hoster Plugin Version: " + downloadLink.getPlugin().getVersion());
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
        }
    }

    private void onErrorLinkBlock(DownloadLink downloadLink, PluginForHost currentPlugin) {

        LinkStatus status = downloadLink.getLinkStatus();

        status.resetWaitTime();

        downloadLink.setEnabled(false);
        DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);
    }

    private void onErrorPluginDefect(DownloadLink downloadLink2, PluginForHost currentPlugin2) {
        logger.severe(" The PLugin for " + currentPlugin.getHost() + " seems to be out of date. Please inform the Support-team http://jdownloader.org/support.");
        logger.severe(downloadLink2.getLinkStatus().getErrorMessage());
        // Dieser Exception deutet meistens auf einen PLuginfehler hin. Deshalb
        // wird in diesem Fall die zuletzt geladene browserseite aufgerufen.

        logger.finest(currentPlugin2.getBrowser() + "");
        downloadLink2.getLinkStatus().addStatus(LinkStatus.ERROR_FATAL);
        downloadLink2.getLinkStatus().setErrorMessage(JDLocale.L("controller.status.pluindefekt", "Plugin out of date"));

        downloadLink.requestGuiUpdate();

    }

    public boolean isAborted() {
        return aborted;
    }

    private void onDownloadFinishedSuccessFull(DownloadLink downloadLink) {
        downloadLink.setProperty(DownloadLink.STATIC_OUTPUTFILE, downloadLink.getFileOutput());
        if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_JDU) {
            new PackageManager().onDownloadedPackage(downloadLink);
        }
        DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);

        Interaction.handleInteraction(Interaction.INTERACTION_SINGLE_DOWNLOAD_FINISHED, downloadLink);
        if (JDUtilities.getController().isContainerFile(new File(downloadLink.getFileOutput()))) {
            Interaction.handleInteraction(Interaction.INTERACTION_CONTAINER_DOWNLOAD, downloadLink);
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RELOADCONTAINER, true)) {
                controller.loadContainerFile(new File(downloadLink.getFileOutput()));
            }
        }

    }

    private void onErrorAGBNotSigned(DownloadLink downloadLink2, PluginForHost plugin) throws InterruptedException {

        downloadLink2.getLinkStatus().setStatusText(JDLocale.L("controller.status.agb_tos", "TOS haven't been accepted."));

        JDUtilities.acquireUserIOSemaphore();
        if (!plugin.isAGBChecked()) {
            showAGBDialog(downloadLink2);

        } else {
            downloadLink2.getLinkStatus().reset();
        }
        JDUtilities.releaseUserIOSemaphore();

        DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);

    }

    /**
     * blockiert EDT sicher bis der Dialog bestätigt wurde
     * 
     * @param downloadLink2
     */
    private void showAGBDialog(final DownloadLink downloadLink2) {
        new GuiRunnable<Object>() {

            public Object runSave() {
                new AgbDialog(downloadLink2, 30);
                return null;

            }
        }.waitForEDT();

    }

    /**
     * Diese Funktion wird aufgerufen wenn ein Download wegen eines
     * captchafehlersabgebrochen wird
     * 
     * @param downloadLink
     * @param plugin2
     * @param step
     */
    private void onErrorCaptcha(DownloadLink downloadLink, PluginForHost plugin) {
        retry(downloadLink, plugin);
    }

    private void retry(DownloadLink downloadLink, PluginForHost plugin) {
        int r;

        if (downloadLink.getLinkStatus().getValue() > 0) {

            downloadLink.getLinkStatus().setStatusText(null);
        }
        if ((r = downloadLink.getLinkStatus().getRetryCount()) <= plugin.getMaxRetries()) {
            downloadLink.getLinkStatus().reset();
            downloadLink.getLinkStatus().setRetryCount(r + 1);
            downloadLink.getLinkStatus().setErrorMessage(null);
            try {
                plugin.sleep(Math.max((int) downloadLink.getLinkStatus().getValue(), 2000), downloadLink);

            } catch (PluginException e) {
                downloadLink.getLinkStatus().setStatusText(null);
                return;
            }

        } else {

            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_FATAL);

        }

    }

    private void onErrorChunkloadFailed(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        if (linkStatus.getErrorMessage() == null) {
            linkStatus.setErrorMessage(JDLocale.L("plugins.error.downloadfailed", "Download failed"));
        }
        if (linkStatus.getValue() != LinkStatus.VALUE_FAILED_HASH) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
            }
            retry(downloadLink, plugin);
        }
    }

    private void onErrorFatal(DownloadLink downloadLink, PluginForHost currentPlugin) {

        downloadLink.requestGuiUpdate();
    }

    private void onErrorFileExists(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus status = downloadLink.getLinkStatus();

        if (JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_FILE_EXISTS) == 1) {
            downloadLink.setEnabled(false);
            status.setErrorMessage(JDLocale.L("controller.status.fileexists.skip", "File already exists."));
            DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);
        } else {
            if (new File(downloadLink.getFileOutput()).delete()) {
                status.reset();
            } else {
                status.addStatus(LinkStatus.ERROR_FATAL);
                status.setErrorMessage(JDLocale.L("controller.status.fileexists.overwritefailed", "Überschreiben fehlgeschlagen ") + downloadLink.getFileOutput());
            }
        }
        DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);

    }

    /**
     * Wird aufgerufenw ennd as Plugin einen filenot found Fehler meldet
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorFileNotFound(DownloadLink downloadLink, PluginForHost plugin) {
        downloadLink.setDupecheckAllowed(false);
        logger.severe("File not found :" + downloadLink);
    }

    private void onErrorIncomplete(DownloadLink downloadLink, PluginForHost plugin) {

        retry(downloadLink, plugin);

    }

    private void onErrorNoConnection(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        logger.severe("Error occurred: No Server connection");
        long milliSeconds = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(WAIT_TIME_ON_CONNECTION_LOSS, 5 * 60) * 1000;
        linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        linkStatus.setWaitTime(milliSeconds);

        downloadLink.setEnabled(false);
        if (linkStatus.getErrorMessage() == null) {
            linkStatus.setErrorMessage(JDLocale.L("controller.status.connectionproblems", "Connection lost."));
        }
        DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);

    }

    /**
     * Fehlerfunktion für einen UNbekannten premiumfehler.
     * Plugin-premium-support wird deaktiviert und link wird erneut versucht
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorPremium(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        linkStatus.reset();
    }

    private void onErrorLocalIO(DownloadLink downloadLink, PluginForHost plugin) {

        LinkStatus status = downloadLink.getLinkStatus();
        /*
         * Value<=0 bedeutet das der link dauerhauft deaktiviert bleiben soll.
         * value>0 gibt die zeit an die der link deaktiviert bleiben muss in ms.
         * Der DownloadWatchdoggibt den Link wieder frei ewnn es zeit ist.
         */

        status.setWaitTime(30 * 60 * 1000l);

        downloadLink.setEnabled(false);
        DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);
    }

    /**
     * Wird aufgerufen wenn ein Link kurzzeitig nicht verfügbar ist. ER wird
     * deaktiviert und kann zu einem späteren zeitpunkt wieder aktiviert werden
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorTemporarilyUnavailable(DownloadLink downloadLink, PluginForHost plugin) {
        logger.severe("Error occurred: Temporarily unavailably: " + downloadLink.getLinkStatus().getValue() + " ms");

        LinkStatus status = downloadLink.getLinkStatus();
        if (status.getErrorMessage() == null) status.setErrorMessage(JDLocale.L("controller.status.tempUnavailable", "kurzzeitig nicht verfügbar"));

        /*
         * Value<=0 bedeutet das der link dauerhauft deaktiviert bleiben soll.
         * value>0 gibt die zeit an die der link deaktiviert bleiben muss in ms.
         * Der DownloadWatchdoggibt den Link wieder frei ewnn es zeit ist.
         */
        if (status.getValue() > 0) {
            status.setWaitTime(status.getValue());
        } else {
            status.resetWaitTime();
        }

        downloadLink.setEnabled(false);
        DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);
    }

    /**
     * Diese Funktion wird aufgerufen wenn Ein Download mit einem Waittimefehler
     * abgebrochen wird
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorWaittime(DownloadLink downloadLink, PluginForHost plugin) {
        logger.finer("Error occurred: Wait Time ");
        LinkStatus status = downloadLink.getLinkStatus();
        long milliSeconds = downloadLink.getLinkStatus().getValue();

        if (milliSeconds <= 0) {
            logger.severe("Es wurde vom PLugin keine Wartezeit übergeben");
            logger.severe("Nutze Default-Wartezeit: 60 mins");
            logger.severe(JDLocale.L("plugins.errors.pluginerror", "Plugin error. Please inform Support"));
            milliSeconds = 3600000l;
        }
        status.setWaitTime(milliSeconds);
        plugin.setHosterWaittime(milliSeconds);

        Reconnecter.requestReconnect();

        DownloadController.getDownloadController().fireRefresh_Specific(downloadLink);

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        /**
         * Das Plugin, das den aktuellen Download steuert
         */
        PluginForHost plugin;
        linkStatus.setStatusText(null);
        linkStatus.setErrorMessage(null);
        linkStatus.resetWaitTime();
        logger.info("working on " + downloadLink.getName());
        currentPlugin = plugin = (PluginForHost) downloadLink.getPlugin();
        fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_ACTIVE, this));
        linkStatus.setInProgress(true);
        // plugin.resetPlugin();
        handlePlugin();
        linkStatus.setInProgress(false);

        fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_INACTIVE, this));
        plugin.clean();
        downloadLink.requestGuiUpdate();
    }

}