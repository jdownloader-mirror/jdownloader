package jd.config;

import java.io.Serializable;
import java.util.Vector;

import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.InteractionTrigger;
import jd.controlling.interaction.ManuelCaptcha;
import jd.controlling.interaction.WebUpdate;
import jd.router.RouterData;
import jd.utils.JDUtilities;

/**
 * In dieser Klasse werden die benutzerspezifischen Einstellungen festgehalten
 * 
 * @author astaldo
 */

public class Configuration extends Property implements Serializable {
    /**
     * Gibt an ob die SerializeFunktionen im XMl MOdus Arbeiten oder nocht
     */
    public transient static boolean saveAsXML             = false;

    /**
     * serialVersionUID
     */
    private static final long       serialVersionUID      = -2709887320616014389L;
/**
 * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden soll
 */
    public static final String PARAM_DOWNLOAD_READ_TIMEOUT = "DOWNLOAD_READ_TIMEOUT";
    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden soll
     */
    public static final String PARAM_DOWNLOAD_CONNECT_TIMEOUT = "DOWNLOAD_CONNECT_TIMEOUT";
    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden soll
     */
    public static final String PARAM_DOWNLOAD_MAX_SIMULTAN = "DOWNLOAD_MAX_SIMULTAN";
    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden soll
     */
    public static final String PARAM_LOGGER_LEVEL = "LOGGER_LEVEL";
    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden soll
     */
    public static final String PARAM_HOME_DIRECTORY = "HOME_DIRECTORY";
    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden soll
     */
    public static final String PARAM_DOWNLOAD_DIRECTORY = "DOWNLOAD_DIRECTORY";
    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden soll
     */
    public static final String PARAM_FINISHED_DOWNLOADS_ACTION = "FINISHED_DOWNLOADS_ACTION";
    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden soll
     */
    public static final String PARAM_MANUAL_CAPTCHA_USE_JAC = "MANUAL_CAPTCHA_USE_JAC";
    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden soll
     */
    public static final String PARAM_MANUAL_CAPTCHA_WAIT_FOR_JAC = "MANUAL_CAPTCHA_WAIT_FOR_JAC";
    /**
     * String ID um einen fertiggestellten download beim programmstart aus der queue zu entfernen
     */
    public static final String FINISHED_DOWNLOADS_REMOVE_AT_START = "beim Programstart entfernen";
    /**
     * String ID um einen fertiggestellten download nimcht zu entfernen
     */
    public static final String FINISHED_DOWNLOADS_NO_REMOVE = "nicht entfernen";

    /**
     * String ID um einen fertiggestellten download sofort zu entfernen
     */

    public static final String FINISHED_DOWNLOADS_REMOVE = "sofort entfernen";

    private boolean                 useJAC                = true;

    /**
     * Hier wird das Downloadverzeichnis gespeichert
     */
    private String                  downloadDirectory     = ".";

    /**
     * Die unterschiedlichen Interaktionen. (ZB Reconnect nach einem Download)
     */

    private Vector<Interaction>     interactions          = new Vector<Interaction>();

    /**
     * Hier sind die Angaben für den Router gespeichert
     */

    private RouterData              routerData            = new RouterData();

    /**
     * Benutzername für den Router
     */

    private String                  routerUsername        = null;

    /**
     * Gibt an wie oft Versucht werden soll eine neue IP zu bekommen. (1&1 lässt
     * grüßen)
     */
    private int                     reconnectRetries      = 0;

    /**
     * Password für den Router
     */
    private String                  routerPassword        = null;



 

    /**
     * Wartezeit zwischen reconnect und erstem IP Check
     */
    private int                     waitForIPCheck        = 0;

    private String                  version="";
   
    /**
     * Konstruktor für ein Configuration Object
     */
    public Configuration() {
    // WebUpdate updater=new WebUpdate();
    // updater.setTrigger(Interaction.INTERACTION_APPSTART);
    // interactions.add(updater);
    }

