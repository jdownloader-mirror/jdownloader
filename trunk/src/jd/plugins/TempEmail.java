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

package jd.plugins;

import java.io.IOException;
import java.net.MalformedURLException;

import jd.http.Browser;
import jd.parser.Regex;

/**
 * @author jDTeam
 * 
 */
public class TempEmail {
    public String emailname = System.currentTimeMillis() + "jd";
    private String[][] emails = null;
    private Browser br;

    public TempEmail() {
        br = new Browser();
    }

    /**
     * Die aktuelle Emailadresse
     * 
     * @return
     */
    public String getEmailAdress() {
        return emailname + "@mailinator.com";
    }

    public String getFilteredMail(MailFilter mailFilter) throws MalformedURLException, IOException {
        if (getEmailAdress() == null) return null;
        for (int i = 0; i < emails.length; i++) {
            if (mailFilter.fromAdress(emails[i])) return getMail(i);
        }
        return null;
    }

    /**
     * Gibt den Inhalt der mail i zurück
     * 
     * @param index
     * @return
     * @throws IOException
     */
    public String getMail(int index) throws IOException {
        if (getEmailAdress() == null || emails.length <= index) return null;
        return br.getPage("http://mailin8r.com" + emails[index][1].replaceFirst("showmail", "showmail2"));
    }

    /**
     * new String[][] {{"From", "TargetURL", "Subject"}};
     * 
     * @return
     * @throws IOException
     */
    public String[][] getMailInfos() throws IOException {
        if (emails != null) return emails;
        emails = new Regex(br.getPage("http://mailin8r.com/maildir.jsp?email=" + emailname), "<tr><td bgcolor=\\#EEEEFF><b>(.*?)</b></td><td bgcolor=\\#EEEEFF align=center><a href=(.*?)>(.*?)</a></td></tr>").getMatches();
        return emails;
    }
}