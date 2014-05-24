//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wdr.de" }, urls = { "http://([a-z0-9]+\\.)?wdr\\.de/([a-z0-9\\-_/]+/sendungen/[a-z0-9\\-_/]+\\.html|tv/rockpalast/extra/videos/\\d+/\\d+/\\w+\\.jsp)" }, flags = { 32 })
public class WdrDeDecrypt extends PluginForDecrypt {

    private static final String Q_LOW           = "Q_LOW";
    private static final String Q_MEDIUM        = "Q_MEDIUM";
    private static final String Q_BEST          = "Q_BEST";
    private static final String Q_SUBTITLES     = "Q_SUBTITLES";
    private boolean             BEST            = false;

    private static final String TYPE_INVALID    = "http://([a-z0-9]+\\.)?wdr\\.de/mediathek/video/sendungen/index\\.html";
    private static final String TYPE_ROCKPALAST = "http://(www\\.)?wdr\\.de/tv/rockpalast/extra/videos/\\d+/\\d+/\\w+\\.jsp";

    public WdrDeDecrypt(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        /* Remove unneeded url part */
        final String player_part = new Regex(parameter, "(\\-videoplayer(_size\\-[A-Z])?\\.html)").getMatch(0);
        if (player_part != null) {
            parameter = parameter.replace(player_part, ".html");
        }
        boolean offline = false;
        br.setFollowRedirects(true);

        if (parameter.matches(TYPE_ROCKPALAST)) {
            final DownloadLink dl = createDownloadlink("http://wdrdecrypted.de/?format=mp4&quality=1x1&hash=" + JDHash.getMD5(parameter));
            dl.setProperty("mainlink", parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            offline = true;
        }
        // Add offline link so user can see it
        if (offline || parameter.matches(TYPE_INVALID) || parameter.contains("filterseite-") || parameter.contains("uebersicht") || br.getURL().contains("/fehler.xml") || br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink dl = createDownloadlink("http://wdrdecrypted.de/?format=mp4&quality=1x1&hash=" + JDHash.getMD5(parameter));
            dl.setAvailable(false);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        String sendung = br.getRegex("<strong>([^<>\"]*?)<span class=\"hidden\">:</span></strong>[\t\n\r ]+Die Sendungen im Überblick[\t\n\r ]+<span>\\[mehr\\]</span>").getMatch(0);
        if (sendung == null) {
            sendung = br.getRegex(">Sendungen</a></li>[\t\n\r ]+<li>([^<>\"]*?)<span class=\"hover\">").getMatch(0);
        }
        if (sendung == null) {
            sendung = br.getRegex("<li class=\"active\" >[\t\n\r ]+<strong>([^<>\"]*?)</strong>").getMatch(0);
        }
        if (sendung == null) {
            sendung = br.getRegex("<div id=\"initialPagePart\">[\t\n\r ]+<h1>[\t\n\r ]+<span>([^<>\"]*?)<span class=\"hidden\">:</span>").getMatch(0);
        }
        String episode_name = br.getRegex("</li><li>[^<>\"/]+: ([^<>\"]*?)<span class=\"hover\"").getMatch(0);
        if (episode_name == null) {
            episode_name = br.getRegex("class=\"hover\">:([^<>\"]*?)</span>").getMatch(0);
        }
        if (sendung == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String plain_name;
        sendung = encodeUnicode(Encoding.htmlDecode(sendung).trim());
        if (episode_name != null) {
            episode_name = Encoding.htmlDecode(episode_name).trim();
            episode_name = encodeUnicode(episode_name);
            plain_name = sendung + " - " + episode_name;
        } else {
            plain_name = sendung;
        }

        /* Check for audio stream */
        if (br.containsHTML("<div class=\"audioContainer\">")) {
            final String finallink = br.getRegex("dslSrc: \\'dslSrc=(http://[^<>\"]*?)\\&amp;mediaDuration=\\d+\\'").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink audio = createDownloadlink("http://wdrdecrypted.de/?format=mp3&quality=1x1&hash=" + JDHash.getMD5(parameter));
            audio.setProperty("mainlink", parameter);
            audio.setProperty("direct_link", finallink);
            audio.setProperty("plain_filename", plain_name + ".mp3");
            decryptedLinks.add(audio);
        } else {
            ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
            HashMap<String, DownloadLink> best_map = new HashMap<String, DownloadLink>();
            final SubConfiguration cfg = SubConfiguration.getConfig("wdr.de");
            BEST = cfg.getBooleanProperty(Q_BEST, false);
            final boolean grab_subtitle = cfg.getBooleanProperty(Q_SUBTITLES, false);

            String player_link = br.getRegex("class=\"videoLink\" >[\t\n\r ]+<a href=\"(/[^<>\"]*?)\"").getMatch(0);
            if (player_link == null) {
                player_link = br.getRegex("\"(/[^<>\"]*?)\" rel=\"nofollow\" class=\"videoButton play\"").getMatch(0);
            }
            if (player_link == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("http://www1.wdr.de" + player_link);
            String subtitle_url = br.getRegex("vtCaptionsURL=(http%3A%2F%2[^<>\"]*?\\.xml)\\&vtCaptions").getMatch(0);
            if (subtitle_url != null) {
                subtitle_url = Encoding.htmlDecode(subtitle_url);
            }
            /* Avoid HDS */
            final String[] qualities = br.getRegex("(CMS2010/mdb/ondemand/weltweit/fsk\\d+/[^<>\"]*?)\"").getColumn(0);
            if (qualities == null || qualities.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int counter = 0;
            for (final String final_url : qualities) {
                String resolution;
                String quality_name;
                if (counter == 0) {
                    resolution = "960x544";
                    quality_name = "Q_MEDIUM";
                } else {
                    resolution = "512x288";
                    quality_name = "Q_LOW";
                }
                final String final_video_name = plain_name + "_" + resolution + ".mp4";
                final DownloadLink dl_video = createDownloadlink("http://wdrdecrypted.de/?format=mp4&quality=" + resolution + "&hash=" + JDHash.getMD5(parameter));
                dl_video.setProperty("mainlink", parameter);
                dl_video.setProperty("direct_link", "http://http-ras.wdr.de/" + final_url);
                dl_video.setProperty("plain_filename", final_video_name);
                dl_video.setProperty("plain_resolution", resolution);
                dl_video.setFinalFileName(final_video_name);
                best_map.put(quality_name, dl_video);
                newRet.add(dl_video);
                counter++;
            }

            if (newRet.size() > 1 && cfg.getBooleanProperty(Q_BEST, false)) {
                /* only keep best quality */
                DownloadLink keep = best_map.get("Q_MEDIUM");
                if (keep == null) {
                    keep = best_map.get("Q_LOW");
                }
                newRet.clear();
                newRet.add(keep);

                /* Add subtitle link for every quality so players will automatically find it */
                if (grab_subtitle && subtitle_url != null) {
                    final String subtitle_filename = plain_name + "_" + keep.getStringProperty("plain_resolution", null) + ".xml";
                    final String resolution = keep.getStringProperty("plain_resolution", null);
                    final DownloadLink dl_subtitle = createDownloadlink("http://wdrdecrypted.de/?format=xml&quality=" + resolution + "&hash=" + JDHash.getMD5(parameter));
                    dl_subtitle.setProperty("mainlink", parameter);
                    dl_subtitle.setProperty("direct_link", subtitle_url);
                    dl_subtitle.setProperty("plain_filename", subtitle_filename);
                    dl_subtitle.setProperty("streamingType", "subtitle");
                    dl_subtitle.setAvailable(true);
                    dl_subtitle.setFinalFileName(subtitle_filename);
                    newRet.add(dl_subtitle);
                }
            } else {
                ArrayList<String> selected_qualities = new ArrayList<String>();
                boolean grab_low = cfg.getBooleanProperty(Q_LOW, false);
                boolean grab_medium = cfg.getBooleanProperty(Q_MEDIUM, false);
                /* User deselected all --> Add all */
                if (!grab_low && !grab_medium) {
                    grab_low = true;
                    grab_medium = true;
                }

                if (cfg.getBooleanProperty(Q_LOW, false)) {
                    selected_qualities.add(Q_LOW);
                }
                if (cfg.getBooleanProperty(Q_MEDIUM, false)) {
                    selected_qualities.add(Q_MEDIUM);
                }
                for (final String selected_quality : selected_qualities) {
                    final DownloadLink keep = best_map.get(selected_quality);
                    /* Add subtitle link for every quality so players will automatically find it */
                    if (grab_subtitle && subtitle_url != null) {
                        final String subtitle_filename = plain_name + "_" + keep.getStringProperty("plain_resolution", null) + ".xml";
                        final String resolution = keep.getStringProperty("plain_resolution", null);
                        final DownloadLink dl_subtitle = createDownloadlink("http://wdrdecrypted.de/?format=xml&quality=" + resolution + "&hash=" + JDHash.getMD5(parameter));
                        dl_subtitle.setProperty("mainlink", parameter);
                        dl_subtitle.setProperty("direct_link", subtitle_url);
                        dl_subtitle.setProperty("plain_filename", subtitle_filename);
                        dl_subtitle.setProperty("streamingType", "subtitle");
                        dl_subtitle.setAvailable(true);
                        dl_subtitle.setFinalFileName(subtitle_filename);
                        newRet.add(dl_subtitle);
                    }
                    newRet.add(keep);
                }
            }
            decryptedLinks = newRet;
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(plain_name);
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}