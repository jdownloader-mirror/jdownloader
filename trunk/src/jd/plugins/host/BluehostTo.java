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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.XPath;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class BluehostTo extends PluginForHost {

    public BluehostTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://bluehost.to/premium.php");
    }

    private void correctUrl(DownloadLink downloadLink) {
        String url = downloadLink.getDownloadURL();
        url = url.replaceFirst("\\?dl=", "dl=");
        downloadLink.setUrlDownload(url);
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://bluehost.to", "bluehost_lang", "DE");
        br.getPage("http://bluehost.to/index.php");
        Form login = br.getForm(0);
        login.put("loginname", account.getUser());
        login.put("loginpass", account.getPass());
        br.submitForm(login);
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
            if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("logbycookie.php")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
        }
    }

    @Override
    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        String trafficLeft = br.getXPathElement("/html/body/div/div/ul[2]/div/div").trim();
        XPath path = new XPath(br.toString(), "/html/body/div/div/ul[2]/div[4]/center");
        ai.setTrafficLeft(Long.parseLong(trafficLeft.replaceAll("\\.", "")) * 1024 * 1024);
        ArrayList<String> matches = path.getMatches();
        try {
            ai.setPremiumPoints(JDUtilities.filterLong(matches.get(0)));
        } catch (Exception e) {
        }
        ai.setAccountBalance((long) (Float.parseFloat(Encoding.filterString(matches.get(1), "1234567890.,").replaceAll("\\,", ".")) * 100.0));
        ai.setExpired(false);
        ai.setValidUntil(-1);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        this.setBrowserExclusive();
        getFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(true);
        URLConnectionAdapter con = br.openGetConnection(downloadLink.getDownloadURL());
        if (con.getContentType().contains("text")) {
            br.followConnection();
            Form download = br.getFormbyProperty("name", "download");
            if (download == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            dl = br.openDownload(downloadLink, download, true, 0);
        } else {
            con.disconnect();
            dl = br.openDownload(downloadLink, downloadLink.getDownloadURL(), true, 0);
        }
        if (dl.getConnection().getContentType().contains("text")) {
            dl.getConnection().disconnect();
            login(account);
            String trafficLeft = br.getXPathElement("/html/body/div/div/ul[2]/div/div").trim();
            if (trafficLeft != null && trafficLeft.trim().equalsIgnoreCase("0")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        getFileInformation(downloadLink);
        // Zeit bis free download möglich
        String page = br.getPage("http://bluehost.to/fileinfo/urls=" + downloadLink.getDownloadURL());
        String[] dat = page.split("\\, ");
        int time = Integer.parseInt(dat[4]);
        if (time > 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugin.bluehostto.time", "BH free only allowed from 0 PM to 10 AM"), time * 1000l);

        br.getPage(downloadLink.getDownloadURL());
        String redirect = br.getRegex("<meta http-equiv=\"refresh\" content=\"1\\; URL=(.*?)\" />").getMatch(0);
        br.getPage(redirect);
        br.clearCookies("bluehost.to");
        br.submitForm(br.getForm(1));
        br.getPage(downloadLink.getDownloadURL());
        br.openDownload(downloadLink, br.getForm(1));
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://bluehost.to/agb.php";
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null) return false;

        try {

            logger.finest("Checked Links with one request: " + urls.length);
            StringBuilder sb = new StringBuilder();
            sb.append("urls=");
            for (int i = 0; i < urls.length; i++) {
                sb.append(urls[i].getDownloadURL());
                sb.append(',');
            }
            this.setBrowserExclusive();
            br.forceDebug(true);
            br.setCookie("http://bluehost.to", "bluehost_lang", "DE");
            br.postPage("http://bluehost.to/fileinfo/multi", sb.toString());

            String[] lines = Regex.getLines(br + "");

            for (int i = 0; i < urls.length; i++) {
                String[] dat = lines[i].split("\\, ");
                try {
                    urls[i].setMD5Hash(dat[5].trim());
                    urls[i].setFinalFileName(dat[0]);
                    urls[i].setDupecheckAllowed(true);
                    urls[i].setDownloadSize(Long.parseLong(dat[2]));
                    urls[i].setAvailable(true);
                } catch (Exception e) {
                    urls[i].setAvailable(false);
                }
            }

        } catch (Exception e) {
            System.gc();
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
            return false;
        }

        return true;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        correctUrl(downloadLink);
        // dateiname, dateihash, dateisize, dateidownloads, zeit bis HH
        this.setBrowserExclusive();
        br.setCookie("http://bluehost.to", "bluehost_lang", "DE");
        String page = br.getPage("http://bluehost.to/fileinfo/urls=" + downloadLink.getDownloadURL());

        String[] dat = page.split("\\, ");
        try {
            downloadLink.setMD5Hash(dat[5].trim());
            downloadLink.setFinalFileName(dat[0]);
            downloadLink.setDupecheckAllowed(true);
            downloadLink.setDownloadSize(Long.parseLong(dat[2]));
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
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
}
