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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class FileMojoCom extends PluginForHost {

    public FileMojoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.filemojo.com/help/tos.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());

            String url = downloadLink.getDownloadURL();

            String fileId = new Regex(url, "filemojo\\.com/(\\d+)").getMatch(0);
            if (fileId == null) fileId = new Regex(url, "filemojo\\.com/l\\.php\\?flink=(\\d+)").getMatch(0);
            url = "http://www.filemojo.com/l.php?flink=" + fileId + "&fx=";

            br.getPage(url);

            if (br.containsHTML("Sorry File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String size = br.getRegex("(\\d+\\.\\d+) (MB|KB)").getMatch(-1);
            downloadLink.setDownloadSize(Regex.getSize(size));

            String name = br.getRegex("<b>File Name.*?size=\"2\">[\r\n]*(.*?)<br>").getMatch(0);
            downloadLink.setName(name);

            return AvailableStatus.TRUE;
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    // @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        dl = br.openDownload(downloadLink, br.getForm(1));
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert nachprüfen */
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}