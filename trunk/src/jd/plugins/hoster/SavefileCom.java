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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "savefile.com" }, urls = { "http://[\\w\\.]*?savefile\\.com/files/[0-9]+" }, flags = { 0 })
public class SavefileCom extends PluginForHost {

    public SavefileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.savefile.com/tos.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        br.getPage(url);
        if (!br.containsHTML("File not found")) {
            downloadName = Encoding.htmlDecode(br.getRegex(Pattern.compile("Filename:\\s(.*?)\\s+<br />", Pattern.CASE_INSENSITIVE)).getMatch(0));
            downloadSize = (br.getRegex(Pattern.compile("Filesize:\\s(.*?)\\s+<br />", Pattern.CASE_INSENSITIVE)).getMatch(0));
            if (!(downloadName == null || downloadSize == null)) {
                downloadLink.setName(downloadName);
                downloadLink.setDownloadSize(Regex.getSize(downloadSize.replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }
        }

        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    // @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        /* Link holen */
        br.setFollowRedirects(true);
        String fileID = br.getRegex("savefile.com/files/(.*?)\"").getMatch(0);
        String sessionID = br.getCookie("http://savefile.com", "PHPSESSID");
        if (fileID == null || sessionID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.getPage("http://www.savefile.com/downloadmin/" + fileID);
        String linkurl = br.getRegex("try\\s+<a href=\"(.*?)\"").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        /* Datei herunterladen */
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br,downloadLink, linkurl, true, -10);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() == 416) {
            // HTTP/1.1 416 Requested Range Not Satisfiable
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
