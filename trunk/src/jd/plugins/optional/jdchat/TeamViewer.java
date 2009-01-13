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

package jd.plugins.optional.jdchat;

import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;

/**
 * Diese Klasse kümmert sich um alle Teamviewer Handlings im JDChat
 */
public class TeamViewer {

    private static final long serialVersionUID = -9146764850581039090L;

    public static String[] handleTeamviewer() {
        return TeamViewer.askForTeamviewerIDPW();
    }

    public static String[] askForTeamviewerIDPW() {
        return SimpleGUI.CURRENTGUI.showTwoTextFieldDialog(JDLocale.L("plugin.optional.jdchat.teamviewer.yourtvdata", "Deine Teamviewer Daten:"), "ID:", "PW:", "", "");
    }

}
