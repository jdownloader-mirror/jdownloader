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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-online.biz" }, urls = { "http://[\\w\\.]*?share\\-online\\.biz/download.php\\?id\\=[\\w]+" }, flags = { 2 })
public class ShareOnlineBiz extends PluginForHost {

    public ShareOnlineBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.share-online.biz/service.php?p=31353834353B4A44616363");
    }

    public String getAGBLink() {
        return "http://share-online.biz/rules.php";
    }

    public void correctDownloadLink(DownloadLink link) throws Exception {
        String id = new Regex(link.getDownloadURL(), "id\\=([a-zA-Z0-9]+)").getMatch(0);
        link.setUrlDownload("http://www.share-online.biz/download.php?id=" + id + "&?setlang=en");
    }

    public void login(Account account) throws IOException, PluginException {
        br.setCookie("http://www.share-online.biz", "king_mylang", "en");
        br.postPage("http://www.share-online.biz/login.php", "act=login&location=service.php&dieseid=&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&login=Log+me+in&folder_autologin=1");
        String cookie = br.getCookie("http://www.share-online.biz", "king_passhash");
        if (cookie == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://www.share-online.biz/members.php");
        String expired = br.getRegex(Pattern.compile("<b>Expired\\?</b></td>.*?<td align=\"left\">(.*?)<a", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (expired == null || !expired.trim().equalsIgnoreCase("no")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    private boolean isPremium() throws IOException {
        if (br.getURL() == null || !br.getURL().equalsIgnoreCase("http://www.share-online.biz/members.php") || br.toString().startsWith("Not HTML Code.")) {
            br.getPage("http://www.share-online.biz/members.php");
        }
        if (br.containsHTML("<b>Premium account</b>")) return true;
        if (br.containsHTML("<b>VIP account</b>")) return true;
        return false;
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            account.setValid(false);
            ai.setStatus("No Premium Account!");
            return ai;
        }
        br.getPage("http://www.share-online.biz/members.php");
        String points = br.getRegex(Pattern.compile("<b>Total Points:</b></td>.*?<td align=\"left\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (points != null) ai.setPremiumPoints(points);
        String expire = br.getRegex(Pattern.compile("<b>Package Expire Date:</b></td>.*?<td align=\"left\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        ai.setValidUntil(Regex.getMilliSeconds(expire, "MM/dd/yy", null));
        account.setValid(true);
        return ai;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.share-online.biz", "king_mylang", "en");
        br.setAcceptLanguage("en, en-gb;q=0.8");
        String id = new Regex(downloadLink.getDownloadURL(), "id\\=([a-zA-Z0-9]+)").getMatch(0).toUpperCase();
        if (br.postPage("http://www.share-online.biz/linkcheck/linkcheck.php", "links=" + id).matches("\\s*")) {
            br.getPage(downloadLink.getDownloadURL());
            String[] strings = br.getRegex("</font> \\((.*?)\\) \\.</b></div></td>.*?<b>File name:</b>.*?<b>(.*?)</b></div></td>").getRow(0);
            if (strings == null || strings.length != 2) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setDownloadSize(Regex.getSize(strings[0].trim()));
            downloadLink.setName(strings[1].trim());
            return AvailableStatus.TRUE;
        }
        String infos[] = br.getRegex("(.*?);(.*?);(.*?);(.+)").getRow(0);
        if (infos == null || !infos[1].equalsIgnoreCase("OK")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setDownloadSize(Long.parseLong(infos[3].trim()));
        downloadLink.setName(infos[2].trim());
        return AvailableStatus.TRUE;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        if (!this.isPremium()) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        br.getPage(parameter.getDownloadURL());
        Form form = br.getForm(0);
        if (form.containsHTML("name=downloadpw")) {
            String passCode = null;
            if (parameter.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, parameter);
            } else {
                /* gespeicherten PassCode holen */
                passCode = parameter.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
            br.submitForm(form);
            if (br.containsHTML("Unfortunately the password you entered is not correct")) {
                /* PassCode war falsch, also Löschen */
                parameter.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            /* PassCode war richtig, also Speichern */
            parameter.setProperty("pass", passCode);
        }

        if (br.containsHTML("DL_GotMaxIPPerUid")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);

        String url = br.getRegex("loadfilelink\\.decode\\(\"(.*?)\"\\);").getMatch(0);

        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = jd.plugins.BrowserAdapter.openDownload(br, parameter, url, true, 1);
        dl.startDownload();
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Probleme mit einem Fileserver")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable", "Server temporarily down"), 15 * 60 * 1000l);

        if (br.containsHTML("Server Info: no slots available")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.shareonlinebiz.errors.servernotavailable2", "No slots available"), 15 * 60 * 1000l);
        /* CaptchaCode holen */
        String captchaCode = getCaptchaCode("http://www.share-online.biz/captcha.php", downloadLink);
        Form form = br.getForm(1);
        String passCode = null;
        if (form.containsHTML("name=downloadpw")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("downloadpw", passCode);
        }

        /* Überprüfen(Captcha,Password) */
        form.put("captchacode", captchaCode);
        br.submitForm(form);
        if (br.containsHTML("Captcha number error or expired") || br.containsHTML("Unfortunately the password you entered is not correct")) {
            if (br.containsHTML("Unfortunately the password you entered is not correct")) {
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
            }
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        /* Downloadlimit erreicht */
        if (br.containsHTML("max allowed download sessions") || br.containsHTML("this download is too big")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l); }

        /* PassCode war richtig, also Speichern */
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        /* DownloadLink holen, thx @dwd */
        String all = br.getRegex("eval\\(unescape\\(.*?\"\\)\\)\\);").getMatch(-1);
        String dec = br.getRegex("loadfilelink\\.decode\\(\".*?\"\\);").getMatch(-1);
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        String fun = "function f(){ " + all + "\nreturn " + dec + "} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        String url = Context.toString(result);
        Context.exit();

        // Keine Zwangswartezeit, deswegen auskommentiert
        // sleep(15000, downloadLink);
        br.setFollowRedirects(true);
        /* Datei herunterladen */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {

    }

}
