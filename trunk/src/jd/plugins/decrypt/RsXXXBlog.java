package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.JDUtilities;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

/**
 * http://rs.xxx-blog.org/com-UmNkdzY1MjN/file.rar
 * 
 * 
 * 
 * @author coalado
 * 
 */
public class RsXXXBlog extends PluginForDecrypt {
    static private final String   host                 = "rs.xxx-blog.org";

    private String  version              = "1.0.0.1";

 
   
    private Pattern patternSupported     = Pattern.compile("http://rs\\.xxx-blog\\.org/com-[\\w]*/[^\\s\"']*");


    public RsXXXBlog() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "coalado";
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
    public String getHost() {
        return host;
    }

    @Override
    public boolean isClipboardEnabled() {
        return true;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return "xxx-blog.org-1.0.0.";
    }


 

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        switch (step.getStep()) {
            case PluginStep.STEP_DECRYPT:
                Vector<String> decryptedLinks = new Vector<String>();
             // Zählen aller verfügbaren Treffer
                try {
                    URL url = new URL(cryptedLink);
                    RequestInfo requestInfo = getRequest(url, null, null, false);
                   HashMap<String,String> fields=this.getInputHiddenFields(requestInfo.getHtmlCode(),"postit","starten");
                  String newURL= "http://rapidshare.com"+JDUtilities.htmlDecode(fields.get("uri"));
                  decryptedLinks.add(newURL);
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                step.setParameter(decryptedLinks);
                break;

        }
        return null;

    }
}
