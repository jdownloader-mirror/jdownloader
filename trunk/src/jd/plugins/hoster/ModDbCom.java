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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moddb.com" }, urls = { "http://[\\w\\.]*?moddb\\.com/(games|mods|engines|groups)/.*?/(addons|downloads)/[0-9a-z-]+" }, flags = { 0 })
public class ModDbCom extends PluginForHost {

    public ModDbCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private static final String moddbservers = "moddbservers";

    /** The list of server values displayed to the user */
    private static final String[] servers;

    static {
        servers = new String[] { "fdcservers.net(WORLDWIDE)", "moddb.com #4(TEXAS, US)", "moddb.com #5(COLORADO, US)", "moddb.com #6(COLORADO, US)" };
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), moddbservers, servers, JDL.L("plugins.host.L4dMapsCom.servers", "Use this server:")).setDefaultValue(0));
    }

    private int getConfiguredServer() {
        switch (getPluginConfig().getIntegerProperty(moddbservers, -1)) {
        case 0:
            logger.fine("The server fdcserver.net is configured");
            return 1;
        case 1:
            logger.fine("The servermoddb.com #4  is configured");
            return 2;
        case 2:
            logger.fine("The server  ismoddb.com #5 configured");
            return 3;
        case 3:
            logger.fine("The server  ismoddb.com #6 configured");
            return 4;
        default:
            logger.fine("No server is configured, returning default server (fdcserver.net)");
            return 1;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("An error has occured") || br.containsHTML("The download requested could not be found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h5>Filename</h5>.*?<span class=\"summary\">(.*?)</span>").getMatch(0);
        String filesize = br.getRegex("<h5>Size</h5>.*?<span class=\"summary\">.*?\\((.*?bytes)\\)</span>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("<h5>Size</h5>.*?<span class=\"summary\">(.*?)\\(").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replace(",", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = null;
        int configuredServer = getConfiguredServer();
        // Get pages with the mirror
        String singlemirrorpage = br.getRegex("window\\.open\\('(.*?)'").getMatch(0);
        String mirrorid = br.getRegex("id=\"downloads(\\d+)report\"").getMatch(0);
        if (mirrorid == null) {
            mirrorid = br.getRegex("siteareaid=(\\d+)\"").getMatch(0);
            if (mirrorid == null) {
                mirrorid = br.getRegex("start/(\\d+)").getMatch(0);
            }
        }
        if (mirrorid != null) {
            br.getPage("http://www.moddb.com/downloads/start/" + mirrorid + "/all");
        } else {
            String standardmirrorpage = br.getRegex("attr\\(\"href\", \"(.*?)\\&amp;").getMatch(0);
            if (standardmirrorpage != null) br.getPage("http://www.moddb.com" + standardmirrorpage);
        }

        // Try to find the link for the selected servers
        if (configuredServer == 1) {
            dllink = br.getRegex("Mirror provided by FDCCDN.*?<a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) br.getRegex("http://www\\.fdcservers\\.net.*?<a href=\"(.*?)\"").getMatch(0);

        } else if (configuredServer == 2) {
            dllink = br.getRegex("Mirror provided by Mod DB #4.*?<a href=\"(.*?)\"").getMatch(0);
        } else if (configuredServer == 3) {
            dllink = br.getRegex("Mirror provided by Mod DB #5.*?<a href=\"(.*?)\"").getMatch(0);
        } else if (configuredServer == 4) {
            dllink = br.getRegex("Mirror provided by Mod DB #6.*?<a href=\"(.*?)\"").getMatch(0);
        }
        // Some servers aren't always available, therefore we got this check,
        // but it's basically just good for the logger
        if (dllink.contains("members/register")) {
            logger.info("The server you tried to use is only available for premium users, using other server...");
            dllink = null;
        }
        // If the link for the selected server couldn't be found the plugin
        // tries quite
        // many methods to get *any* other valid link
        if (dllink == null) {
            logger.warning("Something is broken here, your selected server hasn't been found so jd will try to find an alternative link");
            dllink = br.getRegex("Mirror provided by FDCCDN.*?<a href=\"(.*?)\"").getMatch(0);
            if (dllink.contains("members/register")) dllink = null;
            if (dllink == null) {
                dllink = br.getRegex("http://www\\.fdcservers\\.net.*?<a href=\"(.*?)\"").getMatch(0);
                if (dllink.contains("members/register")) dllink = null;
            } else if (dllink == null) {
                dllink = br.getRegex("Mirror provided by Mod DB #4.*?<a href=\"(.*?)\"").getMatch(0);
                if (dllink.contains("members/register")) dllink = null;
            } else if (dllink == null) {
                dllink = br.getRegex("Mirror provided by Mod DB #5.*?<a href=\"(.*?)\"").getMatch(0);
                if (dllink.contains("members/register")) dllink = null;
            } else if (dllink == null) {
                dllink = br.getRegex("Mirror provided by Mod DB #6.*?<a href=\"(.*?)\"").getMatch(0);
                if (dllink.contains("members/register")) dllink = null;
            } else if (dllink == null) {
                dllink = br.getRegex("Mirror provided by Mod DB.*?<a href=\"(.*?)\"").getMatch(0);
                if (dllink.contains("members/register")) dllink = null;
            } else if (dllink == null) {
                dllink = br.getRegex("Click to <a href=\"(.*?)</a>").getMatch(0);
                if (dllink.contains("members/register")) dllink = null;
            } else if (dllink == null) {
                if (singlemirrorpage != null) {
                    br.getPage("http://www.moddb.com" + singlemirrorpage);
                    dllink = br.getRegex("Click to <a href=\"(.*?)</a>").getMatch(0);
                }

            }
        }
        if (dllink == null) {
            logger.info("no final downloadlink (dllink) has been found, the plugin must be defect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = "http://www.moddb.com" + dllink;
        br.setFollowRedirects(false);
        br.getPage(dllink);
        dllink = br.getRedirectLocation();
        if (dllink == null) {
            logger.info("There is a problem with getting the dllink by br.getredirectlocation");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // The moddb servers and the fdcservers servers got different settings
        // so the plugin also got them with this handling
        if (configuredServer != 1 && !dllink.contains("fdccdn")) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            logger.info("invalid final downloadlink (dllink) ?!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getAGBLink() {
        return "http://www.moddb.com/terms-of-use";
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
