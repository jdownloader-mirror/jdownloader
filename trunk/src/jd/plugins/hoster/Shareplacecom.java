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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareplace.com" }, urls = { "http://[\\w\\.]*?shareplace\\.com/\\?[\\w]+(/.*?)?" }, flags = { 0 })
public class Shareplacecom extends PluginForHost {

    private String url;

    public Shareplacecom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://shareplace.com/rules.php";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        url = downloadLink.getDownloadURL();
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        br.getPage(url);
        if (br.containsHTML("Your requested file is not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (br.getRedirectLocation() == null) {
            String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("File name: </b>(.*?)<b>", Pattern.CASE_INSENSITIVE)).getMatch(0));
            String filesize = br.getRegex("File size: </b>(.*?)<b><br>").getMatch(0);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
            return AvailableStatus.TRUE;
        } else
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("File name: </b>(.*?)<b>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        filename = filename.replace("(", "%2528");
        filename = filename.replace(")", "%2529");
        String page = Encoding.urlDecode(br.toString(), true);
        String[] links = HTMLParser.getHttpLinks(page, null);
        boolean found = false;
        // waittime
        if (br.containsHTML("var ziptime =")) {
            int tt = Integer.parseInt(br.getRegex("var ziptime = (\\d+);").getMatch(0));
            sleep(tt * 1001l, downloadLink);
        }
        for (String link : links) {
            if (!link.contains(filename)) continue;
            Browser brc = br.cloneBrowser();
            dl = BrowserAdapter.openDownload(brc, downloadLink, link);
            if (dl.getConnection().isContentDisposition()) {
                found = true;
                break;
            } else {
                dl.getConnection().disconnect();
            }
        }
        if (!found) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        /* Workaround für fehlerhaften Filename Header */
        String name = Plugin.getFileNameFromHeader(dl.getConnection());
        if (name != null) downloadLink.setFinalFileName(Encoding.urlDecode(name, false));
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
