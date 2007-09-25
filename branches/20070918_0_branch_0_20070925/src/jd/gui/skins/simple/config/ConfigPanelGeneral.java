package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jd.Configuration;
import jd.JDUtilities;
import jd.event.UIEvent;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.BrowseFile;
import jd.plugins.Plugin;

public class ConfigPanelGeneral extends ConfigPanel implements ActionListener{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
    private JLabel lblDownloadDir;
    private JLabel lblHomeDir;
 
    private JLabel lblLoggerLevel;
    private BrowseFile brsDownloadDir;
    private BrowseFile brsHomeDir;
    private JComboBox cboLoggerLevel;
    private JButton btnUpdate;
  
   
    
    
  
    
    ConfigPanelGeneral(Configuration configuration, UIInterface uiinterface){
        super( configuration,  uiinterface);
        initPanel();
        
        load();
    
    }
    public void save(){
        Level loggerLevel = (Level)cboLoggerLevel.getSelectedItem();
        configuration.setDownloadDirectory(brsDownloadDir.getText().trim());
        configuration.setLoggerLevel(loggerLevel.getName());
     
        configuration.setLoggerLevel(loggerLevel.getName());
        Plugin.getLogger().setLevel(loggerLevel);
        if(JDUtilities.getHomeDirectory() != null)
            JDUtilities.writeJDHomeDirectoryToWebStartCookie(brsHomeDir.getText().trim());
    }
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == btnUpdate){
          
            uiinterface.fireUIEvent(new UIEvent(uiinterface,UIEvent.UI_INTERACT_UPDATE));
        }
        
    }
    @Override
    public void initPanel() {

        lblDownloadDir = new JLabel("Download Verzeichnis");
        lblHomeDir     = new JLabel("JD Home");

        lblLoggerLevel = new JLabel("Level für's Logging");
        brsHomeDir     = new BrowseFile(30);
  
    
      brsHomeDir.setText(JDUtilities.getHomeDirectory()); 
      brsHomeDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        brsDownloadDir = new BrowseFile(30);
        brsDownloadDir.setText(configuration.getDownloadDirectory());     
        brsDownloadDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        cboLoggerLevel = new JComboBox();
   
        btnUpdate = new JButton("Update");
        btnUpdate.addActionListener(this);

//        Insets insets = new Insets(1,5,1,5);
        
        cboLoggerLevel.addItem(Level.ALL);
        cboLoggerLevel.addItem(Level.FINEST);
        cboLoggerLevel.addItem(Level.FINER);
        cboLoggerLevel.addItem(Level.FINE);
        cboLoggerLevel.addItem(Level.INFO);
        cboLoggerLevel.addItem(Level.WARNING);
        cboLoggerLevel.addItem(Level.SEVERE);
        cboLoggerLevel.addItem(Level.OFF);
     
        cboLoggerLevel.setSelectedItem(Plugin.getLogger().getLevel());
 
        int row=0;
       
        JDUtilities.addToGridBag(panel, lblLoggerLevel, 0, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
      
        JDUtilities.addToGridBag(panel, cboLoggerLevel, 1, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
   
        row++;
        JDUtilities.addToGridBag(panel, lblDownloadDir, 0, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
     
        JDUtilities.addToGridBag(panel, brsDownloadDir, 1, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        row++;
   
        if(JDUtilities.getHomeDirectory() != null){
            
            JDUtilities.addToGridBag(panel, lblHomeDir, 0, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
      
            JDUtilities.addToGridBag(panel, brsHomeDir, 1, row, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
            row++;
        }

 
        add(panel,BorderLayout.BEFORE_FIRST_LINE);

        
    }
    @Override
    public void load() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public String getName() {
        
        return "Allgemein";
    }
    
}
