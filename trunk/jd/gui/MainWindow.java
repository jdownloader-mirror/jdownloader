package jd.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.event.TableModelEvent;

import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
import sun.misc.Service;

public class MainWindow extends JFrame implements PluginListener, ClipboardOwner{
   
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3966433144683787356L;
    
    private static final StringSelection JDOWNLOADER_ID = new StringSelection("JDownloader active");
    /**
     * Toolleiste f�r Kn�pfe
     */
    private JToolBar          toolBar           = new JToolBar();
    /**
     * Tabelle mit den Downloadlinks
     */
    private DownloadLinkTable downloadLinkTable = new DownloadLinkTable();
    /**
     * Scrollkomponente f�r die Tabelle
     */
    private JScrollPane       scrollPane        = new JScrollPane(downloadLinkTable);
    /**
     * Fortschrittsanzeige
     */
    private JProgressBar      progressBar       = new JProgressBar();
    /**
     * Hier werden alle vorhandenen Plugins zum Dekodieren von Links gespeichert
     */
    private Vector<PluginForDecrypt> pluginsForDecrypt = new Vector<PluginForDecrypt>();
    /**
     * Hier werden alle Plugins f�r die Anbieter gespeichert
     */
    private Vector<PluginForHost>    pluginsForHost    = new Vector<PluginForHost>();
    /**
     * Alle verf�gbaren Bilder werden hier gespeichert
     */
    private HashMap<String, ImageIcon> images = new HashMap<String, ImageIcon>();
    /**
     * Logger f�r Meldungen des Programmes
     */
    private Logger logger = Logger.getLogger(Plugin.LOGGER_NAME);
    
