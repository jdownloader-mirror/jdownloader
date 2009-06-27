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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

public class LinkBankeu extends PluginForDecrypt {
    private static final String CHECK_MIRRORS = "CHECK_MIRRORS";

    public LinkBankeu(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        String[][] links = br.getRegex("onclick='posli\\(\"([\\d]+)\",\"([\\d]+)\"\\);'").getMatches();
        String[] mirrors = br.getRegex("onclick='mirror\\(\"(.*?)\"\\);'").getColumn(0);
        for (String[] element : links) {
            br.getPage("http://www.linkbank.eu/posli.php?match=" + element[0] + "&id=" + element[1]);
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        }
        if (getPluginConfig().getBooleanProperty(CHECK_MIRRORS, false) == true) {
            for (String element : mirrors) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(element)));
            }
        }

        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    /**
     * TODO: Umbauen!
     */
    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), CHECK_MIRRORS, JDL.L("plugins.decrypt.linkbankeu", "Check Mirror Links")).setDefaultValue(false));
    }
}
