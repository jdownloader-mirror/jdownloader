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
//    along with this program.  If not, see <http://www.gnu.org/licenses

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class UploadJockeycom extends PluginForDecrypt {

    static private final String HOST = "uploadjockey.com";
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?uploadjockey\\.com/download/[a-zA-Z0-9]+/(.*)", Pattern.CASE_INSENSITIVE);
    private String CODER = "JD-Team";

    public UploadJockeycom() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        String cryptedLink = parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        URL url;
        RequestInfo requestInfo;
        try {
            url = new URL(cryptedLink);
            requestInfo = HTTP.getRequest(url, null, url.toString(), false);
            String links[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"http\\:\\/\\/www\\.uploadjockey\\.com\\/redirect\\.php\\?url=([a-zA-Z0-9=]+)\"", Pattern.CASE_INSENSITIVE)).getMatches();
            for (String[] element : links) {
                decryptedLinks.add(createDownloadlink(Encoding.Base64Decode(element[0])));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
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
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }
}
