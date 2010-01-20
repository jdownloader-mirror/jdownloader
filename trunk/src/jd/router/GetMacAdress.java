//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.router;

import java.net.InetAddress;

import jd.controlling.JDLogger;
import jd.nutils.OSDetector;
import jd.parser.Regex;
import jd.utils.JDUtilities;
import jd.utils.StringUtil;

public final class GetMacAdress {
    /**
     * Don't let anyone instantiate this class.
     */
    private GetMacAdress() {
    }

    public static String getMacAddress() throws Exception {
        try {
            return GetMacAdress.getMacAddress(RouterInfoCollector.getRouterIP());
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public static String getMacAddress(final InetAddress hostAddress) throws Exception {
        final String resultLine = callArpTool(hostAddress.getHostAddress());
        String rd = new Regex(resultLine, "..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?").getMatch(-1).replaceAll("-", ":");
        if (rd == null) return null;
        rd = rd.replaceAll("\\s", "0");
        final String[] d = rd.split("[:\\-]");
        final StringBuilder ret = new StringBuilder(18);
        for (final String string : d) {
            if (string.length() < 2) {
                ret.append('0');
            }
            ret.append(string);
            ret.append(':');
        }
        return ret.toString().substring(0, 17);
    }

    private static String callArpTool(final String ipAddress) throws Exception {
        if (OSDetector.isWindows()) return callArpToolWindows(ipAddress);
        return callArpToolDefault(ipAddress);
    }

    private static String callArpToolWindows(final String ipAddress) throws Exception {
        final ProcessBuilder pb = new ProcessBuilder(new String[] { "ping", ipAddress });
        pb.start();
        final String[] parts = JDUtilities.runCommand("arp", new String[] { "-a" }, null, 10).split(StringUtil.LINE_SEPARATOR);
        pb.directory();
        for (final String part : parts) {
            if (part.indexOf(ipAddress) > -1) { return part; }
        }
        return null;
    }

    private static String callArpToolDefault(final String ipAddress) throws Exception {
        String out = null;
        final InetAddress hostAddress = InetAddress.getByName(ipAddress);
        ProcessBuilder pb = null;
        try {
            pb = new ProcessBuilder(new String[] { "ping", ipAddress });
            pb.start();
            out = JDUtilities.runCommand("arp", new String[] { ipAddress }, null, 10);
            pb.directory();
            if (!out.matches("(?is).*((" + hostAddress.getHostName() + "|" + hostAddress.getHostAddress() + ").*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?|.*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?.*(" + hostAddress.getHostName() + "|" + hostAddress.getHostAddress() + ")).*")) out = null;
        } catch (Exception e) {
            if (pb != null) pb.directory();
        }
        if (out == null || out.trim().length() == 0) {
            try {
                pb = new ProcessBuilder(new String[] { "ping", ipAddress });
                pb.start();
                out = JDUtilities.runCommand("ip", new String[] { "neigh", "show" }, null, 10);
                pb.directory();
                if (out != null) {
                    if (!out.matches("(?is).*((" + hostAddress.getHostName() + "|" + hostAddress.getHostAddress() + ").*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?|.*..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?[:\\-]..?.*(" + hostAddress.getHostName() + "|" + hostAddress.getHostAddress() + ")).*")) {
                        out = null;
                    } else {
                        out = new Regex(out, "(" + hostAddress.getHostName() + "|" + hostAddress.getHostAddress() + ")[^\r\n]*").getMatch(-1);
                    }
                }
            } catch (Exception e) {
                if (pb != null) {
                    pb.directory();
                }
            }
        }
        return out;
    }

}
