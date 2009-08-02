package jd.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import jd.config.Property;
import jd.controlling.JDLogger;
import jd.utils.JDTheme;

/**
 * This abstract class is the parent class for all actions in JDownloader
 * 
 * @author Coalado
 * 
 */
public abstract class JDAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = -2332356042161170120L;
    public static final String IMAGE_KEY = "IMAGE_KEY";
    private ActionListener actionListener;
    private int actionID = -1;
    private Property properties;

    /**
     * 
     * @param title
     *            name of the action
     * @param actionID
     *            optional action id
     */
    public JDAction(String title, int actionID) {
        super(title);
        this.actionID = actionID;

    }

    /**
     * 
     * @param l
     *            name of the action
     * @param ii
     *            icon of the action
     */
    public JDAction(String l, ImageIcon ii) {
        super(l, ii);

    }

    /**
     * 
     * @param l
     *            Name of the Action
     */

    public JDAction(String l) {
        this(l, -1);

    }

    // public String getAccelerator() {
    // KeyStroke stroke = ((KeyStroke) getValue(ACCELERATOR_KEY));
    // if (stroke == null) return null;
    // return stroke.getKeyChar() + "";
    // }

    /**
     * @param key
     *            A JDTHeme Icon Key
     */
    public void setIcon(String key) {
        if (key.length() < 3) return;
        putValue(AbstractAction.SMALL_ICON, JDTheme.II(key, 24, 24));
        putValue(IMAGE_KEY, key);
    }

    /**
     * Sets the Mnemonic for this icon. Mnemonics are used to activate actions
     * using the keyboard (ALT + Mnemonic) usualy the mnemonic is part of the
     * name, and thus gets underlined in menus.
     * 
     * Always set the Mnemonic AFTER! setting the title
     * 
     * @param key
     */
    public void setMnemonic(String key) {
        char mnemonic = key.charAt(0);

        if (mnemonic != 0 && !key.contentEquals("-")) {
            Class<?> b = KeyEvent.class;
            Field f;
            try {
                f = b.getField("VK_" + Character.toUpperCase(mnemonic));
                int m = (Integer) f.get(null);
                putValue(AbstractAction.MNEMONIC_KEY, m);

                putValue(AbstractAction.DISPLAYED_MNEMONIC_INDEX_KEY, getTitle().indexOf(m));
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }

    /**
     * Returns the actions description
     */
    public String getTooltipText() {
        try {
            return getValue(AbstractAction.LONG_DESCRIPTION).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the KeyStroke set by setAccelerator;
     * 
     * @return
     */
    public KeyStroke getKeyStroke() {
        Object ret = getValue(ACCELERATOR_KEY);
        if (ret != null) { return (KeyStroke) ret; }
        return null;
    }

    /**
     * Sets the shortcut fort this action. a System dependend behaviour is
     * choosen. e,g. WIndows+ Strg+ Acceleratir
     * 
     * example: action.setAccelerator("ENTER"); defines a Enter shortcut
     * 
     * @param accelerator
     */
    public void setAccelerator(String accelerator) {
        String org = accelerator;
        KeyStroke ks;
        if (accelerator != null && accelerator.length() > 0 && !accelerator.equals("-")) {
            Class<?> b = KeyEvent.class;
            if (accelerator.contains("+")) accelerator = accelerator.substring(accelerator.lastIndexOf("+") + 1);
            Field f;
            try {
                f = b.getField("VK_" + accelerator.toUpperCase());
                int m = (Integer) f.get(null);

                putValue(AbstractAction.ACCELERATOR_KEY, ks = KeyStroke.getKeyStroke(m, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                JDLogger.getLogger().finest(this.getTitle() + " Shortcuts: mapped " + org + " to " + ks);
            } catch (Exception e) {
                putValue(AbstractAction.ACCELERATOR_KEY, ks = KeyStroke.getKeyStroke(accelerator.charAt(accelerator.length() - 1), Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                JDLogger.getLogger().finest(this.getTitle() + "Shortcuts: mapped " + org + " to " + ks);
            }

        }
    }

    /**
     * Returns the action's id
     * 
     * @return
     */
    public int getActionID() {

        return actionID;
    }

    /**
     * a action may have a actionlistener defined. alternativly the
     * actionPerformed method may be overridden
     * 
     * @return
     */
    public ActionListener getActionListener() {
        return actionListener;
    }

    /**
     * Returns the action's name
     * 
     * @return
     */
    public String getTitle() {
        return (String) getValue(NAME);
    }

    /**
     * For toggle actions, this method returns if it is currently selected
     * 
     * @return
     */
    public boolean isSelected() {
        Object value = getValue(SELECTED_KEY);
        if (value == null) {
            putValue(SELECTED_KEY, false);
            return false;
        }
        return (Boolean) value;

    }

    /**
     * Sets the actionlistener. see getActionListener() for details
     * 
     * @param actionListener
     * @return
     */
    public JDAction setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;

        return this;
    }

    /**
     * Sets the action selected. WARNING. Swing usualy handles the selection
     * state
     * 
     * @param selected
     */
    public void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

    /**
     * Sets the actions title
     * 
     * @param title
     * @return
     */
    public JDAction setTitle(String title) {
        putValue(NAME, title);
        return this;
    }

    /**
     * Sets the ac tions icon
     * 
     * @param ii
     * @return
     */
    public JDAction setIcon(ImageIcon ii) {
        putValue(SMALL_ICON, ii);
        return this;

    }

    /**
     * Returns the Actions icon
     * 
     * @return
     */
    public ImageIcon getIcon() {
        return (ImageIcon) getValue(SMALL_ICON);
    }

    /**
     * A action uses an intern jd.config.Property see jd.config.Property for
     * infos about this delegate
     * 
     * @param string
     * @param value
     */
    public void setProperty(String string, Object value) {
        if (properties == null) properties = new Property();
        this.firePropertyChange(string, getProperty(string), value);
        properties.setProperty(string, value);

    }

    /**
     * A action uses an intern jd.config.Property see jd.config.Property for
     * infos about this delegate
     * 
     * @param string
     * @param value
     */
    public Object getProperty(String string) {
        if (properties == null) properties = new Property();
        return properties.getProperty(string);
    }

    /**
     * A action uses an intern jd.config.Property see jd.config.Property for
     * infos about this delegate
     * 
     * @param string
     * @param value
     */
    public <E> E getGenericProperty(String key, E def) {

        if (properties == null) properties = new Property();
        return properties.getGenericProperty(key, def);
    }

}
