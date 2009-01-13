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

package jd.captcha;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;

/**
 * JAC Tester
 * 
 * 
 * @author JD-Team
 */
public class JACLoad {
    /**
     * @param args
     */
    public static void main(String args[]) {

        JACLoad main = new JACLoad();
        main.go();
    }

    private void go() {
        // String methodsPath=UTILITIES.getFullPath(new String[] {
        // JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(),
        // "jd", "captcha", "methods"});
        String hoster = "SerienJunkies.dl.am";

        loadSerienJunkies(new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/jd/captcha/methods" + "/" + hoster + "/captchas/"), 6000);

    }

    private void loadSerienJunkies(File file, int i) {
        UTILITIES.useCookies = true;
        // long stamp= UTILITIES.getTimer();
        while (i > 0) {
            i--;
            UTILITIES.cookie = null;
            String html = UTILITIES.getPagewithScanner("http://85.17.177.195/sjsafe/f-e5750571accc91e7/rc_las-vegas-s04e01-dvdrip.html");
            html = html.replaceAll("(?s)<!--.*?-->", "");
            html = html.replaceAll("(?s)<.*?c8d1ae64a5be11b14c8140d243b198d9.gif.*?>", "");
            String path = "http://85.17.177.195" + UTILITIES.getMatches(html, "<TD><IMG SRC=\"°\" ALT=\"\" BORDER=\"0\"")[0];
            URL url = null;
            try {
                url = new URL(path);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            int c = 0;
            File dest = new File(file, "captcha_" + c + "_" + url.getPath().substring(url.getPath().lastIndexOf("/") + 1));

            while (dest.exists()) {
                c++;
                UTILITIES.getLogger().info(i + "DOPPELT!! " + path);
                dest = new File(file, "captcha_" + c + "_" + url.getPath().substring(url.getPath().lastIndexOf("/") + 1));
            }

            UTILITIES.downloadBinary(dest.getAbsolutePath(), path);
            UTILITIES.getLogger().info(i + "new captcha: : " + path);

        }
        UTILITIES.useCookies = false;

    }
}