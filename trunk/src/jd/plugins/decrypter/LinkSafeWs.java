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
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LinkSafeWs extends PluginForDecrypt {

    public LinkSafeWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        String[][] files = br.getRegex(Pattern.compile("name='id' value='(.*?)'(.*?)name='f' value='(.*?)' />", Pattern.DOTALL)).getMatches();
        progress.setRange(files.length);
        for (String[] elements : files) {
            br.postPage("http://www.linksafe.ws/go/", "id=" + elements[0] + "&f=" + elements[2] + "&Download.x=5&Download.y=10&Download=Download");
            String codedlink = br.getRegex("iframe.*?src=\"(.*?)\">").getMatch(0);
            codedlink = codedlink.replaceAll("&#", ";&#");
            codedlink = codedlink.replaceFirst(";&", "&");
            codedlink = codedlink + ";";
            System.out.println(codedlink);
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(codedlink)));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
