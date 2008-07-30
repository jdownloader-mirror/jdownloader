package jd.plugins;

import java.io.Serializable;
import java.lang.reflect.Field;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class LinkStatus implements Serializable {
    /**
     * Controlling Zeigt an dass der Link gerade heruntergeladen wird
     */
    public static final int DOWNLOADINTERFACE_IN_PROGRESS = 1 << 10;
    /**
     * Controlling Die AGB wurde noch nicht unterzeichnet.
     */
    public static final int ERROR_AGB_NOT_SIGNED = 1 << 16;
    /**
     * Controlling,Downloadinterface Zeigt an dass die Datei auf der festplatte
     * schon existiert
     */
    public static final int ERROR_ALREADYEXISTS = 1 << 13;
    /**
     * PLugins: Captcha Text war falsch
     */
    public final static int ERROR_CAPTCHA = 1 << 3;
    /**
     * Downloadinterface Zeigt an dass der Eigentliche Download im
     * Downloadinterface fehlgeschlagen ist. z.B. Misslungender Chunkload
     */
    public static final int ERROR_DOWNLOAD_FAILED = 1 << 14;
    /**
     * Downloadinterface Zeigt an dass der Link nicht vollständig geladen wurde
     */
    public static final int ERROR_DOWNLOAD_INCOMPLETE = 1 << 9;
    /**
     * Plugins & Downloadinterface Schwerwiegender fehler. Der Download wird
     * sofort abgebrochen. Es werden keine weiteren versuche mehr gestartet
     */
    public static final int ERROR_FATAL = 1 << 17;
    /**
     * Plugins & Downloadinterface: Die Datei konnte nicht gefunden werden
     */
    public final static int ERROR_FILE_NOT_FOUND = 1 << 5;
    /**
     * Plugins: Download Limit wurde erreicht
     */
    public final static int ERROR_IP_BLOCKED = 1 << 4;
    /**
     * Conttrolling, Downloadinterface, Plugins Zeigt an, dass gerade ein
     * anderes Plugin an der Lokalen Datei arbeitet. Wird eingesetzt um dem
     * Controller mitzuteilen, dass bereits ein Mirror dieser Datei geladen
     * wird.
     * 
     */
    public static final int ERROR_LINK_IN_PROGRESS = 1 << 19;

    /**
     * Downloadinterface LOCAL Input output Fehler. Es kann nicht geschrieben
     * werden etc.
     */
    public static final int ERROR_LOCAL_IO = 1 << 21;

    /**
     * DownloadInterface Zeigt an dass es einen Timeout gab und es scheinbar
     * keine Verbindung emhr zum internet gibt
     */
    public static final int ERROR_NO_CONNECTION = 1 << 15;

    /**
     * Plugins Wird bei Schwerenb Parsing fehler eingesetzt. Über diesen Code
     * kann das Plugin mitteilen dass es defekt ist und aktualisiert werden muss
     */
    public static final int ERROR_PLUGIN_DEFEKT = 1 << 22;

    /**
     * Plugins | Controlling zeigt einen Premiumspezifischen fehler an
     */
    public static final int ERROR_PREMIUM = 1 << 8;

    /**
     * Plugins: Ein unbekannter Fehler ist aufgetreten
     */
    public final static int ERROR_RETRY = 1 << 2;

    // /**
    // * Ein unbekannter Fehler ist aufgetreten. Der Download Soll wiederholt
    // * werden
    // */

    /**
     * PLugins Der download ist zur Zeit nicht möglich
     */
    public static final int ERROR_TEMPORARILY_UNAVAILABLE = 1 << 11;

    /**
     * DownloadINterface & Controlling zeigt an dass es zu einem plugintimeout
     * gekommen ist
     */
    public static final int ERROR_TIMEOUT_REACHED = 1 << 20;

    /**
     * Controlling & Downloadinterface: Link wurde erfolgreich heruntergeladen
     */
    public final static int FINISHED = 1 << 1;

    /**
     * Controlling Ziegt an, dass das zugehörige Plugin den link gerade
     * bearbeitet
     */
    public static final int PLUGIN_IN_PROGRESS = 1 << 18;

    /**
     * 
     */
    private static final long serialVersionUID = 3885661829491436448L;
    /**
     * Controlling: Link muß noch bearbeitet werden.
     */
    public final static int TODO = 1 << 0;
    private DownloadLink downloadLink;
    private transient String errorMessage;

    private int lastestStatus = TODO;
    private int status = TODO;
    private transient String statusText = null;
    private int totalWaitTime = 0;
    private int value;
    private long waitUntil = 0;

    public LinkStatus(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;

    }

    /**
     * Fügt einen LinkStatus.* Status hinzu.Der alte status wird dabei nicht
     * gelöscht.
     * 
     * @param status
     */
    public void addStatus(int status) {
        this.status |= status;
        this.lastestStatus = status;

        System.out.println("");

    }

    public void exceptionToErrorMessage(Exception e) {
        this.setErrorMessage(JDUtilities.convertExceptionReadable(e));

    }

    private String getDefaultErrorMessage() {
        switch (this.lastestStatus) {

        case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
            return JDLocale.L("downloadlink.status.incomplete", "Incomplete");

        case LinkStatus.ERROR_AGB_NOT_SIGNED:
            return JDLocale.L("downloadlink.status.error.agb_not_signed", "TOCs not signed");
        case LinkStatus.ERROR_ALREADYEXISTS:
            return JDLocale.L("downloadlink.status.error.file_exists", "File exists");

        case LinkStatus.ERROR_CAPTCHA:
            return JDLocale.L("downloadlink.status.error.captcha_wrong", "Captcha wrong");
        case LinkStatus.ERROR_DOWNLOAD_FAILED:
            return JDLocale.L("downloadlink.status.error.downloadfailed", "Download failed");
        case LinkStatus.ERROR_IP_BLOCKED:
            return JDLocale.L("downloadlink.status.error.download_limit", "Download Limit reached");

        case LinkStatus.ERROR_FILE_NOT_FOUND:
            return JDLocale.L("downloadlink.status.error.file_not_found", "File not found");

        case LinkStatus.ERROR_NO_CONNECTION:
            return JDLocale.L("downloadlink.status.error.no_connection", "No Connection");

        case LinkStatus.ERROR_PREMIUM:
            return JDLocale.L("downloadlink.status.error.premium", "Premium Error");

        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            return JDLocale.L("downloadlink.status.error.temp_unavailable", "Temp. unavailable");

        case LinkStatus.ERROR_FATAL:
            return JDLocale.L("downloadlink.status.error.fatal", "Fatal Error");

        }
        return null;

    }

    private String getErrorMessage() {
        String ret = errorMessage;
        if (ret == null) ret = this.getDefaultErrorMessage();
        if (ret == null) ret = JDLocale.L("downloadlink.status.error_unexpected", "Unexpected Error");
        return ret;
    }

    public int getLatestStatus() {

        return this.lastestStatus;
    }

    public int getRemainingWaittime() {

        return Math.max(0, (int) (waitUntil - System.currentTimeMillis()));
    }

    /**
     * Erstellt den Statustext, fügt eine eventl Wartezeit hzin und gibt diesen
     * Statusstrin (bevorzugt an die GUI) zurück
     * 
     * @return Statusstring mit eventl Wartezeit
     */

    public String getStatusText() {
        String ret = "";
        if (this.hasStatus(LinkStatus.FINISHED)) {

        return JDLocale.L("gui.downloadlink.finished", "[finished]"); }

        if (!downloadLink.isEnabled() && this.hasStatus(LinkStatus.FINISHED)) {
            ret += JDLocale.L("gui.downloadlink.disabled", "[deaktiviert]");
            if (this.errorMessage != null)
            ;
            ret += ": " + errorMessage;
            return errorMessage;

        }

        if (isFailed()) { return this.getErrorMessage(); }

        // String ret = "";

        //    
        if (hasStatus(ERROR_IP_BLOCKED) && getRemainingWaittime() > 0) {
            if (statusText == null) {
                ret = String.format(JDLocale.L("gui.download.waittime_status", "Wait %s min"), JDUtilities.formatSeconds((getRemainingWaittime() / 1000)));
            } else {
                ret = String.format(JDLocale.L("gui.download.waittime_status", "Wait %s min"), JDUtilities.formatSeconds((getRemainingWaittime() / 1000))) + statusText;

            }
            return ret;
        }

        // + "sek)"; }
        if (downloadLink.getDownloadInstance() == null && hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
            removeStatus(DOWNLOADINTERFACE_IN_PROGRESS);
        }
        if (hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
            int speed = Math.max(0, downloadLink.getDownloadSpeed());
            String chunkString = "(" + downloadLink.getDownloadInstance().getChunksDownloading() + "/" + downloadLink.getDownloadInstance().getChunkNum() + ")";
            if (downloadLink.getDownloadMax() < 0) {
                return JDUtilities.formatKbReadable(speed / 1024) + "/s " + JDLocale.L("gui.download.filesize_unknown", "(Dateigröße unbekannt)");
            } else {
                if (speed > 0) {

                    long remainingBytes = downloadLink.getDownloadMax() - downloadLink.getDownloadCurrent();
                    long eta = remainingBytes / speed;
                    return "ETA " + JDUtilities.formatSeconds((int) eta) + " @ " + JDUtilities.formatKbReadable(speed / 1024) + "/s " + chunkString;
                } else {
                    return JDUtilities.formatKbReadable(speed) + "/s " + chunkString;

                }

            }
        }

        if (this.statusText != null) { return statusText; }
        return "";

    }

    public int getTotalWaitTime() {

        return totalWaitTime;
    }
    
    
    public int getValue() {
        return value;
    }

    private boolean hasOnlyStatus(int statusCode) {
        return  (status&(~statusCode))==0;
    }

    /**
     * Gibt zurück ob der zugehörige Link einen bestimmten status hat.
     * 
     * @param status
     * @return
     */
    public boolean hasStatus(int status) {

        return (this.status & status) > 0;
    }



    public boolean isFailed() {

        return !this.hasOnlyStatus(FINISHED|ERROR_IP_BLOCKED|TODO|PLUGIN_IN_PROGRESS|LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
    }

    public boolean isPluginActive() {
        return this.hasStatus(PLUGIN_IN_PROGRESS);

    }

    public boolean isStatus(int status) {
        return this.status == status;
    }

    /** Entfernt eine Statusid */
    public void removeStatus(int status) {
        int mask = 0xffffffff;
        mask &= ~status;
        this.status &= mask;
    }

    public void reset() {
        setStatus(TODO);
        waitUntil = 0;
        this.errorMessage = null;
        this.statusText = null;
        totalWaitTime = 0;
    }

    public void resetWaitTime() {
        totalWaitTime = 0;
        waitUntil = 0;
        ((PluginForHost) downloadLink.getPlugin()).resetHosterWaitTime();
    }

    public void setErrorMessage(String string) {
        this.errorMessage = string;

    }

    public void setInProgress(boolean b) {
        if (b) {
            this.addStatus(PLUGIN_IN_PROGRESS);
        } else {
            this.removeStatus(PLUGIN_IN_PROGRESS);
        }

    }

    /**
     * Setzt den Linkstatus. Es dürfen nur LInkStatus.*STATUS ids verwendet
     * werden
     * 
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
        this.lastestStatus = status;
        System.out.println("");
    }

    public void setStatusText(String l) {
        this.statusText = l;

    }

    public void setValue(int i) {
        this.value = i;

    }

    public void setWaitTime(int milliSeconds) {
        this.waitUntil = System.currentTimeMillis() + milliSeconds;
        this.totalWaitTime = milliSeconds;

    }

    private String toStatusString() {
        switch (status) {
        case LinkStatus.FINISHED:
            return JDLocale.L("downloadlink.status.done", "Finished");
        case LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS:
            return JDLocale.L("downloadlink.status.downloadInProgress", "Loading");
        case LinkStatus.ERROR_DOWNLOAD_INCOMPLETE:
            return JDLocale.L("downloadlink.status.incomplete", "Incomplete");
        case LinkStatus.TODO:
            return JDLocale.L("downloadlink.status.todo", "");
        case LinkStatus.ERROR_AGB_NOT_SIGNED:
            return JDLocale.L("downloadlink.status.error.agb_not_signed", "TOCs not signed");
        case LinkStatus.ERROR_ALREADYEXISTS:
            return JDLocale.L("downloadlink.status.error.file_exists", "File exists");

        case LinkStatus.ERROR_CAPTCHA:
            return JDLocale.L("downloadlink.status.error.captcha_wrong", "Captcha wrong");
        case LinkStatus.ERROR_DOWNLOAD_FAILED:
            return JDLocale.L("downloadlink.status.error.downloadfailed", "Download failed");
        case LinkStatus.ERROR_IP_BLOCKED:
            return JDLocale.L("downloadlink.status.error.download_limit", "Download Limit reached");
            // case LinkStatus.ERROR_FILE_NOT_FOUND:
            // return JDLocale.L("downloadlink.status.error.file_abused", "File
            // abused");
        case LinkStatus.ERROR_FILE_NOT_FOUND:
            return JDLocale.L("downloadlink.status.error.file_not_found", "File not found");

            // case LinkStatus.ERROR_NO_FREE_SPACE:
            // return JDLocale.L("downloadlink.status.error.no_free_space", "No
            // Free Space");
        case LinkStatus.ERROR_NO_CONNECTION:
            return JDLocale.L("downloadlink.status.error.no_connection", "No Connection");
            // case LinkStatus.ERROR_LINK_IN_PROGRESS:
            // return JDLocale.L("downloadlink.status.error.not_owner", "Link is
            // already in progress");

        case LinkStatus.ERROR_PREMIUM:
            return JDLocale.L("downloadlink.status.error.premium", "Premium Error");
            // case LinkStatus.ERROR_SECURITY:
            // return JDLocale.L("downloadlink.status.error.security",
            // "Read/Write Error");
            // case LinkStatus.ERROR_TRAFFIC_LIMIT:
            // return JDLocale.L("downloadlink.status.error.static_wait",
            // "Waittime");
        case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            return JDLocale.L("downloadlink.status.error.temp_unavailable", "Temp. unavailable");
            // case LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE:
            // return JDLocale.L("downloadlink.status.error.many_users", "Too
            // many User");
        case LinkStatus.ERROR_RETRY:
            return JDLocale.L("downloadlink.status.error.unknown", "Unknown Error");
        case LinkStatus.ERROR_FATAL:
            return JDLocale.L("downloadlink.status.error.fatal", "Fatal Error");
        default:
            return JDLocale.L("downloadlink.status.error_def", "Error");
        }

    }

    public String toString() {
        Class<? extends LinkStatus> cl = this.getClass();
        Field[] fields = cl.getDeclaredFields();
        StringBuffer sb = new StringBuffer();
        sb.append(JDUtilities.fillString(Integer.toBinaryString(status), "0", "", 32) + " <Statuscode\r\n");
        String latest = "";
        for (Field field : fields) {
            if (field.getModifiers() == 25) {
                int value;
                try {
                    value = field.getInt(this);
                    if (hasStatus(value)) {
                        if (value == this.lastestStatus) {
                            latest = "latest:" + field.getName() + "\r\n";
                            sb.append(JDUtilities.fillString(Integer.toBinaryString(value), "0", "", 32) + " |" + field.getName() + "\r\n");

                        } else {

                            sb.append(JDUtilities.fillString(Integer.toBinaryString(value), "0", "", 32) + " |" + field.getName() + "\r\n");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        }

        String ret=latest + sb;
        
        if(statusText!=null){
            ret+="StatusText: "+statusText+"\r\n";
        }
        if(errorMessage!=null){
            ret+="ErrorMessage: "+errorMessage+"\r\n";
        }
        return ret;
    }

}
