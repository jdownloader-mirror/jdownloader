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
import jd.config.SubConfiguration;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.PackageManager;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.gui.skins.simple.AgbDialog;
import jd.gui.skins.simple.Balloon;
import jd.gui.skins.simple.GuiRunnable;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
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

    private long startTime;

    /**
     * Erstellt einen Thread zum Start des Downloadvorganges
     * 
     * @param controller
     *            Controller
     * @param dlink
     *            Link, der heruntergeladen werden soll
     */
    public SingleDownloadController(DownloadLink dlink) {
        super("JD-StartDownloads");
        downloadLink = dlink;
        linkStatus = downloadLink.getLinkStatus();
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
            this.startTime = System.currentTimeMillis();
            linkStatus.setStatusText(JDLocale.L("gui.download.create_connection", "Connecting..."));
            System.out.println("PreDupeChecked: no mirror found!");
            fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, currentPlugin);
            DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
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
                logger.finest("Hoster Plugin Version: " + downloadLink.getPlugin().getVersion());
                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));
            } catch (NullPointerException e) {
                logger.finest("Hoster Plugin Version: " + downloadLink.getPlugin().getVersion());
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));
            } catch (Exception e) {
                logger.finest("Hoster Plugin Version: " + downloadLink.getPlugin().getVersion());
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.error", "Error: ") + JDUtilities.convertExceptionReadable(e));
            }

            if (isAborted()) {
                logger.finest("Thread aborted");
                linkStatus.setStatus(LinkStatus.TODO);
                return;
            }
            if (linkStatus.isFailed()) {
                logger.warning("\r\nError occured- " + downloadLink.getLinkStatus());
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
                Balloon.showIfHidden(JDLocale.L("ballon.download.error.title", "Error"), JDTheme.II("gui.images.bad", 32, 32), JDLocale.LF("ballon.download.fnf.message", "<b>%s<b><hr>File not found", downloadLink.getName() + " (" + Formatter.formatReadable(downloadLink.getDownloadSize()) + ")"));

                onErrorFileNotFound(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_LINK_IN_PROGRESS:
                onErrorLinkBlock(downloadLink, currentPlugin);
                break;
            case LinkStatus.ERROR_FATAL:
                Balloon.showIfHidden(JDLocale.L("ballon.download.error.title", "Error"), JDTheme.II("gui.images.bad", 32, 32), JDLocale.LF("ballon.download.fatalerror.message", "<b>%s<b><hr>Fatal Plugin Error", downloadLink.getHost()));

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
                Balloon.showIfHidden(JDLocale.L("ballon.download.error.title", "Error"), JDTheme.II("gui.images.bad", 32, 32), JDLocale.LF("ballon.download.failed.message", "<b>%s<b><hr>failed", downloadLink.getName() + " (" + Formatter.formatReadable(downloadLink.getDownloadSize()) + ")"));

                break;

            case LinkStatus.ERROR_PLUGIN_DEFEKT:
                onErrorPluginDefect(downloadLink, currentPlugin);
                Balloon.showIfHidden(JDLocale.L("ballon.download.error.title", "Error"), JDTheme.II("gui.images.bad", 32, 32), JDLocale.LF("ballon.download.plugindefect.message", "<b>%s<b><hr>Plugin defect", downloadLink.getHost()));

                break;
            case LinkStatus.ERROR_NO_CONNECTION:
            case LinkStatus.ERROR_TIMEOUT_REACHED:
                Balloon.showIfHidden(JDLocale.L("ballon.download.error.title", "Error"), JDTheme.II("gui.images.bad", 32, 32), JDLocale.LF("ballon.download.connectionlost.message", "<b>%s<b><hr>Connection lost", downloadLink.getHost()));

                onErrorNoConnection(downloadLink, currentPlugin);
                break;

            default:
                if (linkStatus.hasStatus(LinkStatus.FINISHED)) {
                    logger.finest("\r\nFinished- " + downloadLink.getLinkStatus());
                    logger.info("\r\nFinished- " + downloadLink.getFileOutput());
                    onDownloadFinishedSuccessFull(downloadLink);
                } else {
                    retry(downloadLink, currentPlugin);
                }
            }

        } catch (Exception e) {
            logger.severe("Error in Plugin Version: " + downloadLink.getPlugin().getVersion());
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
        }
    }

    private void onErrorLinkBlock(DownloadLink downloadLink, PluginForHost currentPlugin) {
        LinkStatus status = downloadLink.getLinkStatus();
        status.resetWaitTime();
        downloadLink.setEnabled(false);
    }

    private void onErrorPluginDefect(DownloadLink downloadLink2, PluginForHost currentPlugin2) {
        logger.warning("The Plugin for " + currentPlugin.getHost() + " seems to be out of date(rev" + downloadLink.getPlugin().getVersion() + "). Please inform the Support-team http://jdownloader.org/support.");
        logger.warning(downloadLink2.getLinkStatus().getErrorMessage());
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
        if ((System.currentTimeMillis() - startTime) > 30000) Balloon.showIfHidden(JDLocale.L("ballon.download.successfull.title", "Download"), JDTheme.II("gui.images.ok", 32, 32), JDLocale.LF("ballon.download.successfull.message", "<b>%s<b><hr>finished successfully", downloadLink.getName() + " (" + Formatter.formatReadable(downloadLink.getDownloadSize()) + ")"));
        downloadLink.setProperty(DownloadLink.STATIC_OUTPUTFILE, downloadLink.getFileOutput());
        if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_JDU) {
            new PackageManager().onDownloadedPackage(downloadLink);
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
        Interaction.handleInteraction(Interaction.INTERACTION_SINGLE_DOWNLOAD_FINISHED, downloadLink);
        if (JDUtilities.getController().isContainerFile(new File(downloadLink.getFileOutput()))) {
            Interaction.handleInteraction(Interaction.INTERACTION_CONTAINER_DOWNLOAD, downloadLink);
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_RELOADCONTAINER, true)) {
                JDUtilities.getController().loadContainerFile(new File(downloadLink.getFileOutput()));
            }
        }

    }

    private void onErrorAGBNotSigned(DownloadLink downloadLink2, PluginForHost plugin) throws InterruptedException {
        downloadLink2.getLinkStatus().setStatusText(JDLocale.L("controller.status.agb_tos", "TOS haven't been accepted."));
        if (!plugin.isAGBChecked()) {
            synchronized (JDUtilities.userio_lock) {
                if (!plugin.isAGBChecked()) {
                    showAGBDialog(downloadLink2);
                } else {
                    downloadLink2.getLinkStatus().reset();
                }
            }
        } else {
            downloadLink2.getLinkStatus().reset();
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
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
                DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
                return;
            }
        } else {
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_FATAL);
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
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
                return;
            }
            retry(downloadLink, plugin);
        }
    }

    private void onErrorFatal(DownloadLink downloadLink, PluginForHost currentPlugin) {
        downloadLink.requestGuiUpdate();
    }

    private void onErrorFileExists(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus status = downloadLink.getLinkStatus();
        if (SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_FILE_EXISTS) == 1) {
            status.setErrorMessage(JDLocale.L("controller.status.fileexists.skip", "File already exists."));
            downloadLink.setEnabled(false);
        } else {
            if (new File(downloadLink.getFileOutput()).delete()) {
                status.reset();
            } else {
                status.addStatus(LinkStatus.ERROR_FATAL);
                status.setErrorMessage(JDLocale.L("controller.status.fileexists.overwritefailed", "Überschreiben fehlgeschlagen ") + downloadLink.getFileOutput());
            }
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
    }

    /**
     * Wird aufgerufenw ennd as Plugin einen filenot found Fehler meldet
     * 
     * @param downloadLink
     * @param plugin
     * @param step
     */
    private void onErrorFileNotFound(DownloadLink downloadLink, PluginForHost plugin) {
        logger.severe("File not found :" + downloadLink.getDownloadURL());
        downloadLink.setEnabled(false);
    }

    private void onErrorIncomplete(DownloadLink downloadLink, PluginForHost plugin) {
        retry(downloadLink, plugin);
    }

    private void onErrorNoConnection(DownloadLink downloadLink, PluginForHost plugin) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        logger.severe("Error occurred: No Server connection");
        long milliSeconds = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(WAIT_TIME_ON_CONNECTION_LOSS, 5 * 60) * 1000;
        linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        linkStatus.setWaitTime(milliSeconds);
        if (linkStatus.getErrorMessage() == null) {
            linkStatus.setErrorMessage(JDLocale.L("controller.status.connectionproblems", "Connection lost."));
        }
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
        logger.warning("Error occurred: Temporarily unavailably: PLease wait " + downloadLink.getLinkStatus().getValue() + " ms for a retry");
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
            downloadLink.setEnabled(false);
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
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
        LinkStatus status = downloadLink.getLinkStatus();
        long milliSeconds = downloadLink.getLinkStatus().getValue();

        if (milliSeconds <= 0) {
            logger.severe(JDLocale.L("plugins.errors.pluginerror", "Plugin error. Please inform Support"));
            milliSeconds = 3600000l;
        }
        status.setWaitTime(milliSeconds);
        plugin.setHosterWaittime(milliSeconds);
        Reconnecter.requestReconnect();
        DownloadController.getInstance().fireDownloadLinkUpdate(downloadLink);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    // @Override
    public void run() {
        /**
         * Das Plugin, das den aktuellen Download steuert
         */
        PluginForHost plugin;
        linkStatus.setStatusText(null);
        linkStatus.setErrorMessage(null);
        linkStatus.resetWaitTime();
        logger.info("Start working on " + downloadLink.getName());
        currentPlugin = plugin = (PluginForHost) downloadLink.getPlugin();
        fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_ACTIVE, this));
        if (downloadLink.getDownloadURL() == null) {
            downloadLink.getLinkStatus().setStatusText(JDLocale.L("controller.status.containererror", "Container Error"));
            downloadLink.getLinkStatus().setErrorMessage(JDLocale.L("controller.status.containererror", "Container Error"));
            downloadLink.setEnabled(false);
            fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_INACTIVE, this));
            Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_FAILED, this);
            linkStatus.setInProgress(false);
            return;
        }
        /* check ob Datei existiert oder bereits geladen wird */
        if (DownloadInterface.preDownloadCheckFailed(downloadLink)) {
            onErrorLinkBlock(downloadLink, currentPlugin);
            fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_INACTIVE, this));
            linkStatus.setInProgress(false);
            return;
        }
        handlePlugin();
        linkStatus.setInProgress(false);
        fireControlEvent(new ControlEvent(currentPlugin, ControlEvent.CONTROL_PLUGIN_INACTIVE, this));
        plugin.clean();
        downloadLink.requestGuiUpdate();
    }

}