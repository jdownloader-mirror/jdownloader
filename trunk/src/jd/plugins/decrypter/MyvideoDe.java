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

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.swing.components.ConvertDialog.ConversionMode;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myvideo.de" }, urls = { "http://[\\w\\.]*?myvideo\\.de/watch/[0-9]+/"}, flags = { 0 })


public class MyvideoDe extends PluginForDecrypt {

    public MyvideoDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<ConversionMode> possibleconverts = new ArrayList<ConversionMode>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.setFollowRedirects(true);
        br.getPage(parameter);

        // String server =
        // br.getRegex(Pattern.compile("p\\.addVariable\\('SERVER','(.*?)'\\)",
        // Pattern.CASE_INSENSITIVE)).getMatch(0);
        String videoid = br.getRegex(Pattern.compile("p\\.addVariable\\('_videoid','(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String serverpath = br.getRegex(Pattern.compile("<link rel='image_src'.*?href='(.*?)thumbs/.*?'.*?/><link", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (videoid == null || serverpath == null) return null;
        String link = serverpath + videoid + ".flv";
        String name = Encoding.htmlDecode(br.getRegex(Pattern.compile("<h1 class='globalHd'>(.*?)</h1>", Pattern.CASE_INSENSITIVE)).getMatch(0).trim());
        possibleconverts.add(ConversionMode.AUDIOMP3);
        possibleconverts.add(ConversionMode.VIDEOFLV);
        possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);
        ConversionMode ConvertTo = Plugin.showDisplayDialog(possibleconverts, name, param);
        if (ConvertTo == null) return decryptedLinks;
        DownloadLink thislink = createDownloadlink(link);
        thislink.setBrowserUrl(parameter);
        thislink.setFinalFileName(name + ".tmp");
        thislink.setSourcePluginComment("Convert to " + ConvertTo.GetText());
        thislink.setProperty("convertto", ConvertTo.name());
        decryptedLinks.add(thislink);

        return decryptedLinks;
    }

    // @Override
    
}
