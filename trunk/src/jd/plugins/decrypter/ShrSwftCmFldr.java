//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareswift.com" }, urls = { "http://[\\w\\.]*?shareswift\\.com/\\w+/.+" }, flags = { 0 })
public class ShrSwftCmFldr extends PluginForDecrypt {

    public ShrSwftCmFldr(PluginWrapper wrapper) {
      super(wrapper);
    }
    
    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
      ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
      String parameter = param.toString();
      
      if (parameter.matches(".+\\.html")) {
          parameter = parameter.replaceFirst("shareswift.com", "shareswift.viajd");
          decryptedLinks.add(createDownloadlink(parameter));
      } else {
          br.getPage(parameter);
          String[] links = br.getRegex("<TR><TD><a href=\"(.*?)\" target=\"_blank\">").getColumn(0);
          if (links.length == 0) return null;
          for (String dl : links) {
              dl = dl.replaceFirst("shareswift.com", "shareswift.viajd");
              decryptedLinks.add(createDownloadlink(dl));
          } 
      }
      return decryptedLinks;
    }
    
    // @Override

}
