//    jDownloader - Downloadmanager//    Copyright (C) 2009  JD-Team support@jdownloader.org////    This program is free software: you can redistribute it and/or modify//    it under the terms of the GNU General Public License as published by//    the Free Software Foundation, either version 3 of the License, or//    (at your option) any later version.////    This program is distributed in the hope that it will be useful,//    but WITHOUT ANY WARRANTY; without even the implied warranty of//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the//    GNU General Public License for more details.////    You should have received a copy of the GNU General Public License//    along with this program.  If not, see <http://www.gnu.org/licenses/>.package jd.plugins.hoster;import jd.PluginWrapper;import jd.nutils.encoding.Encoding;import jd.parser.Regex;import jd.plugins.DownloadLink;import jd.plugins.HostPlugin;import jd.plugins.LinkStatus;import jd.plugins.PluginException;import jd.plugins.PluginForHost;import jd.plugins.DownloadLink.AvailableStatus;@HostPlugin(revision = "$Revision: 9045 $", interfaceVersion = 2, names = { "filesmonster.com" }, urls = { "http://filesmonster\\.com/download.php\\?id=.+" }, flags = { 2 })public class FilesMonsterCom extends PluginForHost {    public FilesMonsterCom(PluginWrapper wrapper) {        super(wrapper);    }    @Override    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {        br.getPage(downloadLink.getDownloadURL());        String filesize = br.getRegex("File size: <span class=\"em\">(.*?)</span>").getMatch(0);        String filename = br.getRegex("File name: <span class=\"em\">(.*?)</span>").getMatch(0);        if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }        downloadLink.setName(Encoding.htmlDecode(filename.trim()));        if (filesize != null) {            downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));        }        return AvailableStatus.TRUE;    }    @Override    public void handleFree(DownloadLink downloadLink) throws Exception {        br.getPage(downloadLink.getDownloadURL());        String wait = br.getRegex("You can wait for the start of downloading (\\d+) minute").getMatch(0);        if (wait != null){        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(wait) * 60 * 1000l);        }        wait = br.getRegex("is already in use (\\d+) free download").getMatch(0);        if (wait != null){        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);        }        /* get file id */        String fileID = br.getRegex("<input type=\"hidden\" name=\"t\" value=\"(.*?)\"").getMatch(0);        if (fileID == null) {            String isPay = br.getRegex("<input type=\"hidden\" name=\"showpayment\" value=\"(1)").getMatch(0);            String isFree = br.getRegex("(slowdownload)").getMatch(0);            String isFM = br.getRegex("(filesmonster)").getMatch(0);            if (isFM == null){ /* no filesmonsterpage, retry */                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid page", 20 * 1000l);            }else if (isFree == null && isPay != null){                throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");            }else{                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);            }        }        br.postPage("http://filesmonster.com/get/free/", "t=" + fileID);        /* now we have the data page, check for wait time and data id */        String data = br.getRegex("name='data' value='(.*?)'>").getMatch(0);        wait = br.getRegex("'wait_sec'>(\\d+)<").getMatch(0);        if (data == null || wait == null){        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);        }        /* request ticket for this file */        br.postPage("http://filesmonster.com/ajax.php", "act=rticket&data=" + data);        data = br.getRegex("\\{\"text\":\"(.*?)\"").getMatch(0);        /* wait */        sleep(1000l * (Long.parseLong(wait) + 2), downloadLink);        /* request download information */        br.postPage("http://filesmonster.com/ajax.php", "act=getdl&data=" + data);        String url = br.getRegex("\\{\"url\":\"(.*?)\"").getMatch(0);        data = br.getRegex("\"file_request\":\"(.*?)\"").getMatch(0);        if (data == null || url == null){        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);        }        url = url.replaceAll("\\\\/", "/");        /* start download */        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, "X-File-Request=" + data);        if (!dl.getConnection().isContentDisposition()) {            br.followConnection();            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);        }        dl.setFilenameFix(true);        dl.startDownload();    }    @Override    public int getMaxSimultanFreeDownloadNum() {        return 1;    }    @Override    public void reset() {    }    @Override    public void resetDownloadlink(DownloadLink link) {    }    @Override    public String getAGBLink() {        return "http://filesmonster.com/rules.php";    }}