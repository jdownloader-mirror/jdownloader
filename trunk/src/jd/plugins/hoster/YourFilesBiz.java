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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision="$Revision", interfaceVersion=1, names = { "yourfiles.biz"}, urls ={ "http://download\\.youporn\\.com/download/\\d+.*"}, flags = {0})
public class YourFilesBiz extends PluginForHost {

    public YourFilesBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public String getAGBLink() {
        return "http://yourfiles.biz/rules.php";
    }

    //@Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        setLangToEn();
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<b>File name:</b></td>\\s+<td align=left width=[0-9]+%>(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("File size:</b></td>\\s+<td align=left>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    public void setLangToEn() throws IOException {
        br.setCookie("http://yourfiles.to/", "yab_mylang", "en");
    }

    //@Override
    /* public String getVersion() {
        return getVersion("$Revision$");
    } */

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String url = br.getRegex(Pattern.compile("document.location=\"(http.*?getfile\\.php.*?)\"'>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, url);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    //@Override
    public void reset() {
    }

    //@Override
    public void resetPluginGlobals() {
    }

    //@Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
        
    }

}
