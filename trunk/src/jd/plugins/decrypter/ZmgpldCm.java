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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zomgupload.com" }, urls = { "(?!http://[\\w\\.]*?zomgupload\\.com/.+[/0-9a-zA-Z/-]+.html)http://[\\w\\.]*?zomgupload\\.com/.+[/0-9a-zA-Z/-]" }, flags = { 0 })
public class ZmgpldCm extends PluginForDecrypt {

    public ZmgpldCm(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());
        String[] links = br.getRegex("<TR><TD><a href=\"(.*?)\" target=\"_blank\">.*?</a></TD>").getColumn(0);
        progress.setRange(links.length);
        for (String data : links) {
            decryptedLinks.add(createDownloadlink(data));
            progress.increase(1);
        }
        return decryptedLinks;
    }

    // @Override

}
