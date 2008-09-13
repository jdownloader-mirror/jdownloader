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

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class DDLMusicOrg extends PluginForDecrypt {

    private static final Pattern patternLink_Main = Pattern.compile("http://[\\w\\.]*?ddl-music\\.org/index\\.php\\?site=view_download&cat=.+&id=\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Crypt = Pattern.compile("http://[\\w\\.]*?ddl-music\\.org/captcha/ddlm_cr\\d\\.php\\?\\d+\\?\\d+", Pattern.CASE_INSENSITIVE);

    public DDLMusicOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (new Regex(parameter, patternLink_Crypt).matches()) {
            br.getPage(parameter);
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
            }

            Form captchaForm = br.getForm(0);
            String[] calc = br.getRegex(Pattern.compile("method=\"post\">[\\s]*?(\\d*?) (\\+|-) (\\d*?) =", Pattern.DOTALL)).getRow(0);
            if (calc[1].equals("+")) {
                captchaForm.put("calc" + captchaForm.getVars().get("linknr"), String.valueOf(Integer.parseInt(calc[0]) + Integer.parseInt(calc[2])));
            } else {
                captchaForm.put("calc" + captchaForm.getVars().get("linknr"), String.valueOf(Integer.parseInt(calc[0]) + Integer.parseInt(calc[2])));
            }
            br.submitForm(captchaForm);

            decryptedLinks.add(createDownloadlink(br.getRegex(Pattern.compile("<form action=\"(.*?)\" method=\"post\">", Pattern.CASE_INSENSITIVE)).getMatch(0)));
        } else if (new Regex(parameter, patternLink_Main).matches()) {
            br.getPage(parameter);

            String password = br.getRegex(Pattern.compile("<td class=\"normalbold\"><div align=\"center\">Passwort</div></td>.*?<td class=\"normal\"><div align=\"center\">(.*?)</div></td>", Pattern.CASE_INSENSITIVE|Pattern.DOTALL)).getMatch(0);
            if (password != null && password.contains("kein Passwort")) {
                password = null;
            }

            String ids[] = br.getRegex(Pattern.compile("<a href=\"(.*?)\" target=\"_blank\" onMouseOut=\"MM_swapImgRestore", Pattern.CASE_INSENSITIVE)).getColumn(0);
            progress.setRange(ids.length);
            for (String id : ids) {
                if (id.startsWith("/captcha/")) id = "http://ddl-music.org" + id;
                DownloadLink dLink = createDownloadlink(id);
                dLink.addSourcePluginPassword(password);
                decryptedLinks.add(dLink);
                progress.increase(1);
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}