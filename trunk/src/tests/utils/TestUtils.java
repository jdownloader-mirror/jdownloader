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

package tests.utils;

import java.util.HashMap;
import java.util.logging.Level;

import javax.swing.JFrame;

import jd.DecryptPluginWrapper;
import jd.JDInit;
import jd.Main;
import jd.OptionalPluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.userio.SimpleUserIO;
import jd.http.Browser;
import jd.nutils.OSDetector;
import jd.parser.html.Form;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.MacOSController;
import jd.utils.WebUpdate;

public abstract class TestUtils {
/**
 * Normal  usual downloadlink
 */
    public static final String HOSTER_LINKTYPE_NORMAL = "NORMAL_DOWNLOADLINK_";
    /**
     * File not found downloadlink. invalid URL. NOT ABUSED!
     */
    public static final String HOSTER_LINKTYPE_FNF= "FNF_DOWNLOADLINK_";
    /**
     * Very very tiny download. just a few kb or less
     */
    public static final String HOSTER_LINKTYPE_TINY = "TINY_DOWNLOADLINK_";
    /**
     * Bigger than normaldownloadlinks
     */
    public static final String HOSTER_LINKTYPE_OVERSIZE = "OVERSIZE_DOWNLOADLINK_";
    /**
     * Link abused
     */
    public static final String HOSTER_LINKTYPE_ABUSED = "ABUSED_DOWNLOADLINK_";
    /**
     * Serverside error
     */
    public static final String HOSTER_LINKTYPE_ERROR_HARDWARE= "HARDWARE_ERROR_DOWNLOADLINK_";
    /**
     * Tempora. unavailable
     */
    public static final String HOSTER_LINKTYPE_ERROR_TEMP = "TEM_ERROR_DOWNLOADLINK_";
    

    private static JFrame FRAME;

    private static JDInit jdi;

    private static String WIKI_USER;

    private static String WIKI_PASS;

