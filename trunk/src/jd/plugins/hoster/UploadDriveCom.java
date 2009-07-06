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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision="$Revision", interfaceVersion=1, names = { "upload-drive.com"}, urls ={ "http://[\\w\\.]*?upload-drive\\.com/\\d+/.+"}, flags = {0})
public class UploadDriveCom extends PluginForHost {

    public UploadDriveCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
    }

    //@Override
    public String getAGBLink() {
        return "http://www.upload-drive.com/terms.html";
    }

    //@Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Invalid file") || br.containsHTML("Invalid request") || br.containsHTML("Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<b>File name:</b> (.*?)\\s+<b>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        // String filesize =
        // br.getRegex("<b> File size:</b>\\s+(.*?)</p>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // || filesize.equalsIgnoreCase("0.00 KB"))
        downloadLink.setName(filename.trim());
        // downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",",
        // "\\.")));
        return AvailableStatus.TRUE;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String linkurl = br.getRegex("<div class=\"logo\"[^>]*>\\s*<a href=\"(.*?)\"><img").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, linkurl);
        dl.startDownload();
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    //@Override
    public void reset() {
    }

    //@Override
    public void resetPluginGlobals() {
    }

    //@Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
        
    }
}
