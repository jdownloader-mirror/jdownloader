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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class KnofflCom extends PluginForDecrypt {

    public KnofflCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String properurl = br.getRegex("frame src=\"([^\\s]+)\" name=\"main\"").getMatch(0);
        if (properurl != null) br.getPage(properurl);
        if (br.containsHTML("Seite wird geladen"))
        {
            properurl = null;
            properurl = br.getRegex("content=\"0;URL=([\\s]+)\"").getMatch(0);
            if (properurl == null) properurl = br.getRegex("a href=\"([\\s]+)\"").getMatch(0);
            if (properurl != null) br.getPage(properurl);
        }
        String[] links = br.getRegex("dl\\('(.*?)'\\)").getColumn(0);
        if (links.length == 0) {
            String url = br.getRegex("<a href=\"(http://.*?knoffl\\.com.*?/.*?)\">").getMatch(0);
            br.getPage(url);
            links = br.getRegex("dl\\('(.*?)'\\)").getColumn(0);
        }
        String codedurl;
        for (int i=0;i<links.length;i++) {
            codedurl = new Regex(links[i],"&go=([a-zA-Z0-9=]+)").getMatch(0);
            decryptedLinks.add(createDownloadlink(Encoding.Base64Decode(codedurl)));
        }
        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
