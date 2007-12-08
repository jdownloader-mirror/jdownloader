package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.util.Locale;
import java.util.logging.Level;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.UIManager;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.event.UIEvent;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.BrowseFile;
import jd.utils.JDUtilities;

public class ConfigPanelCaptcha extends ConfigPanel {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
    private JLabel            lblHomeDir;
    private BrowseFile        brsHomeDir;
    private Configuration     configuration;
    ConfigPanelCaptcha(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();
        load();
    }
    public void save() {
        this.saveConfigEntries();
        JDUtilities.getLogger().setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL));
        if (JDUtilities.getHomeDirectory() != null && !JDUtilities.getHomeDirectory().equalsIgnoreCase(brsHomeDir.getText().trim())) {
            JDUtilities.writeJDHomeDirectoryToWebStartCookie(brsHomeDir.getText().trim());
            if (uiinterface.showConfirmDialog("Installationsverzeichnis geändert. Soll ein Webupdate durchgeführt werden um das neue Verzeichnis zu aktualisieren(empfohlen)?")) {
                uiinterface.fireUIEvent(new UIEvent(uiinterface, UIEvent.UI_INTERACT_UPDATE));
            }
        }
    }
    @Override
    public void initPanel() {
        GUIConfigEntry ce;
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_LOCALE, new Locale[] { Locale.GERMAN, Locale.ENGLISH }, "Sprache").setDefaultValue(Locale.getDefault()));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_LOGGER_LEVEL, new Level[] { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF }, "Level für's Logging").setDefaultValue(Level.FINER).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, configuration, Configuration.PARAM_DOWNLOAD_DIRECTORY, "Default Download Verzeichnis").setDefaultValue(JDUtilities.getJDHomeDirectory().getAbsolutePath()));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, new String[] { Configuration.FINISHED_DOWNLOADS_REMOVE, Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START, Configuration.FINISHED_DOWNLOADS_NO_REMOVE },
                "Fertig gestellte Downloads ...").setDefaultValue(Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_DISABLE_CONFIRM_DIALOGS, "Bestätigungsdialoge nicht anzeigen").setDefaultValue(false).setExpertEntry(true));
        addGUIConfigEntry(ce);
        
        String[] plafs;
        
        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
        plafs=new String[info.length];
        
        for( int i=0;i<plafs.length;i++){
            plafs[i]=info[i].getName();
        }
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_PLAF, plafs,    "Style(benötigt JD-Neustart)").setDefaultValue("Windows"));
addGUIConfigEntry(ce);

//        if(JDUtilities.getJavaVersion()>=1.6d){
//            ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_NO_TRAY, "Trayicon deaktivieren").setDefaultValue(false));
//               
//        }else{
//        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_NO_TRAY, "Trayicon deaktivieren").setDefaultValue(true).setEnabled(false));
//        }
//        addGUIConfigEntry(ce);
       
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_START_DOWNLOADS_AFTER_START, "Download beim Programmstart beginnen").setDefaultValue(false));
        addGUIConfigEntry(ce);
        
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, "Wenn möglich Unterordner mit Paketname erstellen").setDefaultValue(false));
        addGUIConfigEntry(ce);
        
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, "Zwischenablage immer überwachen").setDefaultValue(false));
        addGUIConfigEntry(ce);
       

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, "Webupdate: Alle Erweiterungen aktualisieren (auch OS-fremde)").setDefaultValue(false).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_AUTO_RESTART, "Webupdate:  automatisch ausführen!").setDefaultValue(false).setExpertEntry(true));
        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WRITE_LOG, "Filelogger: Log in Datei schreiben").setDefaultValue(true));
        addGUIConfigEntry(ce);
        
        ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, configuration, Configuration.PARAM_WRITE_LOG_PATH, "Filelogger: Pfad zur Logfile").setDefaultValue(JDUtilities.getResourceFile("jd_log.txt").getAbsolutePath()));
        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_JAC_LOG, "jAntiCaptcha: Log aktiv").setDefaultValue(false).setExpertEntry(true));
        addGUIConfigEntry(ce);
       
        
        if (JDUtilities.getHomeDirectory() != null) {
            brsHomeDir = new BrowseFile();
            brsHomeDir.setText(JDUtilities.getHomeDirectory());
            brsHomeDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            JDUtilities.addToGridBag(panel, lblHomeDir, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
            JDUtilities.addToGridBag(panel, brsHomeDir, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        }
        add(panel, BorderLayout.NORTH);
    }
    @Override
    public void load() {
        this.loadConfigEntries();
    }
    @Override
    public String getName() {
        return "Allgemein";
    }
}
