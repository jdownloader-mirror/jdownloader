package jd.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import jd.JDClassLoader;
import jd.JDFileFilter;
import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.plugins.Plugin;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginForSearch;
import jd.plugins.PluginOptional;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginListener;
import jd.update.WebUpdater;
import sun.misc.Service;

/**
 * @author astaldo/coalado
 */
public class JDUtilities {
    /**
     * Parametername für den Konfigpath
     */
    public static final String                     CONFIG_PATH         = "jDownloader.config";
    /**
     * Titel der Applikation
     */
    public static final String                     JD_VERSION          = "0.0.";
    public static final String                     JD_REVISION         = "$Id$";
    /**
     * Versionsstring der Applikation
     */
    public static final String                     JD_TITLE            = "jDownloader";
    /**
     * Ein URLClassLoader, um Dateien aus dem HomeVerzeichnis zu holen
     */
    private static JDClassLoader                   jdClassLoader       = null;
    /**
     * Das JD-Home Verzeichnis. Dieses wird nur gesetzt, wenn es aus dem WebStart Cookie gelesen wurde. Diese Variable kann nämlich im KonfigDialog geändert werden
     */
    private static String                          homeDirectory       = null;
    /**
     * Das ist das File Objekt, daß das HomeDirectory darstellt
     */
    private static File                            homeDirectoryFile   = null;
    /**
     * Der DownloadController
     */
    private static JDController                    controller          = null;
    /**
     * RessourceBundle für Texte
     */
    private static ResourceBundle                  resourceBundle      = null;
    /**
     * Angaben über Spracheinstellungen
     */
    private static Locale                          locale              = null;
    /**
     * Alle verfügbaren Bilder werden hier gespeichert
     */
    private static HashMap<String, Image>          images              = new HashMap<String, Image>();
    /**
     * Der Logger für Meldungen
     */
    private static Logger                          logger              = Plugin.getLogger();
    /**
     * Damit werden die JARs rausgesucht
     */
    public static JDFileFilter                     filterJar           = new JDFileFilter(null, ".jar", false);
    /**
     * Das aktuelle Verzeichnis (Laden/Speichern)
     */
    private static File                            currentDirectory;
    /**
     * Hier werden alle vorhandenen Plugins zum Dekodieren von Links gespeichert
     */
    private static Vector<PluginForDecrypt>        pluginsForDecrypt   = new Vector<PluginForDecrypt>();
    /**
     * Hier werden alle vorhandenen Plugins zum Suchen von Links gespeichert
     */
    private static Vector<PluginForSearch>         pluginsForSearch    = new Vector<PluginForSearch>();
    /**
     * Hier werden alle vorhandenen Plugins zum Öffnen verschlüsselter Containerfiles gespeichert
     */
    private static Vector<PluginForContainer>      pluginsForContainer = new Vector<PluginForContainer>();
    /**
     * Hier werden alle Plugins für die Anbieter gespeichert
     */
    private static Vector<PluginForHost>           pluginsForHost      = new Vector<PluginForHost>();
    /**
     * Hier werden optionale Plugins gespeichert
     */
    private static HashMap<String, PluginOptional> pluginsOptional     = new HashMap<String, PluginOptional>();
    /**
     * Die Konfiguration
     */
    private static Configuration                   configuration       = new Configuration();
    /**
     * Geht eine Komponente so lange durch (getParent), bis ein Objekt vom Typ Frame gefunden wird, oder es keine übergeordnete Komponente gibt
     * 
     * @param comp Komponente, dessen Frame Objekt gesucht wird
     * @return Ein Frame Objekt, das die Komponente beinhält oder null, falls keins gefunden wird
     */
    public static Frame getParentFrame(Component comp) {
        if (comp == null) return null;
        while (comp != null && !(comp instanceof Frame))
            comp = comp.getParent();
        if (comp instanceof Frame)
            return (Frame) comp;
        else
            return null;
    }
    /**
     * parsed den JD_REVISION String auf
     * 
     * @return RevissionID
     */
    public static String getRevision() {
        String[] data = JD_REVISION.split(" ");
        if (data.length > 2) return data[2];
        return null;
    }
    /**
     * parsed den JD_REVISION String auf
     * 
     * @return Letztes Änderungs datum
     */
    public static String getLastChangeDate() {
        String[] data = JD_REVISION.split(" ");
        if (data.length > 3) {
            String[] date = data[3].split("-");
            if (date.length != 3) return null;
            return date[2] + "." + date[1] + "." + date[0];
        }
        return null;
    }
    /**
     * parsed den JD_REVISION String auf
     * 
     * @return Letzte änderungsuhrzeit
     */
    public static String getLastChangeTime() {
        String[] data = JD_REVISION.split(" ");
        if (data.length > 4) {
            return data[4].substring(0, data[4].length() - 1);
        }
        return null;
    }
    /**
     * parsed den JD_REVISION String auf
     * 
     * @return Name des programmierers der die letzten Änderungen durchgeführt hat
     */
    public static String getLastChangeAuthor() {
        String[] data = JD_REVISION.split(" ");
        if (data.length > 5) {
            return data[5];
        }
        return null;
    }
    /**
     * Diese Klasse fuegt eine Komponente einem Container hinzu
     * 
     * @param cont Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp Die Komponente, die hinzugefuegt werden soll
     * @param x X-Position innerhalb des GriBagLayouts
     * @param y Y-Position innerhalb des GriBagLayouts
     * @param width Anzahl der Spalten, ueber die sich diese Komponente erstreckt
     * @param height Anzahl der Reihen, ueber die sich diese Komponente erstreckt
     * @param weightX Verteilung von zur Verfuegung stehendem Platz in X-Richtung
     * @param weightY Verteilung von zur Verfuegung stehendem Platz in Y-Richtung
     * @param insets Abständer der Komponente
     * @param iPadX Leerraum zwischen einer GridBagZelle und deren Inhalt (X-Richtung)
     * @param iPadY Leerraum zwischen einer GridBagZelle und deren Inhalt (Y-Richtung)
     * @param fill Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp, int x, int y, int width, int height, int weightX, int weightY, Insets insets, int iPadX, int iPadY, int fill, int anchor) {
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = x;
        cons.gridy = y;
        cons.gridwidth = width;
        cons.gridheight = height;
        cons.weightx = weightX;
        cons.weighty = weightY;
        cons.fill = fill;
        cons.anchor = anchor;
        if (insets != null) cons.insets = insets;
        cons.ipadx = iPadX;
        cons.ipady = iPadY;
        cont.add(comp, cons);
    }
    /**
     * Genau wie add, aber mit den Standardwerten iPadX,iPadY=0
     * 
     * @param cont Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp Die Komponente, die hinzugefuegt werden soll
     * @param x X-Position innerhalb des GriBagLayouts
     * @param y Y-Position innerhalb des GriBagLayouts
     * @param width Anzahl der Spalten, ueber die sich diese Komponente erstreckt
     * @param height Anzahl der Reihen, ueber die sich diese Komponente erstreckt
     * @param weightX Verteilung von zur Verfuegung stehendem Platz in X-Richtung
     * @param weightY Verteilung von zur Verfuegung stehendem Platz in Y-Richtung
     * @param insets Abstände der Komponente
     * @param fill Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp, int x, int y, int width, int height, int weightX, int weightY, Insets insets, int fill, int anchor) {
        if (cont == null) {
            logger.severe("Container ==null");
            return;
        }
        if (comp == null) {
            logger.severe("Componente ==null");
            return;
        }
        addToGridBag(cont, comp, x, y, width, height, weightX, weightY, insets, 0, 0, fill, anchor);
    }
    /**
     * Liefert einen Punkt zurück, mit dem eine Komponente auf eine andere zentriert werden kann
     * 
     * @param parent Die Komponente, an der ausgerichtet wird
     * @param child Die Komponente die ausgerichtet werden soll
     * @return Ein Punkt, mit dem diese Komponente mit der setLocation Methode zentriert dargestellt werden kann
     */
    public static Point getCenterOfComponent(Component parent, Component child) {
        Point center;
        if (parent == null || !parent.isShowing()) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width = screenSize.width;
            int height = screenSize.height;
            center = new Point(width / 2, height / 2);
        }
        else {
            center = parent.getLocationOnScreen();
            center.x += parent.getWidth() / 2;
            center.y += parent.getHeight() / 2;
        }
        // Dann Auszurichtende Komponente in die Berechnung einfließen lassen
        center.x -= child.getWidth() / 2;
        center.y -= child.getHeight() / 2;
        return center;
    }
    /**
     * Liefert eine Zeichenkette aus dem aktuellen ResourceBundle zurück
     * 
     * @param key Identifier der gewünschten Zeichenkette
     * @return Die gewünschte Zeichnenkette
     */
    public static String getResourceString(String key) {
        if (resourceBundle == null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            resourceBundle = ResourceBundle.getBundle("LanguagePack", locale);
        }
        String result = key;
        try {
            result = resourceBundle.getString(key);
        }
        catch (MissingResourceException e) {
            logger.warning("resource missing:" + e.getKey());
        }
        return result;
    }
    /**
     * Liefert einer char aus dem aktuellen ResourceBundle zurück
     * 
     * @param key Identifier des gewünschten chars
     * @return der gewünschte char
     */
    public static char getResourceChar(String key) {
        char result = 0;
        String s = getResourceString(key);
        if (s != null && s.length() > 0) {
            result = s.charAt(0);
        }
        return result;
    }
    /**
     * Liefert aus der Map der geladenen Bilder ein Element zurück
     * 
     * @param imageName Name des Bildes das zurückgeliefert werden soll
     * @return Das gewünschte Bild oder null, falls es nicht gefunden werden kann
     */
    public static Image getImage(String imageName) {
        return images.get(imageName);
    }
    /**
     * Fügt ein Bild zur Map hinzu
     * 
     * @param imageName Name des Bildes, daß hinzugefügt werden soll
     * @param image Das hinzuzufügende Bild
     */
    public static void addImage(String imageName, Image image) {
        Toolkit.getDefaultToolkit().prepareImage(image, -1, -1, null);
        images.put(imageName, image);
    }
    /**
     * Liefert das Basisverzeichnis für jD zurück.
     * 
     * @return ein File, daß das Basisverzeichnis angibt
     */
    public static File getJDHomeDirectoryFromEnvironment() {
        String envDir = System.getenv("JD_HOME");
        if (envDir == null) {
            logger.warning("environment variable JD_HOME not set");
            envDir = System.getProperty("user.home") + System.getProperty("file.separator") + ".jd_home/";
            logger.info("JD_HOME from user.home :" + envDir);
        }
        else
            logger.info("JD_HOME from environment variable:" + envDir);
        if (envDir == null) {
            envDir = "." + System.getProperty("file.separator") + ".jd_home/";
            logger.info("JD_HOME from current directory:" + envDir);
        }
        File jdHomeDir = new File(envDir);
        if (!jdHomeDir.exists()) {
            jdHomeDir.mkdirs();
        }
        return jdHomeDir;
    }
    /**
     * Liest JD-HOME aus dem WebStart Cache. Ist ein solcher nicht vorhanden, wird der Pfad aus der Umgebungsvariable genommen. Ist dieser auch nicht vorhanden, wird einfach in das aktuelle
     * Verzeichnis geschrieben
     * 
     * @return Das Homeverzeichnis
     */
    @SuppressWarnings("unchecked")
    public static File getJDHomeDirectory() {
        String homeDir = null;
        if (homeDirectoryFile != null) return homeDirectoryFile;
        try {
            if (Class.forName("javax.jnlp.ServiceManager") != null) {
                Class webStartHelper = Class.forName("jd.JDWebStartHelper");
                Method method = webStartHelper.getDeclaredMethod("getJDHomeDirectoryFromWebStartCookie", new Class[] {});
                homeDir = (String) method.invoke(webStartHelper, (Object[]) null);
            }
        }
        catch (ClassNotFoundException e) {
        }
        catch (SecurityException e) {
        }
        catch (NoSuchMethodException e) {
        }
        catch (IllegalArgumentException e) {
        }
        catch (IllegalAccessException e) {
        }
        catch (InvocationTargetException e) {
        }
        catch (Exception e) {
        }
        if (homeDir != null) {
            setHomeDirectory(homeDir);
            return homeDirectoryFile;
        }
        return getJDHomeDirectoryFromEnvironment();
    }
    /**
     * Lädt eine Klasse aus dem homedir. UNd instanziert sie mit den gegebenen arumenten
     * 
     * @param classPath
     * @param arguments
     * @return
     */
    public static Object getHomeDirInstance(String classPath, Object[] arguments) {
        classPath = classPath.replaceAll("\\.class", "");
        classPath = classPath.replaceAll("\\/", ".");
        classPath = classPath.replaceAll("\\\\", ".");
        logger.finer("Load Class form homedir: " + classPath);
        Class newClass = null;
        // Zuerst versuchen die klasse aus dem appdir zu laden( praktisch zum
        // entwicklen solcher klassen)
        try {
            newClass = Class.forName(classPath);
        }
        catch (ClassNotFoundException e1) {
        }
        // Falls das nicht geklappt hat wird die klasse im homedir gesucht
        if (newClass == null) {
            try {
                String url = urlEncode(new File((getJDHomeDirectory().getAbsolutePath())).toURI().toURL().toString());
                URLClassLoader cl = new URLClassLoader(new URL[] { new URL(url) }, Thread.currentThread().getContextClassLoader());
                newClass = Class.forName(classPath, true, cl);
            }
            catch (ClassNotFoundException e) {
                logger.severe(classPath + ": " + e.getLocalizedMessage());
                e.printStackTrace();
            }
            catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            // newClass = Class.forName(classPath);
            Class[] classes = new Class[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                classes[i] = arguments[i].getClass();
            }
            Constructor con = newClass.getConstructor(classes);
            return con.newInstance(arguments);
        }
        catch (SecurityException e) {
            logger.severe(classPath + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            logger.severe(classPath + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            logger.severe(classPath + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            logger.severe(classPath + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            logger.severe(classPath + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            logger.severe(classPath + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            logger.severe(classPath + ": " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return null;
    }
    /**
     * Schreibt das Home Verzeichnis in den Webstart Cache
     * 
     * @param newHomeDir Das neue JD-HOME
     */
    @SuppressWarnings("unchecked")
    public static void writeJDHomeDirectoryToWebStartCookie(String newHomeDir) {
        try {
            Class webStartHelper = Class.forName("jd.JDWebStartHelper");
            Method method = webStartHelper.getDeclaredMethod("writeJDHomeDirectoryToWebStartCookie", new Class[] { String.class });
            String homeDir = (String) method.invoke(webStartHelper, newHomeDir);
            setHomeDirectory(homeDir);
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    /**
     * Liefert einen URLClassLoader zurück, um Dateien aus dem Stammverzeichnis zu laden
     * 
     * @return URLClassLoader
     */
    public static JDClassLoader getJDClassLoader() {
        if (jdClassLoader == null) {
            File homeDir = getJDHomeDirectory();
            String url = null;
            // Url Encode des pfads für den Classloader
            logger.info("Create Classloader: for: " + homeDir.getAbsolutePath());
            jdClassLoader = new JDClassLoader(homeDir.getAbsolutePath(), Thread.currentThread().getContextClassLoader());
        }
        return jdClassLoader;
    }
    /**
     * Diese Methode erstellt einen neuen Captchadialog und liefert den eingegebenen Text zurück.
     * 
     * @param controller Der Controller
     * @param plugin Das Plugin, das dieses Captcha fordert (Der Host wird benötigt)
     * @param file
     * @return Der vom Benutzer eingegebene Text
     */
    public static String getCaptcha(JDController controller, Plugin plugin, File file) {
        if (controller == null) controller = getController();
        String host = plugin.getHost();
        logger.info("JAC has Method for: " + host + ": " + JAntiCaptcha.hasMethod(getJACMethodsDirectory(), host));
        if (JAntiCaptcha.hasMethod(getJACMethodsDirectory(), host)) {
            JFrame jf = new JFrame();
            Image captchaImage = new JFrame().getToolkit().getImage(file.getAbsolutePath());
            MediaTracker mediaTracker = new MediaTracker(jf);
            mediaTracker.addImage(captchaImage, 0);
            try {
                mediaTracker.waitForID(0);
            }
            catch (InterruptedException e) {
                return null;
            }
            mediaTracker.removeImage(captchaImage);
            JAntiCaptcha jac = new JAntiCaptcha(getJACMethodsDirectory(), host);
            Captcha captcha = jac.createCaptcha(captchaImage);
            String captchaCode = jac.checkCaptcha(captcha);
            logger.info(captchaCode);
            return captchaCode;
        }
        else {
            return controller.getCaptchaCodeFromUser(plugin, file);
        }
    }
    /**
     * Hier werden alle Plugins aus dem Classpath(".") geparsed 
     */
    @SuppressWarnings("unchecked")
    public static void loadPlugins() {
        getJDClassLoader();
        Iterator iterator;
        // Zuerst Plugins zum Dekodieren verschlüsselter Links
        iterator = Service.providers(PluginForDecrypt.class);
        while (iterator.hasNext()) {
            PluginForDecrypt p = (PluginForDecrypt) iterator.next();
            pluginsForDecrypt.add(p);
            logger.info("Decrypt-Plugin    : " + p.getPluginName());
        }
        // Danach die Plugins der verschiedenen Anbieter
        iterator = Service.providers(PluginForHost.class);
        while (iterator.hasNext()) {
            PluginForHost p = (PluginForHost) iterator.next();
            pluginsForHost.add(p);
            logger.info("Host-Plugin       : " + p.getPluginName());
        }
        // Danach die Plugins der verschiedenen Suchengines
        iterator = Service.providers(PluginForSearch.class);
        while (iterator.hasNext()) {
            PluginForSearch p = (PluginForSearch) iterator.next();
            pluginsForSearch.add(p);
            logger.info("Search-Plugin     : " + p.getPluginName());
        }
        iterator = Service.providers(PluginOptional.class, jdClassLoader);
        while (iterator.hasNext()) {
            try {
                PluginOptional p = (PluginOptional) iterator.next();
                pluginsOptional.put(p.getPluginName(), p);
                logger.info("Optionales-Plugin : " + p.getPluginName());
            }
            catch (Error e) {
                e.printStackTrace();
            }
        }
        // ContainerKlassen
        iterator = Service.providers(ClassLoader.class);
        while (iterator.hasNext()) {
            ClassLoader cl = (ClassLoader) iterator.next();
            // Danach die Plugins für die unterschiedlichen Container
            Iterator iteratorClass = Service.providers(PluginForContainer.class, cl);
            while (iteratorClass.hasNext()) {
                PluginForContainer p = (PluginForContainer) iteratorClass.next();
                pluginsForContainer.add(p);
                logger.info("Container-Plugin : " + p.getPluginName());
            }
        }
    }
    /**
     * Fügt einen PluginListener hinzu
     * 
     * @param listener
     */
    public static void registerListenerPluginsForDecrypt(PluginListener listener) {
        Iterator<PluginForDecrypt> iterator = pluginsForDecrypt.iterator();
        while (iterator.hasNext()) {
            iterator.next().addPluginListener(listener);
        }
    }
    /**
     * Fügt einen PluginListener hinzu
     * 
     * @param listener
     */
    public static void registerListenerPluginsForHost(PluginListener listener) {
        Iterator<PluginForHost> iterator = pluginsForHost.iterator();
        while (iterator.hasNext()) {
            iterator.next().addPluginListener(listener);
        }
    }
    /**
     * Fügt einen PluginListener hinzu
     * 
     * @param listener TODO: unused
     */
    public static void registerListenerPluginsForSearch(PluginListener listener) {
        Iterator<PluginForSearch> iterator = pluginsForSearch.iterator();
        while (iterator.hasNext()) {
            iterator.next().addPluginListener(listener);
        }
    }
    /**
     * Fügt einen PluginListener hinzu TODO: unused
     * 
     * @param listener
     */
    public static void registerListenerPluginsForContainer(PluginListener listener) {
        Iterator<PluginForContainer> iterator = pluginsForContainer.iterator();
        while (iterator.hasNext()) {
            iterator.next().addPluginListener(listener);
        }
    }
    /*
     * TODO: unused
     */
    public static void registerListenerPluginsOptional(PluginListener listener) {
        Iterator<String> iterator = pluginsOptional.keySet().iterator();
        while (iterator.hasNext()) {
            pluginsOptional.get(iterator.next()).addPluginListener(listener);
        }
    }
    /**
     * Lädt ein Objekt aus einer Datei
     * 
     * @param frame Ein übergeordnetes Fenster
     * @param fileInput Falls das Objekt aus einer bekannten Datei geladen werden soll, wird hier die Datei angegeben. Falls nicht, kann der Benutzer über einen Dialog eine Datei aussuchen
     * @param asXML Soll das Objekt von einer XML Datei aus geladen werden?
     * @return Das geladene Objekt
     */
    public static Object loadObject(JFrame frame, File fileInput, boolean asXML) {
        logger.info("load file: " + fileInput + " (xml:" + asXML + ")");
        Object objectLoaded = null;
        if (fileInput == null) {
            JFileChooser fileChooserLoad = new JFileChooser();
            if (currentDirectory != null) fileChooserLoad.setCurrentDirectory(currentDirectory);
            if (fileChooserLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                fileInput = fileChooserLoad.getSelectedFile();
                currentDirectory = fileChooserLoad.getCurrentDirectory();
            }
        }
        if (fileInput != null) {
            String hash = getLocalHash(fileInput);
            try {
                FileInputStream fis = new FileInputStream(fileInput);
                if (asXML) {
                    XMLDecoder xmlDecoder = new XMLDecoder(new BufferedInputStream(fis));
                    objectLoaded = xmlDecoder.readObject();
                    xmlDecoder.close();
                }
                else {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    objectLoaded = ois.readObject();
                    ois.close();
                }
                // Object15475dea4e088fe0e9445da30604acd1
                // Object80d11614908074272d6b79abe91eeca1
                logger.info("Loaded Object (" + hash + "): ");
                return objectLoaded;
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    /**
     * Speichert ein Objekt
     * 
     * @param frame ein Fenster
     * @param objectToSave Das zu speichernde Objekt
     * @param fileOutput Das File, in das geschrieben werden soll. Falls das File ein Verzeichnis ist, wird darunter eine Datei erstellt Falls keins angegeben wird, soll der Benutzer eine Datei
     *            auswählen
     * @param name Dateiname
     * @param extension Dateiendung (mit Punkt)
     * @param asXML Soll das Objekt in eine XML Datei gespeichert werden?
     */
    public static void saveObject(JFrame frame, Object objectToSave, File fileOutput, String name, String extension, boolean asXML) {
        String hashPre;
        if (fileOutput == null) {
            JDFileFilter fileFilter = new JDFileFilter(name, extension, true);
            JFileChooser fileChooserSave = new JFileChooser();
            fileChooserSave.setFileFilter(fileFilter);
            fileChooserSave.setSelectedFile(fileFilter.getFile());
            if (currentDirectory != null) fileChooserSave.setCurrentDirectory(currentDirectory);
            if (fileChooserSave.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                fileOutput = fileChooserSave.getSelectedFile();
                currentDirectory = fileChooserSave.getCurrentDirectory();
            }
        }
        if (fileOutput != null) {
            if (fileOutput.isDirectory()) {
                fileOutput = new File(fileOutput, name + extension);
                logger.info("save file: " + fileOutput + " (xml:" + asXML + ") object: " + objectToSave + " - " + extension);
            }
            hashPre = getLocalHash(fileOutput);
            if (fileOutput.exists()) fileOutput.delete();
            try {
                FileOutputStream fos = new FileOutputStream(fileOutput);
                if (asXML) {
                    XMLEncoder xmlEncoder = new XMLEncoder(new BufferedOutputStream(fos));
                    xmlEncoder.writeObject(objectToSave);
                    xmlEncoder.close();
                }
                else {
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(objectToSave);
                    oos.close();
                }
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            String hashPost = getLocalHash(fileOutput);
            if (hashPost == null) {
                logger.severe("Schreibfehler: " + fileOutput + " Datei wurde nicht erstellt");
            }
            else if (hashPost.equals(hashPre)) {
                logger.warning("Schreibvorgang: " + fileOutput + " Datei wurde nicht überschrieben");
            }
            else {
                logger.finer("Schreibvorgang: " + fileOutput + " erfolgreich: " + hashPost);
            }
        }
        else {
            logger.severe("Schreibfehler: Fileoutput: null");
        }
    }
    /**
     * Formatiert Sekunden in das zeitformat stunden:minuten:sekunden
     * 
     * @param eta toURI().toURL();
     * @return formatierte Zeit
     */
    public static String formatSeconds(int eta) {
        int hours = eta / (60 * 60);
        eta -= hours * 60 * 60;
        int minutes = eta / 60;
        int seconds = eta - minutes * 60;
        if (hours == 0) {
            return fillInteger(minutes, 2, "0") + ":" + fillInteger(seconds, 2, "0");
        }
        return fillInteger(hours, 2, "0") + ":" + fillInteger(minutes, 2, "0") + ":" + fillInteger(seconds, 2, "0");
    }
    /**
     * Hängt an i solange fill vorne an bis die zechenlänge von i gleich num ist
     * 
     * @param i
     * @param num
     * @param fill
     * @return aufgefüllte Zeichenkette
     */
    public static String fillInteger(int i, int num, String fill) {
        String ret = "" + i;
        while (ret.length() < num)
            ret = fill + ret;
        return ret;
    }
    /**
     * Liefert alle geladenen Plugins zum Entschlüsseln zurück
     * 
     * @return Plugins zum Entschlüsseln
     */
    public static Vector<PluginForDecrypt> getPluginsForDecrypt() {
        return pluginsForDecrypt;
    }
    /**
     * Liefert alle geladenen Plugins zum Suchen zurück
     * 
     * @return Plugins zum Suchen
     */
    public static Vector<PluginForSearch> getPluginsForSearch() {
        return pluginsForSearch;
    }
    /**
     * Liefert alle geladenen Plugins zum Laden von Containerdateien zurück
     * 
     * @return Plugins zum Laden von Containerdateien
     */
    public static Vector<PluginForContainer> getPluginsForContainer() {
        return pluginsForContainer;
    }
    /**
     * Gibt alle Ketegorien zurück für die Suchplugins exestieren. Die Kategorien werden in den Plugins selbst als String definiert
     * 
     * @return Alle Search kategorien
     */
    public static Vector<String> getPluginsForSearchCategories() {
        Vector<String> ret = new Vector<String>();
        for (int i = 0; i < pluginsForSearch.size(); i++) {
            for (int b = 0; b < pluginsForSearch.get(i).getCategories().length; b++) {
                if (ret.indexOf(pluginsForSearch.get(i).getCategories()[b]) < 0) ret.add(pluginsForSearch.get(i).getCategories()[b]);
            }
        }
        Collections.sort(ret);
        return ret;
    }
    /**
     * Liefert alle Plugins zum Downloaden von einem Anbieter zurück
     * 
     * @return Plugins zum Downloaden von einem Anbieter
     */
    public static Vector<PluginForHost> getPluginsForHost() {
        return pluginsForHost;
    }
    /**
     * Liefert alle optionalen Plugins zurücl
     * 
     * @return Alle optionalen Plugins
     */
    public static HashMap<String, PluginOptional> getPluginsOptional() {
        return pluginsOptional;
    }
    /**
     * Gibt den MD5 hash eines Strings zurück
     * 
     * @param arg
     * @return MD% hash von arg
     */
    public static String getMD5(String arg) {
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] digest = md.digest(arg.getBytes());
            String ret = "";
            for (byte d : digest) {
                ret += Integer.toHexString(d & 0xFF);
            }
            return ret;
        }
        catch (NoSuchAlgorithmException e) {
        }
        return "";
    }
    /**
     * Sucht ein passendes Plugin für einen Anbieter
     * 
     * @param host Der Host, von dem das Plugin runterladen kann
     * @return Ein passendes Plugin oder null
     */
    public static PluginForHost getPluginForHost(String host) {
        for (int i = 0; i < pluginsForHost.size(); i++) {
            if (pluginsForHost.get(i).getHost().equals(host)) return pluginsForHost.get(i);
        }
        return null;
    }
    public static void loadActivePlugins() {
        File[] plugins = getAvailableUserPlugins();
        if (plugins == null) return;
        for (int i = 0; i < plugins.length; i++) {
            String param = "OPTIONAL_PLUGIN_" + plugins[i].getName();
            boolean b = getConfiguration().getBooleanProperty(param, false);
            if (b) {
                URL url;
                try {
                    url = plugins[i].toURI().toURL();
                    URL u = new URL("jar", "", url + "!/");
                    new JarLoader(u);
                    // new JarClassLoader(u);
                }
                catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    public static File[] getAvailableUserPlugins() {
        File pluginDir = getResourceFile("/plugins/");
        logger.info(pluginDir + "");
        File[] contents = pluginDir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) {
                logger.info(new File(d, name) + " ");
                return name.endsWith(".jar") && new File(d, name).isFile();
            }
        });
        if (contents == null) {
            logger.info("Kein UserPlugin Verzeichnis gefunden: " + pluginDir);
            return contents;
        }
        logger.info(" UserPlugin Verzeichnis gefunden: " + contents);
        return contents;
        //        
        // UserPlugin tmp = null;
        // for (int i = 0; i < contents.length; i++) {
        // logger.info("lade jd/plugins/" + contents[i].getName());
        // try {
        // tmp = (UserPlugin) getHomeDirInstance("jd/plugins/" +
        // contents[i].getName(), new Object[] {});
        // }
        // catch (Exception e) {
        // logger.severe("Fehler beim laden eines UserPlugins: " + contents[i] +
        // " : " + e.getLocalizedMessage());
        // }
        // if (tmp != null) {
        // ret.add(tmp);
        // tmp.addPluginListener(JDUtilities.getController());
        // tmp.enable(true);
        // tmp = null;
        // }
        // }
    }
    /**
     * Sucht ein passendes Plugin für ein Containerfile
     * 
     * @param container Der Host, von dem das Plugin runterladen kann
     * @return Ein passendes Plugin oder null
     */
    public static PluginForContainer getPluginForContainer(String container) {
        for (int i = 0; i < pluginsForContainer.size(); i++) {
            if (pluginsForContainer.get(i).getHost().equals(container)) return pluginsForContainer.get(i);
        }
        return null;
    }
    /**
     * @return Configuration instanz
     */
    public static Configuration getConfiguration() {
        return configuration;
    }
    /**
     * Setzt die Konfigurations instanz
     * 
     * @param configuration
     */
    public static void setConfiguration(Configuration configuration) {
        JDUtilities.configuration = configuration;
    }
    /**
     * @author astaldo
     * @return homeDirectory
     */
    public static String getHomeDirectory() {
        return homeDirectory;
    }
    /**
     * Diese Funktion gibt den Pfad zum JAC-Methodenverzeichniss zurück
     * 
     * @author coalado
     * @return gibt den Pfad zu den JAC Methoden zurück
     */
    public static String getJACMethodsDirectory() {
        String sep = System.getProperty("file.separator");
        return getJDHomeDirectory() + sep + "jd" + sep + "captcha" + sep + "methods";
    }
    /**
     * Gibt ein FileOebject zu einem Resourcstring zurück
     * 
     * @author coalado
     * @param resource Ressource, die geladen werden soll
     * @return File zu arg
     */
    public static File getResourceFile(String resource) {
        JDClassLoader cl = getJDClassLoader();
        if (cl == null) {
            logger.severe("Classloader ==null: ");
            return null;
        }
        URL clURL = getJDClassLoader().getResource(resource);
        if (clURL != null) {
            try {
                return new File(clURL.toURI());
            }
            catch (URISyntaxException e) {
            }
        }
        return null;
    }
    /**
     * public static String getLocalHash(File f) Gibt einen MD% Hash der file zurück
     * 
     * @author coalado
     * @param f
     * @return Hashstring Md5
     */
    public static String getLocalHash(File f) {
        try {
            if (!f.exists()) return null;
            MessageDigest md;
            md = MessageDigest.getInstance("md5");
            byte[] b = new byte[1024];
            InputStream in = new FileInputStream(f);
            for (int n = 0; (n = in.read(b)) > -1;) {
                md.update(b, 0, n);
            }
            byte[] digest = md.digest();
            String ret = "";
            for (int i = 0; i < digest.length; i++) {
                String tmp = Integer.toHexString(digest[i] & 0xFF);
                if (tmp.length() < 2) tmp = "0" + tmp;
                ret += tmp;
            }
            in.close();
            return ret;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * @author coalado Macht ein urlRawEncode und spart dabei die angegebenen Zeichen aus
     * @param str
     * @return str URLCodiert
     */
    public static String urlEncode(String str) {
        try {
            str = URLDecoder.decode(str, "UTF-8");
            String allowed = "1234567890QWERTZUIOPASDFGHJKLYXCVBNMqwertzuiopasdfghjklyxcvbnm-_.?/\\:&=;";
            String ret = "";
            int i;
            for (i = 0; i < str.length(); i++) {
                char letter = str.charAt(i);
                if (allowed.indexOf(letter) >= 0) {
                    ret += letter;
                }
                else {
                    ret += "%" + Integer.toString(letter, 16);
                }
            }
            return ret;
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }
    /**
     * "http://rapidshare.com&#x2F;&#x66;&#x69;&#x6C;&#x65;&#x73;&#x2F;&#x35;&#x34;&#x35;&#x34;&#x31;&#x34;&#x38;&#x35;&#x2F;&#x63;&#x63;&#x66;&#x32;&#x72;&#x73;&#x64;&#x66;&#x2E;&#x72;&#x61;&#x72;";
     * Wandelt alle hexkodierten zeichen in diesem Format in normalen text um
     * 
     * @param str
     * @return decoded string
     */
    public static String htmlDecode(String str) {
        // http://rs218.rapidshare.com/files/&#0052;&#x0037;&#0052;&#x0034;&#0049;&#x0032;&#0057;&#x0031;/STE_S04E04.Borderland.German.dTV.XviD-2Br0th3rs.part1.rar
        if (str == null) return null;
        String pattern = "\\&\\#x(.*?)\\;";
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str); r.find();) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1), 16);
                str = str.replaceFirst("\\&\\#x(.*?)\\;", c + "");
            }
        }
        pattern = "\\&\\#(.*?)\\;";
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str); r.find();) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1), 10);
                str = str.replaceFirst("\\&\\#(.*?)\\;", c + "");
            }
        }
        try {
            str = URLDecoder.decode(str, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
        }
        return HTMLEntities.unhtmlentities(str);
    }
    /**
     * Schreibt content in eine Lokale textdatei
     * 
     * @param file
     * @param content
     * @return true/False je nach Erfolg des Schreibvorgangs
     */
    public static boolean writeLocalFile(File file, String content) {
        try {
            if (file.isFile()) {
                if (!file.delete()) {
                    logger.severe("Konnte Datei nicht löschen " + file);
                    return false;
                }
            }
            if (file.getParent() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            BufferedWriter f = new BufferedWriter(new FileWriter(file));
            f.write(content);
            f.close();
            return true;
        }
        catch (Exception e) {
            // e.printStackTrace();
            return false;
        }
    }
    /**
     * @author coalado
     * @param str
     * @return str als UTF8Decodiert
     */
    public static String UTF8Decode(String str) {
        try {
            return new String(str.getBytes(), "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * @author coalado
     * @param str
     * @return str als UTF8 Kodiert
     */
    public static String UTF8Encode(String str) {
        try {
            return new String(str.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Setzt das Homedirectory und erstellt es notfalls neu
     * 
     * @param homeDirectory
     */
    public static void setHomeDirectory(String homeDirectory) {
        JDUtilities.homeDirectory = homeDirectory;
        homeDirectoryFile = new File(homeDirectory);
        if (!homeDirectoryFile.exists()) homeDirectoryFile.mkdirs();
    }
    /**
     * Lädt eine url lokal herunter
     * 
     * @param file
     * @param urlString
     * @return Erfolg true/false
     */
    public static boolean download(File file, String urlString) {
        try {
            urlString = URLDecoder.decode(urlString, "UTF-8");
            URL url = new URL(urlString);
            URLConnection con = url.openConnection();
            return download(file, con);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * Lädt über eine URLConnection eine datei ehrunter. Zieldatei ist file.
     * 
     * @param file
     * @param con
     * @return Erfolg true/false
     */
    public static boolean download(File file, URLConnection con) {
        try {
            if (file.isFile()) {
                if (!file.delete()) {
                    logger.severe("Konnte Datei nicht überschreiben " + file);
                    return false;
                }
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            BufferedInputStream input = new BufferedInputStream(con.getInputStream());
            byte[] b = new byte[1024];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
            }
            output.close();
            input.close();
            return true;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * TODO: Serverpfad in de Config aufnehmen Gleicht das homedir mit dem server ab. Der Serverpfad steht noch in WebUpdater.java
     * 
     * @author coalado
     * @return Anzahl der aktualisierten Files
     */
    public static int doWebupdate() {
        WebUpdater wu = new WebUpdater(null);
        wu.run();
        return wu.getUpdatedFiles();
    }
    /**
     * Prüft anhand der Globalen IP Check einstellungen die IP
     * 
     * @return ip oder /offline
     */
    public static String getIPAddress() {
        String site = getConfiguration().getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, "http://www.meineip.de");
        String patt = getConfiguration().getStringProperty(Configuration.PARAM_GLOBAL_IP_PATTERN, "\\Q<td><b>\\E([0-9.]*)\\Q</b></td>\\E");
        try {
            logger.finer("IP Check via " + site);
            RequestInfo requestInfo = Plugin.getRequest(new URL(site), null, null, true);
            Pattern pattern = Pattern.compile(patt);
            Matcher matcher = pattern.matcher(requestInfo.getHtmlCode());
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        }
        catch (IOException e1) {
            logger.severe("url not found. " + e1.toString());
        }
        return "offline";
    }
    /**
     * Führt einen Externen befehl aus.
     * 
     * @param command
     * @param parameter
     * @param runIn
     * @param waitForReturn
     * @return null oder die rückgabe des befehls falls waitforreturn == true ist
     */
    public static String runCommand(String command, String parameter, String runIn, int waitForReturn) {
        String[] params = (command + " " + parameter).split("\\ ");
        ProcessBuilder pb = new ProcessBuilder(params);
        if (runIn != null && runIn.length() > 0) {
            if (new File(runIn).exists()) {
                pb.directory(new File(runIn));
            }
            else {
                logger.severe("Working drectory " + runIn + " does not exist!");
            }
        }
        Process process;
        try {
            logger.finer("Start " + command + " " + parameter + " in " + runIn + " wait " + waitForReturn);
            process = pb.start();
            if (waitForReturn > 0) {
                long t = System.currentTimeMillis();
                while (true) {
                    try {
                        process.exitValue();
                        break;
                    }
                    catch (Exception e) {
                        if (System.currentTimeMillis() - t > waitForReturn * 1000) {
                            logger.severe(command + ": Prozess ist nach " + waitForReturn + " Sekunden nicht beendet worden. Breche ab.");
                            process.destroy();
                        }
                    }
                }
                Scanner s = new Scanner(process.getInputStream()).useDelimiter("\\Z");
                String ret = "";
                while (s.hasNext())
                    ret += s.next();
                return ret;
            }
            return null;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.severe("Error executing " + command + ": " + e.getLocalizedMessage());
            return null;
        }
    }
    /**
     * Gibt den verwendeten Controller zurück
     * 
     * @return gerade verwendete controller-instanz
     */
    public static JDController getController() {
        return controller;
    }
    /**
     * Setzt den Controller
     * 
     * @param con controller
     */
    public static void setController(JDController con) {
        controller = con;
    }
    /**
     * @return Gibt die verwendete java Version als Double Value zurück. z.B. 1.603
     */
    public static Double getJavaVersion() {
        String version = System.getProperty("java.version");
        int majorVersion = JDUtilities.filterInt(version.substring(0, version.indexOf(".")));
        int subversion = JDUtilities.filterInt(version.substring(version.indexOf(".") + 1));
        return Double.parseDouble(majorVersion + "." + subversion);
    }
    /**
     * Ersetzt die Platzhalter in einem String
     * 
     * @param command
     * @return Neuer String mit ersetzen Platzhaltern
     */
    public static String replacePlaceHolder(String command) {
        if (controller == null) return command;
        command = command.replaceAll("\\%LASTFILE", controller.getLastFinishedFile());
        command = command.replaceAll("\\%CAPTCHAIMAGE", controller.getLastCaptchaImage());
        return command;
    }
    /**
     * Formatiert Byes in einen MB String [MM.MM MB]
     * 
     * @param downloadMax
     * @return MegaByte Formatierter String
     */
    public static String formatBytesToMB(long downloadMax) {
        return Math.round(downloadMax / (1024 * 10.24)) / 100.0 + " MB";
    }
    /**
     * GIbt den Integer der sich in src befindet zurück. alle nicht integerzeichen werden ausgefiltert
     * 
     * @param src
     * @return Integer in src
     */
    public static int filterInt(String src) {
        try {
            return Integer.parseInt(filterString(src, "1234567890"));
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }
    /**
     * Filtert alle nicht lesbaren zeichen aus str
     * 
     * @param str
     * @return
     */
    public static String filterString(String str) {
        String allowed = "QWERTZUIOPÜASDFGHJKLÖÄYXCVBNMqwertzuiopasdfghjklyxcvbnm;:,._-&%(){}#~+ 1234567890";
        return filterString(str, allowed);
    }
    /**
     * Filtert alle zeichen aus str die in filter nicht auftauchen
     * 
     * @param str
     * @param filter
     * @return
     */
    public static String filterString(String str, String filter) {
        if (str == null || filter == null) return "";
        String allowed = filter;
        String ret = "";
        int i;
        for (i = 0; i < str.length(); i++) {
            char letter = str.charAt(i);
            if (allowed.indexOf(letter) >= 0) {
                ret += letter;
            }
        }
        return ret;
    }
    /**
     * Untersucht zwei String, ob zwei String ähnlich anfangen. Der übereinstimmende Text wird dann zurückgegeben
     * 
     * @param a Erster String, der vergleicht werden soll
     * @param b Zweiter String, der vergleicht werden soll
     * @return Übereinstimmender Text
     */
    public static String getEqualString(String a, String b) {
        String first, second;
        int index = 0;
        if (a.length() <= b.length()) {
            first = a.toLowerCase();
            second = b.toLowerCase();
        }
        else {
            first = b;
            second = a;
        }
        for (int i = 0; i < first.length(); i++) {
            if (first.charAt(i) == second.charAt(i))
                index = i;
            else
                break;
        }
        if (index > 0)
            return first.substring(0, index + 1);
        else
            return "";
    }
    public static String getJDTitle() {
        return JDUtilities.JD_TITLE + " " + JDUtilities.JD_VERSION + JDUtilities.getRevision() + " (" + JDUtilities.getLastChangeDate() + " " + JDUtilities.getLastChangeTime() + ")";
    }
    /**
     * Fügt dem Dateinamen den erkannten Code noch hinzu
     * 
     * @param file Die Datei, der der Captchacode angefügt werden soll
     * @param captchaCode Der erkannte Captchacode
     * @param isGood Zeigt, ob der erkannte Captchacode korrekt ist
     */
    public static void appendInfoToFilename(File file, String captchaCode, boolean isGood) {
        String dest = file.getAbsolutePath();
        String isGoodText;
        if (isGood)
            isGoodText = "_GOOD";
        else
            isGoodText = "_BAD";
        int idx = dest.lastIndexOf('.');
        dest = dest.substring(0, idx) + "_" + captchaCode.toUpperCase() + isGoodText + dest.substring(idx);
        file.renameTo(new File(dest));
    }
    public static Locale getLocale() {
        return locale;
    }
    public static void setLocale(Locale locale) {
        JDUtilities.locale = locale;
    }
}
