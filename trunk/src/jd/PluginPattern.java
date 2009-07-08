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

package jd;

import java.util.logging.Logger;

public class PluginPattern {
    static private Logger logger = jd.controlling.JDLogger.getLogger();

    static public String decrypterPattern_UCMS_Plugin() {
        StringBuilder completePattern = new StringBuilder();
        String[] list = { "ddl-kingz.in", "oxygen-warez.com", "filefox.in", "alphawarez.us", "pirate-loads.com", "fettrap.com", "omega-music.com", "hardcoremetal.biz", "flashload.org", "twin-warez.com", "oneload.org", "steelwarez.com", "fullstreams.info", "lionwarez.com", "1dl.in", "chrome-database.com", "oneload.org", "youwarez.biz", "saugking.net", "leetpornz.com", "freefiles4u.com", "dark-load.net", "crimeland.de", "get-warez.in", "meinsound.com", "projekt-tempel-news.de.vu", "datensau.org", "musik.am", "spreaded.net", "relfreaks.com", "babevidz.com", "serien24.com", "porn-freaks.net", "xxx-4-free.net", "porn-traffic.net", "chili-warez.net", "game-freaks.net", "isos.at", "your-load.com", "mov-world.net", "xtreme-warez.net", "sceneload.to", "epicspeedload.in", "serienfreaks.to", "serienfreaks.in", "warez-load.com", "ddl-scene.com", "mp3king.cinipac-hosting.biz", "sauggirls.com",
                "pornfox.in", "xflat24.com", "alben.ws", "worldofxxx.org", "gamegalaxy.ws", "ddl.byte.to", "interload.biz", "xwebb.extra.hu/1dl", "jokermovie.org", "xtreme-warez.biz", "your-load.com", "top-hitz.com", "wii-reloaded.ath.cx/sites/epic", "wankingking.com", "projekt-tempel-news.org", "porn-ox.in", "music-dome.cc", "sound-load.com", "hoerspiele.to", "jim2008.extra.hu", "ex-yu.extra.hu", "firefiles.in", "gez-load.net", "wrzunlimited.1gb.in", "streamload.in", "toxic.to", "mp3z.to", "sexload.to", "sound-load.com", "sfulc.exofire.net/cms", "fickdiehure.com", "dream-team.bz/cms", "omega-warez.com", "ddl-scene.cc", "xxxstreams.org", "scene-warez.com", "dokuh.tv", "titanload.to", "ddlshock.com", "xtreme-warez.us", "crunkwarez.com", "serienking.in", "stream.szenepic.us", "gate-warez.com", "gateload.info", "hot-porn-ddl.com" };
        for (String pattern : list) {
            if (completePattern.length() > 0) {
                completePattern.append("|");
            }
            completePattern.append("(http://[\\w\\.]*?" + pattern.replaceAll("\\.", "\\\\.") + "/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html|.*?/.*?\\.html))");
        }
        logger.finest("UCMS: " + list.length + " Pattern added!");
        return completePattern.toString();
    }

