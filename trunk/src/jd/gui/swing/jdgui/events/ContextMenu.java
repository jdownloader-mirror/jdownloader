package jd.gui.swing.jdgui.events;

import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import jd.gui.swing.jdgui.JDGui;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ContextMenu {

    public ContextMenu() {
    }

    public void dispatchMouseEvent(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        if (e.getComponent() == null) return;
        Component c = null;
        Point point = e.getPoint();
        if (e.getSource() instanceof JDialog) {
            c = SwingUtilities.getDeepestComponentAt((JDialog) e.getSource(), (int) point.getX(), (int) point.getY());
        } else {
            Component source = JDGui.getInstance().getMainFrame().getContentPane();
            point.x -= (source.getLocationOnScreen().x - JDGui.getInstance().getMainFrame().getLocationOnScreen().x);
            point.y -= (source.getLocationOnScreen().y - JDGui.getInstance().getMainFrame().getLocationOnScreen().y);
            c = SwingUtilities.getDeepestComponentAt(source, (int) point.getX(), (int) point.getY());
        }
        if (!(c instanceof JTextComponent)) return;
        if (MenuSelectionManager.defaultManager().getSelectedPath().length > 0) return;
        final JTextComponent t = (JTextComponent) c;

        JPopupMenu menu = new JPopupMenu();

        menu.add(new MenuAbstractAction(t, _GUI._.gui_textcomponent_context_cut(), NewTheme.I().getIcon("cut", 16), _GUI._.gui_textcomponent_context_cut_acc()) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.cut();
            }

            @Override
            public boolean isEnabled() {
                return !(c instanceof JPasswordField);
            }
        });
        menu.add(new MenuAbstractAction(t, _GUI._.gui_textcomponent_context_copy(), NewTheme.I().getIcon("copy", 16), _GUI._.gui_textcomponent_context_copy_acc()) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.copy();
            }

            @Override
            public boolean isEnabled() {
                return !(c instanceof JPasswordField) && c.isEnabled() && c.getSelectedText() != null;
            }
        });
        menu.add(new MenuAbstractAction(t, _GUI._.gui_textcomponent_context_paste(), NewTheme.I().getIcon("paste", 16), _GUI._.gui_textcomponent_context_paste_acc()) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.paste();
            }

            @Override
            public boolean isEnabled() {
                if (c.isEditable() && c.isEnabled()) {
                    Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
                    return contents.isDataFlavorSupported(DataFlavor.stringFlavor);
                } else {
                    return false;
                }
            }
        });
        menu.add(new MenuAbstractAction(t, _GUI._.gui_textcomponent_context_delete(), NewTheme.I().getIcon("delete", 16), _GUI._.gui_textcomponent_context_delete_acc()) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.replaceSelection(null);
            }
        });

        menu.add(new MenuAbstractAction(t, _GUI._.gui_textcomponent_context_selectall(), NewTheme.I().getIcon("select", 16), _GUI._.gui_textcomponent_context_selectall_acc()) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.selectAll();
            }

            @Override
            public boolean isEnabled() {
                return c.isEnabled() && c.getText().length() > 0;
            }
        });

        Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), t);
        menu.show(t, pt.x, pt.y);
    }

    private static abstract class MenuAbstractAction extends AbstractAction {

        private static final long      serialVersionUID = 1L;

        protected final JTextComponent c;

        public MenuAbstractAction(JTextComponent c, String text, ImageIcon icon, String acc) {
            super(text);
            this.c = c;
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
            }

            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(acc));
        }

        @Override
        public boolean isEnabled() {
            return c.isEditable() && c.isEnabled() && c.getSelectedText() != null;
        }

    }

}