    /**
     * Gibt das gewählte downloadDirectory zurück
     * @return dlDir
     */
    public String getDownloadDirectory() {
        return (String)getProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY,downloadDirectory);
       
    }

    // public HashMap<Integer, Vector<Interaction>> getInteractionMap() { return
    // interactionMap; }
    /**
     * @return Gibt das Routeradmin Passwort zurück
     */
    public String getRouterPassword() {
        return routerPassword;
    }

    /**
     * @return gibt den router-admin-Username zurück
     */
    public String getRouterUsername() {
        return routerUsername;
    }

    /**
     * GIbt das routerdata objekt zurück. darin sind alle informationen gespeichert die aus der routerdata.xml importiert worden sind. (für einen router)
     * @return Gibt das routerdata objekt zurück
     */
    public RouterData getRouterData() {
        return routerData;
    }

    /**
     * Gibt an ob JAC verwendet werden soll
     * TODO: veraltet. nicht emhr verwenden!
     * @return jac oder nicht jac
     */
    public boolean useJAC() {
        return useJAC;
    }

    /**
     * @param downloadDirectory
     */
    public void setDownloadDirectory(String downloadDirectory) {
        this.downloadDirectory = downloadDirectory;
    }

    /**
     * @param routerPassword
     */
    public void setRouterPassword(String routerPassword) {
        this.routerPassword = routerPassword;
    }

    /**
     * @param routerUsername
     */
    public void setRouterUsername(String routerUsername) {
        this.routerUsername = routerUsername;
    }

    /**
     * @param routerData
     */
    public void setRouterData(RouterData routerData) {
        this.routerData = routerData;
    }

    /**
     * @param useJAC
     */
    public void setUseJAC(boolean useJAC) {
        this.useJAC = useJAC;
    }

    /**
     * @return the reconnectRetries
     */
    public int getReconnectRetries() {
        return reconnectRetries;
    }

    /**
     * @param reconnectRetries the reconnectRetries to set
     */
    public void setReconnectRetries(int reconnectRetries) {
        this.reconnectRetries = reconnectRetries;
    }

    /**
     * Wartezeit zwischen reconnect und erstem IP Check
     * 
     * @return Wartezeit zwischen reconnect und ip-check
     */
    public int getWaitForIPCheck() {
        return waitForIPCheck;
    }

    /**
     * Setztd ie Wartezeit zwischen dem Reconnect und dem ersten IP-Check
     * @param waitForIPCheck
     */
    public void setWaitForIPCheck(int waitForIPCheck) {
        this.waitForIPCheck = waitForIPCheck;
    }

    /**
     * Gibt die Interactionen zurück. Alle eingestellten INteractionen werden hier in einem vector zurückgegeben
     * 
     * @return  Vector<Interaction> 
     */

    public Vector<Interaction> getInteractions() {
        return interactions;
    }

    /**
     * Setzt die INteractionen
     * 
     * @param interactions
     */
    public void setInteractions(Vector<Interaction> interactions) {
        this.interactions = interactions;
    }

    /**
     * Gibt alle Interactionen zurück bei denen die TRigger übereinstimmen. z.B.
     * alle reconnect Aktionen
     * 
     * @param it
     * @return Alle interactionen mit dem TRigger it

     */
    public Vector<Interaction> getInteractions(InteractionTrigger it) {
        Vector<Interaction> ret = new Vector<Interaction>();
        for (int i = 0; i < interactions.size(); i++) {
            if (interactions.elementAt(i).getTrigger().getID() == it.getID()) ret.add(interactions.elementAt(i));
        }
        return ret;
    }

    /**
     * Gibt alle Interactionen zurück bei der die AKtion inter gleicht
     * 
     * @param inter
     * @return Alle interactionen mit dem Selben interaction-Event wie inter
     */
    public Vector<Interaction> getInteractions(Interaction inter) {
        Vector<Interaction> ret = new Vector<Interaction>();
        for (int i = 0; i < interactions.size(); i++) {

            if (inter.getInteractionName().equals(interactions.elementAt(i).getInteractionName())) ret.add(interactions.elementAt(i));
        }
        return ret;
    }

    /**
     * Setzt die Version der Configfile
     * 
     * @param version
     */
    public void setConfigurationVersion(String version) {
        this.version = version;
    }

    /**
     * Gibt die version der Configfile zurück. Ändert sich die Konfigversion, werden die defaulteinstellungen erneut geschrieben. So wird sichergestellt, dass bei einem Update eine Aktuelle Configfie erstellt wird
     * 
     * @return Versionsstring der Konfiguration
     */
    public String getConfigurationVersion() {
        if(version==null)return "0.0.0";
        return version;
    }

    /**
     * Legt die defaulteinstellungen in das configobjekt
     */
    public void setDefaultValues() {
        // Setze AutoUpdater

        WebUpdate wu = new WebUpdate();
        if (getInteractions(wu).size() == 0) {
            InteractionTrigger it = Interaction.INTERACTION_APPSTART;
            wu.setTrigger(it);
            interactions.add(wu);
        }
        
        ManuelCaptcha jac = new ManuelCaptcha();
        if (getInteractions(Interaction.INTERACTION_DOWNLOAD_CAPTCHA).size() == 0) {
            InteractionTrigger it = Interaction.INTERACTION_DOWNLOAD_CAPTCHA;
            jac.setTrigger(it);
            interactions.add(jac);
        }
        if(getProperty("maxSimultanDownloads")==null||((Integer)getProperty("maxSimultanDownloads"))==0){
            setProperty("maxSimultanDownloads",3);
        }

        
        setConfigurationVersion(JDUtilities.JD_VERSION);
    }


/**
 * GIbt alle Properties der Config aus
 * @return toString
 */
public String toString(){
    return "Configuration "+this.getProperties()+" INteraction "+this.interactions;
}
/**
 * Gibt den Wert zu key zurück. falls dieser Wert == null ist wird der defaultValue zurückgegeben
 * @param key
 * @param defaultValue
 * @return Wert zu key oder defaultValue
 */
public Object getProperty(String key, Object defaultValue) {
   if(getProperty(key)==null)return defaultValue;
   return getProperty(key);
}


}
