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

package jd.plugins.host;

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class FileBaseTo extends PluginForHost {

    public FileBaseTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filebase.to/tos/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        // br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        downloadLink.setName(Plugin.extractFileNameFromURL(url).replaceAll("&dl=1", ""));
        br.setDebug(true);
        if (br.containsHTML("eider\\s+nicht\\s+gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String size = br.getRegex("Dateigr[^:]*:</td>\\s+<td[^>]*>(.*?)</td>").getMatch(0);
        downloadLink.setDownloadSize(Regex.getSize(size));
        return true;

    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        String formact = downloadLink.getDownloadURL();
        if (br.containsHTML("/captcha/CaptchaImage")) {
            File captchaFile = Plugin.getLocalCaptchaFile(this, ".png");
            String CaptchaFileURL = br.getRegex("src=\"(/captcha/CaptchaImage\\.php.*?)\"").getMatch(0);
            String filecid = br.getRegex("cid\"\\s+value=\"(.*?)\"").getMatch(0);
            Browser.download(captchaFile, br.openGetConnection("http://filebase.to" + CaptchaFileURL));
            String capTxt = getCaptchaCode("uploaded.to", captchaFile, downloadLink);
            br.postPage(formact, "uid=" + capTxt + "&cid=" + Encoding.urlEncode(filecid) + "&submit=+++Best%E4tigung+++&session_code=");
            // if captcha error
            if (br.containsHTML("Code wurde falsch")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        
        String dlAction = br.getRegex("<form action=\"(http.*?)\"").getMatch(0);
        br.openDownload(downloadLink, dlAction, "wait=" + Encoding.urlEncode("Download - " + downloadLink.getName())).startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
        
    }
}