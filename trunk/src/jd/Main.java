package jd;

import java.awt.Toolkit;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import jd.controlling.JDController;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

/**
 * Start der Applikation
 *
 * @author astaldo
 */

//TODO Clipboard Management
//TODO Links speichern / laden / editieren(nochmal laden, Passwort merken)
//TODO LinkGrabber / Pakete
//TODO VerzeichnisauswahlDialog (zB Beim Initialiseren von WebStart Cookie / DownloadDir)
//TODO Klänge wiedergeben 

//TODO Fehlerbehandlung der Plugins verbessern
//TODO HTTPPost Klasse untersuchen
//TODO Plugin.download überprüfen (Geschwindigkeitsreduzierung durch Thread.sleep?)
//TODO Interactions ergänzen (Download nicht möglich, Externes Programm ausführen)
//TODO Interaction - Vector 
//TODO Plugin Wartezeit static, falls kein HTTP Reconnect genutzt wird (Datetime nextPossibleDownloadAt)
//TODO TelnetReconnect (später)
//TODO WebInterface
//TODO GUI Auswahl

public class Main {
    public static void main(String args[]){
        Main main = new Main();
        main.go();
    }
    private void go(){
        
        loadImages();
        Logger logger = Plugin.getLogger();

        URLClassLoader cl=null;
        URL configURL = null;

        try {
            cl = JDUtilities.getURLClassLoader();
            configURL = cl.getResource(JDUtilities.CONFIG_PATH);
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
        if(configURL != null){
            try {
                File fileInput = new File(configURL.toURI());
                Object obj = JDUtilities.loadObject(null, fileInput, true);
                if(obj instanceof Configuration){
                    Configuration configuration = (Configuration)obj;
                    JDUtilities.setConfiguration(configuration);
                    Plugin.getLogger().setLevel(Level.parse(configuration.getLoggerLevel()));
                }
            }
            catch (URISyntaxException e1) { e1.printStackTrace(); }
        }
        else{
            logger.warning("no configuration loaded");
            
        }
//        Configuration c = new Configuration();
//        c.setDownloadDirectory("D:\\Downloads");
//        JDUtilities.saveObject(null, c, JDUtilities.getJDHomeDirectory(), "jdownloader", ".config", true);
        JDUtilities.loadPlugins();
        UIInterface uiInterface = new SimpleGUI();
        JDController controller = new JDController();
        controller.setUiInterface(uiInterface);
        
        Iterator<PluginForHost> iteratorHost = JDUtilities.getPluginsForHost().iterator();
        while(iteratorHost.hasNext()){
            iteratorHost.next().addPluginListener(controller);
        }
        Iterator<PluginForDecrypt> iteratorDecrypt = JDUtilities.getPluginsForDecrypt().iterator();
        while(iteratorDecrypt.hasNext()){
            iteratorDecrypt.next().addPluginListener(controller);
        }
    }
    
    /**
     * Die Bilder werden aus der JAR Datei nachgeladen
     */
    private void loadImages(){
        ClassLoader cl = getClass().getClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        JDUtilities.addImage("add",           toolkit.getImage(cl.getResource("img/add.png")));
        JDUtilities.addImage("configuration", toolkit.getImage(cl.getResource("img/configuration.png")));
        JDUtilities.addImage("delete",        toolkit.getImage(cl.getResource("img/delete.png")));
        JDUtilities.addImage("down",          toolkit.getImage(cl.getResource("img/down.png")));
        JDUtilities.addImage("led_empty",     toolkit.getImage(cl.getResource("img/led_empty.gif")));
        JDUtilities.addImage("led_green",     toolkit.getImage(cl.getResource("img/led_green.gif")));
        JDUtilities.addImage("load",          toolkit.getImage(cl.getResource("img/load.png")));
        JDUtilities.addImage("mind",          toolkit.getImage(cl.getResource("img/mind.png")));
        JDUtilities.addImage("save",          toolkit.getImage(cl.getResource("img/save.png")));
        JDUtilities.addImage("start",         toolkit.getImage(cl.getResource("img/start.png")));
        JDUtilities.addImage("stop",          toolkit.getImage(cl.getResource("img/stop.png")));
        JDUtilities.addImage("up",            toolkit.getImage(cl.getResource("img/up.png")));
        JDUtilities.addImage("exit",          toolkit.getImage(cl.getResource("img/shutdown.png")));
        JDUtilities.addImage("log",           toolkit.getImage(cl.getResource("img/log.png")));
    }
}
