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
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision="$Revision", interfaceVersion=1, names = { "load.to"}, urls ={ "http://(\\w*\\.)?load\\.to/[\\?d=]?[\\w]+.*"}, flags = {0})
public class LoadTo extends PluginForHost {

    public LoadTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public String getAGBLink() {

        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
        this.setStartIntervall(2000l);
        return "http://www.load.to/terms.php";
    }

    //@Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        for (int i = 1; i < 3; i++) {
            try {
                br.getPage(url);
            } catch (Exception e) {
                continue;
            }
            if (!br.containsHTML("Can't find file")) {
                downloadName = Encoding.htmlDecode(br.getRegex(Pattern.compile("<b>(.*?)</b></a><br /><br /><table", Pattern.CASE_INSENSITIVE)).getMatch(0));
                downloadSize = (br.getRegex(Pattern.compile(">([0-9]*? Bytes)</td>", Pattern.CASE_INSENSITIVE)).getMatch(0));
                if (!(downloadName == null || downloadSize == null)) {
                    downloadLink.setName(downloadName);
                    downloadLink.setDownloadSize(Regex.getSize(downloadSize.replaceAll(",", "\\.")));
                    return AvailableStatus.TRUE;
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    //@Override
    /* public String getVersion() {
        return getVersion("$Revision$");
    } */

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        /* Link holen */
        String linkurl = Encoding.htmlDecode(new Regex(br, Pattern.compile("action=\"(http.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, linkurl, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        dl.startDownload();
    }

    //@Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        return 8;
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
