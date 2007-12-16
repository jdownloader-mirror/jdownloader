package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;

import jd.config.Configuration;
import jd.controlling.interaction.ExternReconnect;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.BrowseFile;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelReconnect extends ConfigPanel implements ActionListener {
    /**
     * serialVersionUID
     */
    private static final long      serialVersionUID = 3383448498625377495L;

    private JLabel                 lblHomeDir;

    private BrowseFile             brsHomeDir;

    private Configuration          configuration;

    private JTabbedPane            tabbedPane;

    private JComboBox              box;

    private SubPanelHTTPReconnect  httpReconnect    = null;

    private SubPanelLiveHeaderReconnect lh;

    private ConfigPanelInteraction er;

    private JButton                btn;

    ConfigPanelReconnect(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();
        load();
    }

    public void save() {
        this.saveConfigEntries();
        if (httpReconnect != null) httpReconnect.save();
        if (httpReconnect != null) httpReconnect.saveConfigEntries();
        if (lh != null) lh.save();
        if (lh != null) lh.saveConfigEntries();
        if (er != null) er.save();
        if (er != null) er.saveConfigEntries();
    }

    @Override
    public void initPanel() {
        String reconnectType = configuration.getStringProperty(Configuration.PARAM_RECONNECT_TYPE);
        JPanel p = new JPanel();

        box = new JComboBox(new String[] { JDLocale.L("modules.reconnect.types.httpreconnect","HTTPReconnect/Routercontrol"), JDLocale.L("modules.reconnect.types.liveheader","LiveHeader/Curl"), JDLocale.L("modules.reconnect.types.extern","Extern" )});
        box.addActionListener(this);
        p.add(new JLabel("Bitte Methode auswählen:"));
        p.add(box);
        panel.setLayout(new BorderLayout());
        panel.add(p, BorderLayout.PAGE_START);
        panel.add(new JSeparator(), BorderLayout.CENTER);
        // panel.add(new JSeparator());
        if (reconnectType != null) box.setSelectedItem(reconnectType);
        add(panel, BorderLayout.NORTH);
        panel.add(btn = new JButton("Test Reconnect"));
        btn.addActionListener(this);
        this.setReconnectType();
    }

    private void setReconnectType() {
        logger.finer("III " + ((String) box.getSelectedItem()));
        if (lh != null) panel.remove(lh);
        if (er != null) panel.remove(er);
        if (httpReconnect != null) panel.remove(httpReconnect);
        lh = null;
        er = null;
        httpReconnect = null;

        if (((String) box.getSelectedItem()).equals(JDLocale.L("modules.reconnect.types.httpreconnect","HTTPReconnect/Routercontrol"))) {

            panel.add(httpReconnect = new SubPanelHTTPReconnect(configuration, uiinterface), BorderLayout.PAGE_END);

        }
        else if (((String) box.getSelectedItem()).equals(JDLocale.L("modules.reconnect.types.liveheader","LiveHeader/Curl"))) {

            panel.add(lh = new SubPanelLiveHeaderReconnect(uiinterface, (Interaction) new HTTPLiveHeader()), BorderLayout.PAGE_END);

        }
        else if (((String) box.getSelectedItem()).equals(JDLocale.L("modules.reconnect.types.extern","Extern"))) {

            panel.add(er = new ConfigPanelInteraction(uiinterface, (Interaction) new ExternReconnect()), BorderLayout.PAGE_END);

        }
        this.validate();
    }

    @Override
    public void load() {
        this.loadConfigEntries();
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.reconnect.name","Reconnect");
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == box) {

            configuration.setProperty(Configuration.PARAM_RECONNECT_TYPE, (String) box.getSelectedItem());
            setReconnectType();

        }
        if (e.getSource() == btn) {
          save();
            if (JDUtilities.getController().reconnect()) {
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.warning.reconnectSuccess","Reconnect successfull"));
            }
            else {
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.warning.reconnectFailed","Reconnect failed!"));
            }
        }
    }
}
