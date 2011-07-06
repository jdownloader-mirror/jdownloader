package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.util.ArrayList;

import javax.swing.JPopupMenu;

import jd.controlling.authentication.AuthenticationInfo;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.utils.swing.table.ExtColumn;

public class AuthTable extends BasicJDTable<AuthenticationInfo> {
    private static final long serialVersionUID = 1L;

    public AuthTable() {
        super(new AuthTableModel());
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, AuthenticationInfo contextObject, ArrayList<AuthenticationInfo> selection, ExtColumn<AuthenticationInfo> col) {

        popup.add(new NewAction(this));
        popup.add(new RemoveAction(this, selection));

        return popup;
    }

}
