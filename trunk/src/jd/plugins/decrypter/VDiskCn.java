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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vdisk.cn" }, urls = { "http://(www\\.)?vdisk\\.cn/(?!down/|user|file|api)[a-z0-9]+" }, flags = { 0 })
public class VDiskCn extends PluginForDecrypt {

    public VDiskCn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> allPages = new ArrayList<String>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">找不到您需要的页面\\!<|可能您访问的内容已经删除，或您无权访问本页面。<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String[] pages = br.getRegex("<a href=\\'\\?tag=ALLFILES\\&p=(\\d+)\\'").getColumn(0);
        if (pages == null || pages.length == 0)
            allPages.add("1");
        else {
            for (final String currentPage : pages)
                if (!allPages.contains(currentPage)) allPages.add(currentPage);
        }
        int lastPage = Integer.parseInt(allPages.get(allPages.size() - 1));
        for (int i = 1; i <= lastPage; i++) {
            if (i != 1) br.getPage(parameter + "?p=" + i);
            final String[][] links = br.getRegex("\\'(/down/index/[A-Z0-9]+)\\'.*?blank'>(.*?)</a.*?sizeinfo'>(.*?)\\&").getMatches();
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            DownloadLink link;
            for (String singleLink[] : links) {
                link = createDownloadlink("http://vdisk.cn" + singleLink[0]);
                link.setName(singleLink[1]);
                link.setDownloadSize(SizeFormatter.getSize(singleLink[2].trim()));
                link.setAvailable(true);
                decryptedLinks.add(link);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(parameter, "([a-z0-9]+)$").getMatch(0));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}