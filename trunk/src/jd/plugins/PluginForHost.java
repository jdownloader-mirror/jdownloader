//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import jd.config.Configuration;
import jd.config.MenuItem;
import jd.parser.Regex;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die von einem Anbieter Dateien
 * herunterladen können
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {
    private static final String AGB_CHECKED = "AGB_CHECKED";
    private static final String CONFIGNAME = "pluginsForHost";
    private static int currentConnections = 0;

    private static HashMap<Class,Integer>HOSTER_WAIT_TIMES = new HashMap<Class,Integer>();
    private static HashMap<Class,Long> HOSTER_WAIT_UNTIL_TIMES = new HashMap<Class,Long>();
    public static final String PARAM_MAX_RETRIES = "MAX_RETRIES";
    // public static final String PARAM_MAX_ERROR_RETRIES = "MAX_ERROR_RETRIES";
    // private static long END_OF_DOWNLOAD_LIMIT = 0;
    // public abstract URLConnection getURLConnection();
    protected DownloadInterface dl = null;
    // private int retryOnErrorCount = 0;
    private int maxConnections = 50;
    private int retryCount = 0;

    public boolean[] checkLinks(DownloadLink[] urls) {
        return null;

    }

    @Override
    public void clean() {
        this.requestInfo = null;
        this.request = null;
        this.dl = null;

        super.clean();
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    public abstract String getAGBLink();

    /**
     * Gibt zurück wie lange nach einem erkanntem Bot gewartet werden muss. Bei
     * -1 wird ein reconnect durchgeführt
     * 
     * @return
     */
    public long getBotWaittime() {

        return -1;
    }

    public int getChunksPerFile() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2);
    }

    // /**
    // * Diese methode führt den Nächsten schritt aus. Der gerade ausgeführte
    // * Schritt wir zurückgegeben
    // *
    // * @param parameter
    // * Ein Übergabeparameter
    // * @return der nächste Schritt oder null, falls alle abgearbeitet wurden
    // */
    // public void doNextStep(Object parameter) {
    //
    // nextStep(currentStep);
    //
    // if (//currentStep == null) {
    // logger.info(this + " Pluginende erreicht!");
    // return null;
    // }
    // logger.finer("Current Step: " + currentStep + "/" + steps);
    // if (!this.isAGBChecked()) {
    // current//step.setStatus(PluginStep.STATUS_ERROR);
    // logger.severe("AGB not signed : " + this.getPluginID());
    // ((DownloadLink) parameter).setStatus(LinkStatus.ERROR_AGB_NOT_SIGNED);
    // return currentStep;
    // }
    // //currentStep = doStep(currentStep, parameter);
    // logger.finer("got/return step: " + currentStep + " Linkstatus: " +
    // ((DownloadLink) parameter).getStatus());
    //
    // return currentStep;
    // }

    // public boolean isListOffline() {
    // return true;
    // }

    public int getCurrentConnections() {
        return currentConnections;
    }

    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text
     * gesucht. Gefundene Links werden dann in einem Vector zurückgeliefert
     * 
     * @param data
     *            Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein Vector mit den gefundenen Downloadlinks
     */
    public Vector<DownloadLink> getDownloadLinks(String data, FilePackage fp) {

        Vector<DownloadLink> links = null;

        // Vector<String> hits = SimpleMatches.getMatches(data,
        // getSupportedLinks());
        String[] hits = new Regex(data, getSupportedLinks()).getMatches(0);
        if (hits != null && hits.length > 0) {
            links = new Vector<DownloadLink>();
            for (int i = 0; i < hits.length; i++) {
                String file = hits[i];

                while (file.charAt(0) == '"')
                    file = file.substring(1);
                while (file.charAt(file.length() - 1) == '"')
                    file = file.substring(0, file.length() - 1);

                try {
                    // Zwecks Multidownload braucht jeder Link seine eigene
                    // Plugininstanz
                    PluginForHost plg = this.getClass().newInstance();

                    DownloadLink link = new DownloadLink(plg, file.substring(file.lastIndexOf("/") + 1, file.length()), getHost(), file, true);
                    links.add(link);
                    if (fp != null) link.setFilePackage(fp);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return links;
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

    // /**
    // * Diese Funktion verarbeitet jeden Schritt des Plugins.
    // *
    // * @param step
    // * @param parameter
    // * @return
    // */
    // public abstract PluginStep doStep( DownloadLink parameter) throws
    // Exception;

    public int getFreeConnections() {
        return Math.max(1, maxConnections - currentConnections);
    }

    /**
     * Wird nicht gebraucht muss aber implementiert werden.
     */
    
    @Override
    public String getLinkName() {

        return null;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    // public void abort(){
    // super.abort();
    // if(this.getDownloadInstance()!=null){
    // this.getDownloadInstance().abort();
    // }
    // }
    /*
     * private DownloadInterface getDownloadInstance() {
     * 
     * return this.dl; }
     */
    public int getMaxRetries() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(PARAM_MAX_RETRIES, 3);
    }

    public abstract int getMaxSimultanDownloadNum();

    // public int getMaxRetriesOnError() {
    // return
    // JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(PARAM_MAX_ERROR_RETRIES,
    // 0);
    // }

    // /**
    // * Delegiert den doStep Call mit einem Downloadlink als Parameter weiter
    // an
    // * die Plugins. Und fängt übrige Exceptions ab.
    // *
    // * @param parameter
    // * Downloadlink
    // */
    // public void handle( Object parameter) {
    //
    // try {
    // PluginStep ret = doStep(step, (DownloadLink) parameter);
    // logger.finer("got/return step: " + step + " Linkstatus: " +
    // ((DownloadLink) parameter).getStatus());
    // return ret;
    // // if(ret==null){
    // // return;
    // // }else{
    // // return ret;
    // // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // //step.setStatus(PluginStep.STATUS_ERROR);
    // ((DownloadLink) parameter).setStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);
    // //step.setParameter(e.getLocalizedMessage());
    // logger.finer("got/return 2 step: " + step + " Linkstatus: " +
    // ((DownloadLink) parameter).getStatus());
    //
    // return;
    // }
    // }

    // /**
    // * Kann im Downloadschritt verwendet werden um einen einfachen Download
    // * vorzubereiten
    // *
    // * @param downloadLink
    // * @param step
    // * @param url
    // * @param cookie
    // * @param redirect
    // * @return
    // */
    // protected boolean defaultDownloadStep(DownloadLink downloadLink, String
    // url, String cookie, boolean redirect) {
    // try {
    // requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(url), cookie, null,
    // redirect);
    //
    // int length = requestInfo.getConnection().getContentLength();
    // downloadLink.setDownloadMax(length);
    // logger.finer("Filename: " +
    // getFileNameFormHeader(requestInfo.getConnection()));
    //
    // downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
    // dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
    // dl.startDownload(); \r\n if (!dl.startDownload() && step.getStatus() !=
    // PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
    // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
    //
    // //step.setStatus(PluginStep.STATUS_ERROR);
    //
    // }
    // return true;
    // } catch (MalformedURLException e) {
    //
    // e.printStackTrace();
    // } catch (IOException e) {
    //
    // e.printStackTrace();
    // }
    //
    // //step.setStatus(PluginStep.STATUS_ERROR);
    // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
    // return false;
    //
    // }

    public String getPluginNameExtension(DownloadLink link) {
        return "";
    }

    public int getRemainingHosterWaittime() {
        // TODO Auto-generated method stub
        if(!HOSTER_WAIT_UNTIL_TIMES.containsKey(this.getClass()))return 0;
        return Math.max(0,(int) (HOSTER_WAIT_UNTIL_TIMES.get(this.getClass()) - System.currentTimeMillis()));
    }

    public int getRetryCount() {
        return retryCount;
    }

    public abstract void handle(DownloadLink link) throws Exception;

    public boolean isAGBChecked() {
        if (!this.getProperties().hasProperty(AGB_CHECKED)) {
            getProperties().setProperty(AGB_CHECKED, JDUtilities.getSubConfig(CONFIGNAME).getBooleanProperty("AGBS_CHECKED_" + this.getPluginID(), false) || JDUtilities.getSubConfig(CONFIGNAME).getBooleanProperty("AGB_CHECKED_" + this.getHost(), false));
            getProperties().save();
        }
        return getProperties().getBooleanProperty(AGB_CHECKED, false);
    }

    /**
     * Stellt das Plugin in den Ausgangszustand zurück (variablen intialisieren
     * etc)
     */
    public abstract void reset();

    public void resetHosterWaitTime() {
        HOSTER_WAIT_TIMES.put(this.getClass(), 0);
        HOSTER_WAIT_UNTIL_TIMES.put(this.getClass(), 0l);

    }

    /**
     * Führt alle restevorgänge aus und bereitet das Plugin dadurch auf einen
     * Neustart vor. Sollte nicht überschrieben werden
     */
    public final void resetPlugin() {
        // this.resetSteps();
        this.reset();
        // this.aborted = false;
    }

    public void resetPluginGlobals() {
        resetHosterWaitTime();
    }

    public void setAGBChecked(boolean value) {
        getProperties().setProperty(AGB_CHECKED, value);
        getProperties().save();
    }

    public synchronized void setCurrentConnections(int CurrentConnections) {
        currentConnections = CurrentConnections;
    }

    //
    // public void setRetryOnErrorCount(int retryOnErrorcount) {
    // this.retryOnErrorCount = retryOnErrorcount;
    // }

    // public static long getEndOfDownloadLimit() {
    // return END_OF_DOWNLOAD_LIMIT;
    // }
    //
    // public static void setEndOfDownloadLimit(long end_of_download_limit) {
    // END_OF_DOWNLOAD_LIMIT = end_of_download_limit;
    // }
    //
    // public static void setDownloadLimitTime(long downloadlimit) {
    // END_OF_DOWNLOAD_LIMIT = System.currentTimeMillis() + downloadlimit;
    // }
    //
    // public static long getRemainingWaittime() {
    // return Math.max(0, END_OF_DOWNLOAD_LIMIT - System.currentTimeMillis());
    // }

    public void setHosterWaittime(int milliSeconds) {
        
        
        HOSTER_WAIT_TIMES.put(this.getClass(), milliSeconds);
        HOSTER_WAIT_UNTIL_TIMES.put(this.getClass(), System.currentTimeMillis() + milliSeconds);

    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    protected void sleep(int i, DownloadLink downloadLink) throws InterruptedException {
        while (i > 0 && !downloadLink.getDownloadLinkController().isAborted()) {

            i -= 1000;
            downloadLink.getLinkStatus().setStatusText(String.format(JDLocale.L("gui.downloadlink.status.wait", "wait %s min"), JDUtilities.formatSeconds(i / 1000)));
            downloadLink.requestGuiUpdate();
            Thread.sleep(1000);

        }

        downloadLink.getLinkStatus().setStatusText(null);
    }

    // public void handleDownloadLimit( DownloadLink downloadLink) {
    // long waitTime = getRemainingWaittime();
    // logger.finer("wait (intern) " + waitTime + " minutes");
    // downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_TRAFFIC_LIMIT);
    // ////step.setStatus(PluginStep.STATUS_ERROR);
    // logger.info(" Waittime(intern) set to " + step + " : " + waitTime);
    // //step.setParameter((long) waitTime);
    // return;
    // }

}
