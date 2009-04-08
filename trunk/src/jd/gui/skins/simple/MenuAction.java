//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.skins.simple;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import jd.nutils.JDImage;
import jd.utils.JDLocale;


public class MenuAction extends AbstractAction {

    public static final int APP_EXIT = 16;
    public static final int APP_RESTART = 161;
    public static final int APP_LOAD_CONTAINER = 15;
//    public static final int APP_LOAD_DLC = 23;
    public static final int APP_LOG = 17;
    public static final int APP_BACKUP = 241;
//    public static final int APP_PAUSE_DOWNLOADS = 22;
//    public static final int APP_RECONNECT = 19;
    public static final int APP_SAVE_DLC = 24;
//    public static final int APP_SHOW_LOG = 11;
//    public static final int APP_START_STOP_DOWNLOADS = 10;
//    public static final int APP_STOP_DOWNLOADS = 12;
//    public static final int APP_UPDATE = 20;

    public static final int ITEMS_ADD = 7;
    public static final int ITEMS_DISABLE = 5;

//    public static final int ITEMS_ENABLE = 6;
    // public static final int EXPAND_ALL = 13;
    // public static final int COLLAPSE_ALL = 14;
//    public static final int ITEMS_MOVE_BOTTOM = 4;
//    public static final int ITEMS_MOVE_DOWN = 2;
//    public static final int ITEMS_MOVE_TOP = 3;
//    public static final int ITEMS_MOVE_UP = 1;
//    public static final int ITEMS_REMOVE = 8;
//    public static final int ITEMS_REMOVE_LINKS = 81;
//    public static final int ITEMS_REMOVE_PACKAGES = 82;

    public static final int HELP = 29;

    public static final int ABOUT = 32;

    public static final int WIKI = 33;

    public static final int CHANGES = 35;

//    public static final int APP_OPEN_OPT_CONFIG = 36;

    // public static final int APP_OPEN_HOST_CONFIG = 37;

    private static final long serialVersionUID = 7393495345332708426L;

    private KeyStroke accelerator;
    private int actionID;
    private ActionListener actionListener;
    private String ressourceName;

    /**
     * Erstellt ein neues JDAction-Objekt
     * 
     * @param actionListener
     *            ein ActionListener
     * @param iconName
     *            Name des Icons
     * @param ressourceName
     *            Name der Resource, aus der die Texte geladen werden sollen
     * @param actionID
     *            ID dieser Aktion
     */
    public MenuAction(ActionListener actionListener, String iconName, String ressourceName, int actionID) {
        super();
        this.ressourceName = ressourceName;
        this.actionID = actionID;
        this.actionListener = actionListener;

        Image image = JDImage.getImage(iconName);
        if (image != null) {
            ImageIcon icon = new ImageIcon(image);
            putValue(Action.SMALL_ICON, icon);
        }
        putValue(Action.SHORT_DESCRIPTION, JDLocale.L("gui.menu." + ressourceName + ".desc", ressourceName));
        putValue(Action.NAME, JDLocale.L("gui.menu." + ressourceName + ".name", ressourceName));
        char mnemonic = JDLocale.L("gui.menu." + ressourceName + ".mnem", "-").charAt(0);

        if (mnemonic != 0 && JDLocale.L("gui.menu." + ressourceName + ".mnem", "-").contentEquals("-") == false) {
            Class<?> b = KeyEvent.class;
            Field f;
            try {
                f = b.getField("VK_" + Character.toUpperCase(mnemonic));
                int m = (Integer) f.get(null);
                putValue(Action.MNEMONIC_KEY, m);
            } catch (Exception e) {

                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
            }
        }
        String acceleratorString = JDLocale.L("gui.menu." + ressourceName + ".accel", "-");
        if (acceleratorString != null && acceleratorString.length() > 0) {
            accelerator = KeyStroke.getKeyStroke(acceleratorString);
        }
    }

    public void actionPerformed(ActionEvent e) {
        actionListener.actionPerformed(new ActionEvent(e.getSource(), actionID, ressourceName));
    }

    public KeyStroke getAccelerator() {
        return accelerator;
    }

    public int getActionID() {
        return actionID;
    }
}
