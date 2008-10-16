//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LinkSafeWs extends PluginForDecrypt {

    public LinkSafeWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        String[][] files = br.getRegex(Pattern.compile("<input type='hidden' name='id' value='(.*?)' />(.*?)<input type='hidden' name='f' value='(.*?)' />", Pattern.DOTALL)).getMatches();
        progress.setRange(files.length);
        for (String[] elements : files) {
            br.postPage("http://www.linksafe.ws/go/", "id=" + elements[0] + "&f=" + elements[2] + "&Download.x=5&Download.y=10&Download=Download");
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(br.getRegex("src=\"(.*?)\">").getMatch(0))));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
