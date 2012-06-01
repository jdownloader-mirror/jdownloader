package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.FilterRuleDialog;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;

public class FilterTable extends AbstractFilterTable {

    private static final long serialVersionUID = 4698030718806607175L;
    private LinkgrabberFilter linkgrabberFilter;

    public FilterTable(LinkgrabberFilter linkgrabberFilter) {
        super(new FilterTableModel("FilterTable2"));
        this.setSearchEnabled(true);
        this.linkgrabberFilter = linkgrabberFilter;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.exttable.ExtTable#onContextMenu(javax.swing.JPopupMenu , java.lang.Object, java.util.ArrayList,
     * org.appwork.swing.exttable.ExtColumn)
     */
    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, LinkgrabberFilterRule contextObject, ArrayList<LinkgrabberFilterRule> selection, ExtColumn<LinkgrabberFilterRule> column, MouseEvent ev) {
        popup.add(new JMenuItem(new NewAction(this).toContextMenuAction()));
        popup.add(new JMenuItem(new RemoveAction(this, selection, false).toContextMenuAction()));

        popup.add(new JMenuItem(new DuplicateAction(contextObject, this).toContextMenuAction()));
        popup.addSeparator();
        DefaultRulesAction defaults = new DefaultRulesAction(false);
        if (defaults.getMenuComponentCount() > 0) {
            popup.add(defaults);
            popup.addSeparator();
        }
        popup.add(new ExportAction(selection).toContextMenuAction());
        return popup;
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, LinkgrabberFilterRule obj) {
        try {
            Dialog.getInstance().showDialog(new FilterRuleDialog(obj));
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
        getExtTableModel().fireTableDataChanged();
        LinkFilterController.getInstance().update();
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.swing.exttable.ExtTable#onShortcutDelete(java.util.ArrayList , java.awt.event.KeyEvent, boolean)
     */
    @Override
    protected boolean onShortcutDelete(ArrayList<LinkgrabberFilterRule> selectedObjects, KeyEvent evt, boolean direct) {
        new RemoveAction(this, selectedObjects, direct).actionPerformed(null);
        return true;
    }
}
