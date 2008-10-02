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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RapidshareComFolder extends PluginForDecrypt {

    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

    public RapidshareComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        String parameter = param.toString();

        String page = br.getPage(parameter);
        String password = "";

        for (int retry = 1; retry < 5; retry++) {
            if (page.contains("input type=\"password\" name=\"password\"")) {
                password = getUserInput(null, param);
                page = br.postPage(parameter, "password=" + password);
            } else {
                break;
            }
        }

        getLinks(parameter, password, page);

        return decryptedLinks;
    }

    private void getLinks(String para, String password, String source) throws IOException {
        String[] folders = new Regex(source, "font\\-size:12pt\\;\" href=\"javascript:folderoeffnen\\('(\\d+?)'\\);").getColumn(0);
        String[] links = new Regex(source, "<a style=\"font-size:12pt;\" target=\"_blank\" href=\"http://rapidshare.com/files/(.*?)\">").getColumn(0);
        for (String element : folders) {
            getLinks(para, password, br.postPage(para, "password=" + password + "&subpassword=&browse=ID%3D" + element));
        }

        for (String element : links) {
            decryptedLinks.add(createDownloadlink("http://rapidshare.com/files/" + element));
        }
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}