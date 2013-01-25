//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hidemylinks.net" }, urls = { "https?://(www\\.)?hidemylinks\\.net/(p|d)/[a-z0-9]+" }, flags = { 0 })
public class HideMyLinksNet extends PluginForDecrypt {

    public HideMyLinksNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("safelinking.net");
        final jd.plugins.decrypter.SflnkgNt.GeneralSafelinkingHandling gsh = ((jd.plugins.decrypter.SflnkgNt) solveplug).getGeneralSafelinkingHandling(this.br, param, this.getHost());
        gsh.startUp();
        try {
            gsh.decrypt();
        } catch (final DecrypterException e) {
            final String errormessage = e.getMessage();
            if ("offline".equals(errormessage)) { return decryptedLinks; }
            throw e;
        }
        decryptedLinks = gsh.getDecryptedLinks();

        return decryptedLinks;
    }

}