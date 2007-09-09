package jd.controlling.interaction;

import java.io.Serializable;

import jd.JDUtilities;
import jd.plugins.DownloadLink;

/**
 * Diese Klasse führt eine Test INteraction durch
 * 
 * @author coalado
 */
public class JAntiCaptcha extends Interaction implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = -4390257509319544642L;
    /**
     * serialVersionUID
     */

    private static final String NAME             = "Captcha Erkennung: JAntiCaptcha";

    /**
     * Führt die Normale INteraction zurück. Nach dem Aufruf dieser methode
     * läuft der Download wie geowhnt weiter.
     */
   
    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting JAC");
        DownloadLink dink=(DownloadLink)arg;
        String captchaText = JDUtilities.getCaptcha(JDUtilities.getController(), dink.getPlugin(), dink.getLatestCaptchaFile());
        setProperty("captchaCode",captchaText);        
        if(captchaText!=null && captchaText.length()>0)return true;
        return false;
    }

    /**
     * Nichts zu tun. WebUpdate ist ein Beispiel für eine ThreadInteraction
     */
    public void run() {

    }

    public String toString() {
        return NAME;
    }

    @Override
    public String getInteractionName() {

        return NAME;
    }


  
}
