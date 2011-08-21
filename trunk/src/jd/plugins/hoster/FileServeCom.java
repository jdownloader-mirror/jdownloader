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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileserve.com" }, urls = { "http://(www\\.)?fileserve\\.com/file/[a-zA-Z0-9]+" }, flags = { 2 })
public class FileServeCom extends PluginForHost {

    public String               FILEIDREGEX = "fileserve\\.com/file/([a-zA-Z0-9]+)(http:.*)?";
    public static String        agent       = RandomUserAgent.generate();
    private static final String COOKIE_HOST = "http://www.fileserve.com";

    public FileServeCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.fileserve.com/premium.php");
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            br.setCustomCharset("utf-8");
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            final StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("submit=Check+Urls&urls=");
                links.clear();
                while (true) {
                    /*
                     * we test 100 links at once - its tested with 500 links,
                     * probably we could test even more at the same time...
                     */
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                this.br.getPage("http://fileserve.com/link-checker.php");
                int c = 0;
                for (final DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) {
                        sb.append("%0D%0A");
                    }
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                this.br.postPage("http://jd.fileserve.com/link-checker.php", sb.toString());
                for (final DownloadLink dl : links) {
                    final String fileid = new Regex(dl.getDownloadURL(), this.FILEIDREGEX).getMatch(0);
                    if (fileid == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        return false;
                    }
                    final String regexForThisLink = "(<td>http://fileserve\\.com/file/" + fileid + "([\r\n\t]+)?</td>[\r\n\t ]+<td>.*?</td>[\r\n\t ]+<td>.*?</td>[\r\n\t ]+<td>(Available|Not available)(\\&nbsp;)?(<img|</td>))";
                    final String theData = this.br.getRegex(regexForThisLink).getMatch(0);
                    if (theData == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        return false;
                    }
                    final Regex linkinformation = new Regex(theData, "<td>http://fileserve\\.com/file/" + fileid + "([\r\n\t]+)?</td>[\r\n\t ]+<td>(.*?)</td>[\r\n\t ]+<td>(.*?)</td>[\r\n\t ]+<td>(Available|Not available)(\\&nbsp;)?(<img|</td>)");
                    final String status = linkinformation.getMatch(3);
                    String filename = linkinformation.getMatch(1);
                    String filesize = linkinformation.getMatch(2);
                    if (filename == null || filesize == null) {
                        logger.warning("Fileserve availablecheck is broken!");
                        dl.setAvailable(false);
                    } else if (!status.equals("Available") || filename.equals("--") || filesize.equals("--")) {
                        filename = fileid;
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                    if (filesize != null) {
                        if (filesize.contains(",") && filesize.contains(".")) {
                            /* workaround for 1.000,00 MB bug */
                            filesize = filesize.replaceFirst("\\.", "");
                        }
                        dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // All links should look the same to get no problems with regexing them
        // later
        final String fileId = new Regex(link.getDownloadURL(), this.FILEIDREGEX).getMatch(0);
        link.setUrlDownload("http://fileserve.com/file/" + fileId);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        this.handleErrors(br);
        final String fileId = this.br.getRegex("fileserve\\.com/file/([a-zA-Z0-9]+)").getMatch(0);
        this.br.setFollowRedirects(false);
        String captchaJSPage = this.br.getRegex("\"(/landing/.*?/download_captcha\\.js)\"").getMatch(0);
        if (captchaJSPage == null) {
            logger.warning("captchaJSPage is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        captchaJSPage = "http://fileserve.com" + captchaJSPage;
        final Browser br2 = this.br.cloneBrowser();
        br2.setCustomCharset("utf-8");
        // It doesn't work without accessing this page!!
        br2.getPage(captchaJSPage);
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.postPage(downloadLink.getDownloadURL(), "checkDownload=check");
        if (!br2.containsHTML("success\":\"showCaptcha\"")) {
            logger.info("There seems to be an error, no captcha is shown!");
            handleCaptchaErrors(br2, downloadLink);
            handleErrors(br2);
        }
        // Captcha should appear always
        // if
        // (!this.br.containsHTML("<div id=\"captchaArea\" style=\"display:none;\">")
        // || br2.containsHTML("showCaptcha\\(\\);")) {
        Boolean failed = true;
        for (int i = 0; i <= 10; i++) {
            final String id = this.br.getRegex("var reCAPTCHA_publickey=\\'(.*?)\\';").getMatch(0);
            if (!this.br.containsHTML("api\\.recaptcha\\.net") || id == null || fileId == null) {
                handleCaptchaErrors(br2, downloadLink);
                logger.warning("id or fileId is null or the browser doesn't contain the reCaptcha text...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Form reCaptchaForm = new Form();
            reCaptchaForm.setMethod(Form.MethodType.POST);
            reCaptchaForm.setAction("http://www.fileserve.com/checkReCaptcha.php");
            reCaptchaForm.put("recaptcha_shortencode_field", fileId);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(this.br);
            rc.setForm(reCaptchaForm);
            rc.setId(id);
            rc.load();
            final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
            final String c = this.getCaptchaCode(cf, downloadLink);
            if (c == null || c.length() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Recaptcha failed");
            rc.getForm().put("recaptcha_response_field", c);
            rc.getForm().put("recaptcha_challenge_field", rc.getChallenge());
            br2.submitForm(rc.getForm());
            if (br2.containsHTML("incorrect-captcha-sol")) {
                handleCaptchaErrors(br2, downloadLink);
                this.br.getPage(downloadLink.getDownloadURL());
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        handleCaptchaErrors(br2, downloadLink);
        handleErrors(br2);
        // }
        this.br.postPage(downloadLink.getDownloadURL(), "downloadLink=wait");
        // Ticket Time
        if (!this.br.getHttpConnection().isOK()) {
            logger.warning("The connection is not okay...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String reconTime = br.toString();
        int tt = 60;
        if (reconTime.length() < 500) {
            reconTime = new Regex(reconTime, ".*?(\\d+).*?").getMatch(0);
            logger.info("Waittime detected, waiting " + reconTime + " seconds from now on...");
            tt = Integer.parseInt(reconTime.trim());
        } else {
            logger.warning("Couldn't find dynamic waittime");
            logger.warning(br.toString());
        }
        this.sleep(tt * 1001, downloadLink);
        br2.postPage(downloadLink.getDownloadURL(), "downloadLink=show");
        br.setDebug(true);
        this.br.postPage(downloadLink.getDownloadURL(), "download=normal");
        final String dllink = this.br.getRedirectLocation();
        if (dllink == null) {
            this.handleErrors(br);
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("dllink=" + dllink);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, dllink, false, 1);
        if (this.dl.getConnection().getResponseCode() == 404) {
            logger.info("got a 404 error...");
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.dl.getConnection().getContentType().contains("html")) {
            logger.info("The finallink doesn't seem to be a file...");
            this.br.followConnection();
            this.handleErrors(br);
            logger.warning("Unexpected error at the last step...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (downloadLink.getFinalFileName() == null) {
            /* workaround for buggy server response, see #3545 */
            String name = Plugin.getFileNameFromHeader(dl.getConnection());
            if (name != null) {
                name = name.replaceAll("\\%\\%", "%25%");
                name = Encoding.htmlDecode(name);
                downloadLink.setFinalFileName(name);
            }
        }
        this.dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(br, account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String expire = br.getRegex("Premium Until<\\/h4><\\/th>.*?<td><h5>(.*?)<\\/h5").getMatch(0);
        String type = br.getRegex("Account Type<\\/h4><\\/td> <td><h5 class\\=\\\"inline\\\">(.*?) <\\/h5").getMatch(0);
        account.setValid(true);
        if (type != null) ai.setStatus(type);
        if (!"Premium".equals(type)) {
            try {
                account.setMaxSimultanDownloads(1);
            } catch (final Throwable e) {
                /* not available in 0.9xxx */
            }
            account.setProperty("type", "free");
            account.setValid(false);
        } else {
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy zzz", null));
                ai.setStatus("Premium User");
                return ai;
            }
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileserve.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private void handleErrors(Browser br2) throws PluginException {
        logger.info("Handling errors...");
        if (br2.containsHTML("Your daily download limit has been reached")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily limit reached", 2 * 60 * 60 * 1000l);
        if (br2.containsHTML("li>This file has been deleted by the system")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br2.containsHTML("File not available, please register as <a href=\"/login\\.php\">Premium</a> Member to download<br")) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.FileServeCom.errors.only4premium", "This file is only downloadable for premium users")); }
        if (br2.containsHTML(">Your download link has expired")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download link expired, contact fileserve support", 10 * 60 * 1000l); }
        if (br2.containsHTML("Captcha error") || this.br.containsHTML("incorrect-captcha")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        final String wait = br2.getRegex("You (have to|need to) wait (\\d+) seconds to start another download").getMatch(1);
        if (wait != null) {
            logger.info("WaitRegex: " + wait);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(wait) * 1001l);
        }
        if (br2.containsHTML("landing-error\\.php\\?error_code=404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br2.containsHTML("landing-406\\.php")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l); }
        if (br2.containsHTML("<p>You can only download 1 file at a time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
        if (br2.containsHTML("(landing-error\\.php\\?error_code=2702|is already downloading a file</li>|is already downloading a file <br>|landing\\-1403)") || br2.getURL().contains("landing-2702.html") || br.getURL().contains("landing-1403")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Your IP is already downloading", 5 * 60 * 1000l);
        if (br2.containsHTML("landing-error\\.php\\?error_code=1703")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 15 * 60 * 1000l);
        if (br2.containsHTML("landing-error\\.php") || br.getURL().contains("landing-")) {
            logger.warning("Unknown landing error!");
            logger.warning("Url = " + br2.getURL());
            logger.warning("html code = " + br2.toString());
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unknown landing error, please contact our support!");
        }
    }

    private void handleCaptchaErrors(Browser br2, DownloadLink downloadLink) throws IOException, PluginException {
        // Handles captcha errors and additionsl limits
        logger.info("Checking captcha errors...");
        if (br.containsHTML("No htmlCode read")) {
            logger.info("Unexpected captcha error happened");
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String fail = br2.getRegex("\"(fail|error)\":\"(.*?)\"").getMatch(1);
        String waittime = br2.getRegex("\"(waitTime|msg)\":(\\d+)").getMatch(1);
        if (fail != null && waittime != null) {
            if (fail.equals("captcha-fail") || fail.equals("captchaFail")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
            br2.postPage(downloadLink.getDownloadURL(), "checkDownload=showError&errorType=" + fail + "&waitTime=" + waittime);
            // Just an additional check
            if (br2.containsHTML("Please retry later\\.<") || br2.containsHTML(">Your IP has failed the captcha too many times")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
        } else if (fail != null) {
            // This coiuld be a limit message which appears after posting this,
            // it should then be handled with handleErrors
            br2.postPage(downloadLink.getDownloadURL(), "checkDownload=showError&errorType=" + fail);
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        this.sleep(30 * 1000l, downloadLink);
        this.requestFileInformation(downloadLink);
        this.getDownloadUrlPage(downloadLink);
        this.doFree(downloadLink);
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.requestFileInformation(link);
        this.login(br, account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRedirectLocation();
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, true, 0);
        if (this.dl.getConnection().getResponseCode() == 404) {
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.dl.getConnection().getContentType().contains("html")) {
            this.br.followConnection();
            if (this.dl.getConnection().getLongContentLength() == 0) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            this.handleErrors(br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getFinalFileName() == null) {
            /* workaround for buggy server response, see #3545 */
            String name = Plugin.getFileNameFromHeader(dl.getConnection());
            if (name != null) {
                name = name.replaceAll("\\%\\%", "%25%");
                name = Encoding.htmlDecode(name);
                link.setFinalFileName(name);
            }
        }
        this.dl.startDownload();
    }

    private void login(final Browser useBr, final Account account) throws Exception {
        Browser br = useBr;
        if (br == null) br = new Browser();
        this.setBrowserExclusive();
        br.getPage(COOKIE_HOST + "/index.php");
        Form loginform = br.getForm(1);
        if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String username = account.getUser();
        String password = account.getPass();
        if (username != null && username.length() > 20) {
            username = username.substring(0, 20);
        }
        if (password != null && password.length() > 20) {
            password = password.substring(0, 20);
        }
        loginform.put("loginUserName", Encoding.urlEncode(username));
        loginform.put("loginUserPassword", Encoding.urlEncode(password));
        br.submitForm(loginform);
        br.getPage(COOKIE_HOST + "/dashboard.php");
        if (!br.containsHTML("Login Name")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie(COOKIE_HOST, "cookie") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.br.getHeaders().put("User-Agent", agent);
        this.checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return link.getAvailableStatus();
    }

    private void getDownloadUrlPage(DownloadLink downloadLink) throws IOException {
        // To get the english language
        br.postPage(downloadLink.getDownloadURL(), "locale=en-us");
        if (!br.getURL().equals(downloadLink.getDownloadURL())) br.getPage(downloadLink.getDownloadURL());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}