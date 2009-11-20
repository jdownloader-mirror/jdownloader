//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "books.google.com" }, urls = { "http://books.google.[a-z]+/books\\?id=[0-9a-zA-Z-]+.*" }, flags = { 0 })
public class GglBks extends PluginForDecrypt {

    public GglBks(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String param = parameter.toString();
        String url = param;
        if (url.contains("&")) url = url.split("&")[0];
        String url2 = url.concat("&printsec=frontcover&jscmd=click3");
        br.getPage(url2);
        if (br.containsHTML("http://sorry.google.com/sorry/\\?continue=.*")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000); }
        String[] links = br.getRegex("\\{\\\"pid\\\":\\\"(.*?)\\\"").getColumn(0);
        if (links.length == 0) return null;
        progress.setRange(links.length);
        FilePackage fp = FilePackage.getInstance();
        for (String dl : links) {
            DownloadLink link = createDownloadlink(url.replace("books.google", "googlebooksdecrypter") + "&pg=" + dl);
            link.setName(dl + ".jpg");
            decryptedLinks.add(link);
            progress.increase(1);
        }
        br.getPage(param);
        fp.setName(br.getRegex("<div id=\"titlebar\"><h1 class=title dir=ltr>(.*?)</h1>").getMatch(0));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
