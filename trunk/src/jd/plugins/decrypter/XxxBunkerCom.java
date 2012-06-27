//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

//EmbedDecrypter 0.1
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xxxbunker.com" }, urls = { "http://(www\\.)?xxxbunker\\.com/[a-z0-9_\\-]+" }, flags = { 0 })
public class XxxBunkerCom extends PluginForDecrypt {

    public XxxBunkerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>xxxbunker\\.com : ([^<>\"]*?)</title>").getMatch(0);
        // filename needed for all IDs below here
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());
        String externID = br.getRegex("setTimeout\\(function\\(\\)\\{pl\\((\\d+),").getMatch(0);
        if (externID != null) {
            br.getPage("http://xxxbunker.com/videoPlayer.php?videoid=" + externID + "&autoplay=true&ageconfirm=false&title=true&r=" + System.currentTimeMillis());
            externID = br.getRegex("%2Fflashservices%2Fgateway\\.php%7C(\\d+)%7Cdefault%7").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink("http://www.xvideos.com/video" + externID));
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        externID = br.getRegex("postbackurl=([^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            br.getPage(Encoding.Base64Decode(externID));
            externID = br.getRedirectLocation();
            if (externID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(externID));
            dl.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

}
