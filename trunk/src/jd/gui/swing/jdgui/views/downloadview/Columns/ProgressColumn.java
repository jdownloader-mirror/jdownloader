//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.downloadview.Columns;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;

import jd.controlling.DownloadController;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.downloadview.JDProgressBar;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class ProgressColumn extends JDTableColumn {

    /**
     * 
     */
    private static final long serialVersionUID = 2228210790952050305L;
    private Component co;
    private DownloadLink dLink;
    private JDProgressBar progress;

    private String strPluginDisabled;

    private String strPluginError;
    private Border ERROR_BORDER;
    private Color COL_PROGRESS_ERROR = new Color(0xCC3300);
    private StringBuilder sb = new StringBuilder();
    private Color COL_PROGRESS_NORMAL = null;
    private FilePackage fp;

    public ProgressColumn(String name, JDTableModel table) {
        super(name, table);
        progress = new JDProgressBar();
        progress.setStringPainted(true);
        progress.setOpaque(true);
        COL_PROGRESS_NORMAL = progress.getForeground();
        strPluginDisabled = JDL.L("gui.downloadlink.plugindisabled", "[Plugin disabled]");
        strPluginError = JDL.L("gui.treetable.error.plugin", "Plugin error");
        ERROR_BORDER = BorderFactory.createLineBorder(COL_PROGRESS_ERROR);
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            fp = (FilePackage) value;
            co = getDefaultTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            progress.setBorder(null);
            progress.setString(null);
            if (fp.isFinished()) {
                progress.setMaximum(100);
                progress.setValue(100);
            } else {
                progress.setMaximum(Math.max(1, fp.getTotalEstimatedPackageSize()));
                progress.setValue(fp.getTotalKBLoaded());
            }
            clearSB();
            sb.append(Formatter.formatReadable(fp.getTotalKBLoaded())).append('/').append(Formatter.formatReadable(Math.max(0, fp.getTotalEstimatedPackageSize())));
            progress.setString(sb.toString());
            progress.setForeground(COL_PROGRESS_NORMAL);
            return progress;
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            progress.setBorder(null);
            if (dLink.getPlugin() == null) {
                co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginError);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (!dLink.getPlugin().getWrapper().usePlugin() && !dLink.getLinkStatus().isPluginActive()) {
                co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setIcon(null);
                ((JRendererLabel) co).setText(strPluginDisabled);
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else if (dLink.getPluginProgress() != null) {
                progress.setMaximum(dLink.getPluginProgress().getTotal());
                progress.setValue(dLink.getPluginProgress().getCurrent());
                progress.setForeground(COL_PROGRESS_NORMAL);
                return progress;
            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && dLink.getPlugin().getRemainingHosterWaittime() > 0) || (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && dLink.getLinkStatus().getRemainingWaittime() > 0)) {
                progress.setMaximum(dLink.getLinkStatus().getTotalWaitTime());
                progress.setForeground(COL_PROGRESS_ERROR);
                progress.setBorder(ERROR_BORDER);
                progress.setString(Formatter.formatSeconds(dLink.getLinkStatus().getRemainingWaittime() / 1000));
                progress.setValue(dLink.getLinkStatus().getRemainingWaittime());
                return progress;
            } else if (dLink.getLinkStatus().isFinished()) {
                clearSB();
                sb.append((Formatter.formatReadable(Math.max(0, dLink.getDownloadSize()))));
                progress.setMaximum(100);
                progress.setString(sb.toString());
                progress.setValue(100);
                progress.setForeground(COL_PROGRESS_NORMAL);
                return progress;
            } else if (dLink.getDownloadCurrent() > 0 || dLink.getDownloadSize() > 0) {
                clearSB();
                sb.append(Formatter.formatReadable(dLink.getDownloadCurrent())).append('/').append(Formatter.formatReadable(Math.max(0, dLink.getDownloadSize())));
                progress.setMaximum(dLink.getDownloadSize());
                progress.setString(sb.toString());
                progress.setValue(dLink.getDownloadCurrent());
                progress.setForeground(COL_PROGRESS_NORMAL);
                return progress;
            }
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setIcon(null);
            ((JRendererLabel) co).setText("Unknown FileSize");
            ((JRendererLabel) co).setBorder(null);
        }
        return co;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isSortable(Object obj) {
        /*
         * DownloadView hat nur null(Header) oder ne ArrayList(FilePackage)
         */
        if (obj == null || obj instanceof ArrayList<?>) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sort(Object obj, final boolean sortingToggle) {
        ArrayList<FilePackage> packages = null;
        synchronized (DownloadController.ControllerLock) {
            synchronized (DownloadController.getInstance().getPackages()) {
                packages = DownloadController.getInstance().getPackages();
                if (obj == null && packages.size() > 1) {
                    /* header, sortiere die packages nach namen */
                    Collections.sort(packages, new Comparator<FilePackage>() {
                        public int compare(FilePackage a, FilePackage b) {
                            FilePackage aa = a;
                            FilePackage bb = b;
                            if (sortingToggle) {
                                aa = b;
                                bb = a;
                            }
                            if (aa.getTotalKBLoaded() == bb.getTotalKBLoaded()) return 0;
                            return aa.getTotalKBLoaded() < bb.getTotalKBLoaded() ? -1 : 1;
                        }
                    });
                } else {
                    /*
                     * in obj stecken alle selektierten packages, sortiere die
                     * links nach namen
                     */
                    if (obj != null) packages = (ArrayList<FilePackage>) obj;
                    for (FilePackage fp : packages) {
                        Collections.sort(fp.getDownloadLinkList(), new Comparator<DownloadLink>() {
                            public int compare(DownloadLink a, DownloadLink b) {
                                DownloadLink aa = b;
                                DownloadLink bb = a;
                                if (sortingToggle) {
                                    aa = a;
                                    bb = b;
                                }
                                if (aa.getDownloadCurrent() == bb.getDownloadCurrent()) return 0;
                                return aa.getDownloadCurrent() < bb.getDownloadCurrent() ? -1 : 1;
                            }
                        });
                    }
                }
            }
        }
        /* inform DownloadController that structure changed */
        DownloadController.getInstance().fireStructureUpdate();
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        if (obj instanceof FilePackage) return ((FilePackage) obj).isEnabled();
        return true;
    }

    private void clearSB() {
        sb.delete(0, sb.capacity());
    }

}
