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
package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class Zippysharecom extends PluginForHost {

    public Zippysharecom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
        br.setFollowRedirects(true);
    }

    @Override
    public String getAGBLink() {
        return "http://www.zippyshare.com/terms.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL().replaceAll("locale=..", "locale=en"));
        if (br.containsHTML("<title>Zippyshare.com - File does not exist</title>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<strong>Name: </strong>(.*?)</font><br", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String filesize = br.getRegex(Pattern.compile("<strong>Size: </strong>(.*?)</font><br", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        String linkurl = new Regex(br, Pattern.compile("'(http.*?www.*?zippyshare\\.com.*?)';", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (linkurl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        String downloadURL = Encoding.htmlDecode(linkurl);
        sleep(10000l, downloadLink);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, downloadURL);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}