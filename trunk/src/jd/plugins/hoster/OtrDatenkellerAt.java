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
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "otr.datenkeller.at" }, urls = { "http://(www\\.)?otr\\.datenkeller\\.at/\\?(file|getFile)=.+" }, flags = { 0 })
public class OtrDatenkellerAt extends PluginForHost {

    public static String        agent             = RandomUserAgent.generate();
    private static final String DOWNLOADAVAILABLE = "onclick=\"startCount";

    public OtrDatenkellerAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("getFile", "file").replaceAll("\\&referer=otrkeyfinder\\&lang=[a-z]+", ""));
    }

    @Override
    public String getAGBLink() {
        return "http://otr.datenkeller.at";
    }

    public String getDllink() throws Exception, PluginException {
        Regex allMatches = br.getRegex("onclick=\"startCount\\([0-9]{1}, [0-9]{1}, \\'(.*?)\\', \\'(.*?)\\', \\'(.*?)\\'\\)");
        String firstPart = allMatches.getMatch(1);
        String secondPart = allMatches.getMatch(0);
        String thirdPart = allMatches.getMatch(2);
        if (firstPart == null || secondPart == null || thirdPart == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://" + firstPart + "/" + secondPart + "/" + thirdPart;
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dlPage = downloadLink.getDownloadURL().replace("?file=", "?getFile=");
        br.getPage(dlPage);
        String dllink = null;
        String lowSpeedLink;
        Browser br2 = br.cloneBrowser();
        if (br.containsHTML(DOWNLOADAVAILABLE)) {
            dllink = getDllink();
        } else {
            downloadLink.getLinkStatus().setStatusText("Waiting for ticket...");
            for (int i = 0; i <= 410; i++) {
                br.getPage(dlPage);
                String countMe = br.getRegex("\"(otrfuncs/countMe\\.js\\?r=\\d+)\"").getMatch(0);
                if (countMe != null)
                    countMe = "http://otr.datenkeller.at/" + countMe;
                else
                    countMe = "http://otr.datenkeller.at/otrfuncs/countMe.js?r=050210";
                br2.getPage("http://otr.datenkeller.at/images/style.css");
                br2.getPage(countMe);
                sleep(27 * 1000l, downloadLink);
                String position = br.getRegex("document\\.title = \"(\\d+/\\d+)").getMatch(0);
                if (position == null) position = br.getRegex("<td>Deine Position in der Warteschlange: </td><td>~(\\d+)</td></tr>").getMatch(0);
                if (position != null) downloadLink.getLinkStatus().setStatusText("Waiting for ticket...Position in der Warteschlange: " + position);
                if (br.containsHTML(DOWNLOADAVAILABLE)) {
                    br.getPage(dlPage);
                    dllink = getDllink();
                    break;
                }
                lowSpeedLink = br.getRegex("\"(\\?lowSpeed=[^<>\\'\"]+)\"").getMatch(0);
                if (i > 400 && lowSpeedLink != null) {
                    br2.getPage("http://otr.datenkeller.at/" + lowSpeedLink);
                    dllink = br2.getRegex(">Dein Download Link:<br>[\t\n\r ]+<a href=\"(http://[^<>\\'\"]+)\"").getMatch(0);
                    if (dllink == null) dllink = br2.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/low/[a-z0-9]+/[^<>\\'\"]+)\"").getMatch(0);
                    if (dllink != null) {
                        logger.info("Using lowspeed link for downloadlink: " + downloadLink.getDownloadURL());
                        break;
                    } else {
                        logger.warning("Failed to find low speed link, continuing to look for downloadticket...");
                    }
                }
                if (i > 403) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Didn't get a ticket");
                logger.info("Didn't get a ticket on try " + i + ". Retrying...Position: " + position);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -6);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getHeaders().put("User-Agent", agent);
        br.getPage(link.getDownloadURL());
        if (!br.containsHTML("id=\"reqFileImg\"") && !br.containsHTML("\\(\\'#reqFile\\'\\)\\.hide\\(\\)\\.slideDown") && !br.containsHTML("onclick=\"window\\.open\\(\\'/\\?getFile")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = new Regex(link.getDownloadURL(), "otr\\.datenkeller\\.at/\\?file=(.+)").getMatch(0);
        String filesize = br.getRegex("Größe: </td><td align=\\'center\\'> (.*?) </td>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim().replaceAll("\\&referer=.*?", ""));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize.replace("i", "")));
        return AvailableStatus.TRUE;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}