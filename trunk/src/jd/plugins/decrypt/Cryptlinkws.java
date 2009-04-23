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

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.http.HTMLEntities;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class Cryptlinkws extends PluginForDecrypt {

    final static private Pattern patternSupported_File = Pattern.compile("http://[\\w\\.]*?cryptlink\\.ws/crypt\\.php\\?file=[0-9]+", Pattern.CASE_INSENSITIVE);
    final static private Pattern patternSupported_Folder = Pattern.compile("http://[\\w\\.]*?cryptlink\\.ws/\\?file=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public Cryptlinkws(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (parameter.matches(patternSupported_File.pattern())) {
            /* Einzelne Datei */
            br.getPage(parameter);
            String link = br.getRegex("unescape\\(('|\")(.*?)('|\")\\)").getMatch(1);
            link = fixLink(Encoding.htmlDecode(link));
            
            br.getPage("http://www.cryptlink.ws/" + link);
            link = br.getRegex("unescape\\(('|\")(.*?)('|\")\\)").getMatch(1);
            link = fixLink(Encoding.htmlDecode(link));

            if (link.startsWith("cryptfiles/")) {
                /* Weiterleitung durch Server */
                br.getPage("http://www.cryptlink.ws/" + link);

                /* Das hier ist eher weniger gut gelöst */
                decryptedLinks.addAll(new DistributeData(br.toString()).findLinks(false));
            } else {
                /* Direkte Weiterleitung */
                decryptedLinks.add(createDownloadlink(link));
            }
        } else if (parameter.matches(patternSupported_Folder.pattern())) {
            /* ganzer Ordner */
            boolean do_continue = false;
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {

                br.getPage(parameter);

                Form[] forms = br.getForms();

                if (forms.length == 1) {
                    /* Weder Captcha noch Passwort vorhanden */
                } else {
                    /* Captcha vorhanden, Passwort vorhanden oder beides */

                    if (forms[0].hasInputFieldByName("folderpass")) {
                        /* Eingabefeld für Passwort vorhanden */
                        String password = getUserInput(null, param);
                        forms[0].put("folderpass", password);

                    }

                    if (forms[0].hasInputFieldByName("captchainput")) {
                        /* Eingabefeld für Captcha vorhanden */

                        File captchaFile = getLocalCaptchaFile(this);
                        String captchaCode;
                        br.cloneBrowser().getDownload(captchaFile, "http://www.cryptlink.ws/captcha.php");
                        /* CaptchaCode holen */
                        captchaCode = getCaptchaCode(captchaFile, this, param);
                        forms[0].put("captchainput", captchaCode);

                    }

                    br.submitForm(forms[0]);
                }

                if (!br.containsHTML("Wrong Password! Klicken Sie") && !br.containsHTML("Wrong Captchacode! Klicken Sie")) {
                    do_continue = true;
                    break;
                }

            }

            if (do_continue == true) {
                String[] links = br.getRegex("href=\"crypt\\.php\\?file=(.*?)\"").getColumn(0);
                progress.setRange(links.length);
                for (String element : links) {
                    decryptedLinks.add(createDownloadlink("http://www.cryptlink.ws/crypt.php?file=" + element));
                    progress.increase(1);
                }
            }
        }

        return decryptedLinks;
    }

    private String fixLink(String link) {
        link = link.replaceAll("&#", ";&#").substring(1) + ";";
        link = HTMLEntities.unhtmlentities(link);
        return link;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}