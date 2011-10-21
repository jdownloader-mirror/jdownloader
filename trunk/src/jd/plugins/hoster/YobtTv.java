//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yobt.tv" }, urls = { "http://(www\\.)?yobt\\.tv/content/\\d+/.*?\\.html" }, flags = { 0 })
public class YobtTv extends PluginForHost {

    private String DLLINK = null;

    public YobtTv(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decryptTheSecret(final String bismarkishID) {
        int i = 0;
        String a = "";
        String b = "";
        int c = 0;
        int d = 0;
        String e = "";
        while (i < bismarkishID.length()) {
            a = String.valueOf(bismarkishID.charAt(i));
            b = String.valueOf(bismarkishID.charAt(i + 1));
            c = (a.codePointAt(0) - 65) * 16;
            d = b.codePointAt(0) - 65;
            e = e + String.valueOf((char) c + d);
            i += 2;
        }
        return e;
    }

    @Override
    public String getAGBLink() {
        return "http://www.yobt.tv/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>This Video Was Not Found On Our Servers<|>404<|<title>Porn tube, Free HD Porn Videos, XXX Porno Tube Movies, Online Streaming Porn and Free Sex Clips, Pornute</title>)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<div id=\"contents\">[\t\n\r ]+<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.getPage("http://www.yobt.tv/freeporn/" + new Regex(downloadLink.getDownloadURL(), "yobt\\.tv/content/(\\d+)/").getMatch(0) + ".xml");
        final String bismarkishID = br.getRegex("file=\\'(.*?)\\'").getMatch(0);
        final String urlPart = br.getRegex("file_screencast=\\'(.*?)\\'").getMatch(0);
        if (bismarkishID == null || urlPart == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        DLLINK = decryptTheSecret(bismarkishID);
        if (DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        DLLINK = urlPart + DLLINK;
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = ".mp4";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
