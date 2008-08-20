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

import java.awt.BorderLayout;
import java.util.logging.Level;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.utils.JDLocale;

public class ConfigPanelGeneral extends ConfigPanel {

    private static final long serialVersionUID = 3383448498625377495L;

    private Configuration configuration;

    public ConfigPanelGeneral(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();
        load();
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.general.name", "Allgemein");
    }

    @Override
    public void initPanel() {
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Configuration.PARAM_LOGGER_LEVEL, new Level[] { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF }, JDLocale.L("gui.config.general.loggerLevel", "Level für's Logging")).setDefaultValue(Level.WARNING)));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.LOGGER_FILELOG, JDLocale.L("gui.config.general.filelogger", "Erstelle Logdatei im ./logs/ Ordner")).setDefaultValue(false)));

        this.add(panel, BorderLayout.NORTH);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();
        logger.setLevel((Level) configuration.getProperty(Configuration.PARAM_LOGGER_LEVEL));
    }
}
