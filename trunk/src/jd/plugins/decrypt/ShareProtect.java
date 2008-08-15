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

import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class ShareProtect extends PluginForDecrypt {
    final static String host = "shareprotect.t-w.at";
    private Pattern patternSupported = Pattern.compile("http://shareprotect\\.t-w\\.at/\\?id\\=[a-zA-Z0-9\\-]{3,10}", Pattern.CASE_INSENSITIVE);

    public ShareProtect() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            Browser.clearCookies(host);

            br.getPage(parameter);
            String[] matches = br.getRegex("unescape\\(\\'(.*?)'\\)").getColumn(-1);
            StringBuffer htmlc = new StringBuffer();
            for (String element : matches) {
                htmlc.append(Encoding.htmlDecode(element) + "\n");
            }

            String[] links = new Regex(htmlc, "<input type=\"button\" value=\"Free\" onClick=.*? window\\.open\\(\\'\\./(.*?)\\'").getColumn(-1);
            progress.setRange(links.length);
            htmlc = new StringBuffer();
            for (String element : links) {

                br.getPage("http://" + br.getHost() + "/" + element);
                htmlc.append(Encoding.htmlDecode(br.getRegex("unescape\\(\\'(.*?)'\\)").getMatch(0)) + "\n");
                progress.increase(1);
            }
            br.getRequest().setHtmlCode(htmlc.toString());
            Form[] forms = br.getForms();
            for (Form element : forms) {
                decryptedLinks.add(createDownloadlink(element.action));
            }
        } catch (Exception e) {
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}