package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import org.jdownloader.controlling.LinkFilter;
import org.jdownloader.controlling.LinkFilterController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RemoveAction extends AbstractAction {
    private static final long     serialVersionUID = -477419276505058907L;
    private ArrayList<LinkFilter> selected;
    private FilterTable           table;

    public RemoveAction(FilterTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_remove());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 20));

    }

    public RemoveAction(FilterTable table, ArrayList<LinkFilter> selected, boolean force) {
        this.table = table;
        this.selected = selected;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_remove());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 16));
    }

    public void actionPerformed(ActionEvent e) {
        ArrayList<LinkFilter> remove = selected;
        if (remove == null) {
            remove = table.getExtTableModel().getSelectedObjects();
        }
        if (remove != null) {
            for (LinkFilter lf : remove) {
                LinkFilterController.getInstance().remove(lf);
            }
            table.getExtTableModel()._fireTableStructureChanged(LinkFilterController.getInstance().list(), false);
        }
    }

    @Override
    public boolean isEnabled() {
        return selected != null && selected.size() > 0;
    }

}
