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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Hubuploadcom extends PluginForDecrypt {

    public Hubuploadcom(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String links[] = new Regex(br.getPage(parameter), Pattern.compile("<form action=\"(.*?)\"><input type=\"submit\" class=\"dlbutton\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
        progress.setRange(links.length);
        for (String element : links) {
            String link = Encoding.htmlDecode(new Regex(br.getPage(element), Pattern.compile("<iframe src=\"(.*?)\" id=\"hub\"", Pattern.CASE_INSENSITIVE)).getMatch(0));
            if (link != null) decryptedLinks.add(createDownloadlink(link));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}