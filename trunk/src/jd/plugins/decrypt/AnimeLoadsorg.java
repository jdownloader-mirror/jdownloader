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
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class AnimeLoadsorg extends PluginForDecrypt {
    static private String host = "anime-loads.org";

    public AnimeLoadsorg(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> links = new ArrayList<String>();
        String parameter = param.toString();

        br.setCookiesExclusive(true);
        br.clearCookies(host);

        if (parameter.contains("crypt")) {
            links.add(parameter);
        } else {
            br.getPage(parameter);
            String[][] help = br.getRegex("<a href=\"(crypt\\.php.*?)\"").getMatches();

            for (String[] help1 : help)
                links.add(help1[0]);
        }

        for (String link : links) {
            br.getPage(link);
            String dllink = Encoding.htmlDecode(br.getRegex("iframe src=\'(.*?)\'").getMatch(0));
            decryptedLinks.add(createDownloadlink(dllink));
        }

        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}