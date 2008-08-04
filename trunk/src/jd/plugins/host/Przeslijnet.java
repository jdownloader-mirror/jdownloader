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
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class Przeslijnet extends PluginForHost {

    private static final String HOST = "przeslij.net";

    static private final Pattern patternSupported = Pattern.compile("http://www2\\.przeslij\\.net/download.php\\?file=(.*)", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String url;

    //

    public Przeslijnet() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "hhttp://www2.przeslij.net/#";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            String url = downloadLink.getDownloadURL();

            requestInfo = HTTP.getRequest(new URL(url));
            if (!requestInfo.containsHTML("Invalid download link")) {
                downloadLink.setName(JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("<font color=#000000>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getFirstMatch()));
                String filesize = null;
                if ((filesize = new Regex(requestInfo.getHtmlCode(), "File Size:</td><td bgcolor=\\#EEF4FB background=\"img\\/button03.gif\"><font color=#000080>(.*)MB</td>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = new Regex(requestInfo.getHtmlCode(), "File Size:</td><td bgcolor=\\#EEF4FB background=\"img\\/button03.gif\"><font color=#000080>(.*)KB</td>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)) * 1024);
                }
                return true;
            }
        } catch (MalformedURLException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        // switch (step.getStep()) {
        // case PluginStep.STEP_PAGE:
        url = downloadLink.getDownloadURL();
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        // case PluginStep.STEP_PENDING:
        /* Zwangswarten, 15seks */
        sleep(15000, downloadLink);

        // case PluginStep.STEP_DOWNLOAD:
        /* Link holen */
        String linkurl = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), "onClick=\"window\\.location=\\\\\'(.*?)\\\\\'").getFirstMatch());
        /* Datei herunterladen */
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(linkurl), requestInfo.getCookie(), url.toString(), false);
        HTTPConnection urlConnection = requestInfo.getConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
