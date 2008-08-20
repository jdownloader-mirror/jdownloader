//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class RelinkUs extends PluginForDecrypt {

    final static String host = "relink.us";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?relink\\.us\\/go\\.php\\?id=\\d+", Pattern.CASE_INSENSITIVE);

    public RelinkUs() {
        super();
    }

    private boolean add_relinkus_container(String page, String cryptedLink, String containerFormat) throws IOException {
        String container_link = new Regex(page, Pattern.compile("<a target=\"blank\" href=\\'([^\\']*?)\\'><img src=\\'images\\/" + containerFormat + "\\.gif\\'", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (container_link != null) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);
            URL container_url = new URL("http://relink.us/" + Encoding.htmlDecode(container_link));
            HTTPConnection container_con = new HTTPConnection(container_url.openConnection());
            container_con.setRequestProperty("Referer", cryptedLink);
            Browser.download(container, container_con);
            JDUtilities.getController().loadContainerFile(container);
            container.delete();
            return true;
        }
        return false;
    }

    private void add_relinkus_links(String page, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        String links[] = new Regex(page, Pattern.compile("action=\\'([^\\']*?)\\' method=\\'post\\' target=\\'\\_blank\\'", Pattern.CASE_INSENSITIVE)).getColumn(0);
        progress.addToMax(links.length);
        for (String link : links) {
            String dl_link = new Regex(br.postPage("http://relink.us/" + Encoding.htmlDecode(link), "submit=Open"), "iframe name=\"pagetext\" height=\"100%\" frameborder=\"no\" width=\"100%\" src=\"[\n\r]*?(.*?)\"", Pattern.CASE_INSENSITIVE).getMatch(0);
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(dl_link)));
            progress.increase(1);
        }
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String page = br.getPage(parameter);
        progress.setRange(0);
        add_relinkus_links(page, decryptedLinks);
        String more_links[] = new Regex(page, Pattern.compile("<a href=\"(go\\.php\\?id=\\d+\\&seite=\\d+)\">", Pattern.CASE_INSENSITIVE)).getColumn(0);
        for (String link : more_links) {
            add_relinkus_links(br.getPage("http://relink.us/" + link), decryptedLinks);
        }
        if (decryptedLinks.size() == 0) {
            if (!add_relinkus_container(page, parameter, "dlc")) {
                if (!add_relinkus_container(page, parameter, "ccf")) {
                    add_relinkus_container(page, parameter, "rsdf");
                }
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}