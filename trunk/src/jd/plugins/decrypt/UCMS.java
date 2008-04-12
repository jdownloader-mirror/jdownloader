//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class UCMS extends PluginForDecrypt {
    static private final String host = "Underground CMS";
    private String version = "1.0.0.0";

    private Pattern patternSupported = getSupportPattern("(http://[*]lesestunde.info/\\?id=[+])"
    		+ "|(http://[*]filefox.in/\\?id=[+])"
    		+ "|(http://[*]alphawarez.us/\\?id=[+])"
    		+ "|(http://[*]ddl-scene.com/\\?id=[+])"
    		+ "|(http://[*]xtreme-warez.net/\\?id=[+])"
    		+ "|(http://[*]pirate-loads.com/\\?id=[+])"
    		+ "|(http://[*]fettrap.com/\\?id=[+])"
    		+ "|(http://[*]hardcoremetal.biz/\\?id=[+])"
    		+ "|(http://[*]hms.x2.to/\\?id=[+])"
    		+ "|(http://[*]serienfreaks.to/category/[+]/[+])"
    		+ "|(http://[*]serienfreaks.to/\\?id=[+])"
    		+ "|(http://[*]serienfreaks.in/category/[+]/[+])"
            + "|(http://[*]serienfreaks.in/\\?id=[+])"
    		+ "|(http://[*]flashload.org/\\?id=[+])"
    		+ "|(http://[*]found-station.net/\\?id=[+])"
    		+ "|(http://[*]twin-warez.com/\\?id=[+])"
    		+ "|(http://[*]oneload.org/\\?id=[+])"
    		+ "|(http://[*]warez-load.com/\\?id=[+])"
    		+ "|(http://[*]steelwarez.com/\\?id=[+])"
    		+ "|(http://[*]warezbase.us/\\?id=[+])"
    		+ "|(http://[*]fullstreams.info/\\?id=[+])"
    		//+ "|(http://[*]toxic.to/\\?id=[+])"
    		+ "|(http://[*]lionwarez.com/\\?id=[+])"
    		+ "|(http://[*]1dl.in/\\?id=[+])"
    		+ "|(http://[*]oxygen-warez.com/\\?id=[+])"
    		+ "|(http://[*]oxygen-warez.com/category/[+]/[+])"
    		+ "|(http://[*]mov-world.net/category/[+]/[+])"
    		+ "|(http://[*]serienking.in/\\?id=[+])"
    		+ "|(http://[*]your-load.com/category/[+]/[+])"
    		+ "|(http://[*]isos.at/[+]/[+])"
    		+ "|(http://[*]game-freaks.net/[+]/[+].html)"
    		+ "|(http://[*]chili-warez.net/[+]/[+].html)"
    		+ "|(http://[*]chrome-database.com/\\?id=[+])"
    		+ "|(http://[*]mp3z.to/\\?id=[+])"
    		+ "|(http://[*]oneload.org/\\?id=[+])"
    		+ "|(http://[*]youwarez.biz/\\?id=[+])"
    		+ "|(http://[*]saugking.net/\\?id=[+])"
    		+ "|(http://[*]leetpornz.com/\\?id=[+])"
    		+ "|(http://[*]freefiles4u.com/\\?id=[+])"
    		+ "|(http://[*]xxx-reactor.net/category/[+]/[+])"
    		+ "|(http://[*]xxx-4-free.net/category/[+]/[+])"
    		+ "|(http://[*]sextradump.com/category/[+]/[+])"
    		+ "|(http://[*]porn-freaks.net/category/[+]/[+])"
    		+ "|(http://[*]serien24.com/category/[+]/[+])"
    		+ "|(http://[*]babeviz.com/category/[+]/[+])"
    		+ "|(http://[*]porn-traffic.net/category/[+]/[+])"
    		+ "|(http://[*]join-music.net/\\?id=[+])"
    		+ "|(http://[*]mixe-downloaden.info/\\?id=[+])"
    		+ "|(http://[*]relfreaks.com/category/[+]/[+])"
    		+ "|(http://[*]spreaded.net/category/[+]/[+])"
    		+ "|(http://[*]romc.extra.hu/\\?id=[+])"
    		+ "|(http://[*]dark-load.net/\\?id=[+])"
    		+ "|(http://[*]sexload.to/\\?id=[+])"
    		+ "|(http://[*]rsxxx.net/\\?id=[+])"
    		+ "|(http://[*]wrzunlimited.1gb.in/\\?id=[+])"
    		+ "|(http://[*]clips-share.net/\\?id=[+])"
    		
    		//+ "|(http://[*]myload.es/\\?id=[+])"
    		
    		
    		+ "|(http://[*]sceneload.to/\\?id=[+])");
    
    private Pattern	PAT_CAPTCHA = Pattern.compile("<TD><IMG SRC=\"/gfx/secure/");
    private Pattern PAT_NO_CAPTCHA = Pattern.compile("(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\"Zum Download\" onClick=\"if)|(<INPUT TYPE=\"SUBMIT\" CLASS=\"BUTTON\" VALUE=\"Download\" onClick=\"if)");

    public UCMS() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
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
        return "Underground CMS-1.0.0.";
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

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		try {
    			URL url = new URL(parameter);
    			
    			RequestInfo reqinfo = getRequest(url);
    			File captchaFile = null;
    			String capTxt = "";
    			String host = url.getHost();
    			
    			if(!host.startsWith("http"))
    				host = "http://" + host;
    			
    			String pass = getBetween(getMatches(reqinfo.getHtmlCode(), Pattern.compile("(Passwort(.*?)\">(.*?)<)|(Password(.*?)\">(.*?)<)")).get(0), "\">", "<");
    			if(!pass.equals("n/a"))
    				this.default_password.add(pass);
    			
    			ArrayList<ArrayList<String>> forms = getAllSimpleMatches(reqinfo.getHtmlCode(), "<FORM ACTION=\"°\" ENCTYPE°NAME=\"°\"°</FORM>");
    			
    			for(int i=0; i<forms.size(); i++) {
    				if(forms.get(i).get(2).contains("download") || forms.get(i).get(2).contains("mirror")) {
		    			for (;;) {
		                    Matcher matcher = PAT_CAPTCHA.matcher(forms.get(i).get(3));
		
		                    if (matcher.find()) {
		                        if (captchaFile != null && capTxt != null) {
		                            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, false);
		                        }
		
		                        logger.finest("Captcha Protected");
		                        String captchaAdress = host + getBetween(forms.get(i).get(3), "<TD><IMG SRC=\"", "\"");
		                        captchaFile = getLocalCaptchaFile(this);
		                        JDUtilities.download(captchaFile, captchaAdress);
		
		                        capTxt = JDUtilities.getCaptcha(this, "hardcoremetal.biz", captchaFile, false);
		                        
		                        String posthelp = getFormInputHidden(forms.get(i).get(3));
		                        if(forms.get(i).get(0).startsWith("http")) {
		                        	reqinfo = postRequest(new URL(forms.get(i).get(0)), posthelp + "&code=" + capTxt);
		                        }
		                        else {
		                        	reqinfo = postRequest(new URL(host + forms.get(i).get(0)), posthelp + "&code=" + capTxt);
		                        }
		                    }
		                    else {
		                        if (captchaFile != null && capTxt != null) {
		                            JDUtilities.appendInfoToFilename(this, captchaFile, capTxt, true);
		                        }
		
		                       	Matcher matcher_no = PAT_NO_CAPTCHA.matcher(reqinfo.getHtmlCode());
		                        
		                       	if(matcher_no.find()) {
		                       		logger.finest("Not Captcha protected");
		                       		String posthelp = getFormInputHidden(forms.get(i).get(3));
		                       		
		                       		if(forms.get(i).get(0).startsWith("http")) {
		                       			reqinfo = postRequest(new URL(forms.get(i).get(0)), posthelp);
		                       		}
		                       		else {
		                       			reqinfo = postRequest(new URL(host + forms.get(i).get(0)), posthelp);
		                       		}
		                       		
		                       		break;
		                       	}
		                        if(!reqinfo.containsHTML("Der Sichheitscode wurde falsch eingeben")) {
		                        	reqinfo = getRequest(url);
		                        }
		                        else {
		                        	break;
		                        }
		                    }
		                    
		                    if(reqinfo.getConnection().getURL().toString().equals(host + forms.get(i).get(0)))
		                    	break;
		                }
		    			ArrayList<ArrayList<String>> links = null;
		    			
		    			if(reqinfo.containsHTML("unescape")) {
		    				links = getAllSimpleMatches(java.net.URLDecoder.decode(java.net.URLDecoder.decode(java.net.URLDecoder.decode(getBetween(reqinfo.getHtmlCode(), "unescape\\(unescape\\(\"", "\"")))), "ACTION=\"°\"");
		    			}
		    			else {
		    				links = getAllSimpleMatches(reqinfo.getHtmlCode(), "ACTION=\"°\"");
		    			}
	        			for(int j=0; j<links.size(); j++){
	        				//System.out.println(links.get(j).get(0));
	        				decryptedLinks.add(this.createDownloadlink(links.get(j).get(0)));
	        			}
    				}
    			}
    		
    			// Decrypten abschliessen
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			 e.printStackTrace();
    		}
    	}
    	return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}