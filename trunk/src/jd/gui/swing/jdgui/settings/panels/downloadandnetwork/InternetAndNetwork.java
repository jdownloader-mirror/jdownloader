//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.settings.panels.downloadandnetwork;

import javax.swing.JTabbedPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.settings.GUIConfigEntry;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class InternetAndNetwork extends ConfigPanel {

    private static final long serialVersionUID = -7292287136387344296L;
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.downloadandnetwork.InternetAndNetwork.";
    private SubConfiguration config;

    public InternetAndNetwork(Configuration configuration) {
        super();

        config = SubConfiguration.getConfig("DOWNLOAD");
        initPanel();
        load();
    }

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "download.internetandnetwork.title", "Internet & Network");
    }

    private ConfigContainer setupContainer() {

        ConfigEntry ce;
        ConfigEntry conditionEntry;
        // Network Tab

        ConfigContainer network = new ConfigContainer(JDL.L("gui.config.download.network.tab", "Internet & Netzwerkverbindung"));

        network.setGroup(new ConfigGroup(JDL.L("gui.config.download.timeout", "Timeout & Connection loss"), JDTheme.II("gui.images.networkerror", 32, 32)));

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, JDL.L("gui.config.download.timeout.read", "Timeout beim Lesen [ms]"), 20000, 120000));
        ce.setDefaultValue(100000);
        ce.setStep(500);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, JDL.L("gui.config.download.timeout.connect", "Timeout beim Verbinden(Request) [ms]"), 20000, 120000));
        ce.setDefaultValue(100000);
        ce.setStep(500);

        network.setGroup(new ConfigGroup(JDL.L("gui.config.download.proxy", "Proxy Settings"), JDTheme.II("gui.images.proxy", 32, 32)));
        network.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.USE_PROXY, JDL.L("gui.config.download.use_proxy", "Http-Proxy Verwenden") + " (" + JDL.L("gui.warning.restartNeeded", "JD-Restart needed after changes!") + ")"));

        conditionEntry.setDefaultValue(false);
        conditionEntry.setPropertyType(PropertyType.NEEDS_RESTART);
        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PROXY_HOST, JDL.L("gui.config.download.proxy.host", "Host/IP")));
        ce.setDefaultValue("");
        ce.setEnabledCondidtion(conditionEntry, true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PROXY_PORT, JDL.L("gui.config.download.proxy.port", "Port"), 1, 65535));
        ce.setDefaultValue(8080);
        ce.setEnabledCondidtion(conditionEntry, true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PROXY_USER, JDL.L("gui.config.download.proxy.user", "User")));
        ce.setDefaultValue("");
        ce.setEnabledCondidtion(conditionEntry, true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, config, Configuration.PROXY_PASS, JDL.L("gui.config.download.proxy.pass", "Pass")));
        ce.setDefaultValue("");
        ce.setEnabledCondidtion(conditionEntry, true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);

        network.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.USE_SOCKS, JDL.L("gui.config.download.use_socks", "Socks-Proxy Verwenden") + " (" + JDL.L("gui.warning.restartNeeded", "JD-Restart needed after changes!") + ")"));
        conditionEntry.setDefaultValue(false);
        conditionEntry.setPropertyType(PropertyType.NEEDS_RESTART);

        /* disabled because not so easy to switch between v4 and v5 */
        // network.addEntry(ce = new
        // ConfigEntry(ConfigContainer.TYPE_RADIOFIELD, config,
        // Configuration.SOCKS_TYPE, new String[] { "Socks v4", "Socks v5" },
        // JDL.L("gui.config.download.sockstype",
        // "Select Socks Type")).setDefaultValue("Socks v5"));
        // ce.setDefaultValue("");
        // ce.setEnabledCondidtion(conditionEntry, "==", true);
        // ce.setPropertyType(PropertyType.NEEDS_RESTART);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.SOCKS_HOST, JDL.L("gui.config.download.socks.host", "Host/IP")));
        ce.setDefaultValue("");
        ce.setEnabledCondidtion(conditionEntry, true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.SOCKS_PORT, JDL.L("gui.config.download.socks.port", "Port"), 1, 65535));
        ce.setDefaultValue(1080);
        ce.setEnabledCondidtion(conditionEntry, true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PROXY_USER_SOCKS, JDL.L("gui.config.download.proxy.user", "User")));
        ce.setDefaultValue("");
        ce.setEnabledCondidtion(conditionEntry, true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, config, Configuration.PROXY_PASS_SOCKS, JDL.L("gui.config.download.proxy.pass", "Pass")));
        ce.setDefaultValue("");
        ce.setEnabledCondidtion(conditionEntry, true);
        ce.setPropertyType(PropertyType.NEEDS_RESTART);
        return network;
    }

    @Override
    public void initPanel() {
        ConfigContainer container = setupContainer();

        for (ConfigEntry cfgEntry : container.getEntries()) {
            GUIConfigEntry ce = new GUIConfigEntry(cfgEntry);
            if (ce != null) addGUIConfigEntry(ce);
        }

        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);

        this.add(tabbed);
    }

}
