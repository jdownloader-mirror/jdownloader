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

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class Vipfilecom extends PluginForHost {

    public Vipfilecom(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        enablePremium("http://vip-file.com/tmpl/premium_en.php");
    }

    //@Override
    public String getAGBLink() {
        return "http://vip-file.com/tmpl/terms.php";
    }

    //@Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        String downloadURL = downloadLink.getDownloadURL();
        this.setBrowserExclusive();
        br.getPage(downloadURL);
        if (br.containsHTML("This file not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileSize = br.getRegex("<span.*?Size:.*?<b style=.*?>(.*?)</b>").getMatch(0);
        if (fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileName = new Regex(downloadURL, "http://[\\w\\.]*?vip-file\\.com/download/[a-zA-z0-9]+/(.*?)\\.html").getMatch(0);
        downloadLink.setDownloadSize(Regex.getSize(fileSize));
        downloadLink.setName(fileName);
        String link = Encoding.htmlDecode(br.getRegex(Pattern.compile("<a href=\"(http://vip-file\\.com/download.*?)\">", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (link == null) {
            downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.hoster.vipfilecom.errors.nofreedownloadlink", "No free download link for this file"));
            return true;
        }
        return true;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        /* DownloadLink holen, 2x der Location folgen */
        String link = Encoding.htmlDecode(br.getRegex(Pattern.compile("<a href=\"(http://vip-file\\.com/download.*?)\">", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (link == null) throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.hoster.vipfilecom.errors.nofreedownloadlink", "No free download link for this file"));

        /* SpeedHack */
        br.setFollowRedirects(false);
        br.getPage(link);
        link = br.getRedirectLocation();
        br.getPage(link);
        link = br.getRedirectLocation();
        link = link.replaceAll("file.com.*?/", "file.com:8080/");
        br.setFollowRedirects(true);

        br.openDownload(downloadLink, link, true, 0).startDownload();
    }

    //@Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        getFileInformation(downloadLink);
        Form form = br.getForm(1);
        form.put("pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        String url = Encoding.htmlDecode(br.getRegex(Pattern.compile("<a href=\"(.*?vip-file\\.com/download.*?)\">", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (url == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        dl = br.openDownload(downloadLink, url, true, 0);
        dl.startDownload();
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    //@Override
    public void reset() {
    }

    //@Override
    public void resetPluginGlobals() {
    }

    //@Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

}
