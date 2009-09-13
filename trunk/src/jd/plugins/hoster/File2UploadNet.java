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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//file2upload by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file2upload.net" }, urls = { "http://[\\w\\.]*?file2upload\\.(net|com)/download/[0-9]+/" }, flags = { 2 })
public class File2UploadNet extends PluginForHost {

    public File2UploadNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://file2upload.net/membership?paid");
    }

    public String getAGBLink() {
        return "http://file2upload.net/toc";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.file2upload.net");
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.put("acc_login", Encoding.urlEncode(account.getUser()));
        form.put("acc_pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        br.setFollowRedirects(false);
        if (!br.containsHTML("Account area")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String passCode = null;
        if (br.containsHTML("Your package allow only")) {
            // sleep(5 * 60 * 1001l, downloadLink);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Too much parallel downloads", LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (br.containsHTML("Enter password to download this file")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            Form pwform = br.getForm(1);
            if (pwform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            pwform.put("password", passCode);
            br.submitForm(pwform);
        }
        if (br.containsHTML("Wrong password")) {
            logger.warning("Wrong password");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        String dllink = br.getRegex("class=\"important\" href=\"(.*?)\">Click").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        dl.startDownload();
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return 2;
    }

    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        br.setFollowRedirects(false);
        if (br.containsHTML("File does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = Encoding.htmlDecode(br.getRegex("content=\"The new file hosting service! we provides web space for your documents, pictures, music and movies., (.*?)\"></meta>").getMatch(0));
        String filesize = Encoding.htmlDecode(br.getRegex("<tr><td><b>File size</b>:</td><td>(.*?)</td></tr>").getMatch(0));
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Form captchaForm = br.getForm(0);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String passCode = null;
        // This host got asecurity issue, if you enter the right pssword and the
        // wrong captcha (if a password is required), you can still download and
        // only if you DON'T have to enter a Password, the Captcha is required.
        if (!br.containsHTML("Enter password to download this file")) {
            String captchaurl = "http://file2upload.net/captcha.php";
            String code = getCaptchaCode(captchaurl, downloadLink);
            captchaForm.put("code", code);
        }
        if (br.containsHTML("Enter password to download this file")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            captchaForm.put("password", passCode);
        }
        br.submitForm(captchaForm);
        if (!br.containsHTML("download_link")) {
            logger.warning("Wrong password!");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        String dllink = br.getRegex("class=\"important\" href=\"(.*?)\">Click to download! <").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    public void reset() {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}