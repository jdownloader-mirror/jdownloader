package jd.gui.skins.jdgui.components.downloadview;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import jd.config.SubConfiguration;
import jd.controlling.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.locale.JDL;

public class DownloadJTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    public static final int COL_PART = 0;
    public static final int COL_HOSTER = 1;
    public static final int COL_STATUS = 2;
    public static final int COL_STATUS_DETAILS = 3;
    public static final int COL_PROGRESS = 4;

    protected static final String[] COLUMN_NAMES = {
            JDL.L("gui.treetable.header_1.tree", "F"),
            JDL.L("gui.treetable.header_3.hoster", "Anbieter"),
            JDL.L("gui.treetable.header_4.status", "Status"),
            JDL.L(" jd.gui.skins.jdgui.components.downloadview.DownloadJTableModel.extendedstatus.status", "Details"),
            JDL.L("gui.treetable.header_5.progress", "Fortschritt")
    };

    /**
     * Default widths in px
     * -1 is AUTO
     */
    protected static final int[] COL_WIDTHS = new int[] {
            -1,30, -1, -1, 10

    };
    /**
     * Default visible
     */
    protected static final boolean[] COL_VISIBLE = new boolean[] {
            true, true, true, true, true

    };
    private ArrayList<Object> downloadlist = new ArrayList<Object>();
    private SubConfiguration config;

    public DownloadJTableModel() {
        super();
        config = SubConfiguration.getConfig("gui");
        refreshModel();
    }

    public int getRowCount() {
        return downloadlist.size();
    }

    public int getRowforObject(Object o) {
        synchronized (downloadlist) {
            return downloadlist.indexOf(o);
        }
    }

    public Object getObjectforRow(int row) {
        synchronized (downloadlist) {
            if (row < downloadlist.size()) return downloadlist.get(row);
            return null;
        }
    }

    public void refreshModel() {
        synchronized (DownloadController.ControllerLock) {
            synchronized (DownloadController.getInstance().getPackages()) {
                synchronized (downloadlist) {
                    downloadlist.clear();
                    for (FilePackage fp : DownloadController.getInstance().getPackages()) {
                        downloadlist.add(fp);
                        if (fp.getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false)) {
                            for (DownloadLink dl : fp.getDownloadLinkList()) {
                                downloadlist.add(dl);
                            }
                        }
                    }
                }
            }
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            return downloadlist.get(rowIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int getRealColumnCount() {
        return COLUMN_NAMES.length;
    }

    public String getRealColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public int getColumnCount() {
        int j = 0;
        for (int i = 0; i < COLUMN_NAMES.length; ++i) {
            if (isVisible(i)) ++j;
        }
        return j;
    }

    public boolean isVisible(int column) {
        return config.getBooleanProperty("VISABLE_COL_" + column, true);
    }

    public void setVisible(int column, boolean visible) {
        config.setProperty("VISABLE_COL_" + column, visible);
        config.save();
    }

    public int toModel(int column) {
        int i = 0;
        int k;
        for (k = 0; k < getRealColumnCount(); ++k) {
            if (isVisible(k)) {
                ++i;
            }
            if (i > column) break;
        }
        return k;
    }

    public int toVisible(int column) {
        int i = column;
        int k;
        for (k = column; k >= 0; --k) {
            if (!isVisible(k)) {
                --i;
            }
        }
        return i;
    }

    // @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[toModel(column)];
    }

    // @Override
    public Class<?> getColumnClass(int column) {
        return Object.class;
    }

}
