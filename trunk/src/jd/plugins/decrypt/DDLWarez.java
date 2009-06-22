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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTMLEntities;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;

public class DDLWarez extends PluginForDecrypt {
    static class DDLWarez_Linkgrabber extends Thread {
        public final static int THREADFAIL = 1;
        public final static int THREADPASS = 0;
        int _status;
        private String downloadlink;
        private Form form;
        private boolean gotjob;
        protected ProgressController progress;
        private int Worker_ID;
        private Browser br;

        public DDLWarez_Linkgrabber(ProgressController progress, int id, Browser br) {
            downloadlink = null;
            gotjob = false;
            _status = THREADFAIL;
            Worker_ID = id;
            this.br = br;

            this.progress = progress;
        }

        public String getlink() {
            return downloadlink;
        }

        @Override
        public void run() {
            if (gotjob == true) {
                logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " started!");
                String base = br.getBaseURL();
                String action = form.getAction(base);

                if (action.contains("get_file") || action.contains("goref.php")) {
                    Browser clone = br.cloneBrowser();
                    clone.setDebug(true);

                    for (int retry = 1; retry <= 10; retry++) {
                        try {
                            clone.submitForm(form);

                            downloadlink = clone.getRegex(Pattern.compile("<frame\\s.*?src=\"(.*?)\r?\n?\" (?=(NAME=\"second\"))", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            break;
                        } catch (Exception e) {
                            logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " PostRequest-Error, try again!");
                            synchronized (DDLWarez.Worker_Delay) {
                                DDLWarez.Worker_Delay = 1000;
                            }
                        }
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                        }
                    }
                } else {
                    logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " finished! (NO DOWNLOAD FORM!)");
                    _status = THREADFAIL;
                    progress.increase(1);
                    return;
                }
            }
            logger.finest("DDLWarez_Linkgrabber: id=" + new Integer(Worker_ID) + " finished!");
            _status = THREADPASS;
            progress.increase(1);
        }

        public void setjob(Form form) {
            this.form = form;

            gotjob = true;
        }

        public int status() {
            return _status;
        }
    }

    public static Integer Worker_Delay = 250;
    private static String captchaText;

    public DDLWarez(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        br.setReadTimeout(5 * 60 * 1000);
        br.setConnectTimeout(5 * 60 * 1000);
        for (int retry = 1; retry <= 10; retry++) {
            try {
                br.clearCookies(this.getHost());
                br.getPage(parameter);

                String pass = br.getRegex(Pattern.compile("<td>Passwort:</td>\\s*<td style=\"padding-left:10px;\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
                ArrayList<String> passwords = new ArrayList<String>();
                passwords.add("ddl-warez");
                if (pass != null && !pass.equals("kein Passwort")) {
                    passwords.add(pass);
                }

                Form form = br.getForm(1);

                if (form != null && !form.getAction().contains("get_file.php") && !form.getAction().contains("goref.php")) {

                    if (form.containsHTML("identifier")) {
                        String id = form.getVarsMap().get("identifier");
                        int what = 0;
                        if (br.containsHTML("der Ausgangsreihenfolge")) what = 2;
                        if (br.containsHTML("NICHT die Farbe blau")) what = 1;
                        String code = getCode(id, br, what);
                        form.put("result", code);
                    } else {
                        for (InputField ipf : form.getInputFields()) {

                            if (ipf.getType().equalsIgnoreCase("text") && ipf.getValue() == null) {
                                if (captchaText != null)
                                    ipf.setValue(captchaText);
                                else {
                                    String text = form.getHtmlCode().replaceAll("<.*?>", "").trim();
                                    String res = UserIO.getInstance().requestInputDialog(0, JDLocale.L("plugins.decrypt.ddlwarez.humanverification", "DDL-Warez Human Verification"), new Regex(Encoding.deepHtmlDecode(HTMLEntities.unhtmlAngleBrackets(text)), "[A-Za-z0-9_äÄöÖüÜß\\s\\,\\.]+[^A-Za-z0-9_äÄöÖüÜß\\,\\.]+\\s(\\S+)").getMatch(0), null, UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), null, null);
                                    if (res == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                                    captchaText = res;
                                    ipf.setValue(captchaText);

                                }
                            }
                        }
                    }

                    br.submitForm(form);
                    form = br.getForm(1);
                    if (form.getAction().contains("crypt.php")) {
                        form.put("submit", Encoding.urlEncode("zu den Links..."));
                        this.sleep(10 * 1000l, param);
                        br.submitForm(form);
                        form = br.getForm(1);
                    }
                    if (form != null && !form.getAction().contains("get_file.php") && !form.getAction().contains("goref.php")) {
                        captchaText = null;
                        continue;
                    }
                }

                Form[] forms = br.getForms();
                progress.setRange(forms.length);
                DDLWarez_Linkgrabber DDLWarez_Linkgrabbers[] = new DDLWarez_Linkgrabber[forms.length];
                for (int i = 0; i < forms.length; ++i) {
                    synchronized (Worker_Delay) {
                        Thread.sleep(Worker_Delay);
                    }
                    DDLWarez_Linkgrabbers[i] = new DDLWarez_Linkgrabber(progress, i, br.cloneBrowser());
                    DDLWarez_Linkgrabbers[i].setjob(forms[i]);
                    DDLWarez_Linkgrabbers[i].start();
                }
                for (int i = 0; i < forms.length; ++i) {
                    try {
                        DDLWarez_Linkgrabbers[i].join();
                        if (DDLWarez_Linkgrabbers[i].status() == DDLWarez_Linkgrabber.THREADPASS) {
                            DownloadLink link = createDownloadlink(DDLWarez_Linkgrabbers[i].getlink());
                            link.setSourcePluginPasswordList(passwords);
                            decryptedLinks.add(link);
                        }
                    } catch (InterruptedException e) {
                        logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                    }
                }
                return decryptedLinks;
            } catch (DecrypterException e) {
                throw e;
            } catch (Exception e) {
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                logger.finest("DDLWarez: PostRequest-Error, try again!");
            }
        }
        return null;
    }

    // public static File convert(File file) {
    // try {
    // JAntiCaptcha jas = new JAntiCaptcha("DUMMY", "DUMMY");
    // jas.getJas().setColorType("RGB");
    // GifDecoder d = new GifDecoder();
    // d.read(file.getAbsolutePath());
    // int n = d.getFrameCount();
    //
    // int width = (int) d.getFrameSize().getWidth();
    // int height = (int) d.getFrameSize().getHeight();
    //
    // Captcha tmp;
    // Captcha[] frames = new Captcha[n];
    // for (int i = 0; i < n; i++) {
    // BufferedImage frame = d.getFrame(i);
    // tmp = new Captcha(width, height);
    // tmp.setOwner(jas);
    // PixelGrabber pg = new PixelGrabber(frame, 0, 0, width, height, false);
    // try {
    // pg.grabPixels();
    // } catch (Exception e) {
    // logger.log(java.util.logging.Level.SEVERE,"Exception occurred",e);
    // }
    // ColorModel cm = pg.getColorModel();
    // tmp.setColorModel(cm);
    // frames[i] = tmp;
    // if (!(cm instanceof IndexColorModel)) {
    //
    // tmp.setPixel((int[]) pg.getPixels());
    // } else {
    // tmp.setPixel((byte[]) pg.getPixels());
    // }
    // // BasicWindow.showImage(tmp.getFullImage(), "img " + i);
    // }
    // if (n < 4) return null;
    // frames[n - 4].crop(10, 20, 10, 10);
    // frames[n - 3].crop(10, 20, 10, 10);
    // frames[n - 2].crop(10, 20, 10, 10);
    // frames[n - 1].crop(10, 20, 10, 10);
    //
    // Captcha merged = new Captcha(frames[n - 4].getWidth() * 4 + 3, frames[n -
    // 4].getHeight());
    // merged.setOwner(jas);
    // merged.addAt(0, 0, frames[n - 4]);
    // merged.addAt(frames[n - 4].getWidth() + 1, 0, frames[n - 3]);
    // merged.addAt(frames[n - 4].getWidth() * 2 + 2, 0, frames[n - 2]);
    // merged.addAt(frames[n - 4].getWidth() * 3 + 3, 0, frames[n - 1]);
    // // merged.toBlackAndWhite(0.01);
    // // BasicWindow.showImage(merged.getImage());
    // ImageIO.write((BufferedImage) merged.getImage(), "png", new
    // File(file.getAbsoluteFile() + ".png"));
    // return new File(file.getAbsoluteFile() + ".png");
    // } catch (Exception e) {
    // // TODO Auto-generated catch block
    // logger.log(java.util.logging.Level.SEVERE,"Exception occurred",e);
    // }
    // return null;
    //
    // }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public static String getCode(String id, Browser br, int what) throws IOException {
        Browser brc = br.cloneBrowser();
        brc.getPage("http://www.ddl-warez.org/getcaptcha.php?id=" + id);
        String[][] ss = brc.getRegex("color=\"(.*?)\".*?kerning=\".*?\">(.*?)</font>").getMatches();
        if (ss.length == 0) return null;
        HashMap<String, String> codes = new HashMap<String, String>();
        for (String[] c : ss) {
            codes.put(Encoding.htmlDecode(c[1]).trim().replaceAll("\\W+", ""), c[0]);
        }
        String code = "";
        for (String c : codes.keySet()) {
            if (c.length() > code.length()) {
                code = c;
            }
        }
        if (what == 2) return code; /* falls nur ein ergebnis */
        String color = codes.get(code);
        for (String c : codes.keySet()) {
            switch (what) {
            case 0:/* gleich wie alle */
                if (codes.get(c).equalsIgnoreCase(color) && c.length() < code.length()) return c;
                break;
            case 1:/* nicht gleiche wie alle */
                if (!codes.get(c).equalsIgnoreCase(color) && c.length() < code.length()) return c;
                break;
            default:
                break;
            }
        }
        if (what == 1) {
            String retcode = code;
            for (String c : codes.keySet()) {
                if (c.equalsIgnoreCase(code)) continue;
                for (int i = 0; i < c.length(); i++) {
                    retcode.replaceAll("" + c.charAt(i), "");
                }
            }
            return retcode;
        }
        return null; /* keines */
    }

    private String unpack(String packedScript, String[] tokens) {
        int seed = 95;
        HashMap<String, String> dictionary = new HashMap<String, String>();
        for (int i = 0; i < tokens.length; i++) {
            dictionary.put(createKey(i, seed), tokens[i]);
        }

        Pattern p = Pattern.compile("([\\xa1-\\xff]+)");
        Matcher m = p.matcher(packedScript);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String replacement = dictionary.get(m.group());
            if (replacement != null) {
                m.appendReplacement(sb, replacement);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String createKey(int number, int seed) {
        int offset = 256 - seed;
        if (number < seed)
            return String.valueOf((char) (number + offset));
        else
            return createKey(number / seed, seed) + String.valueOf((char) (number % seed + offset));
    }
}
