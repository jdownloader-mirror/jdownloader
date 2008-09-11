//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.net.URL;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Configuration;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class DataHu extends PluginForHost {

  

    public DataHu(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://filebase.to/tos/";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {

        try {
            Browser br = new Browser();

            String url = downloadLink.getDownloadURL();
            String page = br.getPage(url);

            if (page == null || page.length() == 0) { return false; }
            String[][] dat = new Regex(br, "<div class=\"download_filename\">(.*?)<\\/div>.*\\:(.*?)<div class=\"download_not_start\">", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).getMatches();
            long length = Regex.getSize(dat[0][1].trim());
            downloadLink.setDownloadSize(length);
            downloadLink.setName(dat[0][0].trim());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        String url = downloadLink.getDownloadURL();
        RequestInfo requestInfo = HTTP.getRequest(new URL(url));

        String link = new Regex(requestInfo.getHtmlCode(), Pattern.compile("window.location.href='(.*?)'", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String[] test = link.split("/");
        String name = test[test.length - 1];
        downloadLink.setName(name);

        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), null, url, false);

        HTTPConnection urlConnection = requestInfo.getConnection();
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.setResume(true);
        dl.startDownload();
        return;

    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}