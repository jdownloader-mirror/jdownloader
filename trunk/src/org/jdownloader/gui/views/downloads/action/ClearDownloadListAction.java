package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class ClearDownloadListAction extends DeleteAppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 6027982395476716687L;

    public ClearDownloadListAction() {
        super(null);
        setIconKey("clear");
        putValue(SHORT_DESCRIPTION, _GUI._.ClearAction_tt_());
    }

    public void actionPerformed(ActionEvent e) {
        final List<DownloadLink> nodesToDelete = DownloadsTableModel.getInstance().getAllChildrenNodes();
        deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, DownloadsTableModel.getInstance().getTable()), _GUI._.ClearDownloadListAction_actionPerformed_());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
