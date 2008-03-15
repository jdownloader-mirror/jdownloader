package jd.gui.skins.simple.config;

import java.awt.BorderLayout;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 *
 */
public class ConfigPanelUpdater extends ConfigPanel{



/**
     * 
     */
    private static final long serialVersionUID = 4145243293360008779L;
private Configuration configuration;
private SubConfiguration config;
   
    
    
  
    
public ConfigPanelUpdater(Configuration configuration, UIInterface uiinterface){
        super(uiinterface);
      this.configuration=configuration;
        initPanel();

        load();
    
    }
    public void save(){
        logger.info("save");
        this.saveConfigEntries();
        config.save();
     }
   

    public void initPanel() {
       
       config = JDUtilities.getSubConfig("WEBUPDATE");
        GUIConfigEntry ce;
//      ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, JDLocale.L("gui.config.general.webupdate.osFilter", "Webupdate: Alle Erweiterungen aktualisieren (auch OS-fremde)")).setDefaultValue(false).setExpertEntry(true));
//      addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_DISABLE, JDLocale.L("gui.config.general.webupdate.disable", "Update nur manuell durchführen")).setDefaultValue(false).setExpertEntry(true));
        addGUIConfigEntry(ce);
        
      ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_WEBUPDATE_AUTO_RESTART, JDLocale.L("gui.config.general.webupdate.auto", "automatisch, ohne Nachfrage ausführen")).setDefaultValue(false).setExpertEntry(true));
      addGUIConfigEntry(ce);
        
      ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "WEBUPDATE_BETA", JDLocale.L("gui.config.general.webupdate.beta", "Auf Betaversion aktualisieren(Neustart nötig)")).setDefaultValue(false));
      addGUIConfigEntry(ce);  
        
        
        add(panel, BorderLayout.NORTH);
    }
    @Override
    public void load() {
  this.loadConfigEntries();
        
    }
    
    @Override
    public String getName() {
        
        return JDLocale.L("gui.config.download.name","Webupdate");
    }
    
}
