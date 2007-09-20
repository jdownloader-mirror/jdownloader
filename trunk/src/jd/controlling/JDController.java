package jd.controlling;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.JDUtilities;
import jd.config.Configuration;
import jd.controlling.interaction.HTTPReconnect;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.WebUpdate;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;

/**
 * Im Controller wird das ganze App gesteuert. Evebnts werden deligiert.
 * 
 * @author coalado/astaldo
 * 
 */
public class JDController implements PluginListener, ControlListener, UIListener {

    /**
     * Mit diesem Thread wird eingegebener Text auf Links untersucht
     */
    private DistributeData                    distributeData  = null;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #firePluginEvent(PluginEvent)} ein Event losgeschickt wird.
     */
    private transient Vector<ControlListener> controlListener = null;

    /**
     * Die Konfiguration
     */
    protected Configuration                   config          = JDUtilities.getConfiguration();

    /**
     * Schnittstelle zur Benutzeroberfläche
     */
    private UIInterface                       uiInterface;

    /**
     * Die DownloadLinks
     */
    private Vector<DownloadLink>              downloadLinks;

    /**
     * Der Logger
     */
    private Logger                            logger          = Plugin.getLogger();

    private File                              lastCaptchaLoaded;

    private SpeedMeter                        speedMeter;

    private Vector<StartDownloads>            activeLinks     = new Vector<StartDownloads>();

    private DownloadLink                      lastDownloadFinished;

    private ClipboardHandler                  clipboard;

    private boolean                           aborted         = false;

    /**
     * 
     */
    public JDController() {
        downloadLinks = new Vector<DownloadLink>();
        speedMeter = new SpeedMeter(5000);
        clipboard = new ClipboardHandler(this);
        JDUtilities.setController(this);
    }

