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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kewlshare.com" }, urls = { "http://[\\w\\.]*?kewlshare\\.com/dl/[\\w]+/" }, flags = { 0 })
public class KewlshareCom extends PluginForHost {

    public KewlshareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://kewlshare.com/tos";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("The Link You requested not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>: (.*?)</title><link rel").getMatch(0);
        String filesize = br.getRegex("<h4>.*?\\|\\| (.*?)</h4>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        Form freeform = br.getForm(1);
        if (freeform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        freeform.put("x", "2");
        freeform.put("y", "68");
        br.submitForm(freeform);
        if (br.containsHTML("You can download your next file after")) {
            int hour = Integer.parseInt(br.getRegex("You can download your next file after (\\d+):(\\d+):(\\d+)</div>").getMatch(0));
            int minute = Integer.parseInt(br.getRegex("You can download your next file after (\\d+):(\\d+):(\\d+)</div>").getMatch(1));
            int sec = Integer.parseInt(br.getRegex("You can download your next file after (\\d+):(\\d+):(\\d+)</div>").getMatch(2));
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (hour * 3600 + minute * 60 + sec) * 1001);
        }
        br.setFollowRedirects(false);
        Form form = br.getForm(0);
        br.submitForm(form);
        String dllink = br.getRegex("\"padding-right:10px;\">.*?<form action=\"(.*?)\" method").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("your current parallel download")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getTimegapBetweenConnections() {
        return 2500;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {

    }

    public void resetDownloadlink(DownloadLink link) {

    }

}
