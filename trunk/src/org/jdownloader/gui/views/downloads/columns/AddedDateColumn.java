package org.jdownloader.gui.views.downloads.columns;

import java.util.Date;

import javax.swing.SwingConstants;

import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.columns.ExtDateColumn;
import org.jdownloader.gui.translate._GUI;

public class AddedDateColumn extends ExtDateColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = -8841119846403017974L;

    public AddedDateColumn() {
        super(_GUI._.added_date_column_title());
        rendererField.setHorizontalAlignment(SwingConstants.CENTER);

    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public int getMaxWidth() {

        return 110;
    }

    @Override
    protected String getBadDateText(PackageLinkNode value) {
        return _GUI._.added_date_column_invalid();
    }

    protected String getDateFormatString() {

        return _GUI._.added_date_column_dateformat();
    }

    @Override
    protected Date getDate(PackageLinkNode node, Date date) {

        if (node.getCreated() <= 0) return null;

        date.setTime(node.getCreated());

        return date;
    }
}
