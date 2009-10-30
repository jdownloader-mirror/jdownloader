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

//import java.io.File;
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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "safe-it.in" }, urls = { "http://[\\w\\.]*?(safe-it\\.in|evil-warez\\.com)(/\\?s|\\?s)=7&get=[0-9|a-z]+" }, flags = { 0 })
public class SfItIn extends PluginForDecrypt {

    public SfItIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.setFollowRedirects(false);

        /* Error handling */
        if (br.containsHTML(">Downloads: <")) {
            logger.warning("Wrong link");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }

        /* Password handling */
        String pass = br.getRegex("Passwort:.*?style=\"color:#FF0000\">(.*?)</span></td>").getMatch(0);
        ArrayList<String> passwords = new ArrayList<String>();
        if (pass != null && !pass.equals("kein Passwort") && pass.length() != 0) {
            passwords.add(pass);
        }

        /* File package handling */
        //
        if (br.containsHTML("codes/rand.php")) {
            Form captchaForm = br.getForm(0);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String captcha = br.getRegex("wert\" type=\"hidden\" id=\"wert\" value=\"(.*?)\"").getMatch(0);
            if (captcha == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaForm.put("captcha", captcha);
            br.submitForm(captchaForm);
            if (br.containsHTML("codes/rand.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        // Container handling
        if (br.containsHTML("href=\"container") && br.containsHTML("\\.dlc")) {
            decryptedLinks = loadcontainer(br, "dlc");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        String fpName = br.getRegex("<h1 align=\"center\">(.*?)</h1>").getMatch(0).trim();
        fp.setName(fpName);

        String[] links = br.getRegex("background=\"#dbf2f8\";'><a href=\"(.*?)\" target=").getColumn(0);
        if (links == null || links.length == 0) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        progress.setRange(links.length);
        for (String link : links) {
            decryptedLinks.add(createDownloadlink(link));
            progress.increase(1);
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    // by jiaz
    private ArrayList<DownloadLink> loadcontainer(Browser br, String format) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String[] dlclinks = br.getRegex("ter\"><a href=\"(.*?)\"").getColumn(0);
        if (dlclinks == null || dlclinks.length == 0) return null;
        for (String link : dlclinks) {
            link = "http://www.safe-it.in/" + link;
            String test = Encoding.htmlDecode(link);
            File file = null;
            URLConnectionAdapter con = brc.openGetConnection(link);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/safeit/" + test.replaceAll("(http://www.safe-it.in/|/)", ""));
                if (file == null) return null;
                file.deleteOnExit();
                brc.downloadConnection(file, con);
            } else {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            if (file != null && file.exists() && file.length() > 100) {
                ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                if (decryptedLinks.size() > 0) return decryptedLinks;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return null;
    }

}
