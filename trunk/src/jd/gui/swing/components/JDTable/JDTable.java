package jd.gui.swing.components.JDTable;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import jd.config.SubConfiguration;
import jd.gui.swing.components.JExtCheckBoxMenuItem;
import jd.gui.swing.jdgui.views.downloadview.DownloadTable;
import jd.utils.JDUtilities;

public class JDTable extends JTable implements MouseListener {

    /**
     * 
     */
    private static final long serialVersionUID = -6631229711568284941L;
    private JDTableModel model;
    private SubConfiguration tableconfig;
    private TableColumn[] cols;

    public JDTable(JDTableModel model) {
        super(model);
        this.model = model;
        tableconfig = model.getConfig();
        createColumns();
        setShowHorizontalLines(false);
        setShowVerticalLines(false);
        getTableHeader().addMouseListener(this);
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoscrolls(false);

        this.setRowHeight(DownloadTable.ROWHEIGHT);
        getTableHeader().setPreferredSize(new Dimension(getColumnModel().getTotalColumnWidth(), 19));
        // This method is 1.6 only
        if (JDUtilities.getJavaVersion() >= 1.6) this.setFillsViewportHeight(true);
    }

    public JDTableModel getJDTableModel() {
        return model;
    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        return model.getJDTableColumn(col);
    }

    public TableCellEditor getCellEditor(int row, int column) {
        return model.getJDTableColumn(column);
    }

    public void createColumns() {
        setAutoCreateColumnsFromModel(false);
        TableColumnModel tcm = getColumnModel();
        while (tcm.getColumnCount() > 0) {
            tcm.removeColumn(tcm.getColumn(0));
        }
        cols = new TableColumn[getModel().getColumnCount()];
        for (int i = 0; i < getModel().getColumnCount(); ++i) {
            final int j = i;
            TableColumn tableColumn = new TableColumn(i);
            cols[i] = tableColumn;
            tableColumn.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("width")) {
                        tableconfig.setProperty("WIDTH_COL_" + model.getRealColumnName(model.toModel(j)), evt.getNewValue());
                        tableconfig.save();
                    }
                }
            });
            tableColumn.setPreferredWidth(tableconfig.getIntegerProperty("WIDTH_COL_" + model.getRealColumnName(model.toModel(j)), tableColumn.getWidth()));
            addColumn(tableColumn);
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == getTableHeader()) {
            if (e.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu popup = new JPopupMenu();
                JCheckBoxMenuItem[] mis = new JCheckBoxMenuItem[model.getRealColumnCount()];

                for (int i = 0; i < model.getRealColumnCount(); ++i) {
                    final int j = i;
                    final JExtCheckBoxMenuItem mi = new JExtCheckBoxMenuItem(model.getRealColumnName(i));
                    mi.setHideOnClick(false);
                    mis[i] = mi;
                    if (i == 0) mi.setEnabled(false);
                    mi.setSelected(model.isVisible(i));
                    mi.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            model.setVisible(j, mi.isSelected());
                            createColumns();
                            revalidate();
                            repaint();
                        }

                    });
                    popup.add(mi);
                }
                popup.show(getTableHeader(), e.getX(), e.getY());
            }
        }

    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}
