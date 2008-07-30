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
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class DataHu extends PluginForHost {

    private static final String HOST = "data.hu";

    private static final String VERSION = "$Revision$";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?data.hu/get/.+/.+", Pattern.CASE_INSENSITIVE);

    //
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    public DataHu() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public void handle(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        // if (aborted) {
        // logger.warning("Plugin aborted");
        // linkStatus.addStatus(LinkStatus.TODO);
        // //step.setStatus(PluginStep.STATUS_TODO);
        // return;
        // }

        String url = downloadLink.getDownloadURL();
        requestInfo = HTTP.getRequest(new URL(url));

        String link = new Regex(requestInfo.getHtmlCode(), Pattern.compile("window.location.href='(.*?)'", Pattern.CASE_INSENSITIVE)).getFirstMatch();
        String[] test = link.split("/");
        String name = test[test.length - 1];
        downloadLink.setName(name);

        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), null, url, false);

        HTTPConnection urlConnection = requestInfo.getConnection();
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        downloadLink.setDownloadMax(urlConnection.getContentLength());
        final long length = downloadLink.getDownloadMax();

        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setFilesize(length);
        // dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").
        // getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.startDownload();
        // if (!dl.startDownload() && step.getStatus() !=
        // PluginStep.STATUS_ERROR && step.getStatus() !=
        // PluginStep.STATUS_TODO) {
        //
        // linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // return;
        // }
        return;

    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(url));
            if (requestInfo.getHtmlCode().length() == 0) return false;
            String[] test = new Regex(requestInfo.getHtmlCode(), Pattern.compile("window.location.href='(.*?)'", Pattern.CASE_INSENSITIVE)).getFirstMatch().split("/");
            String name = test[test.length - 1];
            downloadLink.setName(name);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getAGBLink() {
        return "http://filebase.to/tos/";
    }
}