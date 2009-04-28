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

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.URLConnectionAdapter;
import jd.parser.JavaScript;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.xml.sax.SAXException;

public class ProtectorTO extends PluginForDecrypt {
    private static Integer lock = new Integer(0);

    public ProtectorTO(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("unchecked")
    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        // Falls es wen interessiert, wie man an die Container kommt,
        // falls welche existieren.
        // Fiel mir gerade auf und wollte es mal hinzufügen.
        /*
         * // Datum erzeugen SimpleDateFormat formatter = new SimpleDateFormat
         * ("yyyyMMdd"); Date currentTime = new Date(); String currentDate =
         * formatter.format(currentTime);
         * 
         * // ID des Ordners md5ieren ;) String folderID3 = JDHash.getMD5(new
         * Regex(parameter, PATTERN_FOLDER_ID).getMatch(0).substring(1));
         * 
         * String link = "dlc://protector.to/container/" + currentDate + "/" +
         * folderID + "_1.dlc"; decryptedLinks.add(createDownloadlink(link));
         */
        synchronized (lock) {
            if (param.getStringProperty("referer", null) != null) {
                br.getHeaders().put("Referer", param.getStringProperty("referer", null));
            }
            if (param.getProperty("protector_cookies", null) != null) {
                br.getCookies().putAll((HashMap<String, HashMap<String, Cookie>>) param.getProperty("protector_cookies", null));
            }
            br.getPage(parameter + "?jd=1");
            if (br.getRedirectLocation() != null) {
                DownloadLink dl;
                decryptedLinks.add(dl = createDownloadlink(br.getRedirectLocation()));
                dl.setProperty("protector_cookies", br.getCookies());
                return decryptedLinks;
            }
            if (br.containsHTML("Source was protected")) {
                JavaScript js = br.getJavaScript();
                try {
                    br.getPage(new Regex(js.getVar(br.getRegex("document.write\\((.*?)\\)").getMatch(0)), "<iframe src=[\"']([^\"']*)[\"']").getMatch(0));
                    String link = null;
                    if (br.toString().contains("rapidshare")) {
                        link = br.getForm(1).getAction();
                    } else {
                        js = br.getJavaScript();
                        br.getRequest().setHtmlCode(js.getVar(br.getRegex("document.write\\((.*?)\\)").getMatch(0)));
                        js = br.getJavaScript();
                        link = js.getVar(br.getRegex("location.href='\"\\+(.*?)\\+\"").getMatch(0));
                    }
                    decryptedLinks.add(createDownloadlink(link));
                    return decryptedLinks;
                } catch (SAXException e) {
                    logger.log(java.util.logging.Level.SEVERE, "Exception occured", e);
                }
            }
            String img = null;
            while ((img = br.getRegex("<img id=[\"']cryptogram[\"'] src=[\"']([^\"']*)[\"']").getMatch(0)) != null) {
                File file = this.getLocalCaptchaFile(this);
                URLConnectionAdapter con = br.cloneBrowser().openGetConnection(img);
                con.connect();
                Browser.download(file, br.cloneBrowser().openGetConnection(con.getRequest().getLocation()));
                Form form = br.getForm(0);
                String captchaCode = getCaptchaCode(file, this, param);
                if (captchaCode == null) return null;
                form.put("code", captchaCode);
                br.submitForm(form);
                img = null;
            }
            String containerlink = br.getRegex("<a href=\"(http://protector.to/container/[^\"]*)").getMatch(0);
            if (containerlink != null) {
                try {
                    URLConnectionAdapter con = br.cloneBrowser().openGetConnection(containerlink);
                    File container = JDUtilities.getResourceFile("container/" + getFileNameFormHeader(con));
                    Browser.download(container, con);
                    Vector<DownloadLink> link = JDUtilities.getController().getContainerLinks(container);
                    for (DownloadLink downloadLink : link) {
                        decryptedLinks.add(downloadLink);
                    }
                    container.delete();
                    if (decryptedLinks.size() > 0) return decryptedLinks;
                } catch (Exception e) {
                    decryptedLinks = new ArrayList<DownloadLink>();
                }
            }
            String[] links = br.getRegex("<a href=[\"']([^\"']*)[\"'] onmouseover=[\"']dl_hover").getColumn(0);
            ArrayList<String> li = new ArrayList<String>();
            for (String string : links) {
                if (!li.contains(string)) {
                    li.add(string);
                    decryptedLinks.add(createDownloadlink(string));
                }

            }
        }
        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