    /**
     * Startet den Downloadvorgang
     */
    private void startDownloads() {
logger.info("StartDownloads");
aborted=false;
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOAD_START, this));
        setDownloadActive();

        

    }

    private void setDownloadActive() {
        DownloadLink dlink;
        logger.info("Gleichzeitige Downloads erlaubt: " + getSimultanDownloadNum()+" aktiv:  "+activeLinks.size());
        while (activeLinks.size() < getSimultanDownloadNum()) {
            dlink = this.getNextDownloadLink();
            if (dlink == null) break;
            this.startDownloadThread(dlink);
        }
        // gehe in die Warteschleife wenn noch nicht genug downloads laufen
        
      
        if (activeLinks.size() ==0) this.waitForDownloadLinks();
    }

    private int getDownloadNumByHost(PluginForHost plugin) {
        int num = 0;
        for (int i = 0; i < this.activeLinks.size(); i++) {
            if (this.activeLinks.get(i).getDownloadLink().getPlugin().getPluginID().equals(plugin.getPluginID())) {
                num++;
            }

        }
        return num;
    }

    private void cleanActiveVector() {
        int statusD;
        logger.info("Clean Activevector");
        for (int i = this.activeLinks.size() - 1; i >= 0; i--) {
            statusD = this.activeLinks.get(i).getDownloadLink().getStatus();

            if (statusD == DownloadLink.STATUS_DONE || (this.activeLinks.get(i).getDownloadLink().getPlugin().getCurrentStep() != null && this.activeLinks.get(i).getDownloadLink().getPlugin().getCurrentStep().getStatus() == PluginStep.STATUS_ERROR)) {
                activeLinks.remove(i);
            }

        }
        
        logger.info("Clean ünrig_ "+activeLinks.size());

    }

    private void startDownloadThread(DownloadLink dlink) {
        StartDownloads download = new StartDownloads(this, dlink);
        logger.info("start download: " + dlink);
        download.addControlListener(this);
        download.start();
        activeLinks.add(download);
    }

    private int getSimultanDownloadNum() {
        return (Integer) JDUtilities.getConfiguration().getProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 3);
    }

    /**
     * Diese Methode prüft wiederholt die Downloadlinks solange welche dabei
     * sind die Wartezeit haben. Läuft die Wartezeit ab, oder findet ein
     * reconnect statt, wird wieder die Run methode aufgerifen
     */
    private void waitForDownloadLinks() {
        
        logger.info("wait");
        Vector<DownloadLink> links;
        DownloadLink link;
        boolean hasWaittimeLinks = false;

        boolean returnToRun = false;

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {

            e.printStackTrace();
        }

        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, null));

        links = getDownloadLinks();

        for (int i = 0; i < links.size(); i++) {
            link = links.elementAt(i);
            if (!link.isEnabled()) continue;
            // Link mit Wartezeit in der queue
            if (link.getStatus() == DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT) {
                if (link.getRemainingWaittime() == 0) {

                    link.setStatus(DownloadLink.STATUS_TODO);
                    link.setEndOfWaittime(0);
                    returnToRun = true;

                }

                hasWaittimeLinks = true;
                // Neuer Link hinzugefügt
            }
            else if (link.getStatus() == DownloadLink.STATUS_TODO) {
                returnToRun = true;
            }

        }
        
        
        if (aborted) {

            logger.warning("Download aborted");
//            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
//            Interaction.handleInteraction((Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED), this);
            
            return;

        }
        else if (returnToRun) {
            logger.info("return. there are downloads waiting");
            this.setDownloadActive();
            return;
        }

        if (!hasWaittimeLinks) {

            logger.info("Alle Downloads beendet");
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED, this));
            Interaction.handleInteraction((Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED), this);
            
        }
        else {
            waitForDownloadLinks();
        }

    }

    private void stopDownloads() {
        this.aborted = true;
        for (int i = 0; i < this.activeLinks.size(); i++)
            activeLinks.get(i).abortDownload();

        this.clearDownloadListStatus();

    }

    /**
     * Beendet das Programm
     */
    private void exit() {
        saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
        System.exit(0);
    }

    /**
     * Setzt den Status der Downloadliste zurück. zB. bei einem Abbruch
     */
    private void clearDownloadListStatus() {
        Vector<DownloadLink> links;
        activeLinks.removeAllElements();
        logger.finer("Clear");
        links = getDownloadLinks();
        for (int i = 0; i < links.size(); i++) {
            if (links.elementAt(i).getStatus() != DownloadLink.STATUS_DONE) {
                links.elementAt(i).setInProgress(false);
                links.elementAt(i).setStatusText("");
                links.elementAt(i).setStatus(DownloadLink.STATUS_TODO);
            }

        }
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED, null));

    }

    /**
     * Eventfunktion für den PLuginlistener
     * 
     * @param event PluginEvent
     */
    public void pluginEvent(PluginEvent event) {
        uiInterface.deligatedPluginEvent(event);
        switch (event.getEventID()) {
            case PluginEvent.PLUGIN_DOWNLOAD_BYTES:
                speedMeter.addValue((Integer) event.getParameter1());
                break;

        }
    }

    /**
     * Hier werden ControlEvent ausgewertet
     * 
     * @param event
     */

    public void controlEvent(ControlEvent event) {

        switch (event.getID()) {

            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_FINISHED:
                lastDownloadFinished = (DownloadLink) event.getParameter();
                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                if (this.getMissingPackageFiles(lastDownloadFinished) == 0) {
                    Interaction.handleInteraction(Interaction.INTERACTION_DOWNLOAD_PACKAGE_FINISHED, this);
                }
                // Entferne link aus activevector
                cleanActiveVector();
                // Starte neuen download
                this.setDownloadActive();
                break;
            case ControlEvent.CONTROL_CAPTCHA_LOADED:
                lastCaptchaLoaded = (File) event.getParameter();
                break;

            case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:

                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                break;
            case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                Object links = event.getParameter();
                if (links != null && links instanceof Vector && ((Vector) links).size() > 0) {
                    // schickt die Links zuerst mal zum Linkgrabber
                    uiInterface.addLinksToGrabber((Vector<DownloadLink>) links);
                }
                break;
            case ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE:
                Interaction interaction = (Interaction) event.getParameter();
                // Macht einen Wartezeit reset wenn die HTTPReconnect
                // Interaction eine neue IP gebracht hat
                if (interaction instanceof HTTPReconnect && interaction.getCallCode() == Interaction.INTERACTION_CALL_SUCCESS) {
                    Iterator<DownloadLink> iterator = downloadLinks.iterator();
                    // stellt die Wartezeiten zurück
                    DownloadLink i;
                    while (iterator.hasNext()) {
                        i = iterator.next();
                        if (i.getRemainingWaittime() > 0) {
                            i.setEndOfWaittime(0);
                            i.setStatus(DownloadLink.STATUS_TODO);
                        }
                    }
                    Interaction.handleInteraction(Interaction.INTERACTION_AFTER_RECONNECT, this);
                }
                else if (interaction instanceof WebUpdate) {
                    if (interaction.getCallCode() == Interaction.INTERACTION_CALL_ERROR) {
                        // uiInterface.showMessageDialog("Keine Updates
                        // verfügbar");
                    }
                    else {
                        uiInterface.showMessageDialog("Aktualisierte Dateien: " + ((WebUpdate) interaction).getUpdater().getUpdatedFiles());
                    }
                }

                break;
            default:

                break;
        }
        uiInterface.deligatedControlEvent(event);
    }

    /**
     * Hier werden die UIEvente ausgewertet
     * 
     * @param uiEvent UIEent
     */
    public void uiEvent(UIEvent uiEvent) {
        Vector<DownloadLink> newLinks;
        switch (uiEvent.getActionID()) {
            case UIEvent.UI_START_DOWNLOADS:
                startDownloads();
                break;
            case UIEvent.UI_STOP_DOWNLOADS:
                stopDownloads();
                break;
            case UIEvent.UI_LINKS_TO_PROCESS:
                String data = (String) uiEvent.getParameter();
                distributeData = new DistributeData(data);
                distributeData.addControlListener(this);
                distributeData.start();
                break;
            case UIEvent.UI_SAVE_CONFIG:

                JDUtilities.saveObject(null, JDUtilities.getConfiguration(), JDUtilities.getJDHomeDirectory(), JDUtilities.CONFIG_PATH.split("\\.")[0], "." + JDUtilities.CONFIG_PATH.split("\\.")[1], Configuration.saveAsXML);
                break;
            case UIEvent.UI_LINKS_GRABBED:

                // Event wenn der Linkgrabber mit ok bestätigt wird. Die
                // ausgewählten Links werden als Eventparameter übergeben und
                // können nun der Downloadliste zugeführt werden
                Object links = uiEvent.getParameter();
                if (links != null && links instanceof Vector && ((Vector) links).size() > 0) {
                    downloadLinks.addAll((Vector<DownloadLink>) links);
                    saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                    uiInterface.setDownloadLinks(downloadLinks);
                }

                break;
            case UIEvent.UI_SAVE_LINKS:
                File file = (File) uiEvent.getParameter();
                saveDownloadLinks(file);
                break;
            case UIEvent.UI_LOAD_LINKS:
                file = (File) uiEvent.getParameter();
                loadDownloadLinks(file);
                break;
            case UIEvent.UI_EXIT:
                exit();
                break;

            case UIEvent.UI_SET_CLIPBOARD:
                this.clipboard.setEnabled((Boolean) uiEvent.getParameter());
                break;
            case UIEvent.UI_LINKS_CHANGED:
                newLinks = uiInterface.getDownloadLinks();
                abortDeletedLink(downloadLinks, newLinks);
                downloadLinks = newLinks;
                saveDownloadLinks(JDUtilities.getResourceFile("links.dat"));
                break;
            case UIEvent.UI_INTERACT_RECONNECT:
                if (getRunningDownloadNum() > 0) {
                    logger.info("Es laufen noch Downloads. Breche zum reconnect Downloads ab!");
                    stopDownloads();
                }
                if (Interaction.handleInteraction(Interaction.INTERACTION_NEED_RECONNECT, this)) {
                    uiInterface.showMessageDialog("Reconnect erfolgreich");
                    Iterator<DownloadLink> iterator = downloadLinks.iterator();
                    // stellt die Wartezeiten zurück
                    DownloadLink i;
                    while (iterator.hasNext()) {
                        i = iterator.next();
                        if (i.getRemainingWaittime() > 0) {
                            i.setEndOfWaittime(0);
                            i.setStatus(DownloadLink.STATUS_TODO);

                        }
                    }

                    if (Interaction.getInteractions(Interaction.INTERACTION_NEED_RECONNECT).length != 1) {
                        uiInterface.showMessageDialog("Es sind " + Interaction.getInteractions(Interaction.INTERACTION_NEED_RECONNECT).length + " Interactionen für den Reconnect festgelegt. \r\nEventl. wurde der Reconnect mehrmals ausgeführt. \r\nBitte Event einstellen (Konfiguration->Eventmanager)");

                    }

                }
                else {

                    if (Interaction.getInteractions(Interaction.INTERACTION_NEED_RECONNECT).length != 1) {
                        uiInterface.showMessageDialog("Reconnect fehlgeschlagen\r\nEs ist kein Event(oder mehrere) für die Reconnect festgelegt. \r\nBitte Event einstellen (Konfiguration->Eventmanager)");
                    }
                    else {
                        uiInterface.showMessageDialog("Reconnect fehlgeschlagen");
                    }
                }
                uiInterface.setDownloadLinks(downloadLinks);
                break;
            case UIEvent.UI_INTERACT_UPDATE:
                WebUpdate wu = new WebUpdate();
                wu.addControlListener(this);
                wu.interact(this);
                break;
        }
    }

    /**
     * bricht downloads ab wenn diese entfernt wurden
     * 
     * @param oldLinks
     * @param newLinks
     */
    private void abortDeletedLink(Vector<DownloadLink> oldLinks, Vector<DownloadLink> newLinks) {
        for (int i = 0; i < oldLinks.size(); i++) {
            if (newLinks.indexOf(oldLinks.elementAt(i)) == -1) {
                // Link gefunden der entfernt wurde
                oldLinks.elementAt(i).setAborted(true);
            }
        }

    }

    /**
     * Speichert die Linksliste ab
     * 
     * @param file Die Datei, in die die Links gespeichert werden sollen
     */
    public void saveDownloadLinks(File file) {
        // JDUtilities.saveObject(null, downloadLinks.toArray(new
        // DownloadLink[]{}), file, "links", "dat", true);
        JDUtilities.saveObject(null, downloadLinks, file, "links", "dat", Configuration.saveAsXML);
    }

    /**
     * Lädt eine LinkListe
     * 
     * @param file Die Datei, aus der die Links gelesen werden
     * @return Ein neuer Vector mit den DownloadLinks
     */
    public Vector<DownloadLink> loadDownloadLinks(File file) {
        if (file.exists()) {
            Object obj = JDUtilities.loadObject(null, file, Configuration.saveAsXML);
            if (obj != null && obj instanceof Vector) {
                Vector<DownloadLink> links = (Vector<DownloadLink>) obj;
                Iterator<DownloadLink> iterator = links.iterator();
                DownloadLink localLink;
                PluginForHost pluginForHost;
                while (iterator.hasNext()) {
                    localLink = iterator.next();
                    pluginForHost = JDUtilities.getPluginForHost(localLink.getHost());
                    if (pluginForHost != null) {
                        localLink.setLoadedPlugin(pluginForHost);
                    }
                    else {
                        logger.severe("couldn't find plugin(" + localLink.getHost() + ") for this DownloadLink." + localLink.getName());
                    }
                }
                return links;
            }
        }
        return new Vector<DownloadLink>();
    }

    /**
     * Liefert alle DownloadLinks zurück
     * 
     * @return Alle DownloadLinks zurück
     */
    protected Vector<DownloadLink> getDownloadLinks() {
        return downloadLinks;
    }

    /**
     * Setzt alle DownloadLinks neu
     * 
     * @param links Die neuen DownloadLinks
     */
    protected void setDownloadLinks(Vector<DownloadLink> links) {
        downloadLinks = links;

    }

    /**
     * Lädt zum Start das erste Mal alle Links aus einer Datei
     */
    public void initDownloadLinks() {
        downloadLinks = loadDownloadLinks(JDUtilities.getResourceFile("links.dat"));
        if (uiInterface != null) uiInterface.setDownloadLinks(downloadLinks);
    }

    /**
     * Liefert den nächsten DownloadLink
     * 
     * @return Der nächste DownloadLink oder null
     */
    public DownloadLink getNextDownloadLink() {
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (!this.isDownloadLinkActive(nextDownloadLink) && !nextDownloadLink.isInProgress() && nextDownloadLink.isEnabled() && nextDownloadLink.getStatus() == DownloadLink.STATUS_TODO && nextDownloadLink.getRemainingWaittime() == 0 && getDownloadNumByHost((PluginForHost) nextDownloadLink.getPlugin()) < ((PluginForHost) nextDownloadLink.getPlugin()).getMaxSimultanDownloadNum()) return nextDownloadLink;
        }
        return null;
    }

    private boolean isDownloadLinkActive(DownloadLink nextDownloadLink) {
        for (int i = 0; i < this.activeLinks.size(); i++) {
            if (this.activeLinks.get(i).getDownloadLink() == nextDownloadLink) {
                return true;
            }

        }
        return false;
    }

    /**
     * Gibt ale links zurück die im selben Package sind wie downloadLink
     * 
     * @param downloadLink
     * @return Alle DownloadLinks die zum selben Package gehören
     */
    public Vector<DownloadLink> getPackageFiles(DownloadLink downloadLink) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        ret.add(downloadLink);
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (downloadLink.getFilePackage() == nextDownloadLink.getFilePackage()) ret.add(nextDownloadLink);
        }
        return ret;
    }

    /**
     * Gibt die Anzahl der fertigen Downloads im package zurück
     * 
     * @param downloadLink
     * @return Anzahl der fertigen Files in diesem paket
     */
    public int getPackageReadyNum(DownloadLink downloadLink) {
        int i = 0;
        if (downloadLink.getStatus() == DownloadLink.STATUS_DONE) i++;
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (downloadLink.getFilePackage() == nextDownloadLink.getFilePackage() && nextDownloadLink.getStatus() == DownloadLink.STATUS_DONE) i++;
        }
        return i;
    }

    /**
     * Gibt die Anzahl der fehlenden FIles zurück
     * 
     * @param downloadLink
     * @return Anzahl der fehlenden Files in diesem Paket
     */
    public int getMissingPackageFiles(DownloadLink downloadLink) {
        int i = 0;
        if (downloadLink.getStatus() != DownloadLink.STATUS_DONE) i++;
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (downloadLink.getFilePackage() == nextDownloadLink.getFilePackage() && nextDownloadLink.getStatus() != DownloadLink.STATUS_DONE) i++;
        }
        return i;
    }

    /**
     * Liefert die Anzahl der gerade laufenden Downloads. (nur downloads die
     * sich wirklich in der downloadpahse befinden
     * 
     * @return Anzahld er laufenden Downloadsl
     */
    public int getRunningDownloadNum() {
        int ret = 0;
        Iterator<DownloadLink> iterator = downloadLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();
            if (nextDownloadLink.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) ret++;

        }
        return ret;
    }

    /**
     * Der Benuter soll den Captcha Code erkennen
     * 
     * @param plugin Das Plugin, das den Code anfordert
     * @param captchaAddress Adresse des anzuzeigenden Bildes
     * @return Text des Captchas
     */
    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress) {
        return uiInterface.getCaptchaCodeFromUser(plugin, captchaAddress);
    }

    /**
     * Setzt das UIINterface
     * 
     * @param uiInterface
     */
    public void setUiInterface(UIInterface uiInterface) {
        if (this.uiInterface != null) this.uiInterface.removeUIListener(this);
        this.uiInterface = uiInterface;
        uiInterface.addUIListener(this);
    }

    /**
     * Gibt das verwendete UIinterface zurpck
     * 
     * @return aktuelles uiInterface
     */
    public UIInterface getUiInterface() {
        return uiInterface;
    }

    /**
     * 
     * @return Die zuletzte fertiggestellte datei
     */
    public String getLastFinishedFile() {
        if (this.lastDownloadFinished == null) return "";
        return this.lastDownloadFinished.getFileOutput();
    }

    /**
     * 
     * @return ZUletzt bearbeiteter Captcha
     */
    public String getLastCaptchaImage() {
        if (this.lastCaptchaLoaded == null) return "";
        return this.lastCaptchaLoaded.getAbsolutePath();
    }

    /**
     * @return gibt das globale speedmeter zurück
     */
    public SpeedMeter getSpeedMeter() {
        return speedMeter;
    }

    /**
     * Fügt einen Listener hinzu
     * 
     * @param listener Ein neuer Listener
     */
    public void addControlListener(ControlListener listener) {
        if (controlListener == null) controlListener = new Vector<ControlListener>();
        if (controlListener.indexOf(listener) == -1) {
            controlListener.add(listener);
        }
    }

    /**
     * Emtfernt einen Listener
     * 
     * @param listener Der zu entfernende Listener
     */
    public void removeControlListener(ControlListener listener) {
        controlListener.remove(listener);
    }

    /**
     * Verteilt Ein Event an alle Listener
     * 
     * @param controlEvent ein abzuschickendes Event
     */
    public void fireControlEvent(ControlEvent controlEvent) {
        if (controlListener == null) controlListener = new Vector<ControlListener>();
        Iterator<ControlListener> iterator = controlListener.iterator();
        while (iterator.hasNext()) {
            ((ControlListener) iterator.next()).controlEvent(controlEvent);
        }
    }

}
