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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Inflater;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shahid.mbc.net" }, urls = { "http://(www\\.)?shahid\\.mbc\\.net/media/video/\\d+(/\\w+)?" }, flags = { 0 })
public class ShaHidMbcNetDecrypter extends PluginForDecrypt {

    public static enum Quality {
        ALLOW_HD("3f3f", "HD"),
        ALLOW_HIGH("3f20", "HIGH"),
        ALLOW_MEDIUM("7d3f", "MEDIUM"),
        ALLOW_LOW("7420", "LOW"),
        ALLOW_LOWEST("6000", "LOWEST");

        private String hex;
        private String name;

        Quality(String hexvalue, String fileprefix) {
            hex = hexvalue;
            name = fileprefix;
        }

        public String getHexValue() {
            return hex;
        }

        public String getName() {
            return name;
        }
    }

    private String KEY     = "UzJpJCtQWCxYZiEsNXxeOA==";

    private String HASHKEY = "amEtPj5HQmNMa2E5P2hiVA==";

    public ShaHidMbcNetDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        setBrowserExclusive();
        br.getPage(parameter);

        String[] setCookie = br.getRegex("setCookie\\(\'(\\w+)\',\\s?\'([0-9\\.]+)\',\\s?\\d+\\)").getRow(0);
        if (!(setCookie == null || setCookie.length != 2)) {
            br.setCookie(br.getHost(), setCookie[0], setCookie[1]);
            br.getPage(parameter);
        }

        String channelId = br.getRegex("channelId=(.*?)\\&").getMatch(0);
        String playerForm = br.getRegex("playerForm=(.*?)\\&").getMatch(0);
        String mediaId = br.getRegex("mediaId=(.*?)\\&").getMatch(0);
        if (channelId == null || playerForm == null || mediaId == null) { return null; }

        int page = 0;
        String quality;
        boolean next = true;
        byte[] buffer = new byte[1024];
        Map<String, String> qStr = new HashMap<String, String>();
        Map<String, String> links = new HashMap<String, String>();

        // processing plugin configuration
        SubConfiguration cfg = SubConfiguration.getConfig("shahid.mbc.net");
        Map<String, Object> shProperties = new LinkedHashMap<String, Object>(cfg.getProperties());

        for (Entry<String, Object> property : shProperties.entrySet()) {
            if (property.getKey().matches("(ALLOW_HD|ALLOW_HIGH|ALLOW_MEDIUM|ALLOW_LOW|ALLOW_LOWEST)") && (Boolean) property.getValue()) {
                qStr.put(Quality.valueOf(property.getKey()).getHexValue(), Quality.valueOf(property.getKey()).toString());
            }
        }

