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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class RomHustlerNet extends PluginForHost {

    private String downloadUrl;

    public RomHustlerNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public String getAGBLink() {
        return "http://romhustler.net/disclaimer.php";
    }

    //@Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) {
        try {
            this.setBrowserExclusive();
            br.getPage(downloadLink.getDownloadURL());
            downloadUrl = decodeurl(br.getRegex(Pattern.compile("link_enc=new Array\\((.*?)\\);", Pattern.CASE_INSENSITIVE)).getMatch(0));
            if (downloadUrl == null) return AvailableStatus.FALSE;
            String name = Encoding.htmlDecode(downloadUrl.replaceAll("^.*/", ""));
            downloadLink.setName(name);
            return AvailableStatus.TRUE;
        } catch (Exception e) {
        }
        return AvailableStatus.FALSE;
    }

    //@Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        if (!downloadLink.isAvailable()) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        br.openDownload(downloadLink, downloadUrl, true, 1).startDownload();
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert nachprüfen */
        return 1;
    }

    //@Override
    public void reset() {
    }

    //@Override
    public void resetPluginGlobals() {
    }

    private static String decodeurl(String page) {
        if (page == null) return null;
        StringBuffer sb = new StringBuffer();
        String pattern = "('.'),?";
        Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(page);
        while (r.find()) {
            if (r.group(1).length() > 0) {
                String content = r.group(1).replaceAll("'|,", "");
                r.appendReplacement(sb, content);
            }
        }
        r.appendTail(sb);
        return sb.toString();
    }

    //@Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}