    private JTabbedPane tabbedPane              = new JTabbedPane();
    private JPanel      panelForDownloadTable   = new JPanel();
    private JPanel      panelForPluginsHost     = new JPanel();
    private JPanel      panelForPluginsDecrypt  = new JPanel();
    private JButton start;
    /**
     * Das Hauptfenster wird erstellt
     */
    public MainWindow(){
        loadImages();
        buildUI();
        getPlugins();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(JDOWNLOADER_ID, this);

        setSize(500,300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    /**
     * Hier wird die komplette Oberfl�che der Applikation zusammengestrickt 
     */
    private void buildUI(){
        tabbedPane = new JTabbedPane();
        panelForDownloadTable = new JPanel(new BorderLayout());
        panelForDownloadTable.add(scrollPane);
        
        tabbedPane.addTab("Downloads",            panelForDownloadTable);
        tabbedPane.addTab("Anbieter-Plugins",     panelForPluginsHost);
        tabbedPane.addTab("Entschl�ssel-Plugins", panelForPluginsDecrypt);
        
        progressBar.setStringPainted(true);
        
        start = new JButton(images.get("start")); 
        start.setFocusPainted(false);start.setBorderPainted(false);
        
        toolBar.setFloatable(false);
        toolBar.add(start);

        setLayout(new GridBagLayout());
        Utilities.addToGridBag(this, toolBar,     0, 0, 1, 1, 0, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH); 
        Utilities.addToGridBag(this, tabbedPane,  0, 1, 1, 1, 1, 1, null, GridBagConstraints.BOTH,       GridBagConstraints.CENTER); 
        Utilities.addToGridBag(this, progressBar, 0, 2, 1, 1, 1, 1, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.SOUTH); 
    }
    /**
     * Die Bilder werden aus der JAR Datei nachgeladen
     */
    private void loadImages(){
        ClassLoader cl = getClass().getClassLoader();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        images.put("start", new ImageIcon(toolkit.getImage(cl.getResource("GIF/start.gif"))));
    }
    /**
     * Hier werden alle Plugins im aktuellen Verzeichnis geparsed (und im Classpath)
     */
    private void getPlugins(){
        Iterator iterator;
        
        //Zuerst Plugins zum Dekodieren verschl�sselter Links
        iterator = Service.providers(PluginForDecrypt.class);
        while(iterator.hasNext())
        {
            PluginForDecrypt p = (PluginForDecrypt) iterator.next();
            pluginsForDecrypt.add(p);
            p.addPluginListener(this);
        }

        //Danach die Plugins der verschiedenen Anbieter
        iterator = Service.providers(PluginForHost.class);
        while(iterator.hasNext())
        {
            PluginForHost p = (PluginForHost) iterator.next();
            pluginsForHost.add(p);
            p.addPluginListener(this);
        }
    }
    /**
     * Reagiert auf Pluginevents
     */
    public void pluginEvent(PluginEvent event) {
        switch(event.getEventID()){
            case PluginEvent.PLUGIN_PROGRESS_MAX:
                progressBar.setMaximum((Integer)event.getParameter());
                repaint();
                break;
            case PluginEvent.PLUGIN_PROGRESS_INCREASE:
                progressBar.setValue(progressBar.getValue()+1);
                repaint();
                break;
        }
    }
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        new ClipboardHandler().start();
    }
    private class ClipboardHandler extends Thread{
        public void run(){
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            synchronized (clipboard) {
                try {
                    clipboard.wait(500);
                }
                catch (InterruptedException e) { }
                try {
                    String data = (String)clipboard.getData(DataFlavor.stringFlavor);
                    new DistributeData(data).start();
//                    System.out.println(data);
                }
                catch (UnsupportedFlavorException e1) {}
                catch (IOException e1)                {}

                clipboard.setContents(JDOWNLOADER_ID, MainWindow.this);
            }
        }
    }

    /**
     * Diese Klasse l�uft in einem Thread und verteilt den Inhalt der Zwischenablage an (unter Umst�nden auch mehrere) Plugins
     * Die gefundenen Treffer werden ausgeschnitten.
     * 
     * @author astaldo
     */
    private class DistributeData extends Thread{
        private String data;
        /**
         * Erstellt einen neuen Thread mit dem Text, der verteilt werden soll
         * 
         * @param data Daten, die verteilt werden sollen
         */
        public DistributeData (String data){
            this.data = data;
        }
        public void run(){
            Vector<DownloadLink> links    = new Vector<DownloadLink>();
            Vector<String> cryptedLinks   = new Vector<String>();
            Vector<String> decryptedLinks = new Vector<String>();
            PluginForDecrypt pDecrypt;
            PluginForHost    pHost;

            // Zuerst wird �berpr�ft, ob ein Decrypt-Plugin einen Teil aus der
            // Zwischenablage entschl�sseln kann. Ist das der Fall, wird die entsprechende Stelle
            // verarbeitet und gel�scht, damit sie keinesfalls nochmal verarbeitet wird.
            for(int i=0; i<pluginsForDecrypt.size();i++){
                pDecrypt = pluginsForDecrypt.elementAt(i);
                if(pDecrypt.isClipboardEnabled() && pDecrypt.canHandle(data)){
                    cryptedLinks.addAll(pDecrypt.getMatches(data));
                    data = pDecrypt.cutMatches(data);
                    decryptedLinks.addAll(pDecrypt.decryptLinks(cryptedLinks));
                }
            }
            // Danach wird der (noch verbleibende) Inhalt der Zwischenablage an die Plugins der Hoster geschickt.
            for(int i=0; i<pluginsForHost.size();i++){
                pHost = pluginsForHost.elementAt(i);
                if(pHost.isClipboardEnabled() && pHost.canHandle(data)){
                    links.addAll(pHost.getDownloadLinks(data));
                    data = pHost.cutMatches(data);
                }
            }
            // Als letztes werden die entschl�sselten Links (soweit �berhaupt vorhanden)
            // an die HostPlugins geschickt, damit diese einen Downloadlink erstellen k�nnen
            Iterator<String> iterator = decryptedLinks.iterator();
            while(iterator.hasNext()){
                String decrypted = iterator.next();
                for(int i=0; i<pluginsForHost.size();i++){
                    pHost = pluginsForHost.elementAt(i);
                    if(pHost.isClipboardEnabled() && pHost.canHandle(decrypted)){
                        links.addAll(pHost.getDownloadLinks(decrypted));
                        iterator.remove();
                    }
                }
            }

            if(links!=null && links.size()>0){
                downloadLinkTable.addLinks(links);
                downloadLinkTable.tableChanged(new TableModelEvent(downloadLinkTable.getModel()));
            }
        }
    }
}
