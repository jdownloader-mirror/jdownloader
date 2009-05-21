//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class ZeroSecWs extends PluginForDecrypt {

    public ZeroSecWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> passwords;
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String directComment = new Regex(param.toString(), "http://[\\w\\.]*?zerosec\\.ws/.+/.+/#comments|/.+/#comments|/.+/.*?#(comment-\\d+)").getMatch(0);

        if (directComment != null) {
            String comment = br.getRegex(Pattern.compile("<div class=\"even.*?\" id=\"" + directComment + "\"><a name=\"comment-\\d+\"></a>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            passwords = HTMLParser.findPasswords(comment);
            String[] links = new Regex(comment, "rel=\"nofollow\">(.*?)</a>", Pattern.CASE_INSENSITIVE).getColumn(0);
            for (String link : links) {
                if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link, true)) {
                    DownloadLink dLink = createDownloadlink(link);
                    dLink.addSourcePluginPasswordList(passwords);
                    decryptedLinks.add(dLink);
                }
            }
        } else {
            ArrayList<String> pages = new ArrayList<String>();
            pages.add(param.toString());
            String comment_pages_tag = br.getRegex(Pattern.compile("<!-- Comment page numbers -->(.*?)<!-- End comment page numbers -->", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (comment_pages_tag != null) {
                String comment_pages[] = new Regex(comment_pages_tag, "<a class=\".*?-comment-page\" href=\"(.*?)\"").getColumn(0);
                for (String page : comment_pages) {
                    pages.add(page);
                }
            }
            for (String page : pages) {
                br.getPage(page);
                String comments[] = br.getRegex(Pattern.compile("<div class=\"even.*?\" id=\"comment-\\d+\".*?>(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getColumn(0);
                for (String comment : comments) {
                    passwords = HTMLParser.findPasswords(comment);
                    String[] links = new Regex(comment, "rel=\"nofollow\">(.*?)</a>", Pattern.CASE_INSENSITIVE).getColumn(0);
                    for (String link : links) {
                        if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link, true)) {
                            DownloadLink dLink = createDownloadlink(link);
                            dLink.addSourcePluginPasswordList(passwords);
                            decryptedLinks.add(dLink);
                        }
                    }
                }
            }
        }
        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
