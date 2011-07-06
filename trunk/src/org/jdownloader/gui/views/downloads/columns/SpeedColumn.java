package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;

import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class SpeedColumn extends ExtTextColumn<jd.plugins.PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SpeedColumn() {
        super(_GUI._.SpeedColumn_SpeedColumn());
    }

    @Override
    protected Icon getIcon(PackageLinkNode value) {

        return null;
    }

    @Override
    public String getStringValue(PackageLinkNode value) {

        if (value instanceof DownloadLink) {
            if (((DownloadLink) value).getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                if (((DownloadLink) value).getDownloadSpeed() > 0) {
                    return Formatter.formatReadable(((DownloadLink) value).getDownloadSpeed()) + "/s";
                } else {
                    return _JDT._.gui_download_create_connection();
                }

            }

        } else if (value instanceof FilePackage) {

        }
        return null;
    }

}