    static public String decrypterPattern_Wordpress_Plugin() {
        StringBuilder completePattern = new StringBuilder();
        completePattern.append("http://[\\w\\.]*?(");
        completePattern.append("(game-blog\\.us/game-.+\\.html)");
        completePattern.append("|(cinetopia\\.ws/.*\\.html)");
        completePattern.append("|(ladekabel\\.us/\\?p=\\d+.*)");
        completePattern.append("|(load-it\\.biz/[^/]*/?)");
        completePattern.append("|(guru-world\\.net/wordpress/\\d+.*)");
        completePattern.append("|(klee\\.tv/blog/\\d+.*)");
        completePattern.append("|(blogload\\.org/\\d+.*)");
        completePattern.append("|(pressefreiheit\\.ws/[\\d]+/.+\\.html)");
        completePattern.append("|(zeitungsjunge\\.info/.*?/.*?/.*?/)");
        completePattern.append("|(serien-blog\\.com/download/[\\d]+/.+\\.html)");
        String[] listType1 = { "hd-area.org", "movie-blog.org", "doku.cc", "sound-blog.org" };
        for (String pattern : listType1) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/\\d{4}/\\d{2}/\\d{2}/.+)");
        }
        String[] listType2 = { "hoerbuch.in", "xxx-blog.org", "serien-blog.com" };
        for (String pattern : listType2) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/blog\\.php\\?id=[\\d]+)");
        }
        String[] listType3 = { "sky-porn.info/blog", "best-movies.us/enter", "ladekabel.us/enter" };
        for (String pattern : listType3) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/\\?p=[\\d]+)");
        }
        completePattern.append(")");
        logger.finest("Wordpress: " + (12 + listType1.length + listType2.length + listType3.length) + " Pattern added!");
        return completePattern.toString();
    }

    static public final String decrypterPattern_Redirecter_Plugin() {
        StringBuilder completePattern = new StringBuilder();
        String[] list = { "http://[\\w\\.]*?fyad\\.org/[a-zA-Z0-9]+", "http://[\\w\\.]*?is\\.gd/[a-zA-Z0-9]+", "http://[\\w\\.]*?redirect\\.wayaround\\.org/[a-zA-Z0-9]+/(.*)", "http://[\\w\\.]*?rurl\\.org/[a-zA-Z0-9]+", "http://[\\w\\.]*?tinyurl\\.com/[a-zA-Z0-9\\-]+", "http://[\\w\\.]*?smarturl\\.eu/\\?[a-zA-Z0-9]+", "http://[\\w\\.]*?linkmize\\.com\\/[a-zA-Z0-9]+", "http://go2\\.u6e\\.de/[a-zA-Z0-9]+", "http://[\\w\\.]*?shrinkify\\.com/[a-zA-Z0-9]+", "http://[\\w\\.]*?s7y\\.com/[a-zA-Z0-9]+", "http://[\\w\\.]*?rln\\.me/[0-9a-zA-Z]+", "http://[\\w\\.]*?sp2\\.ro/[0-9a-zA-Z]+", "http://[\\w\\.]*?s7y.us/[a-zA-Z0-9]+" };
        for (String pattern : list) {
            if (completePattern.length() > 0) {
                completePattern.append("|");
            }
            completePattern.append(pattern);
        }
        logger.finest("Redirecter: " + list.length + " Pattern added!");
        return completePattern.toString();
    }

    static public final String DECRYPTER_ANIMEANET_SERIES = "http://[\\w\\.]*?animea\\.net/download/[\\d]+/(.*?)\\.html";
    static public final String DECRYPTER_ANIMEANET_EPISODE = "http://[\\w\\.]*?animea\\.net/download/[\\d]+-[\\d]+/(.*?)\\.html";
    static public final String DECRYPTER_ANIMEANET_PLUGIN = DECRYPTER_ANIMEANET_SERIES + "|" + DECRYPTER_ANIMEANET_EPISODE;

    static public final String DECRYPTER_DDLMSC_MAIN = "http://[\\w\\.]*?ddl-music\\.org/index\\.php\\?site=view_download&cat=.+&id=\\d+";
    static public final String DECRYPTER_DDLMSC_CRYPT = "http://[\\w\\.]*?ddl-music\\.org/captcha/ddlm_cr\\d\\.php\\?\\d+\\?\\d+";
    static public final String DECRYPTER_DDLMSC_PLUGIN = DECRYPTER_DDLMSC_MAIN + "|" + DECRYPTER_DDLMSC_CRYPT;

    static public final String DECRYPTER_3DLAM_1 = "http://[\\w\\.]*?3dl\\.am/link/[a-zA-Z0-9]+";
    static public final String DECRYPTER_3DLAM_2 = "http://[\\w\\.]*?3dl\\.am/download/start/[0-9]+/";
    static public final String DECRYPTER_3DLAM_3 = "http://[\\w\\.]*?3dl\\.am/download/[0-9]+/.+\\.html";
    static public final String DECRYPTER_3DLAM_4 = "http://[\\w\\.]*?3dl\\.am/\\?action=entrydetail&entry_id=[0-9]+";
    static public final String DECRYPTER_3DLAM_PLUGIN = DECRYPTER_3DLAM_1 + "|" + DECRYPTER_3DLAM_2 + "|" + DECRYPTER_3DLAM_3 + "|" + DECRYPTER_3DLAM_4;

    static public final String URLCASH = "(sealed\\.in|urlcash\\.net|urlcash\\.org|clb1\\.com|urlgalleries\\.com|celebclk\\.com|smilinglinks\\.com|peekatmygirlfriend\\.com|looble\\.net)";

}
