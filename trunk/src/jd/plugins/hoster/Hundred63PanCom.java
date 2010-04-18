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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "163pan.com" }, urls = { "http://[\\w\\.]*?163pan\\.com/files/[a-z0-9]+\\.html" }, flags = { 0 })
public class Hundred63PanCom extends PluginForHost {

    public Hundred63PanCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.163pan.com/help/service.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<font>系统提示</font>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"font_w\">(.*?)<img").getMatch(0);
        if (filename == null) filename = br.getRegex("id=\"FileName\" value=\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("span>文件大小：</span>(.*?)\\(").getMatch(0);
        if (filesize == null) filesize = br.getRegex("\\(([0-9,]+ 字节)\\)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        filesize = filesize.replace("字节", "bytes").replace(",", "");
        link.setDownloadSize(Regex.getSize(filesize));
        String md5 = br.getRegex("<span>MD5值：</span>(.*?)</li>").getMatch(0);
        String sh1 = br.getRegex("<span>SHA1值：</span>(.*?)</li>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
        if (sh1 != null) link.setMD5Hash(sh1);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL().replace("/files/", "/download/"));
        String dllink = br.getRegex("oncontextmenu='Flashget_SetHref_js\\(.*?\\);'></a> -->.*?<a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\.163pan\\.com/download/index/id=[a-z0-9]+\\&hash=[a-z0-9]+\\&tt=\\d+/www\\.163pan\\.com.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}