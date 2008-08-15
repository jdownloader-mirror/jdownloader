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
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class NewzFindCom extends PluginForDecrypt {

    final static String host = "newzfind.com";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?newzfind\\.com/(video|music|games|software|mac|graphics|unix|magazines|e-books|xxx|other)/.+", Pattern.CASE_INSENSITIVE);

    public NewzFindCom() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        try {

            String url = "http://newzfind.com/ajax/links.html?a=" + parameter.substring(parameter.lastIndexOf("/") + 1);
            RequestInfo reqinfo = HTTP.getRequest(new URL(url));
            String links[][] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<link title=\"(.*?)\"")).getMatches();

            reqinfo = HTTP.getRequest(new URL(parameter));
            Vector<String> pws = HTMLParser.findPasswords(reqinfo.getHtmlCode());
            default_password.addAll(pws);

            progress.setRange(links.length);

            for (String[] element : links) {
                decryptedLinks.add(createDownloadlink(element[0]));
                progress.increase(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

}