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

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharebank.ws" }, urls = { "http://[\\w\\.]*?(mygeek|sharebank)\\.ws/\\?(v|go)=[\\w]+"}, flags = { 0 })


public class SharebankWs extends PluginForDecrypt {
    private static final String REGEX_FOLDER = ".*?ws/\\?v=[a-zA-Z0-9]+";
    private static final String REGEX_DLLINK = ".*?ws/\\?go=([a-zA-Z0-9]+)";

    public SharebankWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = param.toString();
        String pref = "http://sharebank.ws";
        String[] links = null;
        if (url.matches(REGEX_FOLDER)) {
            br.getPage(url);
            links = br.getRegex(Pattern.compile("go=(.*?)'")).getColumn(0);
        } else if (url.matches(REGEX_DLLINK)) {
            links = new Regex(url, REGEX_DLLINK).getColumn(0);
        } else {
            logger.severe("Ungültiges Pattern in der JDinit für Sharebank.Ws");
        }

        progress.setRange(links.length);
        if (url.contains("mygeek")) {
            pref = "http://mygeek.ws";
        }
        for (String element : links) {
            /* Get the security id and save them */
            Browser brc = br.cloneBrowser();
            brc.getPage(pref + "/?go=" + element);
            String finalLink = null;
            for (int retry = 0; retry < 5; retry++) {
                String securityId = brc.getRegex(Pattern.compile("(go=.*?&q1=.*?&q2=.*?)>")).getMatch(0);
                brc.getPage(pref + "/?" + securityId);
                finalLink = brc.getRegex(Pattern.compile(">document.location='(.*?)';<")).getMatch(0);
                if (finalLink == null) {
                    finalLink = brc.getRegex(Pattern.compile("base64_decode\\('(.*?)'\\)")).getMatch(0);
                    finalLink = Encoding.Base64Decode(finalLink);
                }
                if (finalLink == null && brc.getRedirectLocation() != null) {
                    finalLink = brc.getRedirectLocation();
                }
                if (finalLink != null && !finalLink.startsWith("?go")) {
                    break;
                }
            }
            if (finalLink == null) return null;
            /* find base64 coded url */
            decryptedLinks.add(createDownloadlink(finalLink));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    // @Override
    
}
