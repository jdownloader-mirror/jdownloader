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

package jd.captcha.specials;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;

/**
 * 
 * 
 * @author JD-Team
 */
public class RapidshareCom {



    private static Logger       logger                  = JDUtilities.getLogger();

    public static void onlyCats( Vector<LetterComperator> lcs) {
        String methodsPath = UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods" });
        String hoster = "rscat.com";
        
      for(Iterator<LetterComperator> it = lcs.iterator();it.hasNext();){
          LetterComperator next = it.next();
          Letter dif = next.getDifference();
          dif.removeSmallObjects(0.8, 0.8, 20);
          dif.clean();
          JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
          
          if(!jac.getLetter(dif).getDecodedValue().equalsIgnoreCase("k")){
             //it.remove(); 
          }
          
          
      }

    }


}