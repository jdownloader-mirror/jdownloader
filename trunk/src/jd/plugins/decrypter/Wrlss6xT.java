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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision: 7139 $", interfaceVersion = 2, names = { "chaoz.ws" }, urls = { "http://[\\w\\.]*?chaoz\\.ws/woireless/page/album_\\d+\\.html"}, flags = { 0 })


public class Wrlss6xT extends PluginForDecrypt {

    public Wrlss6xT(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

        String fileId = new Regex(parameter, "album_(\\d+)\\.html").getMatch(0);
        String password = br.getRegex("Passwort:(.*?)<br />").getMatch(0);

        br.getPage("http://chaoz.ws/woireless/page/page/crypt.php?a=" + fileId + "&part=0&mirror=a");
        String link = br.getRegex("src=\"(.*?)\"").getMatch(0);
        if (link == null) return null;

        DownloadLink dl_link = createDownloadlink(link);
        dl_link.addSourcePluginPassword(password.trim());
        dl_link.setDecrypterPassword("woireless.6x.to");
        decryptedLinks.add(dl_link);

        return decryptedLinks;
    }

    //@Override
    
}
