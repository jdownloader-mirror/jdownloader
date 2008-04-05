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
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins.redirect;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.Form;
import jd.plugins.Plugin;
import jd.plugins.PluginForRedirect;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.domain.JDFile;
import jd.plugins.domain.JDFileContainer;
import jd.utils.JDUtilities;

public class Lixin extends PluginForRedirect {

    static private final String host             = "lix.in";

    private String              version          = "1.0.0.0";
    //lix.in/cc1d28
    static private final Pattern patternSupported = Pattern.compile("http://.{0,5}lix\\.in/[a-zA-Z0-9]{6,10}", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternCaptcha = Pattern.compile("<img\\s+src=\"(.*?captcha.*?)\"");
    static private final Pattern patternIframe = Pattern.compile("<iframe.*src=\"(.+?)\"", Pattern.DOTALL);
    static private final Pattern patternCaptchaWrong = Pattern.compile("<title>Lix.in - Linkprotection</title>");
    

    public Lixin() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_REDIRECT, null));
        currentStep = steps.firstElement();
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
        return "Lix.in-1.0.0.";
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
    public PluginStep doStep(PluginStep step, URL url) {
    	if(step.getStep() == PluginStep.STEP_REDIRECT) {
            JDFileContainer jdFileContainer = new JDFileContainer();
    		try {
    			progress.setRange(1);
    			
    			RequestInfo reqInfo = getRequest(url);
    		    Form form = reqInfo.getForm();
                form.method = Form.METHOD_POST;
                
                //check if this link is captcha protected
                //funny thing is, even the same link can have a captcha if you open
                //it the first time, and the second time there is no captcha, therefore
                //we have to check
    			Matcher matcher = patternCaptcha.matcher(reqInfo.getHtmlCode());

    			if( matcher.find()){
    				logger.info("has captcha");
    				String captchaAddress  = "http://"+getHost() + "/" + matcher.group(1);
    				
    				File captchaFile = this.getLocalCaptchaFile(this);
                    if (!JDUtilities.download(captchaFile, captchaAddress) || !captchaFile.exists()) {
                        logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    String captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                    form.put("capt", captchaCode);
    			}else{
    				logger.info("no captcha");
    			}
    			
                RequestInfo reqInfoForm = form.getRequestInfo();
                
        		// Link herausfiltern
                matcher = patternIframe.matcher(reqInfoForm.getHtmlCode());
                if(!matcher.find()){
                	
                	step.setStatus(PluginStep.STATUS_ERROR);
                	
                	matcher = patternCaptchaWrong.matcher(reqInfoForm.getHtmlCode());
                	
                	if(matcher.find()){
                		logger.info("entered captcha code seems to be wrong");
                	}else{
                    	logger.severe("unable to detect Link in iframe (see next INFO log line) - adapt patternIframe");
                    	logger.info(reqInfoForm.getHtmlCode());
                	}
                	
                	return null;
                }
                
                String link = matcher.group(1);
                
                //on lix.in you can not determine the filename with the information provided
                //in the link or the content of the page, therefore just use the url.toString();
                JDFile jdFile = new JDFile( generateJDFileName(url) );
                jdFile.addMirror(this.createDownloadlink((link)));
                jdFileContainer.addFile( jdFile);

        		progress.increase(1);
    		}
    		catch(IOException e) {
    			 e.printStackTrace();
    		}
    		step.setParameter(jdFileContainer);
    	}
    	
    	return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
