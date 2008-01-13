package jd.plugins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die von einem Anbieter Dateien
 * herunterladen können
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {
    private static final String CONFIGNAME = "pluginsForHost";
    // public abstract URLConnection getURLConnection();

    private String data;

    /**
     * Stellt das Plugin in den Ausgangszustand zurück (variablen intialisieren
     * etc)
     */
    public abstract void reset();

    /**
     * Führt alle restevorgänge aus und bereitet das Plugin dadurch auf einen
     * Neustart vor. Sollte nicht überschrieben werden
     */
    public final void resetPlugin() {
        this.resetSteps();
        this.reset();
        this.aborted = false;
    }
    /**
     * Setzt globale Plugineinstellungen wie eine Globale Hosterwartezeit zurück.
     */
    public abstract void resetPluginGlobals();

    /**
     * Diese methode führt den Nächsten schritt aus. Der gerade ausgeführte
     * Schritt wir zurückgegeben
     * 
     * @param parameter Ein Übergabeparameter
     * @return der nächste Schritt oder null, falls alle abgearbeitet wurden
     */
    public PluginStep doNextStep(Object parameter) {
      
        currentStep = nextStep(currentStep);
        if (currentStep == null) {
            logger.info(this + " Pluginende erreicht!");
            return null;
        }
        logger.info("Current Step:  " + currentStep);
        if(!this.isAGBChecked()){
            currentStep.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("AGB not signed : "+this.getPluginID());
            ((DownloadLink)parameter).setStatus(DownloadLink.STATUS_ERROR_AGB_NOT_SIGNED);
            return currentStep;
        }
        PluginStep ret = doStep(currentStep, parameter);

        return ret;
    }
    public boolean isListOffline(){
        return true;
    }

    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text
     * gesucht. Gefundene Links werden dann in einem Vector zurückgeliefert
     * 
     * @param data Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein Vector mit den gefundenen Downloadlinks
     */
    public Vector<DownloadLink> getDownloadLinks(String data) {
        this.data = data;
        Vector<DownloadLink> links = null;

        Vector<String> hits = getMatches(data, getSupportedLinks());
        if (hits != null && hits.size() > 0) {
            links = new Vector<DownloadLink>();
            for (int i = 0; i < hits.size(); i++) {
                String file = hits.get(i);
              //( logger.info("File" +file);
                while (file.charAt(0) == '"')
                    file = file.substring(1);
                while (file.charAt(file.length() - 1) == '"')
                    file = file.substring(0, file.length() - 1);

                try {
                    // Zwecks Multidownload braucht jeder Link seine eigene
                    // Plugininstanz
                    PluginForHost plg = this.getClass().newInstance();
                    plg.addPluginListener(JDUtilities.getController());

                    links.add(new DownloadLink(plg, file.substring(file.lastIndexOf("/") + 1, file.length()), getHost(), file, true));
                }
                catch (InstantiationException e) {
                     e.printStackTrace();
                }
                catch (IllegalAccessException e) {
                     e.printStackTrace();
                }
            }
        }
        return links;
    }

    /**
     * prüft ob genug Speicherplatz für den download zur Verfügung steht
     * 
     * @param downloadLink
     * @return true/false
     */
    protected boolean hasEnoughHDSpace(DownloadLink downloadLink) {
        return true;
        // File file = new File(downloadLink.getFileOutput());
        // if (file.getParentFile() != null && file.getParentFile().exists()) {
        // file = file.getParentFile();
        // }
        // return file.getFreeSpace()-downloadLink.getDownloadMax() > ((long)
        // JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_MIN_FREE_SPACE,
        // 100)) * 1024l * 1024l;
    }

    /**
     * Holt Informationen zu einem Link. z.B. dateigröße, Dateiname,
     * verfügbarkeit etc.
     * 
     * @param parameter
     * @return true/false je nach dem ob die Datei noch online ist (verfügbar)
     */
    public abstract boolean getFileInformation(DownloadLink parameter);

    /**
     * Gibt einen String mit den Dateiinformationen zurück. Die Defaultfunktion
     * gibt nur den dateinamen zurück. Allerdings Sollte diese Funktion
     * überschrieben werden. So kann ein Plugin zusatzinfos zu seinen Links
     * anzeigen (Nach dem aufruf von getFileInformation()
     * 
     * @param parameter
     * @return
     */
    public String getFileInformationString(DownloadLink parameter) {
        return parameter.getName();
    }

    /**
     * Diese Funktion verarbeitet jeden Schritt des Plugins.
     * 
     * @param step
     * @param parameter
     * @return
     */
    public abstract PluginStep doStep(PluginStep step, DownloadLink parameter) throws Exception;

    public abstract int getMaxSimultanDownloadNum();
    public abstract String getAGBLink();
    public boolean isAGBChecked(){
        //Schreibfehler ist bekannt, allerdings müssten die user alles nochmal einstellenw enn man diesen Key hier ändert.
        return JDUtilities.getSubConfig(CONFIGNAME).getBooleanProperty("AGBS_CHECKED_"+this.getPluginID(), false);
    }
    public void setAGBChecked(boolean value){
        JDUtilities.getSubConfig(CONFIGNAME).setProperty("AGBS_CHECKED_"+this.getPluginID(), value);
        JDUtilities.getSubConfig(CONFIGNAME).save();
           
    }
    /**
     * Delegiert den doStep Call mit einem Downloadlink als Parameter weiter an
     * die Plugins. Und fängt übrige Exceptions ab.
     * 
     * @param parameter Downloadlink
     */
    public PluginStep doStep(PluginStep step, Object parameter) {

        try {
            return doStep(step, (DownloadLink) parameter);
        }
        catch (Exception e) {
             e.printStackTrace();
            step.setStatus(PluginStep.STATUS_ERROR);
            ((DownloadLink) parameter).setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

            return step;
        }
    }

    /**
     * Kann im Downloadschritt verwendet werden um einen einfachen Download
     * vorzubereiten
     * 
     * @param downloadLink
     * @param step
     * @param url
     * @param cookie
     * @param redirect
     * @return
     */
    protected boolean defaultDownloadStep(DownloadLink downloadLink, PluginStep step, String url, String cookie, boolean redirect) {
        try {
            requestInfo = getRequestWithoutHtmlCode(new URL(url), cookie, null, redirect);

            int length = requestInfo.getConnection().getContentLength();
            downloadLink.setDownloadMax(length);
            logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));

            downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
            if (!download(downloadLink, (URLConnection) requestInfo.getConnection())) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            }
            else {
                step.setStatus(PluginStep.STATUS_DONE);
                downloadLink.setStatus(DownloadLink.STATUS_DONE);
            }
            return true;
        }
        catch (MalformedURLException e) {

             e.printStackTrace();
        }
        catch (IOException e) {

             e.printStackTrace();
        }

        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        return false;

    }

    /**
     * Wird nicht gebraucht muss aber implementiert werden.
     */
    @Override
    public String getLinkName() {

        return data;
    }
/**
 * Gibt zurück wie lange nach einem erkanntem Bot gewartet werden muss. Bei -1 wird ein reconnect durchgeführt
 * @return
 */
    public long getBotWaittime() {
       
        return -1;
    }

}
