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

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.download.RAFDownload;

public class DataHu extends PluginForHost {

    private static final String HOST = "data.hu";

    private static final String VERSION = "1.0.0";

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
        //steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public void handle( final DownloadLink downloadLink) {
        // if (aborted) {
        // logger.warning("Plugin aborted");
        // downloadLink.setStatus(LinkStatus.TODO);
        // //step.setStatus(PluginStep.STATUS_TODO);
        // return;
        // }
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(url));

            String link = new Regex(requestInfo.getHtmlCode(), Pattern.compile("window.location.href='(.*?)'",Pattern.CASE_INSENSITIVE)).getFirstMatch();
            String[] test = link.split("/");
            String name = test[test.length - 1];
            downloadLink.setName(name);

            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), null, url, false);

            HTTPConnection urlConnection = requestInfo.getConnection();
            if (!getFileInformation(downloadLink)) {
                downloadLink.setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                //step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }

            downloadLink.setDownloadMax(urlConnection.getContentLength());
            final long length = downloadLink.getDownloadMax();

            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.setFilesize(length);
            // dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").
            // getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));

            if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {

                downloadLink.setStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                //step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
            return;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);

        return;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(url));
            if (requestInfo.getHtmlCode().length() == 0) return false;
            String[] test = new Regex(requestInfo.getHtmlCode(), Pattern.compile("window.location.href='(.*?)'",Pattern.CASE_INSENSITIVE)).getFirstMatch().split("/");
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