    /**
     * Returns a stored property or asks to enter it
     * 
     * @param string
     * @return
     */
    public static String getStringProperty(String string) {
        SubConfiguration cfg = SubConfiguration.getConfig("UNITTEST");
        String ret = cfg.getStringProperty(string);

        ret = SimpleUserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, "PLease enter String", string, ret, null, null, null);
        ;
        cfg.setProperty(string, ret);
        cfg.save();
        return ret;
    }

    public static int getIntegerProperty(String string) {
        SubConfiguration cfg = SubConfiguration.getConfig("UNITTEST");
        int ret = cfg.getIntegerProperty(string);

        ret = Integer.parseInt(SimpleUserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, "Please enter Integer", string, ret + "", null, null, null));
        ;

        cfg.setProperty(string, ret);
        cfg.save();
        return ret;
    }

    public static boolean ask(String question) {
        return SimpleUserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, "We need to know if..?", question, null, null, null) == UserIO.RETURN_OK;

    }

    public static void initJD() {
        TestUtils.mainInit();
        TestUtils.initGUI();
        TestUtils.initDecrypter();
        TestUtils.initContainer();
        TestUtils.initHosts();
        TestUtils.finishInit();
        // JDLogger.getLogger().setLevel(Level.ALL);
    }

    public static String log(String msg) {
        System.out.println(new Exception().getStackTrace()[1].toString() + " : " + msg);
        return new Exception().getStackTrace()[1].toString() + " : " + msg;
    }

    public static void mainInit() {
        if (JDUtilities.getController() != null) return;

        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                FRAME = new JFrame();
                FRAME.setVisible(false);
                return null;
            }
        }.waitForEDT();

        // frame.setAlwaysOnTop(true);
        System.setProperty("file.encoding", "UTF-8");
        // Mac specific //
        if (OSDetector.isMac()) {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "jDownloader");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            new MacOSController();
        }

        Interaction.initTriggers();

        JDTheme.setTheme("default");

        jdi = new JDInit();
        jdi.init();

        if (jdi.loadConfiguration() == null) {
            UserIO.getInstance().requestMessageDialog("JDownloader cannot create the config files. Make sure, that JD_HOME/config/ exists and is writeable");
        }

        JDUtilities.setController(JDController.getInstance());
    }

    public static void initDecrypter() {
        if (DecryptPluginWrapper.getDecryptWrapper().size() > 0) return;

        jdi.loadPluginForDecrypt();
    }

    public static void initHosts() {
        if (JDUtilities.getPluginsForHost().size() > 0) return;

        jdi.loadPluginForHost();
    }

    public static void initOptionalPlugins() {
        if (OptionalPluginWrapper.getOptionalWrapper().size() > 0) return;

        jdi.loadPluginOptional();
    }

    public static void initAllPlugins() {
        initDecrypter();
        initHosts();
        initOptionalPlugins();
    }

    public static void initGUI() {
        if (SimpleGUI.CURRENTGUI != null) return;

        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                UserIO.setInstance(SimpleUserIO.getInstance());
                jdi.initGUI(JDUtilities.getController());
                return null;
            }
        }.waitForEDT();

        // SimpleGUI.CURRENTGUI.setVisible(false);
    }

    public static void initControllers() {
        jdi.initControllers();
    }

    public static void finishInit() {
      
        try {
           Main.loadDynamics();
        } catch (Exception e1) {
            JDLogger.exception(Level.FINEST, e1);
        }
        new WebUpdate().doUpdateCheck(true, false);
        JDUtilities.getController().setInitStatus(JDController.INIT_STATUS_COMPLETE);
        JDUtilities.getController().fireControlEvent(new ControlEvent(new Object(), ControlEvent.CONTROL_INIT_COMPLETE, null));
    }

    /**
     * Returns a hashmap of examplelinks. See
     * http://jdownloader.net:8081/knowledge/wiki/development/intern/testlinks/
     * 
     * Musthave: NORMAL_DOWNLOADLINK_1 FNF_DOWNLOADLINK_1
     * 
     * @param string
     * @return
     */
    public static HashMap<String, String> getHosterLinks(String string) {
        HashMap<String, String> ret = new HashMap<String, String>();
        try {

            Browser br = new Browser();
            br.setFollowRedirects(true);
            br.setDebug(true);
            br.getPage("http://jdownloader.net:8081/knowledge/wiki/development/intern/testlinks/hoster/" + string + "?lng=en");
            String login = br.getRegex("(http://jdownloader.net:8081/knowledge/wiki/development/intern/testlinks/hoster/" + string + "\\?do=login\\&amp\\;sectok=.*?)\"").getMatch(0);
            br.getPage(login);

            Form form = br.getForm(2);
            if (WIKI_USER == null) {
                WIKI_USER = getStringProperty("JD_WIKI_USER");
                WIKI_PASS = getStringProperty("JD_WIKI_PASS");
            }
            form.put("u", WIKI_USER);
            form.put("p", WIKI_PASS);
            br.submitForm(form);
            String[][] matches = br.getRegex("<div class=\"li\"> <a href=\"(.*?)\" class=\"urlextern\" target=\"_blank\" title=\".*?\"  rel=\"nofollow\">(.*?)</a>").getMatches();
            if (matches == null) return ret;
            for (String[] m : matches) {
                if (!m[0].trim().equalsIgnoreCase("http://downloadlink")) {
                    ret.put(m[1].trim(), m[0].trim());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
        return ret;

    }
/**
 * 

    *
      NORMAL_DECRYPTERLINK_1

    *
      DLC_DECRYPTER_LINK_1

    *
      PASSWORD_PROTECTED_1:12345
    *
      PASSWORD_PROTECTED_2:12345

    *
      CAPTCHA_DECRYPTER_1
    *
      FOLDER_DECRYPTER_1


 * @param string
 * @return
 */
    public static  HashMap<String, String> getDecrypterLinks(String string) {
        HashMap<String, String> ret = new HashMap<String, String>();
        try {

            Browser br = new Browser();
            br.setFollowRedirects(true);
            br.setDebug(true);
            br.getPage("http://jdownloader.net:8081/knowledge/wiki/development/intern/testlinks/decrypter/" + string + "?lng=en");
            String login = br.getRegex("(http://jdownloader.net:8081/knowledge/wiki/development/intern/testlinks/decrypter/" + string + "\\?do=login\\&amp\\;sectok=.*?)\"").getMatch(0);
            br.getPage(login);

            Form form = br.getForm(2);
            if (WIKI_USER == null) {
                WIKI_USER = getStringProperty("JD_WIKI_USER");
                WIKI_PASS = getStringProperty("JD_WIKI_PASS");
            }
            form.put("u", WIKI_USER);
            form.put("p", WIKI_PASS);
            br.submitForm(form);
            String[][] matches = br.getRegex("<div class=\"li\"> <a href=\"(.*?)\" class=\"urlextern\" target=\"_blank\" title=\".*?\"  rel=\"nofollow\">(.*?)</a>").getMatches();
            if (matches == null) return ret;
            for (String[] m : matches) {
                if (!m[0].trim().equalsIgnoreCase("http://decryptlink")) {
                    ret.put(m[1].trim(), m[0].trim());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
        return ret;

    }

public static void initContainer() {
    jdi.loadCPlugins();
    
}
}