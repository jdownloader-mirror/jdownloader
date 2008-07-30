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
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class FileUploadnet extends PluginForDecrypt {
    static private final String host = "File-Upload.net";
    private String version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?member\\.file-upload\\.net/(.*?)/(.*)", Pattern.CASE_INSENSITIVE);

    public FileUploadnet() {
        super();
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getHost() {
        return host;
    }



    
    public String getPluginName() {
        return host;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getVersion() {
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String user = new Regex(parameter, "upload\\.net/(.*?)/").getFirstMatch();
        String file = new Regex(parameter, user + "/(.*)").getFirstMatch();
        String link = "http://www.file-upload.net/member/data3.php?user=" + user + "&name=" + file;
        if (link != null) {
            link.replaceAll(" ", "%20");
            decryptedLinks.add(this.createDownloadlink(link));
            return decryptedLinks;
        } else
            return null;
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}