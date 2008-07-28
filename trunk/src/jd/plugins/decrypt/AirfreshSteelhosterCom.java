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

import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class AirfreshSteelhosterCom extends PluginForDecrypt {

    final static String host             = "airfresh.steelhoster.com";
    private String      version          = "1.0.0.0";
    static private final Pattern patternSupported = Pattern.compile("http://airfresh\\.steelhoster\\.com/\\?[\\d]{4}", Pattern.CASE_INSENSITIVE);

    public AirfreshSteelhosterCom() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        //currentStep = steps.firstElement();
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
        return "Airfrash.steelhoster.com-1.0.0.";
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

    @Override public ArrayList<DownloadLink> decryptIt(String parameter) {
    	//if(step.getStep() == PluginStep.STEP_DECRYPT) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = HTTP.getRequest(url);
    			
//    			ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "a href=\"°\" target=\"");
    			String[] links = reqinfo.getRegexp("a href=\"(.*?)\" target=\"").getMatches(1);
    			progress.setRange( links.length);
    			
    			for(int i=0; i<links.length; i++) {
    				reqinfo = HTTP.getRequest(new URL("http://airfresh.steelhoster.com/" + links[i]));
    				decryptedLinks.add(this.createDownloadlink(reqinfo.getFirstMatch("src=\"(.*?)\"").trim()));
    				progress.increase(1);
    			}
    			
    			// Decrypt abschliessen
    			
    			//step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			 e.printStackTrace();
    		}
    	
    	return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}