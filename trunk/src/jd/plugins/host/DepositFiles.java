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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDLocale;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

public class DepositFiles extends PluginForHost {

    private static final String DOWNLOAD_NOTALLOWED = "Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus anwenden";

    static private final String FILE_NOT_FOUND = "Dieser File existiert nicht";

    private static final String PATTERN_PREMIUM_FINALURL = "<div id=\"download_url\">.*?<a href=\"(.*?)\"";

    private Pattern FILE_INFO_NAME = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);

    private Pattern FILE_INFO_SIZE = Pattern.compile("Dateigr.*?: <b>(.*?)</b>");

    private static int simultanpremium = 1;

    public DepositFiles(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://depositfiles.com/signup.php?ref=down1");
    }

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        requestFileInformation(downloadLink);
        String link = downloadLink.getDownloadURL();
        br.getPage(link);
        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
            br.getPage(link);
        }

        if (br.containsHTML(DOWNLOAD_NOTALLOWED)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l); }

        Form form = br.getFormBySubmitvalue("Kostenlosen+download");
        if (form != null) {
            br.submitForm(form);
        }
        if (br.containsHTML("We are sorry, but all downloading slots for your country are busy")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.hoster.depositfilescom.errors.allslotsbusy", "All download slots for your country are busy"), 10 * 60 * 1000l);
        String wait = br.getRegex("Bitte versuchen Sie noch mal nach(.*?)<\\/strong>").getMatch(0);
        if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Regex.getMilliSeconds(wait)); }

        if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) { throw new PluginException(LinkStatus.ERROR_RETRY); }

        form = br.getFormBySubmitvalue("Die+Datei+downloaden");
        sleep(60 * 1000l, downloadLink);
        if (form == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        br.setDebug(true);
        dl = br.openDownload(downloadLink, form, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (!con.isContentDisposition()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    public void login(Account account) throws Exception {
        br.setFollowRedirects(true);
        setLangtoGer();
        br.getPage("http://depositfiles.com/de/gold/payment.php");
        Form login = br.getFormBySubmitvalue("Anmelden");
        login.put("login", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);
        br.setFollowRedirects(false);
        String cookie = br.getCookie("http://depositfiles.com", "autologin");
        if (cookie == null || br.containsHTML("Benutzername-Passwort-Kombination")) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public void setLangtoGer() throws IOException {
        br.setCookie("http://depositfiles.com", "lang_current", "de");
    }

    public boolean isFreeAccount() throws IOException {
        setLangtoGer();
        br.getPage("http://depositfiles.com/de/gold/");
        if (br.containsHTML("<div class=\"access\">Ihr Gold")) return false;
        String status = br.getRegex("<div class=\"access\">Ihre aktuelle Status:(.*?)- Mitglied</div>").getMatch(0);
        if (status == null) return true;
        if (status.trim().equalsIgnoreCase("frei")) return true;
        return false;
    }

    //@Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        setBrowserExclusive();
        br.setDebug(true);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setStatus(JDLocale.L("plugins.hoster.depositfilescom.accountbad", "Account expired or not valid."));
            ai.setValid(false);
            return ai;
        }
        if (isFreeAccount()) {
            ai.setStatus(JDLocale.L("plugins.hoster.depositfilescom.accountok", "Account is OK."));
            ai.setValid(true);
            return ai;
        }
        String expire = br.getRegex("<div class=\"access\">Ihr Gold- Account ist.*?bis zum: <b>(.*?)</b></div>").getMatch(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
        if (expire == null) {
            ai.setStatus(JDLocale.L("plugins.hoster.depositfilescom.accountbad", "Account expitred or not valid."));
            ai.setValid(false);
            return ai;
        }
        ai.setStatus(JDLocale.L("plugins.hoster.depositfilescom.accountok", "Account is OK."));
        Date date;
        try {
            date = dateFormat.parse(expire);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occured", e);
        }

        return ai;
    }

    //@Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        if (this.isFreeAccount()) {
            simultanpremium = 1;
            handleFree(downloadLink);
            return;
        } else {
            if (simultanpremium + 1 > 20) {
                simultanpremium = 20;
            } else {
                simultanpremium++;
            }
        }
        String link = downloadLink.getDownloadURL();
        br.getPage(link);

        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
            br.getPage(link);
        }

        if (br.containsHTML(DOWNLOAD_NOTALLOWED)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l); }
        link = br.getRegex(PATTERN_PREMIUM_FINALURL).getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        br.setDebug(true);
        dl = br.openDownload(downloadLink, link, true, 0);
        URLConnectionAdapter con = dl.getConnection();
        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (!con.isContentDisposition()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    //@Override
    public String getAGBLink() {
        return "http://depositfiles.com/en/agreem.html";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.com(/.*?)?/files", ".com/de/files"));
    }

    //@Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();        
        String link = downloadLink.getDownloadURL();

        setLangtoGer();
        br.setFollowRedirects(false);
        br.getPage(link);

        // Datei geloescht?
        if (br.containsHTML(FILE_NOT_FOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("<strong>Achtung! Sie haben ein Limit")) {
            downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.hoster.depositfilescom.errors.limitreached", "Download limit reached"));
            return AvailableStatus.TRUE;
        }
        String fileName = br.getRegex(FILE_INFO_NAME).getMatch(0);
        String fileSizeString = br.getRegex(FILE_INFO_SIZE).getMatch(0);
        if (fileName == null || fileSizeString == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(fileName);
        downloadLink.setDownloadSize(Regex.getSize(fileSizeString));
        return AvailableStatus.TRUE;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    //@Override
    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    //@Override
    public void reset() {
    }

    //@Override
    public void resetPluginGlobals() {
    }

    //@Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    //@Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

}
