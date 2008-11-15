//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

public class DepositFiles extends PluginForHost {

    private static final String DOWNLOAD_NOTALLOWED = "Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus anwenden";

    static private final String FILE_NOT_FOUND = "Dieser File existiert nicht";

    private static final String PATTERN_PREMIUM_FINALURL = "<div id=\"download_url\">.*?<a href=\"(.*?)\"";

    private Pattern FILE_INFO_NAME = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);

    private Pattern FILE_INFO_SIZE = Pattern.compile("Dateigr.*?: <b>(.*?)</b>");

    // Rechtschreibfehler übernommen
    private String PASSWORD_PROTECTED = "<strong>Bitte Password fuer diesem File eingeben</strong>";

    private static int simultanpremium = 1;

    public DepositFiles(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://depositfiles.com/signup.php?ref=down1");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        getFileInformation(downloadLink);
        String link = downloadLink.getDownloadURL().replaceAll("/\\w{2}/files/", "/de/files/");
        br.getPage(link);
        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
            br.getPage(link);
        }

        if (br.containsHTML(DOWNLOAD_NOTALLOWED)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l); }

        Form form = br.getFormbyValue("Kostenlosen download");
        if (form != null) {
            br.submitForm(form);
        }
        String wait = br.getRegex("Bitte versuchen Sie noch mal nach(.*?)<\\/strong>").getMatch(0);
        if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Regex.getMilliSeconds(wait)); }

        if (br.containsHTML(PASSWORD_PROTECTED)) {
            // TODO: wo bekomm ich pw links her?
            // MUss wohl noch angepasst werden
            String password = Plugin.getUserInput(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"), downloadLink);
            br.postPage(link, "go=1&gateway_result=1&file_password=" + password);
        }

        if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) { throw new PluginException(LinkStatus.ERROR_RETRY); }

        form = br.getFormbyValue("Die Datei downloaden");
        if (form == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        br.setDebug(true);
        dl = br.openDownload(downloadLink, form, true, 1);
        HTTPConnection con = dl.connect(br);
        if (con == null) {
            if (br.containsHTML("IP-Addresse werden schoneinige Files")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            }
        }

        if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }

        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (!con.isOK()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    public void login(Account account) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.getPage("http://depositfiles.com/en/gold/payment.php");
        Form login = br.getFormbyValue("enter");
        login.put("login", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);
        br.setFollowRedirects(false);
        String cookie = br.getCookie("http://depositfiles.com", "autologin");
        if (cookie == null || br.containsHTML("Benutzername-Passwort-Kombination")) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.VALUE_ID_PREMIUM_DISABLE | LinkStatus.ERROR_PREMIUM);
        }
    }

    public boolean isFreeAccount() throws IOException {
        br.getPage("http://depositfiles.com/de/gold/");
        if (br.containsHTML("<div class=\"access\">Ihr Gold")) return false;
        String status = br.getRegex("<div class=\"access\">Ihre aktuelle Status:(.*?)- Mitglied</div>").getMatch(0);
        if (status == null) return true;
        if (status.trim().equalsIgnoreCase("frei")) return true;
        return false;
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            ai.setStatus("Account expired or logins not valid");
            ai.setValid(false);
            return ai;
        }
        if (isFreeAccount()) {
            ai.setStatus("Account is ok");
            ai.setValid(true);
            return ai;
        }
        String expire = br.getRegex("<div class=\"access\">Ihr Gold- Account ist.*?bis zum: <b>(.*?)</b></div>").getMatch(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK);
        if (expire == null) {
            ai.setStatus("Account expired or logins not valid");
            ai.setValid(false);
            return ai;
        }
        ai.setStatus("Account is ok");
        Date date;
        try {
            date = dateFormat.parse(expire);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        getFileInformation(downloadLink);
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
        String link = downloadLink.getDownloadURL().replaceAll("/\\w{2}/files/", "/de/files/");
        br.getPage(link);

        if (br.getRedirectLocation() != null) {
            link = br.getRedirectLocation().replaceAll("/\\w{2}/files/", "/de/files/");
            br.getPage(link);
        }

        if (br.containsHTML(DOWNLOAD_NOTALLOWED)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l); }

        if (br.containsHTML(PASSWORD_PROTECTED)) {
            String password = Plugin.getUserInput(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"), downloadLink);
            br.postPage(link, "go=1&gateway_result=1&file_password=" + password);
        } else {
            // logger.info(br + "");
        }
        link = br.getRegex(PATTERN_PREMIUM_FINALURL).getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        br.setDebug(true);
        dl = br.openDownload(downloadLink, link, true, 0);
        HTTPConnection con = dl.connect(br);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().indexOf("error") > 0) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }

        if (Plugin.getFileNameFormHeader(con) == null || Plugin.getFileNameFormHeader(con).indexOf("?") >= 0) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (!con.isOK()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://depositfiles.com/en/agreem.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("\\.com[/..]?/files", ".com/de/files"));
        String link = downloadLink.getDownloadURL();

        br.setFollowRedirects(true);
        br.setCookie("http://depositfiles.com", "lang_current", "de");
        br.setFollowRedirects(false);
        br.getPage(link);

        // Datei geloescht?
        if (br.containsHTML(FILE_NOT_FOUND)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("<strong>Achtung! Sie haben ein Limit")) {
            downloadLink.getLinkStatus().setStatusText("DownloadLimit reached!");
            return true;
        }
        String fileName = br.getRegex(FILE_INFO_NAME).getMatch(0);
        String fileSizeString = br.getRegex(FILE_INFO_SIZE).getMatch(0);
        if (fileName == null || fileSizeString == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(fileName);
        downloadLink.setDownloadSize(Regex.getSize(fileSizeString));
        return true;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
    
    public int getTimegapBetweenConnections() {
        return 800;
    }

}
