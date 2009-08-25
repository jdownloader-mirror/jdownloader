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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

//shareswift.com by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareswift.com" }, urls = { "http://[\\w\\.]*?shareswift\\.viajd/\\w+/.+" }, flags = { 0 })
public class ShareSwiftCom extends PluginForHost {

    public ShareSwiftCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://shareswift.com/tos.html";
    }
    
    // @Overrid
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("shareswift.viajd" , "shareswift.com"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.shareswift.com", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("You have to wait")) {
            logger.warning(JDL.L("plugins.host.ShareSwiftCom.uncheckableduewaittime", "You have to wait or perform a reconnect to check the available status"));
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "You need to perform a reconnect to check the available status");
        }
        if (br.containsHTML("No such file with this filename")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Filename:</b></td><td nowrap>(.*?)</td>").getMatch(0);
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
        if (br.containsHTML("You have to wait")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        Form DLForm = br.getFormbyProperty("name", "F1");
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String passCode = null;
        if (br.containsHTML("<br><b>Passwort:</b>")) {
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            DLForm.put("password", passCode);
        }
        int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
        sleep(tt * 1001l, link);
        br.submitForm(DLForm);
        if (br.containsHTML("Wrong password")) {
            logger.warning("Wrong password!");
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        String dllink = br.getRegex("Dieser direkte Downloadlink.*?href=\"(.*?)\">").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        BrowserAdapter.openDownload(br, link, dllink, true, -20);
        dl.startDownload();
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

}
