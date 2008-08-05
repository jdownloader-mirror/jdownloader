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

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class UCMS extends PluginForDecrypt {
    static private final String host = "Underground CMS";
    private Pattern PAT_CAPTCHA = Pattern.compile("<IMG SRC=\".*?/gfx/secure/", Pattern.CASE_INSENSITIVE);

    private Pattern PAT_NO_CAPTCHA = Pattern.compile("(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\".*?Download.*?\".*?Click=\"if)", Pattern.CASE_INSENSITIVE);

    final static private Pattern patternSupported = UCMS.create_patternSupported();

    public UCMS() {
        super();
    }

    static private Pattern create_patternSupported() {
        String Complete_Pattern = "";
        String[] List = { "saugking.net", "oxygen-warez.com", "filefox.in", "alphawarez.us", "pirate-loads.com", "fettrap.com", "omega-music.com", "hardcoremetal.biz", "flashload.org", "twin-warez.com", "oneload.org", "steelwarez.com", "fullstreams.info", "lionwarez.com", "1dl.in", "chrome-database.com", "oneload.org", "youwarez.biz", "saugking.net", "leetpornz.com", "freefiles4u.com", "dark-load.net", "crimeland.de", "get-warez.in", "meinsound.com", "projekt-tempel-news.de.vu", "datensau.org", "musik.am", "spreaded.net", "relfreaks.com", "babevidz.com", "serien24.com", "porn-freaks.net", "xxx-4-free.net", "porn-traffic.net", "chili-warez.net", "game-freaks.net", "isos.at", "your-load.com", "mov-world.net", "xtreme-warez.net", "sceneload.to", "oxygen-warez.com", "epicspeedload.in", "serienfreaks.to", "serienfreaks.in", "warez-load.com", "ddl-scene.com", "mp3king.cinipac-hosting.biz",
                "xwebb.extra.hu/1dl", "wii-reloaded.ath.cx/sites/epic", "wankingking.com", "projekt-tempel-news.org", "porn-ox.in", "music-dome.cc", "sound-load.com", "lister.hoerspiele.to", "jim2008.extra.hu", "ex-yu.extra.hu", "firefiles.in", "gez-load.net", "wrzunlimited.1gb.in", "streamload.in", "toxic.to", "mp3z.to", "sexload.to", "sound-load.com", "sfulc.exofire.net/cms" };
        for (String Pattern : List) {
            if (Complete_Pattern.length() > 0) {
                Complete_Pattern += "|";
            }
            Complete_Pattern += "(http://[\\w\\.]*?" + Pattern.replaceAll("\\.", "\\\\.") + "/(\\?id=.+|category/.+/.+\\.html|download/.+/.+\\.html))";
        }
        logger.finest("UCMS: " + List.length + " Pattern added!");
        return Pattern.compile(Complete_Pattern, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);

            RequestInfo reqinfo = HTTP.getRequest(url);
            File captchaFile = null;
            String capTxt = "";
            String host = url.getHost();

            if (!host.startsWith("http")) {
                host = "http://" + host;
            }

            String pass = new Regex(reqinfo.getHtmlCode(), Pattern.compile("CopyToClipboard\\(this\\)\\; return\\(false\\)\\;\">(.*?)<\\/a>", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            if (pass != null) {
                if (pass.equals("n/a") || pass.equals("-") || pass.equals("-kein Passwort-")) {
                    pass = null;
                }
            }
            String forms[][] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<FORM ACTION=\"([^\"]*)\" ENCTYPE=\"multipart/form-data\" METHOD=\"POST\" NAME=\"(mirror|download)[^\"]*\"(.*?)</FORM>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
            for (String[] element : forms) {
                for (int retry = 0; retry < 5; retry++) {
                    Matcher matcher = PAT_CAPTCHA.matcher(element[2]);

                    if (matcher.find()) {
                        if (captchaFile != null && capTxt != null) {
                            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
                        }

                        logger.finest("Captcha Protected");
                        String captchaAdress = host + new Regex(element[2], Pattern.compile("<IMG SRC=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                        captchaFile = getLocalCaptchaFile(this);
                        JDUtilities.download(captchaFile, captchaAdress);
                        capTxt = JDUtilities.getCaptcha(this, "hardcoremetal.biz", captchaFile, false);
                        String posthelp = HTMLParser.getFormInputHidden(element[2]);
                        if (element[0].startsWith("http")) {
                            reqinfo = HTTP.postRequest(new URL(element[0]), posthelp + "&code=" + capTxt);
                        } else {
                            reqinfo = HTTP.postRequest(new URL(host + element[0]), posthelp + "&code=" + capTxt);
                        }
                    } else {
                        if (captchaFile != null && capTxt != null) {
                            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);
                        }

                        Matcher matcher_no = PAT_NO_CAPTCHA.matcher(element[2]);
                        if (matcher_no.find()) {
                            logger.finest("Not Captcha protected");
                            String posthelp = HTMLParser.getFormInputHidden(element[2]);
                            if (element[0].startsWith("http")) {
                                reqinfo = HTTP.postRequest(new URL(element[0]), posthelp);
                            } else {
                                reqinfo = HTTP.postRequest(new URL(host + element[0]), posthelp);
                            }
                            break;
                        }
                    }
                    if (reqinfo.containsHTML("Der Sichheitscode wurde falsch eingeben")) {
                        logger.warning("Captcha Detection failed");
                        reqinfo = HTTP.getRequest(url);
                    } else {
                        break;
                    }
                    if (reqinfo.getConnection().getURL().toString().equals(host + element[0])) {
                        break;
                    }
                }
                String links[][] = null;
                if (reqinfo.containsHTML("unescape")) {
                    String temp = JDUtilities.htmlDecode(JDUtilities.htmlDecode(JDUtilities.htmlDecode(new Regex(reqinfo.getHtmlCode(), Pattern.compile("unescape\\(unescape\\(\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch())));
                    links = new Regex(temp, Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
                } else {
                    links = new Regex(reqinfo.getHtmlCode(), Pattern.compile("ACTION=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatches();
                }
                for (String[] element2 : links) {
                    DownloadLink link = createDownloadlink(JDUtilities.htmlDecode(element2[0]));
                    link.addSourcePluginPassword(pass);
                    decryptedLinks.add(link);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
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
}