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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareator.net" }, urls = { "http://[\\w\\.]*?shareator\\.net/[0-9a-z]+" }, flags = { 0 })
public class ShareatorNet extends PluginForHost {

    public ShareatorNet(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://shareator.net/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.teradepot.com", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("You're using all download slots for IP")) {                
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = br.getRegex("<b>Filename:</b></td><td nowrap>(.*?)</b>").getMatch(0);
        String filesize = br.getRegex("<small>\\((.*?)\\)</small>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("You're using all download slots for IP")) {                
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 1 * 60 * 1001l);
        }
        Form dlform0 = br.getFormbyProperty("name", "F1");
        if (dlform0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dlform0.remove("method_premium");
        // waittime
        int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
        sleep(tt * 1001l, link);
        String passCode = null;
        if (br.containsHTML("<br><b>Password:</b>")) {
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            dlform0.put("password", passCode);
        }
        br.submitForm(dlform0);
        if (br.containsHTML("Wrong password")) {
            logger.warning("Wrong password!");
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        String dllink = br.getRegex(Pattern.compile("dotted #bbb;padding:[0-9]px;\">.*?<a href=\"(.*?)\"", Pattern.DOTALL)).getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1).startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
