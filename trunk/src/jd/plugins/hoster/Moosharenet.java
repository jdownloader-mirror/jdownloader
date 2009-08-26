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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mooshare.net" }, urls = { "http://[\\w\\.]*?mooshare\\.net/files/(\\d+)/(.*?)\\.html" }, flags = { 2 })
public class Moosharenet extends PluginForHost {

    public Moosharenet(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://mooshare.net/?section=becomemember");
    }

    @Override
    public String getAGBLink() {
        return "http://mooshare.net/?section=faq";
    }

    private void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://mooshare.net/index.php?section=premium");
        br.postPage("http://mooshare.net/index.php?section=premium", "user=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
        if (br.getCookie("http://mooshare.net", "premiumlogin") == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String expires = br.getRegex(Pattern.compile("<td class=.*?>Ende der Mitgliedschaft</td>.*?<td class=.*?>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (expires == null) {
            account.setValid(false);
            return ai;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy - hh:mm", Locale.UK);
        try {
            Date date = dateFormat.parse(expires);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
        }
        String trafficleft = br.getRegex(Pattern.compile("<td>verbleibender Traffic</td>.*?<td>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (trafficleft != null) {
            ai.setTrafficLeft(trafficleft);
        }
        String collpoints = br.getRegex(Pattern.compile("<td class=.*?>gesammelte Downloadpunkte</td>.*?<td class=.*?>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (collpoints != null) {
            ai.setPremiumPoints(collpoints);
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        String dlLink = null;
        if (br.getRedirectLocation() != null) {
            dlLink = br.getRedirectLocation();
        } else {
            Form form = br.getForm(0);
            br.submitForm(form);
            dlLink = br.getRedirectLocation();
        }
        if (dlLink == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        String[] infos = new Regex(downloadLink.getDownloadURL(), "http://[\\w\\.]*?mooshare\\.net/files/(\\d+)/(.*?)\\.html").getRow(0);

        br.getPage("http://mooshare.net/api/checkfile.php?name=" + infos[1] + "&id=" + infos[0]);
        try {
            int a = Integer.parseInt(br.toString().trim());
            if (a < 1) return AvailableStatus.FALSE;
        } catch (Exception e) {
            e.printStackTrace();
            return AvailableStatus.FALSE;
        }

        br.getPage("http://mooshare.net/api/checkfilesize.php?name=" + infos[1] + "&id=" + infos[0]);
        downloadLink.setName(infos[1]);
        downloadLink.setDownloadSize(Regex.getSize(br.toString()));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /*
         * warten da nach einem download noch kurz die meldung kommt,
         * gleichzeitiges download verboten
         */
        sleep(15000, downloadLink);
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        if (br.containsHTML("keine Dateien parallel downloaden")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
        if (br.containsHTML("Sie haben Ihr Downloadlimit für den Moment erreicht!")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l); }
        String wait = br.getRegex("var time = (.*?);").getMatch(0);
        if (wait != null) {
            wait = wait.replaceAll("\\.", "");
            sleep(Integer.parseInt(wait) * 100l, downloadLink);
        } else
            sleep(15000, downloadLink);
        String captchaurl = br.getRegex("<img src=\"(http://mooshare.net/html/images/captcha.php.*?)\" alt=\"captcha\"").getMatch(0);
        String captchaCode = getCaptchaCode(captchaurl, downloadLink);
        Form form = br.getForm(1);
        form.put("captcha", captchaCode);
        br.setFollowRedirects(false);
        br.submitForm(form);
        String dlLink = br.getRedirectLocation();
        if (dlLink == null) {
            if (br.containsHTML("falschen Code eingegeben")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
