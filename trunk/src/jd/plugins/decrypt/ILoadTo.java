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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class ILoadTo extends PluginForDecrypt {

    private String patternSupported_Info = "http://iload\\.to/view/.*?/";

    public ILoadTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (parameter.matches(patternSupported_Info)) {
            br.getPage(parameter);
            String password = br.getRegex("class='Value Password'>(.*?)<").getMatch(0);
            String links_page = br.getRegex(Pattern.compile("Head'>OneClick-Hoster<(.+)(class='Head'>Sample)?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            String links[] = new Regex(links_page, Pattern.compile("href='/go/(\\d+)/'", Pattern.CASE_INSENSITIVE)).getColumn(0);
            if (links == null) return null;
            for (String link : links) {
                DownloadLink dl_link = createDownloadlink("http://iload.to/go/" + link + "/");
                dl_link.addSourcePluginPassword(password);
                decryptedLinks.add(dl_link);
            }
        } else {
            br.getPage(parameter);
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
