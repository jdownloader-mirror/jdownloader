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

package jd.utils;

import java.util.ArrayList;
import java.util.Calendar;

import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.nrouter.IPCheck;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

/**
 * Diese Klasse stellt Methoden zur Verfügung um in einen String mit
 * Platzhaltern werte einzusetzen
 */
public class Replacer {

    private static ArrayList<String[]> KEYS = null;

    public static String[] getKeyList() {
        if (KEYS == null) Replacer.initKeys();
        String[] keys = new String[KEYS.size()];
        for (int i = 0; i < KEYS.size(); i++) {
            keys[i] = "%" + KEYS.get(i)[0] + "%" + "   (" + KEYS.get(i)[1] + ")";
        }
        return keys;
    }

    public static String getKey(int index) {
        if (KEYS == null) Replacer.initKeys();
        if (index >= KEYS.size()) return null;
        return KEYS.get(index)[0];
    }

    private static void initKeys() {
        KEYS = new ArrayList<String[]>();
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.PASSWORD", JDL.L("replacer.password", "Last finished package: Password") });
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.FILELIST", JDL.L("replacer.filelist", "Last finished package: Filelist") });
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.PACKAGENAME", JDL.L("replacer.packagename", "Last finished package: Packagename") });
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.COMMENT", JDL.L("replacer.comment", "Last finished package: Comment") });
        KEYS.add(new String[] { "LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY", JDL.L("replacer.downloaddirectory", "Last finished package: Download Directory") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.DOWNLOAD_PATH", JDL.L("replacer.filepath", "Last finished File: Filepath") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.INFOSTRING", JDL.L("replacer.informationstring", "Last finished File: Plugin given informationstring") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.HOST", JDL.L("replacer.hoster", "Last finished File: Hoster") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.NAME", JDL.L("replacer.filename", "Last finished File: Filename") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.FILESIZE", JDL.L("replacer.filesize", "Last finished File: Filesize") });
        KEYS.add(new String[] { "LAST_FINISHED_FILE.AVAILABLE", JDL.L("replacer.available", "Last finished File: is Available (Yes,No)") });
        KEYS.add(new String[] { "SYSTEM.IP", JDL.L("replacer.ipaddress", "Current IP Address") });
        KEYS.add(new String[] { "SYSTEM.DATE", JDL.L("replacer.date", "Current Date") });
        KEYS.add(new String[] { "SYSTEM.TIME", JDL.L("replacer.time", "Current Time") });
        KEYS.add(new String[] { "SYSTEM.JAVA_VERSION", JDL.L("replacer.javaversion", "Used Java Version") });
        KEYS.add(new String[] { "JD.REVISION", JDL.L("replacer.jdversion", "jDownloader: Revision/Version") });
        KEYS.add(new String[] { "JD.HOME_DIR", JDL.L("replacer.jdhomedirectory", "jDownloader: Homedirectory/Installdirectory") });
    }

    public static String getReplacement(String key) {
        JDController controller = JDUtilities.getController();
        DownloadLink dLink = controller.getLastFinishedDownloadLink();

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.PASSWORD")) {
            if (dLink == null) return "";
            return dLink.getFilePackage().getPassword();
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.FILELIST")) {
            if (dLink == null) return "";
            ArrayList<DownloadLink> files = dLink.getFilePackage().getDownloadLinkList();
            return files.toString();
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.PACKAGENAME")) {
            if (dLink == null) return "";
            if (dLink.getFilePackage().getName() == null || dLink.getFilePackage().getName().length() == 0) return dLink.getName();
            return dLink.getFilePackage().getName();
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.COMMENT")) {
            if (dLink == null) return "";
            return dLink.getFilePackage().getComment();
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY")) {
            if (dLink == null) return "";
            return dLink.getFilePackage().getDownloadDirectory();
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.DOWNLOAD_PATH")) {
            if (dLink == null) return "";
            return dLink.getFileOutput();
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.INFOSTRING")) {
            if (dLink == null) return "";
            return dLink.getFileInfomationString();
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.HOST")) {
            if (dLink == null) return "";
            return dLink.getHost();
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.NAME")) {
            if (dLink == null) return "";
            return dLink.getName();
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.FILESIZE")) {
            if (dLink == null) return "";
            return dLink.getDownloadSize() + "";
        }

        if (key.equalsIgnoreCase("LAST_FINISHED_FILE.AVAILABLE")) {
            if (dLink == null) return "";
            return dLink.isAvailable() ? "YES" : "NO";
        }

        if (key.equalsIgnoreCase("SYSTEM.IP")) return IPCheck.getIPAddress();

        if (key.equalsIgnoreCase("SYSTEM.DATE")) {
            Calendar c = Calendar.getInstance();
            return Formatter.fillInteger(c.get(Calendar.DATE), 2, "0") + "." + Formatter.fillInteger((c.get(Calendar.MONTH) + 1), 2, "0") + "." + c.get(Calendar.YEAR);
        }

        if (key.equalsIgnoreCase("SYSTEM.TIME")) {
            Calendar c = Calendar.getInstance();
            return Formatter.fillInteger(c.get(Calendar.HOUR_OF_DAY), 2, "0") + ":" + Formatter.fillInteger(c.get(Calendar.MINUTE), 2, "0") + ":" + Formatter.fillInteger(c.get(Calendar.SECOND), 2, "0");
        }

        if (key.equalsIgnoreCase("JD.REVISION")) return JDUtilities.getRevision();

        if (key.equalsIgnoreCase("SYSTEM.JAVA_VERSION")) return JDUtilities.getJavaVersion().toString();

        if (key.equalsIgnoreCase("JD.HOME_DIR")) return JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();

        return "";

    }

    public static String insertVariables(String str) {
        if (str == null) return "";
        if (KEYS == null) Replacer.initKeys();
        for (String[] element : KEYS) {
            if (str.indexOf("%" + element[0] + "%") >= 0) {
                JDLogger.getLogger().finer("%" + element[0] + "%" + " --> *****");
                str = Replacer.replace(str, "%" + element[0] + "%", Replacer.getReplacement(element[0]));
            }
        }
        return str;
    }

    private static String replace(String in, String remove, String replace) {
        if (in == null || remove == null || remove.length() == 0) { return in; }
        StringBuilder sb = new StringBuilder();
        int oldIndex = 0;
        int newIndex = 0;
        int remLength = remove.length();
        while ((newIndex = in.indexOf(remove, oldIndex)) > -1) {
            sb.append(in.substring(oldIndex, newIndex));
            sb.append(replace);

            oldIndex = newIndex + remLength;
        }

        int inLength = in.length();
        if (oldIndex < inLength) {
            sb.append(in.substring(oldIndex, inLength));
        }
        return sb.toString();
    }
}