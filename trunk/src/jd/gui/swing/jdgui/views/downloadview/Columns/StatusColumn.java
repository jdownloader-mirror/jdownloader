package jd.gui.swing.jdgui.views.downloadview.Columns;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTable;

import jd.controlling.JDController;
import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.views.downloadview.StatusLabel;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class StatusColumn extends JDTableColumn {

    /**
     * 
     */
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.downloadview.TableRenderer.";
    private static final long serialVersionUID = 2228210790952050305L;
    private Component co;
    private DownloadLink dLink;
    private StatusLabel statuspanel;
    private int counter = 0;
    private ImageIcon imgFinished;
    private ImageIcon imgFailed;
    private ImageIcon imgExtract;
    private ImageIcon imgPriorityS;
    private ImageIcon imgPriority2;
    private ImageIcon imgPriority1;
    private ImageIcon imgStopMark;
    private ImageIcon imgPriority3;
    private String strStopMark;
    private String strFinished;
    private String strFailed;
    private String strPriorityS;
    private String strPriority1;
    private String strPriority2;
    private String strExtract;
    private String strPriority3;
    private FilePackage fp;
    private StringBuilder sb = new StringBuilder();
    private String strDownloadLinkActive;
    private String strETA;

    public StatusColumn(String name, JDTableModel table) {
        super(name, table);
        statuspanel = new StatusLabel();
        imgFinished = JDTheme.II("gui.images.ok", 16, 16);
        imgFailed = JDTheme.II("gui.images.bad", 16, 16);
        imgExtract = JDTheme.II("gui.images.update_manager", 16, 16);
        imgStopMark = JDTheme.II("gui.images.stopmark", 16, 16);
        imgPriorityS = JDTheme.II("gui.images.priority-1", 16, 16);
        imgPriority1 = JDTheme.II("gui.images.priority1", 16, 16);
        imgPriority2 = JDTheme.II("gui.images.priority2", 16, 16);
        imgPriority3 = JDTheme.II("gui.images.priority3", 16, 16);
        strStopMark = JDL.L(JDL_PREFIX + "stopmark", "Stopmark is set");
        strFinished = JDL.L(JDL_PREFIX + "finished", "Download finished");
        strFailed = JDL.L(JDL_PREFIX + "failed", "Download failed");
        strExtract = JDL.L(JDL_PREFIX + "extract", "Extracting");
        strPriorityS = JDL.L("gui.treetable.tooltip.priority-1", "Low Priority");
        strPriority1 = JDL.L("gui.treetable.tooltip.priority1", "High Priority");
        strPriority2 = JDL.L("gui.treetable.tooltip.priority2", "Higher Priority");
        strPriority3 = JDL.L("gui.treetable.tooltip.priority3", "Highest Priority");
        strDownloadLinkActive = JDL.L("gui.treetable.packagestatus.links_active", "aktiv");
        strETA = JDL.L("gui.eta", "ETA");
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public boolean defaultEnabled() {
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
            statuspanel.setBackground(co.getBackground());
            statuspanel.setEnabled(co.isEnabled());
            statuspanel.setForeground(co.getForeground());
            if (fp.isFinished()) {
                statuspanel.setText("");
            } else if (fp.getTotalDownloadSpeed() > 0) {
                clearSB();
                sb.append('[').append(fp.getLinksInProgress()).append('/').append(fp.size()).append("] ");
                sb.append(strETA).append(' ').append(Formatter.formatSeconds(fp.getETA())).append(" @ ").append(Formatter.formatReadable(fp.getTotalDownloadSpeed())).append("/s");
                statuspanel.setText(sb.toString());
            } else if (fp.getLinksInProgress() > 0) {
                clearSB();
                sb.append(fp.getLinksInProgress()).append('/').append(fp.size()).append(' ').append(strDownloadLinkActive);
                statuspanel.setText(sb.toString());
            } else {
                statuspanel.setText("");
            }
            counter = 0;
            if (fp.isFinished()) {
                statuspanel.setIcon(counter, imgFinished, strFinished);
                counter++;
            } else if (JDController.getInstance().getWatchdog() != null && JDController.getInstance().getWatchdog().isStopMark(value)) {
                statuspanel.setIcon(counter, imgStopMark, strStopMark);
                counter++;
            } else if (fp.getTotalDownloadSpeed() > 0) {

            } else if (fp.getLinksInProgress() > 0) {

            } else {

            }
            statuspanel.clearIcons(counter);
            statuspanel.setBorder(null);
            co = statuspanel;
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            statuspanel.setBackground(co.getBackground());
            statuspanel.setEnabled(co.isEnabled());
            statuspanel.setForeground(co.getForeground());
            statuspanel.setText(dLink.getLinkStatus().getStatusString());
            counter = 0;
            if (JDController.getInstance().getWatchdog() != null && JDController.getInstance().getWatchdog().isStopMark(value)) {
                statuspanel.setIcon(counter, imgStopMark, strStopMark);
                counter++;
            }
            if (dLink.getLinkStatus().getStatusIcon() != null) {
                statuspanel.setIcon(counter, dLink.getLinkStatus().getStatusIcon(), dLink.getLinkStatus().getStatusText());
                counter++;
            } else if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                statuspanel.setIcon(counter, imgFinished, strFinished);
                counter++;
            } else if (dLink.getLinkStatus().isFailed()) {
                statuspanel.setIcon(counter, imgFailed, strFailed);
                counter++;
            }
            if (counter <= StatusLabel.ICONCOUNT && dLink.getPluginProgress() != null && dLink.getPluginProgress().getPercent() > 0.0 && dLink.getPluginProgress().getPercent() < 100.0) {
                statuspanel.setIcon(counter, imgExtract, strExtract);
                counter++;
            }
            if (counter <= StatusLabel.ICONCOUNT) {
                switch (dLink.getPriority()) {
                case 0:
                default:
                    break;
                case -1:
                    statuspanel.setIcon(counter, imgPriorityS, strPriorityS);
                    counter++;
                    break;
                case 1:
                    statuspanel.setIcon(counter, imgPriority1, strPriority1);
                    counter++;
                    break;
                case 2:
                    statuspanel.setIcon(counter, imgPriority2, strPriority2);
                    counter++;
                    break;
                case 3:
                    statuspanel.setIcon(counter, imgPriority3, strPriority3);
                    counter++;
                    break;
                }
            }
            statuspanel.clearIcons(counter);
            statuspanel.setBorder(null);
            co = statuspanel;
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
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
        // TODO Auto-generated method stub

    }

    private void clearSB() {
        sb.delete(0, sb.capacity());
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        if (obj instanceof FilePackage) return ((FilePackage) obj).isEnabled();
        return true;
    }

}
