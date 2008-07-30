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
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.UTILITIES;
import jd.controlling.DistributeData;
import jd.event.ControlEvent;
import jd.parser.Form;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

public class Serienjunkies extends PluginForHost {
    private final static String HOST = "Serienjunkies.org";

    private static final String VERSION = "$Revision$";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?sjdownload.org.*", Pattern.CASE_INSENSITIVE);
    private Pattern patternCaptcha = null;
    private String dynamicCaptcha = "<FORM ACTION=\".*?\" METHOD=\"post\"(?s).*?(?-s)<INPUT TYPE=\"HIDDEN\" NAME=\"s\" VALUE=\"([\\w]*)\">(?s).*?(?-s)<IMG SRC=\"([^\"]*)\"";
    private String subdomain = "download.";

    //
    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

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

    public Serienjunkies() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    @Override
    public boolean collectCaptchas() {
       
        return false;
    }

    @Override
    public boolean useUserinputIfCaptchaUnknown() {
       
        return false;
    }

    public ArrayList<DownloadLink> getDLinks(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {

            URL url = new URL(parameter);
            subdomain = new Regex(parameter, "http://(.*?)serienjunkies.org.*").getFirstMatch();
            String modifiedURL = JDUtilities.htmlDecode(url.toString());
            modifiedURL = modifiedURL.replaceAll("safe/", "safe/f");
            modifiedURL = modifiedURL.replaceAll("save/", "save/f");
            modifiedURL = modifiedURL.substring(modifiedURL.lastIndexOf("/"));

            patternCaptcha = Pattern.compile(dynamicCaptcha);
            logger.fine("using patternCaptcha:" + patternCaptcha);
            RequestInfo reqinfo = HTTP.getRequest(url, null, null, true);
            if (reqinfo.getLocation() != null) reqinfo = HTTP.getRequest(url, null, null, true);
            if (reqinfo.containsHTML("Download-Limit")) {
                logger.info("Sj Downloadlimit(decryptlimit) reached. Wait for reconnect(max 5 min)");
                if (Reconnecter.waitForNewIP(2 * 60 * 1000l)) {
                    logger.info("Reconnect successfull. try again");
                    reqinfo = HTTP.getRequest(url, null, null, true);
                    if (reqinfo.getLocation() != null) reqinfo = HTTP.getRequest(url, null, null, true);
                } else {
                    logger.severe("Reconnect failed. abort.");
                    return decryptedLinks;
                }
            }
            String furl = SimpleMatches.getSimpleMatch(reqinfo.getHtmlCode(), "<FRAME SRC=\"°" + modifiedURL.replaceAll("[^0-1a-zA-Z]", ".") + "\"", 0);
            if (furl != null) {
                url = new URL(furl + modifiedURL);
                logger.info("Frame found. frame url: " + furl + modifiedURL);
                reqinfo = HTTP.getRequest(url, null, null, true);
                parameter = furl + modifiedURL;

            }

            // logger.info(reqinfo.getHtmlCode());

            ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), " <a href=\"http://°\"");
            Vector<String> helpvector = new Vector<String>();
            String helpstring = "";
            // Einzellink

