package org.jdownloader.gui.toolbar.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class GenericDeleteSelectedToolbarAction extends SelectionBasedToolbarAction {

    public static final String DELETE_DISABLED = "deleteDisabled";
    public static final String DELETE_FAILED   = "deleteFailed";
    public static final String DELETE_FINISHED = "deleteFinished";
    public static final String DELETE_OFFLINE  = "deleteOffline";
    private List<AbstractNode> currentSelection;

    private boolean            deleteDisabled  = false;

    private boolean            deleteFailed    = false;

    private boolean            deleteFinished  = false;

    private boolean            deleteOffline   = false;
    private List<DownloadLink> filteredDownloadLinks;
    private List<CrawledLink>  filteredCrawledLinks;

    public GenericDeleteSelectedToolbarAction() {
        super();
        setIconKey(IconKey.ICON_DELETE);
        // DeleteDisabledSelectedLinks
        setName(_GUI._.DeleteDisabledSelectedLinksToolbarAction_object_());

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (currentSelection != null) {
            if (getTable() != null && getTable() instanceof DownloadsTable) {

                DownloadController.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, filteredDownloadLinks, null, null, e, DownloadsTableModel.getInstance().getTable()), _GUI._.DeleteDisabledLinksFromListAndDiskAction_actionPerformed_object_());

            } else {

                try {
                    if (filteredCrawledLinks.size() > 0) {
                        Dialog.getInstance().showConfirmDialog(0, _GUI._.literally_are_you_sure(), _GUI._.RemoveDisabledAction_actionPerformed_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());

                        LinkCollector.getInstance().removeChildren(filteredCrawledLinks);
                    }
                } catch (DialogNoAnswerException e1) {
                }

            }

        }

    }

    @Override
    protected String createTooltip() {
        return getName();
    }

    @Customizer(name = "Include disabled Links")
    public boolean isDeleteDisabled() {

        return deleteDisabled;
    }

    @Customizer(name = "Include failed")
    public boolean isDeleteFailed() {

        return deleteFailed;
    }

    @Customizer(name = "Include finished Links")
    public boolean isDeleteFinished() {

        return deleteFinished;
    }

    @Customizer(name = "Include Offline Links")
    public boolean isDeleteOffline() {

        return deleteOffline;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    protected void onSelectionUpdate(List<AbstractNode> list) {
        currentSelection = list;
        filteredDownloadLinks = null;
        filteredCrawledLinks = null;

        setEnabled(false);
        if (currentSelection != null) {
            if (getTable() != null && getTable() instanceof DownloadsTable) {
                SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(null, currentSelection, null, null, null, (DownloadsTable) getTable());

                List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
                for (DownloadLink dl : si.getChildren()) {
                    if (isDeleteDisabled() && !dl.isEnabled()) {

                        nodesToDelete.add(dl);
                        continue;
                    }
                    if (isDeleteFailed() && dl.getLinkStatus().isFailed()) {

                        nodesToDelete.add(dl);
                        continue;
                    }
                    if (isDeleteFinished() && dl.getLinkStatus().isFinished()) {
                        nodesToDelete.add(dl);
                        continue;
                    }
                    if (isDeleteOffline() && dl.isAvailabilityStatusChecked() && dl.getAvailableStatus() == AvailableStatus.FALSE) {
                        nodesToDelete.add(dl);
                        continue;
                    }

                }
                filteredDownloadLinks = nodesToDelete;
                setEnabled(filteredDownloadLinks.size() > 0);
            } else if (getTable() != null && getTable() instanceof LinkGrabberTable) {
                SelectionInfo<CrawledPackage, CrawledLink> si = new SelectionInfo<CrawledPackage, CrawledLink>(null, currentSelection, null, null, null, (LinkGrabberTable) getTable());
                List<CrawledLink> filtered = new ArrayList<CrawledLink>();

                for (CrawledLink cl : si.getChildren()) {
                    if (isDeleteDisabled() && !cl.isEnabled()) {
                        filtered.add(cl);
                        continue;
                    }

                    if (isDeleteOffline() && cl.getDownloadLink().isAvailabilityStatusChecked() && cl.getDownloadLink().getAvailableStatus() == AvailableStatus.FALSE) {
                        filtered.add(cl);
                        continue;
                    }

                }
                filteredCrawledLinks = filtered;
                setEnabled(filteredCrawledLinks.size() > 0);
            }
        }

    }

    public void setDeleteDisabled(boolean deleteDisabled) {
        this.deleteDisabled = deleteDisabled;
        updateName();
    }

    public void setDeleteFailed(boolean deleteFailed) {
        this.deleteFailed = deleteFailed;
        updateName();
    }

    public void setDeleteFinished(boolean deleteFinished) {
        this.deleteFinished = deleteFinished;
        updateName();
    }

    public void setDeleteOffline(boolean deleteOffline) {
        this.deleteOffline = deleteOffline;
        updateName();
    }

    private void updateName() {

        if (isDeleteFailed() && isDeleteDisabled() && isDeleteFinished() && isDeleteOffline()) {
            setName(_GUI._.ContextMenuFactory_createPopup_cleanup_only());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(_GUI._.GenericDeleteSelectedToolbarAction_updateName_object_());
            boolean first = true;
            if (isDeleteDisabled()) {
                if (!first) sb.append(" & ");
                sb.append(_GUI._.lit_disabled());
                first = false;
            }
            if (isDeleteFailed()) {
                if (!first) sb.append(" & ");
                first = false;
                sb.append(_GUI._.lit_failed());
            }
            if (isDeleteFinished()) {
                if (!first) sb.append(" & ");
                first = false;
                sb.append(_GUI._.lit_finished());
            }
            if (isDeleteOffline()) {
                if (!first) sb.append(" & ");
                first = false;
                sb.append(_GUI._.lit_offline());
            }
            setName(sb.toString());
        }

    }

}
