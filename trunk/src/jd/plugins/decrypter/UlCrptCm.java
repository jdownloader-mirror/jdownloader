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

package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "urlcrypt.com" }, urls = { "http://[\\w\\.]*?urlcrypt\\.com/open-[A-Za-z0-9]+(-[A-Za-z0-9]+|-[A-Za-z0-9]+-[A-Za-z0-9]+)\\.htm" }, flags = { 0 })
public class UlCrptCm extends PluginForDecrypt {

    public UlCrptCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        /* Error handling */
        if (br.containsHTML("Ordner nicht gefunden")) return decryptedLinks;

        if (br.containsHTML("geben Sie bitte jetzt das Passwort ein") || br.containsHTML("Sicherheitsabfrage")) {
            Form captchaForm = br.getForm(0);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            String passCode = null;
            // Captcha handling
            if (br.containsHTML("Sicherheitsabfrage")) {
                String captchalink = "http://www.urlcrypt.com/captcha.php?ImageWidth=120&ImageHeight=37&FontSize=19&CordX=10&CordY=24";
                String code = getCaptchaCode(captchalink, param);
                captchaForm.put("strCaptcha", code);
            }
            // Password handling
            if (br.containsHTML("geben Sie bitte jetzt das Passwort ein")) {
                passCode = Plugin.getUserInput("Password?", param);
                captchaForm.put("strPassword", passCode);
            }
            br.submitForm(captchaForm);
            // Password errorhandling
            if (br.containsHTML("geben Sie bitte jetzt das Passwort ein")) { throw new DecrypterException(DecrypterException.PASSWORD); }
            // Captcha errorhandling
            if (br.containsHTML("Sicherheitsabfrage")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        /* Password handling */
        String pass = br.getRegex("Passwort: <b>(.*?)</b").getMatch(0);
        if (pass != null && pass.equals("kein Passwort")) pass = null;
        // container handling (if no containers found, use webprotection
        if (br.containsHTML("DLC-Container")) {
            decryptedLinks = loadcontainer(br, "dlc");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("RSDF-Container")) {
            decryptedLinks = loadcontainer(br, "rsdf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("CCF-Container")) {
            decryptedLinks = loadcontainer(br, "ccf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        // Webprotection decryption
        decryptedLinks = new ArrayList<DownloadLink>();
        String[] links = br.getRegex("middle;\"><a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            String link0 = link.replace("==-1", "==-0");
            Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(false);
            brc.getPage(link0);
            String finallink = brc.getRedirectLocation();
            // rapidshare links handling, they crypt rapidshare links
            // "extra safe" but with this, the decrypter can handle these
            // rapidshare links
            if (finallink == null) {
                finallink = brc.getRegex("<form id=\"ff\" action=\"(.*?)\" method").getMatch(0);
            }
            DownloadLink dl = createDownloadlink(finallink);
            dl.addSourcePluginPassword(pass);
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> loadcontainer(Browser br, String format) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String[] dlclinks = br.getRegex("(http://www\\.urlcrypt\\.com/download-" + format + "-.*?)\"").getColumn(0);
        if (dlclinks == null || dlclinks.length == 0) return null;
        for (String link : dlclinks) {
            String test = Encoding.htmlDecode(link);
            File file = null;
            URLConnectionAdapter con = brc.openGetConnection(link);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/urlcrypt/" + test.replace("http://www.urlcrypt.com/", "") + "." + format);
                if (file == null) return null;
                file.deleteOnExit();
                brc.downloadConnection(file, con);
            } else {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }

            if (file != null && file.exists() && file.length() > 100) {
                ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                if (decryptedLinks.size() > 0) return decryptedLinks;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }
        }
        return null;
    }

}
