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
package jd.plugins.hoster;

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//movshare by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "movshare.net" }, urls = { "http://[\\w\\.]*?movshare\\.net/video/[a-z|0-9]+" }, flags = { 0 })
public class MovShareNet extends PluginForHost {

    public MovShareNet(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
        setBrowserExclusive();
    }

    @Override
    public String getAGBLink() {
        return "http://www.movshare.net/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("We need you to prove you're human")) {
            Form IAmAHuman = br.getForm(0);
            if (IAmAHuman == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            br.submitForm(IAmAHuman);
        }
        if (br.containsHTML("This file no longer exists on our servers")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename0 = (br.getRegex("Title: </strong>(.*?)</td> <td>").getMatch(0));
        if (filename0 == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = filename0 + System.currentTimeMillis() + ".avi";
        downloadLink.setName(filename.trim());

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("We need you to prove you're human")) {
            Form IAmAHuman = br.getForm(0);
            if (IAmAHuman == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            br.submitForm(IAmAHuman);
        }
        String dllink = br.getRegex(Pattern.compile("<embed src=\"(.*?)\" width")).getMatch(0);
        if (dllink != null) {
            dl = br.openDownload(downloadLink, dllink, true, -20);
        } else {
            String dllink2 = br.getRegex(Pattern.compile("video/divx\" src=\"(.*?)\"  id=\"embedm")).getMatch(0);
            dl = br.openDownload(downloadLink, dllink2, true, -20);
        }

        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}