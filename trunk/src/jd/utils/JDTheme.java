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

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import jd.http.Encoding;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.parser.Regex;

public class JDTheme {

    private static HashMap<String, String> data = new HashMap<String, String>();

    private static HashMap<String, String> defaultData;

    private static Logger logger = JDUtilities.getLogger();

    public static String THEME_DIR = "jd/themes/";

    public static Vector<String> getThemeIDs() {
        File dir = JDUtilities.getResourceFile(THEME_DIR);
        if (!dir.exists()) return null;
        File[] files = dir.listFiles(new JDFileFilter(null, ".thm", false));
        Vector<String> ret = new Vector<String>();
        for (File element : files) {
            ret.add(element.getName().split("\\.")[0]);
        }
        return ret;
    }

    public static String getThemeValue(String key, String def) {
        if (data == null || defaultData == null) {
            logger.severe("Use setTheme() first!");
            setTheme("default");
        }

        if (data.containsKey(key)) return Encoding.UTF8Decode(data.get(key));
        // logger.info("Key not found: " + key + " (" + def + ")");

        if (defaultData.containsKey(key)) {
            def = Encoding.UTF8Decode(defaultData.get(key));
            // logger.finer("Use default Value: " + def);
        }
        if (def == null) def = key;
        data.put(key, def);

        return def;

    }

    /**
     * Gibt eine Farbe zum key zurück
     * 
     * @param key
     * @return
     */
    public static Color C(String key, String def) {
        return new Color(Integer.parseInt(JDTheme.V(key, def), 16));
    }

    public static Color C(String key, String def, int alpha) {

        String hex = JDTheme.V(key, def);
        return new Color(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4), 16), alpha);
    }

    /**
     * Gibt ein Image zum key zurück
     * 
     * @param key
     * @return
     */
    public static Image I(String key) {
        return JDUtilities.getImage(JDTheme.V(key));
    }

    /**
     * Gibt ein skaliertes Image zurück
     * 
     * @param key
     * @param width
     * @param height
     * @return
     */
    public static Image I(String key, int width, int height) {
        return JDUtilities.getImage(JDTheme.V(key)).getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }

    /**
     * Gibt ein icon zum key zurück
     * 
     * @param key
     * @return
     */
    public static ImageIcon II(String key) {
        return new ImageIcon(JDUtilities.getImage(JDTheme.V(key)));
    }

    /**
     * Gibt ein skaliertes ImageIcon zurück
     * 
     * @param key
     * @param width
     * @param height
     * @return
     */
    public static ImageIcon II(String key, int width, int height) {
        return new ImageIcon(JDUtilities.getImage(JDTheme.V(key)).getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    public static void setTheme(String themeID) {
        File file = JDUtilities.getResourceFile(THEME_DIR + themeID + ".thm");

        if (!file.exists()) {
            logger.severe("Theme " + themeID + " not installed");
            return;
        }
        data = new HashMap<String, String>();
        String str = JDIO.getLocalFile(file);
        String[] lines = Regex.getLines(str);
        for (String element : lines) {
            int split = element.indexOf("=");
            if (split <= 0 || element.startsWith("#")) {
                continue;
            }
            String key = element.substring(0, split).trim();
            String value = element.substring(split + 1).trim();
            if (data.containsKey(key)) {
                logger.severe("Dupe found: " + key);
            } else {
                data.put(key, value);
            }

        }
        if (themeID.equals("default")) {
            defaultData = data;
        }
        if (defaultData == null) {
            defaultData = new HashMap<String, String>();
            file = JDUtilities.getResourceFile(THEME_DIR + "default.thm");

            if (!file.exists()) {
                logger.severe("Theme default not installed");
                return;
            }
            data = new HashMap<String, String>();
            str = JDIO.getLocalFile(file);
            lines = Regex.getLines(str);
            for (String element : lines) {
                int split = element.indexOf("=");
                if (split <= 0 || element.startsWith("#")) {
                    continue;
                }
                String key = element.substring(0, split).trim();
                String value = element.substring(split + 1).trim();
                if (data.containsKey(key)) {
                    logger.severe("Dupe found: " + key);
                } else {
                    data.put(key, value);
                }

            }

        }

    }

    /**
     * Gibt einen Theme String zum Key zurück
     * 
     * @param key
     * @return
     */
    public static String V(String key) {
        return JDTheme.getThemeValue(key, null);
    }

    /**
     * Gibt einen Theme String zum Key zurück
     * 
     * @param key
     * @return
     */
    public static String V(String key, String def) {
        return JDTheme.getThemeValue(key, def);
    }

}