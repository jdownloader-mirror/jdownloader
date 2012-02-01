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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "slingfile.com" }, urls = { "http://(www\\.)?slingfile\\.com/((file|audio|video)/.+|dl/[a-z0-9]+/.*?\\.html)" }, flags = { 2 })
public class SlingFileCom extends PluginForHost {

    public SlingFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.slingfile.com/premium");
    }

    /**
     * Important: Do NOT implement their linkchecker as it's buggy and shows
     * wrong information: http://www.slingfile.com/check-files
     * */
    private static final Object LOCK     = new Object();
    private static final String MAINPAGE = "http://slingfile.com/";

    public void correctDownloadLink(DownloadLink link) {
        String theLink = link.getDownloadURL();
        theLink = theLink.replaceAll("/(audio|video|dl)/", "file");
        link.setUrlDownload(theLink);
    }

    public String getAGBLink() {
        return "http://www.slingfile.com/pages/tos.html";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        // Prevents errors, i don't know why the page sometimes shows this
        // error!
        if (br.containsHTML(">Please enable cookies to use this website")) br.getPage(downloadLink.getDownloadURL());
        if ("http://www.slingfile.com/".equals(br.getRedirectLocation())) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h3>Downloading <span>(.*?)</span></h3>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) \\- SlingFile \\- Free File Hosting \\& Online Storage</title>").getMatch(0);
        }
        String filesize = br.getRegex("<td>(.{2,20}) \\| Uploaded").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String waitthat = br.getRegex("Please wait for another (\\d+) minutes to download another file").getMatch(0);
        if (waitthat != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waitthat) * 60 * 1001l);
        // int wait = 30;
        // String waittime =
        // br.getRegex("\\)\\.innerHTML=\\'(\\d+)\\'").getMatch(0);
        // if (waittime == null) waittime =
        // br.getRegex("id=\"dltimer\">(\\d+)</span><br>").getMatch(0);
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // sleep(wait * 1001l, downloadLink);
        if (br.containsHTML("Please wait until the download is complete")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
        /**
         * TODO: Not sure if this will always work, maybe its different if
         * captcha appears after this step
         */
        br.postPage(downloadLink.getDownloadURL(), "download=yes");
        if (br.containsHTML("(api\\.recaptcha\\.net|g>Invalid captcha entered\\. Please try again\\.<)")) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML("(api\\.recaptcha\\.net|g>Invalid captcha entered\\. Please try again\\.<)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dllink = br.getRegex("(http://sf[0-9\\-].*?.slingfile\\.com/gdl/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/.*?)('|\")").getMatch(0);
        if (dllink == null) dllink = br.getRegex("<td valign=\\'middle\\' align=\\'center\\' colspan=\\'2\\'>[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
                br.setCookiesExclusive(false);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).matches(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).matches(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.postPage("http://www.slingfile.com/login", "f_user=" + Encoding.urlEncode(account.getUser()) + "&f_password=" + Encoding.urlEncode(account.getPass()) + "&f_keepMeLoggedIn=1&submit=Login+%C2%BB");
                if (br.getCookie(MAINPAGE, "cookielogin") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://www.slingfile.com/dashboard");
        if (!br.containsHTML("<li class=\"status premium\"><b>")) {
            ai.setStatus("Not a premium account!");
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex("<h4>Storage</h4>[\t\n\r ]+<p><span>([0-9\\.]+ [A-Za-z]{1,5}) /").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        String uploadedFiles = br.getRegex("<h4>Files Uploaded</h4>[\t\n\r ]+<p><span>(\\d+) files</span>").getMatch(0);
        if (uploadedFiles != null) ai.setFilesNum(Integer.parseInt(uploadedFiles));
        ai.setUnlimitedTraffic();
        String expire = br.getRegex(">Premium Account valid until (\\d{1,2} [A-Za-z]+ \\d{4} \\d{2}:\\d{2})").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy hh:mm", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<td valign=\\'middle\\' align=\\'center\\' colspan=\\'2\\'>[\t\n\r ]+<a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://sf\\d+\\-\\d+\\.slingfile\\.com/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/[^<>\"\\']+)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setReadTimeout(2 * 60 * 1000);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {
    }
}