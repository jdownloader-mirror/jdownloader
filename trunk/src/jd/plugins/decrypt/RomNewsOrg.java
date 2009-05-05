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

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ClickPositionDialog;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RomNewsOrg extends PluginForDecrypt {

    public RomNewsOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        File file = this.getLocalCaptchaFile(this);
        String whattoclick = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        whattoclick = whattoclick.replaceAll("(</span>|\">|<span class=\")", " ");
        String cap = br.getRegex("\"image\" src=\"(.*?image.*?)\"").getMatch(0);
        Form form = br.getForm(0);
        Browser.download(file, br.cloneBrowser().openGetConnection(cap));
        ClickPositionDialog d = ClickPositionDialog.show(SimpleGUI.CURRENTGUI, file, "Captcha", whattoclick, 20, null);
        if (d.abort == true) throw new DecrypterException(DecrypterException.CAPTCHA);
        Point p = d.result;
        form.remove("x");
        form.remove("y");
        form.put("name.x", p.x + "");
        form.put("name.y", p.y + "");
        br.submitForm(form);

        decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));

        return decryptedLinks;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}