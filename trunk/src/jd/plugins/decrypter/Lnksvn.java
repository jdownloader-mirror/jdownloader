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

package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.captcha.specials.Linksave;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.JavaScript;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.xml.sax.SAXException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "Linksave.in" }, urls = { "http://[\\w\\.]*?linksave\\.in/(view.php\\?id=)?[\\w]+" }, flags = { 0 })
public class Lnksvn extends PluginForDecrypt {

    public Lnksvn(PluginWrapper wrapper) {
        super(wrapper);
        br.setRequestIntervalLimit(this.getHost(), 1000);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://linksave.in/", "Linksave_Language", "german");
        br.getPage(param.getCryptedUrl());
        if (br.containsHTML("Ordner nicht gefunden")) return decryptedLinks;
        Form form = br.getFormbyProperty("name", "form");
        for (int retry = 0; retry < 5; retry++) {
            if (form == null) break;
            if (form.containsHTML("besucherpasswort")) {
                String pw = getUserInput("Besucherpasswort", param);
                form.put("besucherpasswort", pw);
            }
            String url = "captcha/cap.php?hsh=" + form.getRegex("\\/captcha\\/cap\\.php\\?hsh=([^\"]+)").getMatch(0);
            File captchaFile = this.getLocalCaptchaFile();
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(url));
            Linksave.prepareCaptcha(captchaFile);
            String captchaCode = getCaptchaCode(captchaFile, param);
            form.put("code", captchaCode);
            br.submitForm(form);
            if (br.containsHTML("Falscher Code") || br.containsHTML("Captcha-code ist falsch") || br.containsHTML("Besucherpasswort ist falsch")) {
                br.getPage(param.getCryptedUrl());
                form = br.getFormbyProperty("name", "form");
            } else {
                break;
            }
        }

        String[] container = br.getRegex("\\.href\\=unescape\\(\\'(.*?)\\'\\)\\;").getColumn(0);
        if (container != null && container.length > 0) {
            for (String c : container) {
                /*
                 * Context cx = Context.enter(); Scriptable scope =
                 * cx.initStandardObjects(); String fun =
                 * "function f(){ \nreturn '" + c + "';} f()"; Object result =
                 * cx.evaluateString(scope, fun, "<cmd>", 1, null);
                 * 
                 * c=result.toString();
                 */
                Browser clone = br.cloneBrowser();
                String test = Encoding.htmlDecode(c);
                File file = null;
                if (test.endsWith(".cnl")) {
                    URLConnectionAdapter con = clone.openGetConnection("http://linksave.in/" + test.replace("dlc://linksave.in/", ""));
                    if (con.getResponseCode() == 200) {
                        file = JDUtilities.getResourceFile("tmp/linksave/" + test.replace(".cnl", ".dlc").replace("dlc://", "http://").replace("http://linksave.in", ""));
                        clone.downloadConnection(file, con);
                    } else {
                        con.disconnect();
                    }
                } else if (test.endsWith(".rsdf")) {
                    URLConnectionAdapter con = clone.openGetConnection(test);
                    if (con.getResponseCode() == 200) {
                        file = JDUtilities.getResourceFile("tmp/linksave/" + test.replace("http://linksave.in", ""));
                        clone.downloadConnection(file, con);
                    } else {
                        con.disconnect();
                    }
                }
                if (file != null && file.exists() && file.length() > 100) {
                    try {
                        decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                }
            }
        }
        if (decryptedLinks.size() == 0) {
            String[] links = br.getRegex("<a href=\"(http://linksave[^\"]*)\" onclick=\"javascript:document.getElementById").getColumn(0);
            final class LsDirektLinkTH extends Thread {
                Browser browser;
                String result;

                public LsDirektLinkTH(Browser browser) {
                    this.browser = browser;
                }

                public void run() {
                    try {
                        result = getDirektLink(browser);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    synchronized (this) {
                        this.notify();
                    }
                }
            }
            LsDirektLinkTH[] dlinks = new LsDirektLinkTH[links.length];
            for (int i = 0; i < dlinks.length; i++) {
                Browser clone = br.cloneBrowser();
                clone.getPage(links[i]);
                dlinks[i] = new LsDirektLinkTH(clone);
                dlinks[i].start();
            }
            for (LsDirektLinkTH lsDirektLinkTH : dlinks) {
                while (lsDirektLinkTH.isAlive()) {
                    synchronized (lsDirektLinkTH) {
                        try {
                            lsDirektLinkTH.wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            // e.printStackTrace();
                        }
                    }
                }
                if (lsDirektLinkTH.result != null) decryptedLinks.add(createDownloadlink(lsDirektLinkTH.result));
            }
            if (decryptedLinks.size() == 0) throw new DecrypterException("Out of date. Try Click'n'Load");

            // throw new DecrypterException("Out of date. Try Click'n'Load");
        }
        return decryptedLinks;
    }

    private static String getDirektLink(Browser br) throws IOException {
        String link = br.getRegex("<frame scrolling=\"auto\" noresize src=\"([^\"]*)\">").getMatch(0);
        if (link != null) br.getPage(link);
        String link2 = Encoding.htmlDecode(br.getRegex("iframe src=\"([^\"]*)\"").getMatch(0));
        if (link2 != null)
            return link2.trim();
        else {
            br.getRequest().setHtmlCode(br.toString().replaceFirst("<script type=\"text/javascript\" src=\"[^\"]*.js\">", ""));

            JavaScript js = new JavaScript(br);
            try {
                js.runPage();
                br.getRequest().setHtmlCode(js.getDocment().getContent().replaceFirst("<script type=\"text/javascript\" src=\"[^\"]*.js\">", ""));
                link2 = br.getRegex("location.replace\\('([^\']*)").getMatch(0);
                if (link2 == null) link2 = br.getRegex("src=\"([^\"]*)\"").getMatch(0);
                if (link2 == null) link2 = br.getRegex("URL=([^\"]*)\"").getMatch(0);
                br.getPage(link2);
                link2 = Encoding.htmlDecode(br.getRegex("iframe src=\"([^\"]*)\"").getMatch(0));
                if (link2 == null) {
                    js = new JavaScript(br);
                    js.runPage();
                    br.getRequest().setHtmlCode(js.getDocment().getContent());
                    link2 = br.getForm(0).getAction();
                }
                if (link2 != null) return link2.trim();

                // br.getRequest().setHtmlCode(js.getVar("o"));

                // String var=js.callFunction(eval);
                // System.out.println(br);
                // System.out.println(br.getForm(0).getAction());
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return null;
    }

    // @Override
    protected boolean isClickNLoadEnabled() {
        return true;
    }

    // @Override

}
