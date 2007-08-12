﻿package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Rapidshare extends PluginForHost{
    private String  host    = "rapidshare.com";
    private String  version = "1.0.0.0";
    // http://(?:[^.]*\.)*rapidshare\.com/files/[0-9]*/[^\s"]+
    private Pattern patternSupported = Pattern.compile("http://(?:[^.]*\\.)*rapidshare\\.com/files/[0-9]+/[^\\s\"]+");
    /**
     * Das findet die Ziel URL für den Post
     */
    private Pattern patternForNewHost = Pattern.compile("<form *action *= *\"([^\\n\"]*)\"");
    /**
     * Das findet die Captcha URL
     * <form *name *= *"dl" (?s).*<img *src *= *"([^\n"]*)">
     */
    private Pattern patternForCaptcha = Pattern.compile("<form *name *= *\"dl\" (?s).*<img *src *= *\"([^\\n\"]*)\">");
    /**
     * 
     * <form name="dl".* action="([^\n"]*)"(?s).*?<input type="submit" name="actionstring" value="[^\n"]*"
     */
    private Pattern patternForFormData = Pattern.compile("<form name=\"dl\".* action=\"([^\\n\"]*)\"(?s).*?<input type=\"submit\" name=\"actionstring\" value=\"([^\\n\"]*)\"");
    /**
     * Das DownloadLimit wurde erreicht
     * (?s)Downloadlimit.*Oder warte ([0-9]*)  
     */
    private Pattern patternDownloadLimitReached = Pattern.compile("(?s)download-*limit.*([0-9]+) minute");
    private Pattern patternCaptchaWrong = Pattern.compile("zugriffscode falsch");
    
    private int waitTime          = 500;
    private String captchaAddress;
    private String postTarget;
    private String actionString;
    private HashMap<String, String> postParameter = new HashMap<String, String>(); 

    @Override public String getCoder()            { return "astaldo";        }
    @Override public String getHost()             { return host;             }
    @Override public String getPluginName()       { return host;             }
    @Override public Pattern getSupportedLinks()  { return patternSupported; }
    @Override public String getVersion()          { return version;          }
    @Override public boolean isClipboardEnabled() { return true;             }
    
    public Rapidshare(){
        super();
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_CAPTCHA,  null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }
    @Override
    public URLConnection getURLConnection() {
        return null;
    }
    @Override
    public PluginStep getNextStep(Object parameter) {
        DownloadLink downloadLink = (DownloadLink)parameter;
        RequestInfo requestInfo;
        PluginStep todo=null;
        if(currentStep == null){
            try {
                //Der Download wird bestätigt
                requestInfo = getRequest(downloadLink.getUrlDownload());
                String newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);
                if(newURL != null){

                    //Auswahl ob free oder prem
                    requestInfo = postRequest(new URL(newURL),"dl.start=free");

                    // captcha Adresse finden
                    captchaAddress = getFirstMatch(requestInfo.getHtmlCode(),patternForCaptcha,1);

                    //post daten lesen
                    postTarget   = getFirstMatch(requestInfo.getHtmlCode(), patternForFormData, 1);
                    actionString = getFirstMatch(requestInfo.getHtmlCode(), patternForFormData, 2);
                }
                currentStep  = steps.firstElement();
                if(newURL == null || captchaAddress == null || postTarget == null || actionString == null){
                    if(newURL == null){
                        logger.severe("file not found");
                        setStatus(STATUS_FILE_NOT_FOUND);
                        currentStep.setStatus(PluginStep.STATUS_ERROR);
                        return currentStep;
                    }
                    String strWaitTime = getFirstMatch(requestInfo.getHtmlCode(), patternDownloadLimitReached, 1);
                    if(strWaitTime != null){
                        logger.severe("wait "+strWaitTime+" minutes");
                        setStatus(STATUS_DOWNLOAD_LIMIT);
                        currentStep.setStatus(PluginStep.STATUS_ERROR);
                        return currentStep;
                    }
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    logger.warning("could not get downloadInfo");
                    return currentStep;
                }
            }
            catch (MalformedURLException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
            
        }
        int index = steps.indexOf(currentStep);
        todo = currentStep;
        if(index+1 < steps.size())
            currentStep = steps.elementAt(index+1);
        logger.info(todo.toString());
        switch(todo.getStep()){
            case PluginStep.STEP_WAIT_TIME:
                todo.setParameter(new Long(waitTime));
                break;
            case PluginStep.STEP_CAPTCHA:
                todo.setParameter(captchaAddress);
                todo.setStatus(PluginStep.STATUS_USER_INPUT);
                break;
            case PluginStep.STEP_DOWNLOAD:
                postParameter.put("mirror",      "on");
                postParameter.put("accesscode",  (String)steps.elementAt(1).getParameter());
                postParameter.put("actionString",actionString);
                boolean success = prepareDownload(downloadLink);
                if(success){
                    todo.setStatus(PluginStep.STATUS_DONE);
                    return null;
                }
                else{
                    logger.severe("captcha wrong");
                    setStatus(STATUS_CAPTCHA_WRONG);
                    todo.setStatus(PluginStep.STATUS_ERROR);
                }
                break;
        }
        return todo;
    }
    private boolean prepareDownload(DownloadLink downloadLink){
        try {
            URLConnection urlConnection = new URL(postTarget).openConnection();
            urlConnection.setDoOutput(true);
            
            //Post Parameter vorbereiten
            String postParams = createPostParameterFromHashMap(postParameter);
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(postParams);
            wr.flush();
            
            int length = urlConnection.getContentLength();
            File fileOutput = downloadLink.getFileOutput();
            downloadLink.setDownloadLength(length);
            return download(downloadLink, urlConnection);
        }
        catch (IOException e) { logger.severe("URL could not be opened. "+e.toString());}
        return false;
    } 
}