            if (parameter.indexOf("/safe/") >= 0 || parameter.indexOf("/save/") >= 0) {
                logger.info("safe link");
                helpstring = EinzelLinks(parameter);
                // if (aborted) return null;
                decryptedLinks.add(new DownloadLink(this, null, this.getHost(), JDUtilities.htmlDecode(helpstring), true));
            } else if (parameter.indexOf(subdomain + "serienjunkies.org") >= 0) {
                logger.info("sjsafe link");
                helpvector = ContainerLinks(parameter);
                // if (aborted) return null;
                for (int j = 0; j < helpvector.size(); j++) {
                    decryptedLinks.add(new DownloadLink(this, null, this.getHost(), JDUtilities.htmlDecode(helpvector.get(j)), true));
                }
            } else if (parameter.indexOf("/sjsafe/") >= 0) {
                logger.info("sjsafe link");
                helpvector = ContainerLinks(parameter);
                // if (aborted) return null;
                for (int j = 0; j < helpvector.size(); j++) {
                    decryptedLinks.add(new DownloadLink(this, null, this.getHost(), JDUtilities.htmlDecode(helpvector.get(j)), true));
                }
            } else {
                logger.info("else link");
                // Kategorien
                for (int i = 0; i < links.size(); i++) {
                    // if (aborted) return null;
                    if (links.get(i).get(0).indexOf("/safe/") >= 0) {
                        helpstring = EinzelLinks(links.get(i).get(0));
                        // if (aborted) return null;
                        decryptedLinks.add(new DownloadLink(this, null, this.getHost(), JDUtilities.htmlDecode(helpstring), true));
                    } else if (links.get(i).get(0).indexOf("/sjsafe/") >= 0) {
                        helpvector = ContainerLinks(links.get(i).get(0));
                        // if (aborted) return null;
                        for (int j = 0; j < helpvector.size(); j++) {
                            decryptedLinks.add(new DownloadLink(this, null, this.getHost(), JDUtilities.htmlDecode(helpvector.get(j)), true));
                        }
                    } else {
                        decryptedLinks.add(new DownloadLink(this, null, this.getHost(), JDUtilities.htmlDecode(links.get(i).get(0)), true));
                    }
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return decryptedLinks;
    }

    // Für Links die bei denen die Parts angezeigt werden
    private Vector<String> ContainerLinks(String url) {
        Vector<String> links = new Vector<String>();
        boolean fileDownloaded = false;
        if (url.matches("http://[\\w\\.]*?.serienjunkies.org/..\\-.*")) {
            url = url.replaceFirst("serienjunkies.org", "serienjunkies.org/frame");
        }
        if (!url.startsWith("http://")) url = "http://" + url;
        try {
            RequestInfo reqinfo = HTTP.getRequest(new URL(url));

            String cookie = reqinfo.getCookie();
            File captchaFile = null;
            String capTxt = null;
            while (true) { // for() läuft bis kein Captcha mehr abgefragt
            // if (aborted) return null;
                reqinfo.setHtmlCode(reqinfo.getHtmlCode().replaceAll("(?s)<!--.*?-->", "").replaceAll("(?i)(?s)<div style=\"display: none;\">.*?</div>", ""));
                Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                    }
                    ArrayList<ArrayList<String>> gifs = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), patternCaptcha);

                    String captchaAdress = "http://" + subdomain + "serienjunkies.org" + gifs.get(0).get(1);
                    // for (int i = 0; i < gifs.size(); i++) {
                    // if (gifs.get(i).get(0).indexOf("secure") >= 0 &&
                    // JDUtilities.filterInt(gifs.get(i).get(2)) > 0 &&
                    // JDUtilities.filterInt(gifs.get(i).get(3)) > 0) {
                    // captchaAdress = "http://85.17.177.195" +
                    // gifs.get(i).get(0);
                    // logger.info(gifs.get(i).get(0));
                    // }
                    // }
                    HTTPConnection con = HTTP.getRequestWithoutHtmlCode(new URL(captchaAdress), cookie, null, true).getConnection();

                    if (con.getResponseCode() < 0) {
                        captchaAdress = "http://" + subdomain + "serienjunkies.org" + gifs.get(0).get(1);
                        con = HTTP.getRequestWithoutHtmlCode(new URL(captchaAdress), cookie, null, true).getConnection();

                    }
                    if (con.getContentLength() < 1000) {
                        if (!Reconnecter.waitForNewIP(5 * 60l)) { return null; }

                        reqinfo = HTTP.getRequest(new URL(url));
                        cookie = reqinfo.getCookie();

                        continue;
                    }

                    captchaFile = getLocalCaptchaFile(this, ".gif");

                    fileDownloaded = JDUtilities.download(captchaFile, con);
                    if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                        logger.severe("Captcha nicht heruntergeladen, warte und versuche es erneut");
                        try {
                            Thread.sleep(1000);
                            reqinfo = HTTP.getRequest(new URL(url));
                            cookie = reqinfo.getCookie();
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                    logger.info("captchafile: " + captchaFile);
                    capTxt = Plugin.getCaptchaCode(captchaFile, this);

                    reqinfo = HTTP.postRequest(new URL(url), "s=" + matcher.group(1) + "&c=" + capTxt + "&action=Download");

                } else {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);

                        if (useUserinputIfCaptchaUnknown() && this.getCaptchaDetectionID() == Plugin.CAPTCHA_USER_INPUT && this.getLastCaptcha() != null && this.getLastCaptcha().getLetterComperators() != null) {
                            LetterComperator[] lcs = this.getLastCaptcha().getLetterComperators();
                            this.getLastCaptcha().setCorrectcaptchaCode(capTxt.trim());

                            if (lcs.length == capTxt.trim().length()) {
                                for (int i = 0; i < capTxt.length(); i++) {

                                    if (lcs[i] != null && lcs[i].getDecodedValue() != null && capTxt.substring(i, i + 1).equalsIgnoreCase(lcs[i].getDecodedValue()) && lcs[i].getValityPercent() < 30.0) { //
                                        logger.severe("OK letter: " + i + ": JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER: " + capTxt.substring(i, i + 1));
                                    } else {

                                        logger.severe("Unknown letter: // " + i + ":  JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER:  " + capTxt.substring(i, i + 1)); // Pixelstring.
                                        // getB()
                                        // ist
                                        // immer
                                        // der
                                        // neue
                                        // letter
                                        final String character = capTxt.substring(i, i + 1);
                                        logger.info("SEND");
                                        Letter letter = lcs[i].getA();
                                        String captchaHash = UTILITIES.getLocalHash(captchaFile);
                                        letter.setSourcehash(captchaHash);
                                        letter.setOwner(this.getLastCaptcha().owner);
                                        letter.setDecodedValue(character);
                                        this.getLastCaptcha().owner.letterDB.add(letter);
                                        this.getLastCaptcha().owner.saveMTHFile();
                                    }
                                }

                            } else {
                                logger.info("LCS not length comp");
                            }
                        }
                    }
                    break;
                }
            }
            if (reqinfo.getLocation() != null) {
                links.add(reqinfo.getLocation());
            }
            Form[] forms = reqinfo.getForms();
            for (int i = 0; i < forms.length; i++) {
                if (!forms[i].action.contains("firstload")) {
                    try {
                        reqinfo = HTTP.getRequest(new URL(forms[i].action));
                        reqinfo = HTTP.getRequest(new URL(SimpleMatches.getBetween(reqinfo.getHtmlCode(), "SRC=\"", "\"")), null, null, false);
                        String loc = reqinfo.getLocation();
                        if (loc != null) links.add(loc);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    // Für Links die gleich auf den Hoster relocaten
    private String EinzelLinks(String url) {
        String links = "";
        boolean fileDownloaded = false;
        if (!url.startsWith("http://")) url = "http://" + url;
        try {
            url = url.replaceAll("safe/", "safe/f");
            url = url.replaceAll("save/", "save/f");
            RequestInfo reqinfo = HTTP.getRequest(new URL(url));
            File captchaFile = null;
            String capTxt = null;
            while (true) { // for() läuft bis kein Captcha mehr abgefragt
            // if (aborted) return null;
                reqinfo.setHtmlCode(reqinfo.getHtmlCode().replaceAll("(?s)<!--.*?-->", "").replaceAll("(?i)(?s)<div style=\"display: none;\">.*?</div>", ""));
                Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                    }
                    String captchaAdress = "http://serienjunki.es" + matcher.group(2);
                    captchaFile = getLocalCaptchaFile(this, ".gif");
                    fileDownloaded = JDUtilities.download(captchaFile, captchaAdress);
                    logger.info("captchafile: " + fileDownloaded);
                    if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                        logger.severe("Captcha nicht heruntergeladen, warte und versuche es erneut");
                        try {
                            Thread.sleep(1000);
                            reqinfo = HTTP.getRequest(new URL(url));
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                    capTxt = JDUtilities.getCaptcha(this, "einzellinks.Serienjunkies.org", captchaFile, false);
                    // System.out.println(capTxt);
                    reqinfo = HTTP.postRequest(new URL(url), "s=" + matcher.group(1) + "&c=" + capTxt + "&dl.start=Download");
                    // System.out.println(reqinfo);
                } else {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);
                        if (useUserinputIfCaptchaUnknown() && this.getCaptchaDetectionID() == Plugin.CAPTCHA_USER_INPUT && this.getLastCaptcha() != null && this.getLastCaptcha().getLetterComperators() != null) {
                            LetterComperator[] lcs = this.getLastCaptcha().getLetterComperators();
                            this.getLastCaptcha().setCorrectcaptchaCode(capTxt.trim());

                            if (lcs.length == capTxt.trim().length()) {
                                for (int i = 0; i < capTxt.length(); i++) {

                                    if (lcs[i] != null && lcs[i].getDecodedValue() != null && capTxt.substring(i, i + 1).equalsIgnoreCase(lcs[i].getDecodedValue()) && lcs[i].getValityPercent() < 30.0) { //
                                        logger.severe("OK letter: " + i + ": JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER: " + capTxt.substring(i, i + 1));
                                    } else {

                                        logger.severe("Unknown letter: // " + i + ":  JAC:" + lcs[i].getDecodedValue() + "(" + lcs[i].getValityPercent() + ") USER:  " + capTxt.substring(i, i + 1)); // Pixelstring.
                                        // getB()
                                        // ist
                                        // immer
                                        // der
                                        // neue
                                        // letter
                                        final String character = capTxt.substring(i, i + 1);
                                        logger.info("SEND");
                                        Letter letter = lcs[i].getA();
                                        String captchaHash = UTILITIES.getLocalHash(captchaFile);
                                        letter.setSourcehash(captchaHash);
                                        letter.setOwner(this.getLastCaptcha().owner);
                                        letter.setDecodedValue(character);
                                        this.getLastCaptcha().owner.letterDB.add(letter);
                                        this.getLastCaptcha().owner.saveMTHFile();
                                    }
                                }

                            } else {
                                logger.info("LCS not length comp");
                            }
                        }
                    }
                    break;
                }
            }

            links = reqinfo.getLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    public void handle(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        handle0(downloadLink);
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));
        return;
    }

    public void handle0(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        // switch (step.getStep()) {

        String link = (String) downloadLink.getProperty("link");
        String[] mirrors = (String[]) downloadLink.getProperty("mirrors");
        downloadLink.getLinkStatus().setStatusText("decrypt");
        downloadLink.requestGuiUpdate();
        ArrayList<DownloadLink> dls = getDLinks(link);

        if (dls.size() < 1) {
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.setErrorMessage(JDLocale.L("plugin.serienjunkies.pageerror", "SJ liefert keine Downloadlinks"));
            logger.warning("SJ returned no Downloadlinks");
            return;
        }

        // if (aborted) return null;
        FilePackage fp = downloadLink.getFilePackage();
        int index = fp.indexOf(downloadLink);
        fp.remove(downloadLink);
        Vector<Integer> down = new Vector<Integer>();
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        for (int i = dls.size() - 1; i >= 0; i--) {
            DistributeData distributeData = new DistributeData(dls.get(i).getDownloadURL());
            Vector<DownloadLink> links = distributeData.findLinks();
            Iterator<DownloadLink> it2 = links.iterator();
            boolean online = false;
            while (it2.hasNext()) {
                DownloadLink downloadLink3 = (DownloadLink) it2.next();
                if (downloadLink3.isAvailable()) {
                    fp.add(index, downloadLink3);

                    online = true;
                } else {
                    down.add(i);
                }

            }
            if (online) ret.addAll(links);
        }
        if (mirrors != null) {
            for (int i = 0; i < mirrors.length; i++) {
                if (down.size() > 0) {
                    try {
                        dls = getDLinks(mirrors[i]);
                        // if (aborted) { return null; }

                        Iterator<Integer> iter = down.iterator();
                        while (iter.hasNext()) {
                            Integer integer = (Integer) iter.next();
                            DistributeData distributeData = new DistributeData(dls.get(down.get(integer)).getDownloadURL());
                            Vector<DownloadLink> links = distributeData.findLinks();
                            Iterator<DownloadLink> it2 = links.iterator();
                            boolean online = false;
                            while (it2.hasNext()) {
                                DownloadLink downloadLink3 = (DownloadLink) it2.next();
                                if (downloadLink3.isAvailable()) {
                                    fp.add(index, downloadLink3);
                                    online = true;
                                    iter.remove();
                                }

                            }
                            if (online) ret.addAll(links);
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                } else
                    break;
            }
        }
        if (down.size() > 0) {
            fp.add(downloadLink);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.setErrorMessage(JDLocale.L("plugin.serienjunkies.archiveincomplete","Archiv nicht komplett"));
            return;
        }

    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        return true;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
        // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public String getAGBLink() {

        return null;
    }
}
