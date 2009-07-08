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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision="$Revision", interfaceVersion=2, names = { "dump.ru"}, urls ={ "http://[\\w\\.]*?dump\\.ru/file/[0-9]+"}, flags = {0})
public class DumpRu extends PluginForHost {

    public DumpRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://dump.ru/pages/about/";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            setBrowserExclusive();
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            // File not found
            if (br.containsHTML("Запрошенный файл не обнаружен")) {
                logger.warning("File not found");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            // Filesize

            // String size =
            // br.getRegex(Pattern.compile("<span class=\"comment\">(.*&nbsp;.*)</span><br>")).getMatch(0);
            // size = size.replaceAll("Кб", "KB").replaceAll("Mб", "MB"); <<<
            // does NOT WORK ...
            // System.out.println(size);
            // downloadLink.setDownloadSize(Regex.getSize(size.replaceAll("Кб",
            // "KB").replaceAll("Mб", "MB")));

            // Filename
            String name = br.getRegex("name_of_file\">\\s(.*?)</span>").getMatch(0).trim();
            downloadLink.setName(name);

            return AvailableStatus.TRUE;
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    // @Override
    /* /* public String getVersion() {
        return getVersion("$Revision$");
    } */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.submitForm(br.getForm(1));
        String link = br.getRegex(Pattern.compile("<a href=\"(http://.*?dump\\.ru/file_download/.*?)\">")).getMatch(0);
        //final filename can't be taken from the header due to encoding problems, set it here
        downloadLink.setFinalFileName(downloadLink.toString());
        dl = br.openDownload(downloadLink, link);
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
