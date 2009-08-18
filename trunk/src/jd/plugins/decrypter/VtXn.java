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

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vetax.in" }, urls = { "http://[\\w\\.]*?vetax\\.in/view/\\d+|http://[\\w\\.]*?vetax\\.in/(dload|mirror)/[\\w]+" }, flags = { 0 })
public class VtXn extends PluginForDecrypt {

    static private final String patternSupported_Download = "http://[\\w\\.]*?vetax\\.in/(dload|mirror)/[a-zA-Z0-9]+";

    public VtXn(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        if (parameter.matches(patternSupported_Download)) {
            String links[] = br.getRegex(Pattern.compile("<input name=\"feld.*?\" value=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
            if (links != null) {
                progress.setRange(links.length);
                for (String element : links) {
                    decryptedLinks.add(createDownloadlink(element));
                    progress.increase(1);
                }
            }
            if (decryptedLinks.size() == 0) {
                links = br.getRegex(Pattern.compile("target=_blank href=(.*?)style", Pattern.CASE_INSENSITIVE)).getColumn(0);
                if (links != null) {
                    progress.setRange(links.length);
                    for (String element : links) {
                        decryptedLinks.add(createDownloadlink(element.trim()));
                        progress.increase(1);
                    }
                }
            }
        } else {
            String pw = br.getRegex(Pattern.compile("<strong>Passwort:</strong></td>.*?<strong>(.*?)</strong>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            String links[] = br.getRegex(Pattern.compile("<a href=\"(/(dload|mirror)/.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
            String rsdf = br.getRegex(Pattern.compile("<a href=\"(/crypt\\.php\\?.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (rsdf != null) {
                File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");
                Browser.download(container, br.openGetConnection("http://vetax.in" + rsdf));
                ArrayList<DownloadLink> dl_links = JDUtilities.getController().getContainerLinks(container);
                container.delete();
                for (DownloadLink dl_link : dl_links) {
                    dl_link.addSourcePluginPassword(pw);
                    decryptedLinks.add(dl_link);
                }

            }
            progress.setRange(links.length);
            for (String element : links) {
                DownloadLink dl_link = createDownloadlink("http://vetax.in" + element);
                dl_link.addSourcePluginPassword(pw);
                decryptedLinks.add(dl_link);
                progress.increase(1);
            }
        }

        return decryptedLinks;
    }

    // @Override

}
