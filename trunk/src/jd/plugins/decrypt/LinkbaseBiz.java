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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LinkbaseBiz extends PluginForDecrypt {

    public static Integer Worker_Delay = 250;

    static class LinkbaseBiz_Linkgrabber extends Thread {
        public final static int THREADFAIL = 1;
        public final static int THREADPASS = 0;
        int _status;
        private String downloadlink;
        private boolean gotjob;

        private int Worker_ID;
        private Browser br;
        private String link;

        public LinkbaseBiz_Linkgrabber(int id, Browser br) {
            this.downloadlink = null;
            this.link = null;
            this.gotjob = false;
            this._status = THREADFAIL;
            this.Worker_ID = id;
            this.br = br;
            this.br.setFollowRedirects(true);
        }

        public String getlink() {
            return this.downloadlink;
        }

        // @Override
        public void run() {
            if (this.gotjob == true) {
                logger.finest("LinkbaseBiz_Linkgrabber: id=" + new Integer(this.Worker_ID) + " started!");

                for (int retry = 1; retry <= 10; retry++) {
                    try {
                        String page = decodePage(this.br.getPage("http://linkbase.biz/?go=" + this.link));
                        String link = new Regex(page, "<iframe src='(.*?)'").getMatch(0);
                        if (link == null) {
                            link = br.getRegex("<iframe src='(.*?)'").getMatch(0);
                        }
                        this.downloadlink = link;
                        break;
                    } catch (Exception e) {
                        logger.finest("LinkbaseBiz_Linkgrabber: id=" + new Integer(this.Worker_ID) + " GetRequest-Error, try again!");
                        synchronized (LinkbaseBiz.Worker_Delay) {
                            LinkbaseBiz.Worker_Delay = 1000;
                        }
                    }
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                    }
                }
            }
            logger.finest("LinkbaseBiz_Linkgrabber: id=" + new Integer(this.Worker_ID) + " finished!");
            this._status = THREADPASS;
        }

        public void setjob(String link) {
            this.link = link;
            this.gotjob = true;
        }

        public int status() {
            return this._status;
        }
    }

    public LinkbaseBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        for (int retry = 1; retry <= 10; retry++) {
            try {
                br.getPage(parameter);
                if (br.getRegex("Du hast.*?Du musst noch").matches()) {
                    param.getProgressController().setRange(30);
                    param.getProgressController().setStatusText("Wrong captcha, please wait 30 secs!");
                    for (int i = 0; i < 30; i++) {
                        Thread.sleep(1000);
                        param.getProgressController().increase(1);
                    }
                    param.getProgressController().setStatusText(null);
                    param.getProgressController().setStatus(0);
                    continue;
                }
                String captchaurl = br.getRegex("<img src='(.*?captcha.*?)'").getMatch(0);
                if (captchaurl != null) {
                    String captchaCode = getCaptchaCode("http://linkbase.biz/" + captchaurl, param);
                    Form form = br.getForm(0);
                    form.put("captcha", captchaCode);
                    br.submitForm(form);
                    if (br.containsHTML("Das war leider Falsch")) {
                        continue;
                    }
                }
                String links[] = br.getRegex("window.open\\('\\?go=(.*?)','.*?'\\)").getColumn(0);
                progress.setRange(links.length);
                LinkbaseBiz_Linkgrabber LinkbaseBiz_Linkgrabbers[] = new LinkbaseBiz_Linkgrabber[links.length];
                for (int i = 0; i < links.length; ++i) {
                    synchronized (Worker_Delay) {
                        Thread.sleep(Worker_Delay);
                    }
                    LinkbaseBiz_Linkgrabbers[i] = new LinkbaseBiz_Linkgrabber(i, br.cloneBrowser());
                    LinkbaseBiz_Linkgrabbers[i].setjob(links[i]);
                    LinkbaseBiz_Linkgrabbers[i].start();
                }
                for (int i = 0; i < links.length; ++i) {
                    try {
                        LinkbaseBiz_Linkgrabbers[i].join();
                        if (LinkbaseBiz_Linkgrabbers[i].status() == LinkbaseBiz_Linkgrabber.THREADPASS) {
                            decryptedLinks.add(createDownloadlink(LinkbaseBiz_Linkgrabbers[i].getlink()));
                        }
                        progress.increase(1);
                    } catch (InterruptedException e) {
                        logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                    }
                }
                progress.finalize();
                return decryptedLinks;
            } catch (DecrypterException e2) {
                logger.severe("LinkBaseBiz: Captcha Error");
                throw e2;
            } catch (Exception e) {
                logger.finest("LinkbaseBiz: GetRequest-Error, try again!");
            }
        }
        return null;
    }

    private static String decodePage(String page) {
        if (page == null) return null;
        StringBuffer sb = new StringBuffer();
        String pattern = "(document\\.write\\(\".*?\"\\);)";
        Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(page);
        while (r.find()) {
            if (r.group(1).length() > 0) {
                String content = r.group(1).replaceAll("^document\\.write\\(\"", "").replaceAll("\"\\);$", "");
                r.appendReplacement(sb, content);
            }
        }
        r.appendTail(sb);
        return sb.toString();
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}