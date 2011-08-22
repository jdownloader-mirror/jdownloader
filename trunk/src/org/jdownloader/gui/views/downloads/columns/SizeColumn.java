package org.jdownloader.gui.views.downloads.columns;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.swing.exttable.columns.ExtFileSizeColumn;
import org.jdownloader.gui.translate._GUI;

public class SizeColumn extends ExtFileSizeColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SizeColumn() {
        super(_GUI._.SizeColumn_SizeColumn());

    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 65;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 83;
    // }

    @Override
    protected long getBytes(PackageLinkNode o2) {
        if (o2 instanceof DownloadLink) {
            return ((DownloadLink) o2).getDownloadSize();
        } else {
            return ((FilePackage) o2).getTotalEstimatedPackageSize();
        }
    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
    }

}
