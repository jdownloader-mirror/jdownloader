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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class FalinksCom extends PluginForDecrypt {

    public FalinksCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        try {
            br.getPage(parameter);
            String pw = br.getRegex("</form>\npw: (.*?)\n.*?</td>").getMatch(0);
            String[] links = br.getRegex("\\<input type=\"hidden\" name=\"url\" value=\"(.*?)\" \\/\\>").getColumn(0);
            progress.setRange(links.length);
            for (String link : links) {
                DownloadLink dlLink = createDownloadlink(link);
                dlLink.addSourcePluginPassword(pw);
                decryptedLinks.add(dlLink);
                progress.increase(1);
            }
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occured", e);
            return null;
        }
        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}