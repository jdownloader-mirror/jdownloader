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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "x-files.kz" }, urls = { "http://[\\w\\.]*?x-files\\.kz/[a-z0-9]+" }, flags = { 0 })
public class XFilesKz extends PluginForHost {

    public XFilesKz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.x-files.kz/index.php?action=agreement";
    }

    private static final String MAINPAGE       = "http://www.x-files.kz/";
    private static final String INVALIDCAPTCHA = "CAPTCHA_NOT_VALID";

    // This hoster is NOT available in Germany!Works only in Kazakhstan.

    // No way to find out if the file is online or not...
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie(MAINPAGE, "lang", "en");
        br.getPage(link.getDownloadURL());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String phpssid = br.getCookie(MAINPAGE, "PHPSESSID");
        if (!br.containsHTML("kcaptcha/index.php?PHPSESSID=") || phpssid == null) {
            logger.warning("Captchalink not found or phpssid is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Browser captchaBrowser = br.cloneBrowser();
        for (int i = 0; i <= 5; i++) {
            String getPage = "http://www.x-files.kz/preparedownload.php?userlink=" + downloadLink.getDownloadURL() + "&downloadcaptcha=" + getCaptchaCode("http://www.x-files.kz/kcaptcha/index.php?PHPSESSID=" + phpssid, downloadLink) + "&downloadpassword=&random=" + System.currentTimeMillis();
            captchaBrowser.getPage(getPage);
            if (captchaBrowser.containsHTML(INVALIDCAPTCHA)) continue;
            break;
        }
        if (captchaBrowser.containsHTML(INVALIDCAPTCHA)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (captchaBrowser.containsHTML("DOWNLOAD_IDENTIFIER_NOT_FOUND")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String dllink = br.getRegex("(http://dw\\d+\\.x-files\\.kz/[a-z0-9]+)").getMatch(0);
        if (dllink == null) {
            logger.warning("Dllink equals null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("This doesn't seem to be a file...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}