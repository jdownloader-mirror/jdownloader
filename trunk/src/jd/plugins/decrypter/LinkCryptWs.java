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

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkcrypt.ws" }, urls = { "http://[\\w\\.]*?linkcrypt\\.ws/dir/[\\w]+"}, flags = { 0 })


public class LinkCryptWs extends PluginForDecrypt {

    public LinkCryptWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * TODO: könntet ihr aus linkcrypt.ws/dirl/id linkcrypt.ws/dlc/id machen?
     * (bezogen auf CNL Links im browser)
     */
    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String containerId = new Regex(parameter, "dir/([a-zA-Z0-9]+)").getMatch(0);

        br.getPage("http://linkcrypt.ws/dlc/" + containerId);

        logger.finest("Captcha Protected");

        boolean valid = true;
        for (int i = 0; i < 5; ++i) {
            if (br.containsHTML("Bitte klicke auf den offenen Kreis!")) {
                valid = false;
                File file = this.getLocalCaptchaFile();
                Form form = br.getForm(0);
                Browser.download(file, br.cloneBrowser().openGetConnection("http://linkcrypt.ws/captx.php"));
                Point p = UserIO.getInstance().requestClickPositionDialog(file, JDL.L("plugins.decrypt.stealthto.captcha.title", "Captcha"), JDL.L("plugins.decrypt.stealthto.captcha", "Please click on the Circle with a gap"));
                if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                form.put("x", p.x + "");
                form.put("y", p.y + "");
                br.submitForm(form);
            } else {
                valid = true;
                break;
            }
        }

        if (valid == false) throw new DecrypterException(DecrypterException.CAPTCHA);

        File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
        if (!container.exists()) container.createNewFile();

        /* TODO: Das kann man sicher besser lösen.. bitte mal wer reinschauen */
        String dlc = br.getRegex("(^.*?)<script").getMatch(0);
        if (dlc == null) dlc = br.toString();

        FileOutputStream out = new FileOutputStream(container);
        for (int i = 0; i < dlc.length(); i++) {
            out.write((byte) dlc.charAt(i));
        }
        out.close();

        decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
        container.delete();

        return decryptedLinks;
    }

    // @Override
    
}
