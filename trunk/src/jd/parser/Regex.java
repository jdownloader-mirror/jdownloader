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

package jd.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.JDLogger;

public class Regex {
    public static String[] getLines(String arg) {
        if (arg == null) { return new String[] {}; }
        String[] temp = arg.split("[\r\n]{1,2}");
        String[] output = new String[temp.length];
        for (int i = 0; i < temp.length; i++) {
            output[i] = temp[i].trim();
        }
        return output;
    }

    /**
     * Gibt zu einem typischem Sizestring (12,34kb , 45 mb etc) die größe in
     * bytes zurück.
     * 
     * @param sizestring
     * @return
     */
    public static long getSize(String string) {

        String[][] matches = new Regex(string, Pattern.compile("([\\d]+)[\\.|\\,|\\:]([\\d]+)", Pattern.CASE_INSENSITIVE)).getMatches();

        if (matches == null || matches.length == 0) {
            matches = new Regex(string, Pattern.compile("([\\d]+)", Pattern.CASE_INSENSITIVE)).getMatches();

        }
        if (matches == null || matches.length == 0) { return -1; }

        double res = 0;
        if (matches[0].length == 1) {
            res = Double.parseDouble(matches[0][0]);
        }
        if (matches[0].length == 2) {
            res = Double.parseDouble(matches[0][0] + "." + matches[0][1]);
        }
        if (Regex.matches(string, Pattern.compile("(gb|gbyte|gig)", Pattern.CASE_INSENSITIVE))) {
            res *= 1024 * 1024 * 1024;
        } else if (Regex.matches(string, Pattern.compile("(mb|mbyte|megabyte)", Pattern.CASE_INSENSITIVE))) {
            res *= 1024 * 1024;
        } else if (Regex.matches(string, Pattern.compile("(kb|kbyte|kilobyte)", Pattern.CASE_INSENSITIVE))) {
            res *= 1024;
        }

        return Math.round(res);
    }

    public static boolean matches(Object str, Pattern pat) {

        return new Regex(str, pat).matches();
    }

    public static boolean matches(Object page, String string) {

        return new Regex(page, string).matches();
    }

    private Matcher matcher;

    public Regex(Matcher matcher) {
        if (matcher == null) { return; }
        this.matcher = matcher;
    }

    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * 
     * @param data
     * @param pattern
     */
    public Regex(Object data, Pattern pattern) {
        this(data.toString(), pattern);
    }

    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * 
     * @param data
     * @param pattern
     */
    public Regex(Object data, String pattern) {
        this(data.toString(), pattern);
    }

    /**
     * Regexp auf ein Objekt (Beim Objekt wird toString aufgerufen)
     * 
     * @param data
     * @param pattern
     * @param flags
     *            flags für den Pattern z.B. Pattern.CASE_INSENSITIVE
     */
    public Regex(Object data, String pattern, int flags) {
        this(data.toString(), pattern, flags);
    }

    public Regex(String data, Pattern pattern) {
        if (data == null || pattern == null) { return; }
        matcher = pattern.matcher(data);
    }

    public Regex(String data, String pattern) {
        if (data == null || pattern == null) { return; }
        matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(data);

    }

    public Regex(String data, String pattern, int flags) {
        if (data == null || pattern == null) { return; }
        matcher = Pattern.compile(pattern, flags).matcher(data);
    }

    // Gibt die Anzahl der Treffer zurück

    public int count() {
        if (matcher == null) { return 0; }
        matcher.reset();
        int c = 0;
        Matcher matchertmp = matcher;
        while (matchertmp.find()) {
            c++;
        }
        return c;
    }

    public String getMatch(int group) {
        if (matcher == null) { return null; }
        Matcher matchertmp = matcher;
        matcher.reset();
        if (matchertmp.find()) { return matchertmp.group(group + 1); }

        return null;

    }

    // Gibt den matcher aus

    public Matcher getMatcher() {
        matcher.reset();
        return matcher;
    }

    // Gibt alle Treffer eines Matches in einem 2D array aus

    public String[][] getMatches() {
        if (matcher == null) { return null; }
        Matcher matchertmp = matcher;
        matcher.reset();
        ArrayList<String[]> ar = new ArrayList<String[]>();
        while (matchertmp.find()) {
            int c = matchertmp.groupCount();
            int d = 1;
            String[] group;
            if (c == 0) {
                group = new String[c + 1];
                d = 0;
            } else {
                group = new String[c];
            }

            for (int i = d; i <= c; i++) {
                group[i - d] = matchertmp.group(i);
            }
            ar.add(group);
        }
        return ar.toArray(new String[][] {});
    }

