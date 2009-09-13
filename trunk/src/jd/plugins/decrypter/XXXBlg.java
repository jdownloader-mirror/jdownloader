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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xxx-blog.org" }, urls = { "http://[\\w\\.]*?xxx-blog\\.org/(sto|com-|u|filefactory/|relink/)[\\w\\./]+" }, flags = { 0 })
public class XXXBlg extends PluginForDecrypt {

    public XXXBlg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        parameter = parameter.substring(parameter.lastIndexOf("http://"));
        br.getPage(parameter);
        DownloadLink dLink;
        if (br.getRedirectLocation() != null) {
            dLink = createDownloadlink(br.getRedirectLocation());
        } else {
            Form form = br.getForm(0);
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            dLink = createDownloadlink(form.getAction(null));
        }
        dLink.addSourcePluginPassword("xxx-blog.dl.am");
        dLink.addSourcePluginPassword("xxx-blog.org");
        decryptedLinks.add(dLink);

        return decryptedLinks;
    }

}
