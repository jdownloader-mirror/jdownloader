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
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class UCMS extends PluginForDecrypt {

    private Pattern PAT_CAPTCHA = Pattern.compile("<IMG SRC=\".*?/gfx/secure/", Pattern.CASE_INSENSITIVE);

    private Pattern PAT_NO_CAPTCHA = Pattern.compile("(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\".*?Download.*?\".*?Click)", Pattern.CASE_INSENSITIVE);

    public UCMS(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        try {
            br.getPage(parameter);
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            File captchaFile = null;
            String capTxt = "";
            String host = br.getHost();

            if (!host.startsWith("http")) {
                host = "http://" + host;
            }

            String pass = br.getRegex(Pattern.compile("CopyToClipboard\\(this\\)\\; return\\(false\\)\\;\">(.*?)<\\/a>", Pattern.CASE_INSENSITIVE)).getMatch(0);
            if (pass == null) pass = br.getRegex("<B>Passwort:</B> <input value=\"(.*?)\".*?<").getMatch(0);
            if (pass == null) pass = br.getRegex("<dt class=\"\">Passwort:</dt>.*?<dd class=\"\">(.*?)</dd>").getMatch(0);
            if (pass != null) {
                if (pass.equals("keins ben&ouml;tigt") || pass.equals("kein pw") || pass.equals("N/A") || pass.equals("n/a") || pass.equals("-") || pass.equals("-kein Passwort-") || pass.equals("-No Pass-") || pass.equals("ohne PW")) {
                    pass = null;
                }
            }

            String forms[][] = br.getRegex(Pattern.compile("<FORM ACTION=\"([^\"]*)\" ENCTYPE=\"multipart/form-data\" METHOD=\"POST\" NAME=\"(mirror|download)[^\"]*\"(.*?)</FORM>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
            if (forms.length != 0) {
                for (String[] element : forms) {
                    for (int retry = 0; retry < 5; retry++) {
                        Matcher matcher = PAT_CAPTCHA.matcher(element[2]);
                        if (matcher.find()) {
                            logger.finest("Captcha Protected");
                            String captchaAdress = host + new Regex(element[2], Pattern.compile("<IMG SRC=\"(/.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            captchaFile = getLocalCaptchaFile(this);
                            Browser.download(captchaFile, captchaAdress);
                            capTxt = this.getCaptchaCode("hardcoremetal.biz", captchaFile, param);

                            String posthelp = HTMLParser.getFormInputHidden(element[2]);
                            if (element[0].startsWith("http")) {
                                br.postPage(element[0], posthelp + "&code=" + capTxt);
                            } else {
                                br.postPage(host + element[0], posthelp + "&code=" + capTxt);
                            }
                        } else {
                            Matcher matcher_no = PAT_NO_CAPTCHA.matcher(element[2]);
                            if (matcher_no.find()) {
                                logger.finest("Not Captcha protected");
                                String posthelp = HTMLParser.getFormInputHidden(element[2]);
                                if (element[0].startsWith("http")) {
                                    br.postPage(element[0], posthelp);
                                } else {
                                    br.postPage(host + element[0], posthelp);
                                }
                                break;
                            }
                        }
                        if (br.containsHTML("Der Sichheitscode wurde falsch eingeben")) {
                            logger.warning("Captcha Detection failed");
                            br.getPage(parameter);
                        } else {
                            break;
                        }
                        if (br.getHttpConnection().getURL().toString().equals(host + element[0])) {
                            break;
                        }
                    }
                    /*
                     * Bei hardcoremetal.biz wird mittlerweile der Download als
                     * DLC-Container angeboten! Workaround für diese Seite
                     */
                    if (br.containsHTML("ACTION=\"/download\\.php\"")) {
                        Form forms2[] = br.getForms();
                        for (Form form : forms2) {
                            if (form.containsHTML("dlc")) {
                                File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
                                Browser.download(container, br.openFormConnection(form));
                                decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
                                break;
                            }
                        }
                    } else {
                        String links[] = null;
                        if (br.containsHTML("unescape\\(unescape\\(unescape")) {
                            String temp = br.getRegex(Pattern.compile("unescape\\(unescape\\(unescape\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            String temp2 = Encoding.htmlDecode(Encoding.htmlDecode(Encoding.htmlDecode(temp)));
                            links = new Regex(temp2, Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                        } else if (br.containsHTML("unescape\\(unescape")) {
                            String temp = br.getRegex(Pattern.compile("unescape\\(unescape\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            String temp2 = Encoding.htmlDecode(Encoding.htmlDecode(temp));
                            links = new Regex(temp2, Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                        } else {
                            links = br.getRegex(Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
                        }
                        for (String element2 : links) {
                            DownloadLink link = createDownloadlink(Encoding.htmlDecode(element2));
                            link.addSourcePluginPassword(pass);
                            decryptedLinks.add(link);
                        }
                    }
                }
            } else {
                /* workaround for java ucms */
                String[] forms2 = br.getRegex("document.writeln\\('(<form.*?'</form>)").getColumn(0);
                ArrayList<Form> forms3 = new ArrayList<Form>();
                for (String form : forms2) {
                    String temp = form.replaceAll("(document\\.writeln\\('|'\\);)", "");
                    Form tform = new Form(temp);
                    tform.setAction(param.getCryptedUrl());
                    tform.remove(null);
                    tform.remove(null);
                    forms3.add(tform);
                }
                boolean cont = false;
                Browser brc = null;
                for (Form tform : forms3) {
                    for (int retry = 0; retry < 5; retry++) {
                        brc = br.cloneBrowser();
                        cont = false;
                        if (tform.containsHTML("<img src=")) {
                            logger.finest("Captcha Protected");
                            String captchaAdress = host + tform.getRegex(Pattern.compile("<img src=\"(/captcha/.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                            captchaFile = getLocalCaptchaFile(this);
                            Browser.download(captchaFile, captchaAdress);
                            capTxt = this.getCaptchaCode("hardcoremetal.biz", captchaFile, UserIO.NO_JAC, param, null, null);
                            tform.put("code", capTxt);
                            brc.submitForm(tform);
                        } else {
                            logger.finest("Not Captcha protected");
                            brc.submitForm(tform);
                        }
                        if (brc.containsHTML("UCMS.RELEASE")) {
                            cont = true;
                            break;
                        }
                    }
                    if (cont) {
                        String[] links2 = brc.getRegex("href=\\\\\"(.*?)\\\\\"").getColumn(0);
                        for (String dl : links2) {
                            dl = dl.replaceAll("\\\\/", "/");
                            Browser br2 = br.cloneBrowser();
                            br2.getPage(dl);
                            String flink = br2.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
                            if (flink == null && br2.getRedirectLocation() != null) flink = br2.getRedirectLocation();
                            DownloadLink link = createDownloadlink(flink);
                            link.addSourcePluginPassword(pass);
                            decryptedLinks.add(link);
                        }
                    }
                }

            }
        } catch (PluginException e2) {
            throw e2;
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occured", e);
            return null;
        }
        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}