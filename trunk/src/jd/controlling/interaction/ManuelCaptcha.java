package jd.controlling.interaction;

import java.io.Serializable;

import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

/**
 * Manuelle captchaeingabe
 * 
 * @author coalado
 */
public class ManuelCaptcha extends Interaction implements Serializable{



    /**
     * 
     */
    private static final long serialVersionUID = 4732389782312830473L;
    private static final String NAME             = "Captcha: Manuelle Eingabe";


   
    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting Manuell captcha");
        DownloadLink dink=(DownloadLink)arg;
        String captchaText = JDUtilities.getController().getCaptchaCodeFromUser(dink.getPlugin(), dink.getLatestCaptchaFile());
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
        return "Captcha: Manuelle Eingabe und Kontrolle (Bestätigung)";
    }

    @Override
    public String getInteractionName() {

        return NAME;
    }


  
}
