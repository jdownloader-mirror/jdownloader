//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package tests.singletests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.DecryptPluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class SingleDecrypter {
    private String[] urls = {};
    private HashMap<String, String> links;

    @Before
    public void setUp() {

        TestUtils.mainInit();
        TestUtils.initGUI();
        TestUtils.initDecrypter();
        TestUtils.initContainer();
        TestUtils.initHosts();
        TestUtils.finishInit();
        // JDLogger.getLogger().setLevel(Level.ALL);
        links = TestUtils.getDecrypterLinks("stealth.to");
    }

    @Test
    public void decrypt() {
        String url = links.get("NORMAL_DECRYPTERLINK_1");
        for (Iterator<Entry<String, String>> it = links.entrySet().iterator(); it.hasNext();) {

            Entry<String, String> next = it.next();
            if (next.getKey().equalsIgnoreCase("NORMAL_DECRYPTERLINK_1")) {
                decryptURL(next.getValue());

            } else if (next.getKey().startsWith("PASSWORD_PROTECTED_1:")){
                decryptPWURL(next.getValue(), next.getKey().substring("PASSWORD_PROTECTED_1:".length()));
            }else{
                System.out.println("No Test for "+next.getKey());
            }

        }

    }

    private void decryptPWURL(String url, String pw) {
        boolean found = false;
        System.out.println("Enter password: " + pw);
        for (DecryptPluginWrapper pd : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pd.canHandle(url)) {
                found = true;
                PluginForDecrypt plg = (PluginForDecrypt) pd.getNewPluginInstance();

                CryptedLink[] d = plg.getDecryptableLinks(url);

                try {
                    ProgressController pc;
                    d[0].setProgressController(pc = new ProgressController("test", 10));
                    ArrayList<DownloadLink> a = plg.decryptIt(d[0], pc);

                    if (a.size() > 1 || (a.size() == 1 && a.get(0).getBrowserUrl() != null))
                        assertTrue(true);
                    else {
                        TestUtils.log("Error with url: " + url);
                        assertTrue(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        if (!found) {
            TestUtils.log("Url not found: " + url);
            fail();
        }
    }

    private void decryptURL(String url) {
        boolean found = false;
        for (DecryptPluginWrapper pd : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pd.canHandle(url)) {
                found = true;
                PluginForDecrypt plg = (PluginForDecrypt) pd.getNewPluginInstance();

                CryptedLink[] d = plg.getDecryptableLinks(url);

                try {
                    ArrayList<DownloadLink> a = plg.decryptIt(d[0], new ProgressController("test", 10));

                    if (a.size() > 1 || (a.size() == 1 && a.get(0).getBrowserUrl() != null))
                        assertTrue(true);
                    else {
                        TestUtils.log("Error with url: " + url);
                        assertTrue(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        if (!found) {
            TestUtils.log("Url not found: " + url);
            fail();
        }
    }

    @After
    public void tearDown() throws Exception {
        // JDUtilities.getController().exit();
    }
}