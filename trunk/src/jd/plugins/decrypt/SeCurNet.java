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

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class SeCurNet extends PluginForDecrypt {
    final static String host = "se-cur.net";
    private String FRAME = "src=\"(.*?)\"";
    private String LINK_OUT_PATTERN = "href=\"http://se-cur\\.net/out\\.php\\?d=(.*?)\"";
    private String LINK_OUT_TEMPLATE = "http://se-cur.net/out.php?d=";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?se-cur\\.net/q\\.php\\?d=.+", Pattern.CASE_INSENSITIVE);
    // private String version = "0.1.0";

    public SeCurNet() {
        super();
    }

    
    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo requestInfo = HTTP.getRequest(url);
            String layerLinks[][] = new Regex(requestInfo.getHtmlCode(), LINK_OUT_PATTERN, Pattern.CASE_INSENSITIVE).getMatches();
            progress.setRange(layerLinks.length);
            for (int i = 0; i < layerLinks.length; i++) {
                requestInfo = HTTP.getRequest(new URL(LINK_OUT_TEMPLATE + layerLinks[i][0]));
                String link = new Regex(requestInfo.getHtmlCode(), FRAME, Pattern.CASE_INSENSITIVE).getFirstMatch();
                link = JDUtilities.htmlDecode(link);
                decryptedLinks.add(this.createDownloadlink(link));
                progress.increase(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;

    }

    
    @Override
    public boolean doBotCheck(File file) {
        return false;
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
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }
}