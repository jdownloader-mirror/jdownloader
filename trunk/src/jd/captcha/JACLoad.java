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
import java.io.IOException;

import jd.controlling.JDLogger;
import jd.http.Browser;

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
        // String methodsPath=Utilities.getFullPath(new String[] {
        // JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(),
        // "jd", "captcha", "methods"});

        loadMegaUpload();
    }

    private void loadMegaUpload() {
        final String dir = "/home/dwd/.jd_home/captchas/badongo.com/";

        for (int i = 0; i < 500; i++) {
        	final int b = i;
            new Thread(new Runnable() {
                public void run() {
                    Browser br = new Browser();
                    try {
                    	br.getPage("http://www.badongo.com/de/file/14240881"+ "?rs=displayCaptcha&rst=&rsrnd=" + System.currentTimeMillis() + "&rsargs[]=yellow");
                        String cid = br.getRegex("cid=(\\d+)").getMatch(0);
                        br.getDownload(new File(dir + b + ".jpg"), "http://www.badongo.com/ccaptcha.php?cid=" + cid);
                    } catch (IOException e) {
                        JDLogger.exception(e);
                    }
                }
            }).start();
        }
    }

}