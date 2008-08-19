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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class ArchivTo extends PluginForHost {

    static private final String FILENAME = "<td width=\".*\">Original-Dateiname</td>\n	<td width=\".*\">: <a href=\".*\" style=\".*\">(.*?)</a></td>";

    static private final String FILESIZE = "<td width=\".*\">: ([0-9]+) Byte";

    private static final String HOST = "archiv.to";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?archiv\\.to/\\?Module\\=Details\\&HashID\\=.*", Pattern.CASE_INSENSITIVE);

    public ArchivTo() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://archiv.to/?Module=Policy";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        Browser.clearCookies(HOST);

        try {
            String url = downloadLink.getDownloadURL();
            br.getPage(url);

            downloadLink.setName(br.getRegex(FILENAME).getMatch(0));
            if (!br.containsHTML(":  Bytes (~ 0 MB)")) {
                downloadLink.setDownloadSize(Integer.parseInt(br.getRegex(FILESIZE).getMatch(0)));
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        LinkStatus linkStatus = downloadLink.getLinkStatus();
        Browser.clearCookies(HOST);
        try {
            String url = downloadLink.getDownloadURL();

            br.getPage(url);

            downloadLink.setDownloadSize(Integer.parseInt(br.getRegex("<td width=.*?>: ([\\d]*?) Bytes").getMatch(0)));
            RequestInfo requestInfo = HTTP.getRequestWithoutHtmlCode(new URL("http://archiv.to/Get/?System=Download&Hash=" + new Regex(url, ".*HashID=(.*)").getMatch(0)), null, url, true);

            HTTPConnection urlConnection = requestInfo.getConnection();
            if (!getFileInformation(downloadLink)) {
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                return;
            }
            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.startDownload();
            return;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        return;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
