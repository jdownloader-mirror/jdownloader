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


package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class YouPornCom extends PluginForDecrypt {

    public YouPornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);

        br.postPage(parameter, "user_choice=Enter");
        String matches = br.getRegex("addVariable\\('file', encodeURIComponent\\('(.*?)'\\)\\);").getMatch(0);
        String filename = br.getRegex("<title>(.*?)- Free Porn Videos - YouPorn.com Lite \\(BETA\\)</title>").getMatch(0);
        if (matches == null) { return null; }
        matches = matches.replaceAll("&xml=1", "");
        DownloadLink dlink = createDownloadlink(matches);
        if (filename != null) dlink.setFinalFileName(filename.trim().replaceAll(" ", "-") + ".flv");
        dlink.setBrowserUrl(parameter);
        decryptedLinks.add(dlink);

        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
