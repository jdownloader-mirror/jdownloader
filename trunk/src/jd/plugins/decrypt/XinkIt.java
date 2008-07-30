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

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class XinkIt extends PluginForDecrypt {
    final static String host = "xink.it";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?xink\\.it/f-[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    

    public XinkIt() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            RequestInfo reqinfo = HTTP.getRequest(new URL(parameter));
            File captchaFile = null;
            String capTxt = "";
            String session = "PHPSESSID=" + new Regex(reqinfo.getHtmlCode(), "\\?PHPSESSID=(.*?)\"").getFirstMatch();
            boolean do_continue = false;
            for (int retry = 1; retry < 5; retry++) {

                if (reqinfo.getHtmlCode().indexOf("captcha_send") != -1) {

                    logger.info("Captcha Protected");

                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                    }

                    String captchaAdress = "http://xink.it/captcha-" + new Regex(reqinfo.getHtmlCode(), "src=\"captcha-(.*?)\"", Pattern.CASE_INSENSITIVE).getFirstMatch();
                    captchaAdress += "?" + session;

                    captchaFile = getLocalCaptchaFile(this);
                    JDUtilities.download(captchaFile, captchaAdress);

                    capTxt = JDUtilities.getCaptcha(this, "xink.it", captchaFile, false);

                    String post = "captcha=" + capTxt.toUpperCase() + "&x=70&y=11&" + session;

                    HashMap<String, String> requestHeaders = new HashMap<String, String>();
                    requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

                    reqinfo = HTTP.postRequest(new URL(parameter), null, parameter, requestHeaders, post, true);

                } else {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);
                    }
                    do_continue = true;
                    break;
                }
            }
            if (do_continue == true) {
                String ids[][] = new Regex(reqinfo.getHtmlCode(), "startDownload\\('(.*?)'\\);", Pattern.CASE_INSENSITIVE).getMatches();
                progress.setRange(ids.length);
                for (String[] element : ids) {
                    reqinfo = HTTP.getRequest(new URL("http://xink.it/encd_" + element[0]));
                    decryptedLinks.add(createDownloadlink(XinkItDecodeLink(reqinfo.getHtmlCode())));
                    progress.increase(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    /**
     * 
     * Nachbau der Javascript Entschlüsselung auf xink.it (u.a.
     * http.xink.it/lcv1.js)
     * 
     * @param source
     *            codierte Zeichenkette
     * @return entschlüsselter Link
     * 
     */
    private String XinkItDecodeLink(String source) {

        // implementiert von js vorlage http.xink.it/lcv1.js
        // l001011l10110101l11010101l101l01l( decodiert Base64
        // TODO: hier bitte die JS lib nutzen
        String evalCode = JDUtilities.Base64Decode(source);

        String l010 = JDUtilities.Base64Decode(new Regex(evalCode, "l010 \\= l001011l10110101l11010101l101l01l\\(\"(.*?)\"\\);").getFirstMatch());
        String gt = new Regex(evalCode, "gt\\=\"(.*?)\";").getFirstMatch();
        String l011 = JDUtilities.Base64Decode(new Regex(evalCode, "l011 \\= l001011l10110101l11010101l101l01l\\(\"(.*?)\"\\);").getFirstMatch());
        String l012 = JDUtilities.Base64Decode(gt);

        String r = l012;
        String ar = r;
        String re = "";

        for (int a = 2; a < r.length(); a = a + 4) {

            String temp1 = "";

            int temp2 = a;
            if (temp2 > ar.length()) {
                temp2 = ar.length();
            } else if (temp2 < 0) {
                temp2 = 0;
            }

            int temp3 = a + 2;
            if (temp3 > ar.length()) {
                temp3 = ar.length();
            }

            int temp4 = a + 2 + ar.length() - a;
            if (temp4 > ar.length()) {
                temp4 = ar.length();
            } else if (temp4 < 0) {
                temp4 = temp3;
            }

            temp1 += ar.substring(0, temp2);
            temp1 += ar.substring(temp3, temp4);

            ar = temp1;

        }

        for (int a = 0; a < ar.length(); a = a + 2) {

            for (int i = 0; i < l011.length(); i = i + 2) {

                int temp5 = a + 2;
                if (temp5 > ar.length()) {
                    temp5 = ar.length();
                }

                int temp6 = i + 2;
                if (temp6 > l011.length()) {
                    temp6 = l011.length();
                }

                if (ar.substring(a, temp5).equals(l011.substring(i, temp6))) {

                    re += l010.substring((int) Math.floor(i / 2), (int) Math.floor(i / 2) + 1);

                }

            }

        }

        return re;

    }

}