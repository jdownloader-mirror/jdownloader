package jd.plugins;

import jd.gui.UserIO;
import jd.gui.swing.components.Balloon;
import jd.utils.locale.JDL;
/**
 * LIttle Helper class for often used PLugin issues
 * @author Coalado
 *
 */
public class PluginUtils {
    /**
     * Asks the user to entere a password for plugin
     */
    public static String askPassword(Plugin plg) {
        return UserIO.getInstance().requestInputDialog(0, JDL.LF("jd.plugins.PluginUtils.askPassword", "Please enter the password for %s", plg.getHost()), "");
    }

    /**
     * Informs the user that the password has been wrong
     * 
     * @param plg
     * @param password
     */
    public static void informPasswordWrong(Plugin plg, String password) {
        Balloon.show(JDL.LF("jd.plugins.PluginUtils.informPasswordWrong.title", "Password wrong: %s", password), UserIO.getInstance().getIcon(UserIO.ICON_ERROR), JDL.LF("jd.plugins.PluginUtils.informPasswordWrong.message", "The password you entered for %s has been wrong.", plg.getHost()));

    }

}
