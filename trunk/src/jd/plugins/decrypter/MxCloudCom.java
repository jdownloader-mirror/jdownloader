//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixcloud.com" }, urls = { "http://(www\\.)?mixcloud\\.com/.*?/[A-Za-z0-9_\\-]+/" }, flags = { 0 })
public class MxCloudCom extends PluginForDecrypt {

    private static String MAINPAGE = "http://www.mixcloud.com";

    public MxCloudCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private byte[] AESdecrypt(final byte[] plain, final byte[] key, final byte[] iv) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
        return cipher.doFinal(plain);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        String theName = br.getRegex("class=\"cloudcast\\-name\" itemprop=\"name\">(.*?)</h1>").getMatch(0);
        if (theName == null) {
            theName = br.getRegex("data-resourcelinktext=\"(.*?)\"").getMatch(0);
        }
        if (theName == null) { return null; }

        final String playResource = parameter.replace(MAINPAGE, "");
        String playerUrl = br.getRegex("playerUrl:\'(.*?)\'").getMatch(0);
        playerUrl = playerUrl == null ? MAINPAGE + "/player/" : MAINPAGE + playerUrl;
        br.setCookie(MAINPAGE, "play-resource", Encoding.urlEncode(playResource));
        br.getPage(playerUrl);
        String playInfoUrl = br.getRegex("playinfo: ?\'(.*?)\'").getMatch(0);
        final String playModuleSwfUrl = br.getRegex("playerModuleSwfUrl: ?\'(.*?)\'").getMatch(0);
        if (playInfoUrl == null || playModuleSwfUrl == null) { return null; }

        playInfoUrl = playInfoUrl + "?key=" + playResource + "&module=" + playModuleSwfUrl + "&page=" + playerUrl;

        /**
         * FIXME: Hi jiaz, nachfolgender Aufruf liefert eigentl. als Antwort
         * einen Base64 kodierten String. Der Browser enkodiert das automatisch.
         * Dolle Sache :-) Scheinbar ist das Ergebnis kaputt bzw. nicht
         * vollständig. Wenn ich den request nun über die URL-Klasse mache und
         * den Base64 kodierten Ergebnisstring dann mittels jd.crypt.Base64 in
         * ein ByteArray packe ist alles i.O.
         */
        // byte[] plain = br.getPage(playInfoUrl).getBytes();

        String encryptedContent = "", result = null;
        try {
            final URL url = new URL(playInfoUrl);
            final InputStream page = url.openStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(page));
            try {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    encryptedContent = line;
                }
            } finally {
                try {
                    reader.close();
                } catch (final Throwable e) {
                }
            }
        } catch (final Throwable e) {
            return null;
        }

        final byte[] enc = jd.crypt.Base64.decode(encryptedContent);
        final byte[] key = JDHexUtils.getByteArray(Encoding.Base64Decode("NjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MTYxNjE2MQ=="));
        final byte[] iv = new byte[16];
        System.arraycopy(enc, 0, iv, 0, 16);

        try {
            result = new String(AESdecrypt(enc, key, iv)).substring(16);
        } catch (final InvalidKeyException e) {
            if (e.getMessage().contains("Illegal key size")) {
                getPolicyFiles();
            }
        } catch (final Throwable e) {
        }

        final String sets[] = new Regex(result, "\\[(.*?)\\]").getColumn(0);
        if (sets == null || sets.length == 0) { return null; }
        URLConnectionAdapter con = null;
        for (final String set : sets) {
            final String[] links = new Regex(set, "\"(.*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                continue;
            }
            for (final String dl : links) {
                if (!dl.endsWith("mp3")) {
                    break;
                }
                final DownloadLink dlink = createDownloadlink("directhttp://" + dl);
                dlink.setFinalFileName(theName + new Regex(dl, "(\\..{3}$)").getMatch(0));
                /* Nicht alle Links im Array sets[] sind verfügbar. */
                try {
                    con = br.openGetConnection(dl);
                    if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                        continue;
                    }
                    decryptedLinks.add(dlink);
                    break;
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(theName);
        fp.addLinks(decryptedLinks);
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private void getPolicyFiles() throws Exception {
        int ret = -100;
        UserIO.setCountdownTime(120);
        ret = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_LARGE, "Java Cryptography Extension (JCE) Error: 32 Byte keylength is not supported!", "At the moment your Java version only supports a maximum keylength of 16 Bytes but the veoh plugin needs support for 32 byte keys.\r\nFor such a case Java offers so called \"Policy Files\" which increase the keylength to 32 bytes. You have to copy them to your Java-Home-Directory to do this!\r\nExample path: \"jre6\\lib\\security\\\". The path is different for older Java versions so you might have to adapt it.\r\n\r\nBy clicking on CONFIRM a browser instance will open which leads to the downloadpage of the file.\r\n\r\nThanks for your understanding.", null, "CONFIRM", "Cancel");
        if (ret != -100) {
            if (UserIO.isOK(ret)) {
                LocalBrowser.openDefaultURL(new URL("http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html"));
                JDUtilities.openExplorer(new File(System.getProperty("java.home") + "/lib/security"));
            } else {
                return;
            }
        }
    }

}