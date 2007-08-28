package jd.gui.skins.simple.config;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;

import jd.Configuration;
import jd.JDUtilities;

public class ConfigurationDialog extends JDialog implements ActionListener{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 4046836223202290819L;
    private Configuration configuration;
    private JTabbedPane tabbedPane;
    private ConfigPanelGeneral configPanelGeneral;
    private ConfigPanelRouter configPanelRouter;
    private ConfigPanelAutomatic configPanelAutomatic;
    private JButton btnSave;
    private JButton btnCancel;
    private boolean configChanged = false;
    
    
    private ConfigurationDialog(){
        setModal(true);
        setLayout(new GridBagLayout());
        configuration = JDUtilities.getConfiguration();
        configPanelGeneral   = new ConfigPanelGeneral(configuration);
        configPanelRouter    = new ConfigPanelRouter(configuration);
        configPanelAutomatic = new ConfigPanelAutomatic(configuration);
        btnSave = new JButton("Speichern");
        btnSave.addActionListener(this);
        btnCancel = new JButton("Abbrechen");
        btnCancel.addActionListener(this);
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Allgemein",     configPanelGeneral);
        tabbedPane.addTab("Router",        configPanelRouter);
        tabbedPane.addTab("Automatisches", configPanelAutomatic);
        
        
        JDUtilities.addToGridBag(this, tabbedPane, 0, 0, 2, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnSave,    0, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnCancel,  1, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        
        pack();
    }
    public static boolean showConfig(Component parent){
        ConfigurationDialog c = new ConfigurationDialog();
        c.setLocation(JDUtilities.getCenterOfComponent(parent, c));
        c.setVisible(true);
        return c.configChanged;
    }

    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == btnSave){
            configPanelGeneral.save();
            configPanelRouter.save();
            configChanged=true;
        }
        setVisible(false);
    }
    
}
