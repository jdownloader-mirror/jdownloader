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

package jd.controlling.reconnect;

import java.io.File;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Diese Klasse ruft ein Externes Programm auf. Anschließend wird auf eine Neue
 * IP geprüft
 * 
 * @author JD-Team
 */
public class ExternReconnect extends ReconnectMethod {

    private Configuration configuration;

    private static final String PROPERTY_IP_WAIT_FOR_RETURN = "WAIT_FOR_RETURN4";

    private static final String PROPERTY_RECONNECT_COMMAND = "InteractionExternReconnect_Command";

    private static final String PROPERTY_RECONNECT_PARAMETER = "EXTERN_RECONNECT__PARAMETER";

    public ExternReconnect() {
        configuration = JDUtilities.getConfiguration();
    }

    // @Override
    public void initConfig() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, configuration, PROPERTY_RECONNECT_COMMAND, JDL.L("interaction.externreconnect.command", "Befehl (absolute Pfade verwenden)")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, PROPERTY_RECONNECT_PARAMETER, JDL.L("interaction.externreconnect.parameter", "Parameter (1 Parameter/Zeile)")));

    }

    // @Override
    protected boolean runCommands(ProgressController progress) {
        String command = configuration.getStringProperty(PROPERTY_RECONNECT_COMMAND);

        File f = new File(command);
        String t = f.getAbsolutePath();
        String executeIn = t.substring(0, t.indexOf(f.getName()) - 1);

        String parameter = configuration.getStringProperty(PROPERTY_RECONNECT_PARAMETER);
        /*
         * timeout set to 0 to avoid blocking streamobserver, because not every
         * external tool will use stdin/stdout/stderr! we do not use the streams
         * anyway so who cares
         */
        logger.finer("Execute Returns: " + JDUtilities.runCommand(command, Regex.getLines(parameter), executeIn, 0));

        return true;
    }

    // @Override
    public String toString() {
        return JDL.L("interaction.externreconnect.toString", "Externes Reconnectprogramm aufrufen");
    }

}