        // try and catch, da Anzahl der Seitenaufrufe(Variable page) unbekannt
        while (next) {
            // Get -> RC4 encrypted http stream to byte array
            String req = "http://cache2.delvenetworks.com/ps/c/v1/" + getHmacSHA256("RC4\n" + Encoding.Base64Decode(KEY)) + "/" + getHmacSHA256("Channel") + "/" + getHmacSHA256(channelId) + "/" + page++;

            byte[] enc = null;
            try {
                br.getPage(req);
                if (br.getHttpConnection().getResponseCode() == 403) {
                    next = false;
                } else {
                    /* will throw Exception in stable <=9581 */
                    if (br.getRequest().isContentDecoded()) {
                        enc = br.getRequest().getResponseBytes();
                    } else {
                        throw new Exception("use fallback");
                    }
                }
            } catch (Throwable e) {
                /* fallback */
                ByteArrayOutputStream result = null;
                try {
                    URL url = new URL(req);
                    InputStream input = url.openStream();
                    result = new ByteArrayOutputStream();
                    try {
                        int amount = 0;
                        while (amount != -1) {
                            result.write(buffer, 0, amount);
                            amount = input.read(buffer);
                        }
                    } finally {
                        try {
                            input.close();
                        } catch (Throwable e2) {
                        }
                        try {
                            result.close();
                        } catch (Throwable e3) {
                        }
                        enc = result.toByteArray();
                    }
                } catch (Throwable e4) {
                    next = false;
                }
            }

            if (!next) {
                break;
            }

            try {
                // Decrypt -> http stream
                /* TODO: change me after 0.9xx public --> jd.crypt.RC4 */
                LnkCrptWs.KeyCaptchaShowDialogTwo arkfour = new LnkCrptWs.KeyCaptchaShowDialogTwo();
                byte[] compressedPlainData = arkfour.D(Encoding.Base64Decode(KEY).getBytes(), enc);

                // Decompress -> decrypted http stream
                Inflater decompressor = new Inflater();
                decompressor.setInput(compressedPlainData);
                ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedPlainData.length);

                try {
                    while (true) {
                        int count = decompressor.inflate(buffer);
                        if (count == 0 && decompressor.finished()) {
                            break;
                        } else if (count == 0) {
                            logger.severe("bad zip data, size:" + compressedPlainData.length);
                            return null;
                        } else {
                            bos.write(buffer, 0, count);
                        }
                    }
                } catch (Throwable t) {
                } finally {
                    decompressor.end();
                }

                // Parsing -> video urls
                String decompressedPlainData = new String(bos.toByteArray(), "UTF-8");
                String[] finalContent = decompressedPlainData.split(new String(new byte[] { 0x05, 0x40, 0x39 }));
                if (finalContent == null || finalContent.length == 0) { return null; }
                if (finalContent.length == 1) finalContent = decompressedPlainData.split(new String(new byte[] { 0x05, 0x40, 0x38 }));

                for (String fC : finalContent) {
                    if (fC.length() < 40) {
                        continue;
                    }
                    // Einzellink oder Alles
                    if (fC.contains("rtmp")) {
                        if (fC.contains(mediaId + "-") || (Boolean) shProperties.get("COMPLETE_SEASON")) {
                            quality = JDHexUtils.getHexString(fC.substring(16, 18));
                            if (quality != null && qStr.containsKey(quality)) {
                                links.put(new Regex(fC, "(rtmp[\\w:\\/\\.\\-]+)").getMatch(0), Quality.valueOf(qStr.get(quality)).getName());
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                continue;
            }
        }

        FilePackage fp = FilePackage.getInstance();
        String fpName;

        for (Entry<String, String> link : links.entrySet()) {
            if (link.getKey() == null) {
                continue;
            }
            DownloadLink dl = createDownloadlink(link.getKey());
            if (dl.getName() == null) {
                continue;
            }
            fpName = new Regex(dl.getName(), "(.*?)(_s\\d+|_?\\-?ep_?\\-?\\d+|_\\d+_vod)").getMatch(0);
            fpName = fpName == null ? dl.getName() : fpName;
            fp.setName(fpName);
            fp.add(dl);
            if (dl.getName().contains("vod")) {
                dl.setFinalFileName(dl.getName().replace("vod", "(" + link.getValue() + ")"));
            } else {
                dl.setFinalFileName("(" + link.getValue() + ")__" + dl.getName());
            }
            try {
                distribute(dl);
            } catch (Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(dl);
        }

        if (decryptedLinks == null || decryptedLinks.size() == 0) { return null; }
        return decryptedLinks;
    }

    private String getHmacSHA256(String s) {
        try {
            SecretKey key = new SecretKeySpec(Encoding.Base64Decode(HASHKEY).getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            mac.reset();
            mac.update(s.getBytes());
            return safeUrl(jd.crypt.Base64.encodeBytes(mac.doFinal()));
        } catch (Throwable e) {
            return null;
        }
    }

    private String safeUrl(String s) {
        if (s == null) return null;
        return s.replace("=", "").replace("+", "-").replace("/", "_");
    }

}
