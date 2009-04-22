package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class KewlshareCom extends PluginForHost {

    public KewlshareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://kewlshare.com/tos";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<tr><td>File Name : <strong>(.*?)\\|\\|").getMatch(0);
        String filesize = br.getRegex("<tr><td>File Name : <strong>.*?\\|\\|(.*?)</strong></td></tr>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        Form form = br.getForm(1);
        br.submitForm(form);
        if (br.containsHTML("Your Current Download Limit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
        form = br.getForm(0);
        br.submitForm(form);
        form = br.getForm(0);
        br.submitForm(form);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, br.getMatch("Your download should have started automatically.*?href=\"(.*?)\">"), false, 1);
        if (dl.getConnection().getURL().toString().contains("MAX_BY_IP")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 2 * 60 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2500;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

    @Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
        
    }

}
