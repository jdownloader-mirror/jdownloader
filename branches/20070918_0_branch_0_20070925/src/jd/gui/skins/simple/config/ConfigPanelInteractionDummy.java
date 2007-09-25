package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import jd.Configuration;
import jd.controlling.interaction.DummyInteraction;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;
import jd.plugins.PluginConfig;

public class ConfigPanelInteractionDummy extends ConfigPanel implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -7983057329558110899L;

    /**
     * serialVersionUID
     */
    private Interaction       interaction;

    ConfigPanelInteractionDummy(Configuration configuration, UIInterface uiinterface, DummyInteraction interaction) {
        super(configuration, uiinterface);
        this.interaction = interaction;
        initPanel();
        
        load();
    }

    public void save() {
        this.saveConfigEntries();
    }

    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void initPanel() {
        ConfigEntry ce;

        ce = new ConfigEntry(PluginConfig.TYPE_TEXTFIELD, interaction, DummyInteraction.PROPERTY_QUESTION, "Frage");

        addConfigEntry(ce);

        add(panel, BorderLayout.CENTER);

    }

    @Override
    public String getName() {

        return "Dummy Konfiguration";
    }

    @Override
    public void load() {
        loadConfigEntries();

    }

}
