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

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class JDLoader extends PluginForDecrypt {

    public JDLoader(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String format = null;
        String url = null;
        String jdlist = new Regex(parameter.getCryptedUrl(), Pattern.compile("jdlist://(.+)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (jdlist != null) {
            /* Links einlesen */
            jdlist = Encoding.Base64Decode(jdlist);
            Vector<DownloadLink> links = new DistributeData(jdlist).findLinks();
            decryptedLinks.addAll(links);
        } else {
            /* Container einlesen */
            if (new Regex(parameter.getCryptedUrl(), Pattern.compile("dlc://", Pattern.CASE_INSENSITIVE)).matches()) {
                format = ".dlc";
                url = new Regex(parameter.getCryptedUrl(), Pattern.compile("dlc://(.+)", Pattern.CASE_INSENSITIVE)).getMatch(0);
            } else if (new Regex(parameter.getCryptedUrl(), Pattern.compile("ccf://", Pattern.CASE_INSENSITIVE)).matches()) {
                format = ".ccf";
                url = new Regex(parameter.getCryptedUrl(), Pattern.compile("ccf://(.+)", Pattern.CASE_INSENSITIVE)).getMatch(0);
            } else if (new Regex(parameter.getCryptedUrl(), Pattern.compile("rsdf://", Pattern.CASE_INSENSITIVE)).matches()) {
                format = ".rsdf";
                url = new Regex(parameter.getCryptedUrl(), Pattern.compile("rsdf://(.+)", Pattern.CASE_INSENSITIVE)).getMatch(0);
            }
            if (format == null) throw new DecrypterException("Unknown Container prefix");
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + format);
            Browser.download(container, br.cloneBrowser().openGetConnection("http://" + url));
            Vector<DownloadLink> links = JDUtilities.getController().getContainerLinks(container);
            for (DownloadLink dLink : links) {
                decryptedLinks.add(dLink);
            }
            container.delete();
        }
        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