    public String[] getColumn(int x) {
        if (matcher == null) { return null; }
        x++;
        Matcher matchertmp = matcher;
        matcher.reset();

        ArrayList<String> ar = new ArrayList<String>();
        while (matchertmp.find()) {
            ar.add(matchertmp.group(x));
        }
        return ar.toArray(new String[ar.size()]);
    }

    public boolean matches() {
        matcher.reset();
        return matcher.find();
    }

    // Setzt den Matcher

    public void setMatcher(Matcher matcher) {
        this.matcher = matcher;
    }

    //@Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        String[][] match = getMatches();
        for (int i = 0; i < match.length; i++) {
            for (int j = 0; j < match[i].length; j++) {
                ret.append("match[");
                ret.append(i);
                ret.append("][");
                ret.append(j);
                ret.append("] = ");
                ret.append(match[i][j]);
                ret.append(System.getProperty("line.separator"));
            }
        }
        matcher.reset();
        return ret.toString();
    }

    public static long getMilliSeconds(String wait) {
        String[][] matches = new Regex(wait, "([\\d]+) ?[\\.|\\,|\\:] ?([\\d]+)").getMatches();
        if (matches == null || matches.length == 0) {
            matches = new Regex(wait, Pattern.compile("([\\d]+)")).getMatches();

        }

        if (matches == null || matches.length == 0) { return -1; }

        double res = 0;
        if (matches[0].length == 1) {
            res = Double.parseDouble(matches[0][0]);
        }
        if (matches[0].length == 2) {
            res = Double.parseDouble(matches[0][0] + "." + matches[0][1]);
        }

        if (Regex.matches(wait, Pattern.compile("(h|st)", Pattern.CASE_INSENSITIVE))) {
            res *= 60 * 60 * 1000l;
        } else if (Regex.matches(wait, Pattern.compile("(m)", Pattern.CASE_INSENSITIVE))) {
            res *= 60 * 1000l;
        } else {
            res *= 1000l;
        }

        return Math.round(res);

    }

    public static long getMilliSeconds(String expire, String timeformat, Locale l) {
        SimpleDateFormat dateFormat;

        if (l != null) {
            dateFormat = new SimpleDateFormat(timeformat, l);
        } else {
            dateFormat = new SimpleDateFormat(timeformat);
        }
        if (expire == null) { return -1; }

        Date date;
        try {
            date = dateFormat.parse(expire);
            return (date.getTime());
        } catch (ParseException e) {
            JDLogger.getLogger().severe("Could not format date " + expire + " with formater " + timeformat + ": " + dateFormat.format(new Date()));

            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
        return -1;

    }

    public String getMatch(int entry, int group) {
        if (matcher == null) { return null; }
        Matcher matchertmp = matcher;
        matcher.reset();
        // group++;
        entry++;
        int groupCount = 0;
        while (matchertmp.find()) {
            if (groupCount == group) { return matchertmp.group(entry); }

            groupCount++;
        }
        return null;
    }

    public String[] getRow(int y) {
        if (matcher == null) { return null; }
        Matcher matchertmp = matcher;
        matcher.reset();
        int groupCount = 0;
        while (matchertmp.find()) {
            if (groupCount == y) {
                int c = matchertmp.groupCount();

                String[] group = new String[c];

                for (int i = 1; i <= c; i++) {
                    group[i - 1] = matchertmp.group(i);
                }
                return group;
            }
            groupCount++;
        }
        return null;
    }

    /**
     * Formatiert Zeitangaben 2h 40 min 2 sek
     * 
     * @param wait
     */
    public static int getMilliSeconds2(String wait) {
        String minutes = new Regex(wait, "(\\d*?)[ ]*m").getMatch(0);
        String hours = new Regex(wait, "(\\d*?)[ ]*(h|st)").getMatch(0);
        String seconds = new Regex(wait, "(\\d*?)[ ]*se").getMatch(0);
        if (minutes == null) minutes = "0";
        if (hours == null) hours = "0";
        if (seconds == null) seconds = "0";
        return Integer.parseInt(hours) * 60 * 60 * 1000 + Integer.parseInt(minutes) * 60 * 1000 + Integer.parseInt(seconds) * 1000;

    }

    /**
     * Setzt vor alle Steuerzeichen ein \
     * 
     * @param pattern
     * @return
     */
    public static String escape(String pattern) {
        char[] specials = new char[] { '(', '[', '{', '\\', '^', '-', '$', '|', ']', '}', ')', '?', '*', '+', '.' };
        StringBuilder sb = new StringBuilder();
        sb.setLength(pattern.length());
        char act;
        for (int i = 0; i < pattern.length(); i++) {
            act = pattern.charAt(i);
            for (char s : specials) {
                if (act == s) {
                    sb.append('\\');
                    break;
                }

            }
            sb.append(act);
        }
        return sb.toString().trim();
    }
}
