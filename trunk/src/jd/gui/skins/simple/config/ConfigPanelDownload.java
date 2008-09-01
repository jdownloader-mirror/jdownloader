//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple.config;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.SingleDownloadController;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 * 
 */
public class ConfigPanelDownload extends ConfigPanel {

    private static final long serialVersionUID = 4145243293360008779L;

    private ConfigEntriesPanel cep;

    private SubConfiguration config;

    public ConfigPanelDownload(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        initPanel();
        load();
    }

    public String getName() {
        return JDLocale.L("gui.config.download.name", "Netzwerk/Download");
    }

    public void initPanel() {
        this.add(cep = new ConfigEntriesPanel(setupContainer()));
    }

    public void load() {
        loadConfigEntries();
    }

    public void save() {
        cep.save();
        config.save();
    }

    public ConfigContainer setupContainer() {
        ConfigContainer container = new ConfigContainer(this);
        config = JDUtilities.getSubConfig("DOWNLOAD");

        ConfigEntry ce;
        ConfigEntry conditionEntry;

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, JDLocale.L("gui.config.general.downloadDirectory", "Downloadverzeichnis")));
        ce.setDefaultValue(JDUtilities.getResourceFile("downloads").getAbsolutePath());

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, JDLocale.L("gui.config.general.createSubFolders", "Wenn möglich Unterordner mit Paketname erstellen")));
        ce.setDefaultValue(false);

        String[] removeDownloads = new String[] { JDLocale.L("gui.config.general.toDoWithDownloads.immediate", "immediately"), JDLocale.L("gui.config.general.toDoWithDownloads.atStart", "at startup"), JDLocale.L("gui.config.general.toDoWithDownloads.packageReady", "when package is ready"), JDLocale.L("gui.config.general.toDoWithDownloads.never", "never") };
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, JDUtilities.getConfiguration(), Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, removeDownloads, JDLocale.L("gui.config.general.toDoWithDownloads", "Remove finished downloads ...")));
        ce.setDefaultValue(removeDownloads[1]);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME), SimpleGUI.PARAM_START_DOWNLOADS_AFTER_START, JDLocale.L("gui.config.download.startDownloadsOnStartUp", "Download beim Programmstart beginnen")));
        ce.setDefaultValue(false);

        String[] fileExists = new String[] { JDLocale.L("system.download.triggerfileexists.overwrite", "Datei überschreiben"), JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen") };
        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, config, Configuration.PARAM_FILE_EXISTS, fileExists, JDLocale.L("system.download.triggerfileexists", "Wenn eine Datei schon vorhanden ist:")));
        ce.setDefaultValue(fileExists[1]);

        // Network Tab
        ConfigContainer network = new ConfigContainer(this, JDLocale.L("gui.config.download.network.tab", "Internet & Netzwerkverbindung"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, network));

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, JDLocale.L("gui.config.download.timeout.read", "Timeout beim Lesen [ms]"), 0, 120000));
        ce.setDefaultValue(100000);
        ce.setStep(500);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, JDLocale.L("gui.config.download.timeout.connect", "Timeout beim Verbinden(Request) [ms]"), 0, 120000));
        ce.setDefaultValue(100000);
        ce.setStep(500);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, SingleDownloadController.WAIT_TIME_ON_CONNECTION_LOSS, JDLocale.L("gui.config.download.connectionlost.wait", "Wartezeit nach Verbindungsabbruch [s]"), 0, 24 * 60 * 60));
        ce.setDefaultValue(5 * 60);
        ce.setStep(1);

        network.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        network.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.USE_PROXY, JDLocale.L("gui.config.download.use_proxy", "Http-Proxy Verwenden") + " (" + JDLocale.L("gui.warning.restartNeeded", "JD-Restart needed after changes!") + ")"));
        conditionEntry.setDefaultValue(false);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PROXY_HOST, JDLocale.L("gui.config.download.proxy.host", "Host/IP")));
        ce.setEnabledCondidtion(conditionEntry, "==", true);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PROXY_PORT, JDLocale.L("gui.config.download.proxy.port", "Port"), 1, 65000));
        ce.setDefaultValue(8080);
        ce.setEnabledCondidtion(conditionEntry, "==", true);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PROXY_USER, JDLocale.L("gui.config.download.proxy.user", "User")));
        ce.setEnabledCondidtion(conditionEntry, "==", true);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PROXY_PASS, JDLocale.L("gui.config.download.proxy.pass", "Pass")));
        ce.setEnabledCondidtion(conditionEntry, "==", true);

        network.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        network.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.USE_SOCKS, JDLocale.L("gui.config.download.use_socks", "Socks-Proxy Verwenden") + " (" + JDLocale.L("gui.warning.restartNeeded", "JD-Restart needed after changes!") + ")"));
        conditionEntry.setDefaultValue(false);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.SOCKS_HOST, JDLocale.L("gui.config.download.socks.host", "Host/IP")));
        ce.setEnabledCondidtion(conditionEntry, "==", true);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.SOCKS_PORT, JDLocale.L("gui.config.download.socks.port", "Port"), 1, 65000));
        ce.setDefaultValue(1080);
        ce.setEnabledCondidtion(conditionEntry, "==", true);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PROXY_USER_SOCKS, JDLocale.L("gui.config.download.proxy.user", "User")));
        ce.setEnabledCondidtion(conditionEntry, "==", true);

        network.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PROXY_PASS_SOCKS, JDLocale.L("gui.config.download.proxy.pass", "Pass")));
        ce.setEnabledCondidtion(conditionEntry, "==", true);

        // Download Tab
        ConfigContainer download = new ConfigContainer(this, JDLocale.L("gui.config.download.download.tab", "Downloadsteuerung"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, download));

        download.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, JDLocale.L("gui.config.download.simultan_downloads", "Maximale gleichzeitige Downloads"), 1, 20));
        ce.setDefaultValue(2);
        ce.setStep(1);

        download.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, JDLocale.L("gui.config.download.chunks", "Anzahl der Verbindungen/Datei(Chunkload)"), 1, 20));
        conditionEntry.setDefaultValue(2);
        conditionEntry.setStep(1);

        download.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "PARAM_DOWNLOAD_AUTO_CORRECTCHUNKS", JDLocale.L("gui.config.download.autochunks", "Chunks an Dateigröße anpassen.")));
        ce.setDefaultValue(true);
        ce.setEnabledCondidtion(conditionEntry, ">", 1);

        download.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, PluginForHost.PARAM_MAX_RETRIES, JDLocale.L("gui.config.download.retries", "Max. Neuversuche bei vorrübergehenden Hosterproblemen"), 0, 20));
        ce.setDefaultValue(3);
        ce.setStep(1);

        download.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "PARAM_DOWNLOAD_AUTORESUME_ON_RECONNECT", JDLocale.L("gui.config.download.autoresume", "Let Reconnects interrupt resumeable downloads")));
        ce.setDefaultValue(true);

        // Extended Tab
        ConfigContainer extended = new ConfigContainer(this, JDLocale.L("gui.config.download.network.extended", "Erweiterte Einstellungen"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, extended));

        extended.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_GLOBAL_IP_DISABLE, JDLocale.L("gui.config.download.ipcheck.disable", "IP Überprüfung deaktivieren")));
        conditionEntry.setDefaultValue(false);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_CHECK_SITE, JDLocale.L("gui.config.download.ipcheck.website", "IP prüfen über (Website)")));
        ce.setDefaultValue("http://checkip.dyndns.org");
        ce.setEnabledCondidtion(conditionEntry, "==", false);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_PATTERN, JDLocale.L("gui.config.download.ipcheck.regex", "RegEx zum filtern der IP")));
        ce.setDefaultValue("Address\\: ([0-9.]*)\\<\\/body\\>");
        ce.setEnabledCondidtion(conditionEntry, "==", false);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_MASK, JDLocale.L("gui.config.download.ipcheck.mask", "Erlaubte IPs")));
        ce.setDefaultValue("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
        ce.setEnabledCondidtion(conditionEntry, "==", false);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, "EXTERNAL_IP_CHECK_INTERVAL", JDLocale.L("gui.config.download.ipcheck.externalinterval", "External IP Check Interval [sec]"), 10, 60 * 60));
        ce.setDefaultValue(10 * 60);
        ce.setEnabledCondidtion(conditionEntry, "==", false);

        extended.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_DO_CRC, JDLocale.L("gui.config.download.crc", "SFV/CRC Check wenn möglich durchführen")));
        ce.setDefaultValue(false);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "USEWRITERTHREAD", JDLocale.L("gui.config.download.downloadThread", "Gleichzeitig downloaden und auf Festplatte schreiben")));
        ce.setDefaultValue(false);

        extended.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, "MAX_BUFFER_SIZE", JDLocale.L("gui.config.download.buffersize", "Max. Buffersize[MB]"), 3, 30));
        ce.setDefaultValue(4);

        return container;

    }

}
