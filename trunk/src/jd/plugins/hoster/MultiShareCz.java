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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.Property;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multishare.cz" }, urls = { "https?://[\\w\\.]*?multishare\\.cz/stahnout/[0-9]+/" }, flags = { 2 })
public class MultiShareCz extends PluginForHost {

    public MultiShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.multishare.cz/cenik/");
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            ai.setProperty("multiHostSupport", Property.NULL);
            throw e;
        }
        account.setValid(true);
        try {
            account.setConcurrentUsePossible(true);
            account.setMaxSimultanDownloads(-1);
        } catch (final Throwable e) {
        }
        final String trafficleft = getJson("credit");
        if (trafficleft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft + " MB"));
        }
        ai.setStatus("Premium User");
        if (System.getProperty("jd.revision.jdownloaderrevision") != null) {
            try {
                br.getPage("https://www.multishare.cz/api/?sub=supported-hosters");
                final String hostsText = br.getRegex("\\{\"server\":\\[(.*?)\\]").getMatch(0);
                String[] hosts = hostsText.split(",");
                ArrayList<String> supportedHosts = new ArrayList<String>();
                for (String host : hosts) {
                    host = host.replace("\"", "");
                    if ("freakshare.net".equalsIgnoreCase(host)) host = "freakshare.com";
                    supportedHosts.add(host);
                }
                /*
                 * set ArrayList<String> with all supported multiHosts of this service
                 */
                ai.setProperty("multiHostSupport", supportedHosts);
            } catch (Throwable e) {
                account.setProperty("multiHostSupport", Property.NULL);
                logger.info("Could not fetch ServerList from Multishare: " + e.toString());
            }
        }
        return ai;
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    @Override
    public String getAGBLink() {
        return "http://www.multishare.cz/kontakt/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String fileid = new Regex(downloadLink.getDownloadURL(), "/stahnout/(\\d+)/").getMatch(0);
        String dllink = "https://www.multishare.cz/html/download_free.php?ID=" + fileid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        String fileid = new Regex(link.getDownloadURL(), "/stahnout/(\\d+)/").getMatch(0);
        String dllink = "https://www.multishare.cz/html/download_premium.php?ID=" + fileid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        /* login to get u_ID and u_HASH */
        br.getPage("https://www.multishare.cz/api/?sub=download-link&login=" + Encoding.urlEncode(acc.getUser()) + "&password=" + Encoding.urlEncode(acc.getPass()) + "&link=" + Encoding.urlEncode(link.getDownloadURL()));
        String dllink = getJson("link");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else {
            /*
             * download is not contentdisposition, so remove this host from premiumHosts list
             */
            br.followConnection();
        }
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage("https://www.multishare.cz/api/?sub=account-details&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("ERR: User does not exists")) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(Požadovaný soubor neexistuje|Je možné, že byl již tento soubor vymazán uploaderem nebo porušoval autorská práva)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>MultiShare\\.cz :: Stáhnout soubor \"(.*?)\"</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<li>Název: <strong>(.*?)</strong>").getMatch(0);
        String filesize = br.getRegex("Velikost: <strong>(.*?)</strong").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        filesize = filesize.replace("&nbsp;", "");
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}