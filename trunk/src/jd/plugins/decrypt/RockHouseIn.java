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

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class RockHouseIn extends PluginForDecrypt {
    final static String host = "rock-house.in";
    private String version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rock-house\\.in/warez/warez_download\\.php\\?id=.+", Pattern.CASE_INSENSITIVE);

    public RockHouseIn() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        // currentStep = steps.firstElement();
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
    public String getPluginID() {
        return "Rock-House.in-1.0.0.";
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
        return version;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        // //if (step.getStep() == PluginStep.STEP_DECRYPT) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);

            ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "<td><a href=\'°\' target=\'_blank\'>");

            default_password.add(jd.utils.JDUtilities.htmlDecode(SimpleMatches.getBetween(reqinfo.getHtmlCode(), "<td class=\'button\'>Passwort:</td><td class=\'button\'>", "<")));
            for (int i = 0; i < links.size(); i++) {
                decryptedLinks.add(this.createDownloadlink(links.get(i).get(0).replaceAll("\n", "")));
            }

            // Decrypt abschliessen
            // step.setParameter(decryptedLinks);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}