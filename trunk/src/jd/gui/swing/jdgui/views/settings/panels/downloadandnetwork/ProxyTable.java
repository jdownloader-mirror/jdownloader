package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;

import jd.controlling.proxy.ProxyInfo;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.utils.swing.table.ExtColumn;

public class ProxyTable extends BasicJDTable<ProxyInfo> {

    /**
     * 
     */
    private static final long serialVersionUID = 1153823766916158314L;

    public ProxyTable() {
        super(new ProxyTableModel());
        setSearchEnabled(true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    public void update() {
        ((ProxyTableModel) getExtTableModel()).fill();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.table.ExtTable#onContextMenu(javax.swing.JPopupMenu
     * , java.lang.Object, java.util.ArrayList,
     * org.appwork.utils.swing.table.ExtColumn)
     */
    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, ProxyInfo contextObject, ArrayList<ProxyInfo> selection, ExtColumn<ProxyInfo> column) {
        popup.add(new JMenuItem(new ProxyAddAction()));
        popup.add(new JMenuItem(new ProxyDeleteAction(selection, false)));
        return popup;
    }

    @Override
    protected boolean onShortcutDelete(final ArrayList<ProxyInfo> selectedObjects, final KeyEvent evt, final boolean direct) {
        new ProxyDeleteAction(selectedObjects, direct).actionPerformed(null);
        return true;
    }

}
