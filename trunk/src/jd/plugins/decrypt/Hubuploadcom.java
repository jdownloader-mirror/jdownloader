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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Hubuploadcom extends PluginForDecrypt {

    static private String host = "hubupload.com";

    final static private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?hubupload\\.com/files/[a-zA-Z0-9]+/[a-zA-Z0-9]+/(.*)", Pattern.CASE_INSENSITIVE);

    

    public Hubuploadcom() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(cryptedLink);
            RequestInfo reqinfo = HTTP.getRequest(url);
            String Cookie = reqinfo.getCookie();
            String links[] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<form action=\"(.*?)\"><input type=\"submit\" class=\"dlbutton\"", Pattern.CASE_INSENSITIVE)).getMatches(1);

            progress.setRange(links.length);
            for (String element : links) {
                reqinfo = HTTP.getRequest(new URL(element), Cookie, cryptedLink, false);
                String link = JDUtilities.htmlDecode(new Regex(reqinfo.getHtmlCode(), Pattern.compile("<iframe src=\"(.*?)\" id=\"hub\"", Pattern.CASE_INSENSITIVE)).getFirstMatch());
                if (link != null) {
                    decryptedLinks.add(createDownloadlink(link));
                }
                progress.increase(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
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