package jd.captcha;



import java.io.File;

import jd.JDUtilities;



/**
 * JAC Tester

 * 
 * @author coalado
 */
public class JACTest {
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACTest main = new JACTest();
        main.go();
    }
    private void go(){
      
      

       JAntiCaptcha jac= new JAntiCaptcha(null,"rapidshare.com");
     //sharegullicom47210807182105.gif
      jac.setShowDebugGui(true);
       LetterComperator.CREATEINTERSECTIONLETTER=true;
      jac.displayLibrary();
       jac.getJas().set("quickScanFilter", 100);
     jac.showPreparedCaptcha(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods"+"/rapidshare.com/captchas/1188481784981_rapidsharecom22190807214807.jpg"));
      
     //UTILITIES.getLogger().info(JAntiCaptcha.getCaptchaCode(UTILITIES.loadImage(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods"+"/rapidshare.com/captchas/rapidsharecom24190807214810.jpg")), null, "rapidshare.com"));
     //jac.removeBadLetters();
      //jac.addLetterMap();
      //jac.saveMTHFile();

      
   
    }
}