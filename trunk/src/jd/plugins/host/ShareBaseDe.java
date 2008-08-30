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

import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class ShareBaseDe extends PluginForHost {

    private static final String CODER = "JD-Team";

    private static final String DL_LIMIT = "Das Downloaden ohne Downloadlimit ist nur mit einem Premium-Account";

    private static final String DOWLOAD_RUNNING = "Von deinem Computer ist noch ein Download aktiv";

    private static final String FILENAME = "<title>(.*?)</title>";

    private static final String FILESIZE = "<span class=\"f1\">.*?\\((.*?)\\)</span></td>";

    private static final String HOST = "sharebase.de";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?sharebase\\.de/files/[a-zA-Z0-9]{10}\\.html", Pattern.CASE_INSENSITIVE);

    private static final String PLUGIN_NAME = HOST;

    private static final String SIM_DL = "Das gleichzeitige Downloaden";

    private static final Pattern WAIT = Pattern.compile("Du musst noch (.*?):(.*?):(.*?) warten!", Pattern.CASE_INSENSITIVE);

    private String cookies = "";

    /*
     * Konstruktor
     */
    public ShareBaseDe() {

        super();
        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

    @Override
    public String getAGBLink() {

        return "http://sharebase.de/pp.html";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            RequestInfo requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
            String fileName = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), FILENAME).getMatch(0));
            String fileSize = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), FILESIZE).getMatch(0));
            boolean sim_dl = new Regex(requestInfo.getHtmlCode(), SIM_DL).count() > 0;
            boolean dl_limit = new Regex(requestInfo.getHtmlCode(), DL_LIMIT).count() > 0;
            // Wurden DownloadInfos gefunden? --> Datei ist vorhanden/online
            if (fileName != null && fileSize != null) {

                fileName = fileName.trim();
                fileSize = fileSize.trim();
                downloadLink.setName(fileName);

                try {
                    String[] fileSizeData = fileSize.split(" ");

                    double length = Double.parseDouble(fileSizeData[0].trim());

                    if (fileSizeData[1].equals("KB")) {
                        length *= 1024;
                    } else if (fileSizeData[1].equals("MB")) {
                        length *= 1048576;
                    }
                    downloadLink.setDownloadSize((int) length);
                } catch (Exception e) {
                }

                return true;
            }

            if (sim_dl || dl_limit) { return true; }

        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }

        // Datei scheinbar nicht mehr verfuegbar, Fehler?
        return false;

    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        RequestInfo requestInfo;

        URL downloadUrl = new URL(downloadLink.getDownloadURL());
        String finishURL = null;

        requestInfo = HTTP.getRequest(downloadUrl);

        String fileName = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), FILENAME).getMatch(0));

        if (requestInfo.containsHTML(DOWLOAD_RUNNING)) {
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(60 * 1000);
            return;
        }
        // Download-Limit erreicht
        if (requestInfo.getHtmlCode().contains(DL_LIMIT)) {

            String[] temp = new Regex(requestInfo.getHtmlCode(), WAIT).getRow(0);
            String hours = temp[0];
            String minutes = temp[1];
            String seconds = temp[2];
            int waittime = 0;

            if (hours != null && minutes != null && seconds != null) {

                try {

                    waittime += Integer.parseInt(seconds);
                    waittime += Integer.parseInt(minutes) * 60;
                    waittime += Integer.parseInt(hours) * 3600;

                } catch (Exception Exc) {
                	waittime = 600;
                }

            }

            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            linkStatus.setValue(waittime * 1000);
            return;
        }

        // DownloadInfos nicht gefunden? --> Datei nicht vorhanden
        if (fileName == null) {
            logger.severe("download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        fileName = fileName.trim();

        // SessionId auslesen
        cookies = requestInfo.getCookie().split("; ")[0];

        requestInfo = HTTP.postRequest(downloadUrl, cookies, downloadLink.getDownloadURL(), null, "doit=Download+starten", false);
        finishURL = Encoding.htmlDecode(requestInfo.getConnection().getHeaderField("Location"));

        if (finishURL == null) {
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        // Download vorbereiten
        HTTPConnection urlConnection = new HTTPConnection(new URL(finishURL).openConnection());

        // Download starten
        dl = new RAFDownload(this, downloadLink, urlConnection);

        dl.startDownload();

    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }
    
    @Override
    public void reset() {
        cookies = "";
    }

    @Override
    public void resetPluginGlobals() {

    }

}
