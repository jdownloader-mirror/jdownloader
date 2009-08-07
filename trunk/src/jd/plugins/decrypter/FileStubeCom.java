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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filestube.com" }, urls = { "http://[\\w\\.]*?filestube\\.com/.+"}, flags = { 0 })


public class FileStubeCom extends PluginForDecrypt {

    public FileStubeCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter.toString());
        String temp = br.getRegex(Pattern.compile("<pre  id=\"copy_paste_links\" style=\".*auto\">(.*?)</pre></div>", Pattern.DOTALL)).getMatch(0);
        String[] links = temp.split("<br />");
        progress.setRange(links.length);
        for (String data : links) {
            decryptedLinks.add(createDownloadlink(data));
            progress.increase(1);
        }
        return decryptedLinks;
    }

   
    

}
