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
import java.util.SortedMap;
import java.util.TreeMap;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.pluginUtils.Recaptcha;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "azsharing.com" }, urls = { "http://[\\w\\.]*?azsharing\\.com/[a-z0-9]{12}" }, flags = { 0 })
public class XFileSharingProBasic extends PluginForHost {

    public XFileSharingProBasic(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.examplehost.com/tos.html";
    }

    // Made with the XFileSharingProBasic v.1.0 template
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.azsharing.com", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("You have reached the download-limit")) {
            logger.warning("Waittime detected, please reconnect to make the linkchecker work!");
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML("(No such file|No such user exist|File not found)")) {
            logger.warning("file is 99,99% offline, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("You have requested.*?http://.*?/.*?/(.*?)</font>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("fname\" value=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h2>Download File(.*?)</h2>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("Filename.*?nowrap>(.*?)</td").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("File Name.*?nowrap>(.*?)</td").getMatch(0);
                    }
                }
            }
        }
        String filesize = br.getRegex("\\(([0-9]+ bytes)\\)").getMatch(0);
        if (filesize == null) filesize = br.getRegex("</font>.*?\\((.*?)\\).*?</font>").getMatch(0);
        if (filename == null) {
            logger.warning("The filename equals null, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = filename.replace("</b>", "");
        link.setName(filename.trim());
        if (filesize != null) {
            logger.info("Filesize found, filesize = " + filesize);
            link.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // If the filesize regex above doesn't match you can copy this part into
        // the available status (and delete it here)
        Form freeform = br.getFormBySubmitvalue("Kostenloser+Download");
        if (freeform == null) {
            freeform = br.getFormBySubmitvalue("Free+Download");
            if (freeform == null) {
                freeform = br.getFormbyKey("download1");
            }
        }
        if (freeform != null) br.submitForm(freeform);
        if (br.containsHTML("This file reached max downloads")) { throw new PluginException(LinkStatus.ERROR_FATAL, "This file reached max downloads"); }
        if (br.containsHTML("You have to wait")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            logger.info("Detected waittime #1, waiting " + waittime + "milliseconds");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        if (br.containsHTML("You have reached the download-limit")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 *
            // 1000l);
        }
        br.setFollowRedirects(false);
        Form DLForm = br.getFormbyProperty("name", "F1");
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        String ttt = br.getRegex("countdown\">.*?(\\d+).*?</span>").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001, downloadLink);
        }
        String c = null;
        String passCode = null;
        boolean password = false;
        boolean recaptcha = false;
        // The String "loginpw" is only made for fileop.com
        String loginpw = br.getRegex("value=\"login\">(.*?)value=\" Login\"").getMatch(0);
        if (br.containsHTML("name=\"password\"") && !(loginpw != null && loginpw.contains("password"))) {
            password = true;
            logger.info("The downloadlink seems to be password protected.");
        }
        if (br.containsHTML("(background:#ccc;text-align|/captchas/|api.recaptcha.net)")) {
            if (br.containsHTML("background:#ccc;text-align")) {
                logger.info("Detected captcha method \"plaintext captchas\" for this host");
                // Captcha method by ManiacMansion
                String[][] letters = br.getRegex("<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(\\d)</span>").getMatches();
                if (letters.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
                for (String[] letter : letters) {
                    capMap.put(Integer.parseInt(letter[0]), letter[1]);
                }
                StringBuilder code = new StringBuilder();
                for (String value : capMap.values()) {
                    code.append(value);
                }
                DLForm.put("code", code.toString());
                logger.info("Put captchacode " + code.toString() + " obtained by captcha metod\"plaintext captchas\" in the form.");
            } else if (br.containsHTML("/captchas/")) {
                logger.info("Detected captcha method \"Standard captcha\" for this host");
                String[] lol = HTMLParser.getHttpLinks(br.toString(), null);
                System.out.print(br.toString());
                String captchaurl = null;
                if (lol.length == 0 || lol == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                for (String link : lol) {
                    if (link.contains("/captchas/")) {
                        captchaurl = link;
                    }
                }
                if (captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String code = getCaptchaCode(captchaurl, downloadLink);
                DLForm.put("code", code);
                logger.info("Put captchacode " + code + " obtained by captcha metod\"Standard captcha\" in the form.");
            } else if (br.containsHTML("api.recaptcha.net")) {
                logger.info("Detected captcha method \"Re Captcha\" for this host");
                Recaptcha rc = new Recaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                c = getCaptchaCode(cf, downloadLink);
                if (password == true) {
                    if (downloadLink.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", downloadLink);
                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = downloadLink.getStringProperty("pass", null);
                    }
                    rc.getForm().put("password", passCode);
                    password = false;
                }
                recaptcha = true;
                rc.setCode(c);
                logger.info("Put captchacode " + c + " obtained by captcha metod\"Re Captcha\" in the form.");
            }
        }
        // If the hoster uses Re Captcha the form has already been sent before
        // here so here it's checked. Most hosters don't use Re Captcha so
        // usually recaptcha is false
        if (recaptcha == false) {
            if (password == true) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                DLForm.put("password", passCode);
            }
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLForm, false, 1);
            logger.info("Sent DLForm");
        }
        if (br.getRedirectLocation() != null || dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info("followed connection...");
            System.out.print(br.toString());
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                if (br.containsHTML("You have to wait")) {
                    int minutes = 0, seconds = 0, hours = 0;
                    String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
                    if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                    String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
                    if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                    String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
                    if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                    int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                    logger.info("Detected waittime #1, waiting " + waittime + "milliseconds");
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
                }
                if (br.containsHTML("You have reached the download-limit")) {
                    int minutes = 0, seconds = 0, hours = 0;
                    String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
                    if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                    String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
                    if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                    String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
                    if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                    int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                    logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
                    // throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60
                    // * 60 * 1000l);
                }
                if (br.containsHTML("(name=\"password\"|Wrong password)") && !(loginpw != null && loginpw.contains("password"))) {
                    logger.warning("Wrong password!");
                    downloadLink.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (br.containsHTML("Wrong captcha")) {
                    logger.warning("Wrong captcha or wrong password!");
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                if (dllink == null) {
                    dllink = br.getRegex("dotted #bbb;padding.*?<a href=\"(.*?)\"").getMatch(0);
                    if (dllink == null) {
                        // This was for fileop.com, maybe also works for others!
                        dllink = br.getRegex("Download: <a href=\"(.*?)\"").getMatch(0);
                    }
                }
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        if (!(dl.getConnection().isContentDisposition())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("File Not Found")) {
                logger.warning("Server says link offline, please recheck that!");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}