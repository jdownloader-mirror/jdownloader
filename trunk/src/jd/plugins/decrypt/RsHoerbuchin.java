package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * http://rs.xxx-blog.org/com-UmNkdzY1MjN/file.rar
 * http://rs.hoerbuch.in/com-UmY3YGNiRjN/PP-Grun.rar
 * 
 * 
 * @author coalado
 * 
 */
public class RsHoerbuchin extends PluginForDecrypt {
    static private final String host             = "rs.hoerbuch.in";

    private String              version          = "1.0.0.1";
    static private final Pattern patternSupported = Pattern.compile("http://rs\\.hoerbuch\\.in/com-[a-zA-Z0-9]{11}/.*", Pattern.CASE_INSENSITIVE);



    public RsHoerbuchin() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        default_password.add("www.hoerbuch.in");
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
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return "hoerbuch.in-1.0.0.";
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
           
                Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
                // Zählen aller verfügbaren Treffer
                try {
                    URL url = new URL(cryptedLink);
                    RequestInfo requestInfo = getRequest(url, null, null, false);
                    HashMap<String, String> fields = this.getInputHiddenFields(requestInfo.getHtmlCode(), "postit", "starten");
                    String newURL = "http://rapidshare.com" + JDUtilities.htmlDecode(fields.get("uri"));
                    decryptedLinks.add(this.createDownloadlink(newURL));
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
