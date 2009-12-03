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

package jd.plugins.optional.routersend;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.DefaultListCellRenderer;

import jd.PluginWrapper;
import jd.config.Configuration;
import jd.controlling.reconnect.ReconnectMethod;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = true, id = "routersend", interfaceversion = 5)
public class SendRouter extends PluginOptional implements ControlListener {
    private static final String JDL_PREFIX = "jd.plugins.optional.SendRouter.";
    private String scripturl = "http://127.0.0.1/jd/index.php";
    private String manufactururl = "http://127.0.0.1/jd/hersteller"; // List
                                                                     // separated
                                                                     // by ","
    private String currentScript = "";
    private int reconnectCounter = 0;
    private Boolean send = false;

    public SendRouter(PluginWrapper wrapper) {
        super(wrapper);

    }

    @Override
    public boolean initAddon() {
        reconnectCounter = getPluginConfig().getIntegerProperty("ReconnectCounter", 0);
        currentScript = getPluginConfig().getStringProperty("CurrentScript", "");
        send = getPluginConfig().getBooleanProperty("send", false);
        if (check() == false || send == false) 
            JDUtilities.getController().addControlListener(this);
        return false;
    }

    @Override
    public void onExit() {

    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void controlEvent(ControlEvent e) {
        if (e.getID() == ControlEvent.CONTROL_AFTER_RECONNECT) {
            if (JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) == ReconnectMethod.LIVEHEADER) {
                reconnectCounter++;
                getPluginConfig().setProperty("ReconnectCounter", reconnectCounter);
                getPluginConfig().save();
                if (reconnectCounter > 5 && check()) {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            execute_send();
                        }
                    });

                }
            }
        }
    }

    private void execute_send() {
        int ret = UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDL.L(JDL_PREFIX + "info.topic", "Help to Improve JD"), JDL.L(JDL_PREFIX + "info.msg", "JD have detected that you hade 5 successfull reconnects \r\n You now can send the script to our server so we can include it permanently in JD"), UserIO.getInstance().getIcon(UserIO.ICON_INFO), null, null);
        if (ret != 2) {
            JDUtilities.getController().removeControlListener(this);
            return;
        }
        if (submitdata() == 0) {
            UserIO.getInstance().requestMessageDialog(JDL.L(JDL_PREFIX + "send.successfull", "Thank you for your help"));
            send = true;
            getPluginConfig().setProperty("send", send);
            getPluginConfig().save();
            JDUtilities.getController().removeControlListener(this);
        } else {
            UserIO.getInstance().requestMessageDialog(JDL.L(JDL_PREFIX + "send.failed", "A error eccourd while sending your data.\r\n We will ask you again later."));
            reconnectCounter = 0;
            getPluginConfig().setProperty("ReconnectCounter", reconnectCounter);
            getPluginConfig().save();
            JDUtilities.getController().removeControlListener(this);
        }
    }

    private boolean check() {
        if (!currentScript.equals(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS))) {
            currentScript = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS);
            send = false;
            reconnectCounter = 0;
            getPluginConfig().setProperty("CurrentScript", currentScript);
            getPluginConfig().setProperty("send", send);
            getPluginConfig().setProperty("ReconnectCounter", reconnectCounter);
            getPluginConfig().save();
            return false;
        }
        return true;
    }

    private int submitdata() {
        Form data = new Form();
        data.setMethod(Form.MethodType.POST);
        data.setAction(scripturl);
        String[] Manufactor_List;
        try {
            Manufactor_List = br.getPage(manufactururl).split(",");
        } catch (IOException e) {
            return 1;
        }
        int selection = UserIO.getInstance().requestComboDialog(UserIO.NO_COUNTDOWN | UserIO.NO_CANCEL_OPTION, JDL.L(JDL_PREFIX + "manufactor.list", "Manufactor"), JDL.L(JDL_PREFIX + "manufactor.message", "Which manufactor ?"), Manufactor_List, 0, null, JDL.L(JDL_PREFIX + "ok", "OK"), null, new DefaultListCellRenderer());
        InputField hersteller = new InputField("hersteller", Encoding.urlEncode(Manufactor_List[selection]));
        InputField name = new InputField("name", Encoding.urlEncode(UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.NO_CANCEL_OPTION, JDL.L(JDL_PREFIX + "manufactor.name", "Routername?"), "")));
        InputField script = new InputField("script", Encoding.Base64Encode(currentScript));
        data.addInputField(hersteller);
        data.addInputField(name);
        data.addInputField(script);
        try {
            br.submitForm(data);
            if (br.toString().contains("2")) return 1;
        } catch (Exception e) {
            return 1;
        }
        return 0;
    }
}
