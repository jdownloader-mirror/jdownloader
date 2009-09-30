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

package jd.gui.swing.components.table;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import jd.config.SubConfiguration;
import jd.gui.swing.Factory;
import jd.gui.swing.components.JDTextField;
import jd.gui.swing.components.JExtCheckBoxMenuItem;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;

class SortMenuItem extends JMenuItem implements ActionListener {

    private static final long serialVersionUID = 6328630034846759725L;
    private Object obj = null;
    private JDTableColumn column = null;
    private static String defaultString = JDL.L("gui.table.contextmenu.sort", " Sort");

    public SortMenuItem() {
        super(defaultString);
        this.setIcon(JDTheme.II("gui.images.sort", 16, 16));
        this.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (column == null) return;
        if (column.isSortable(obj)) column.doSort(obj);
    }

    public void set(JDTableColumn column, Object obj, String desc) {
        if (desc == null) desc = defaultString;
        this.column = column;
        this.obj = obj;
        this.setText(desc);
    }
}

public class JDTable extends JXTable {

    /**
     * 
     */
    private static final long serialVersionUID = -6631229711568284941L;
    private JDTableModel model;
    private SubConfiguration tableconfig;
    private SortMenuItem defaultSortMenuItem;
    public static final int ROWHEIGHT = 19;

    public JDTable(JDTableModel model) {
        super(model);
        this.model = model;
        model.setJDTable(this);
        tableconfig = model.getConfig();
        createColumns();
        setSortable(false);
        setShowHorizontalLines(false);
        setShowVerticalLines(false);
        UIManager.put("Table.focusCellHighlightBorder", null);
        defaultSortMenuItem = new SortMenuItem();
        getTableHeader().addMouseListener(new JDMouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int col = realColumnAtPoint(e.getPoint());
                    if (getJDTableModel().getJDTableColumn(col).isSortable(null)) getJDTableModel().getJDTableColumn(col).doSort(null);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    JCheckBoxMenuItem[] mis = new JCheckBoxMenuItem[getJDTableModel().getColumnCount()];

                    for (int i = 0; i < getJDTableModel().getColumnCount(); ++i) {
                        final int j = i;
                        final JExtCheckBoxMenuItem mi = new JExtCheckBoxMenuItem(getJDTableModel().getColumnName(i));
                        mi.setHideOnClick(false);
                        mis[i] = mi;
                        if (i == 0) mi.setEnabled(false);
                        mi.setSelected(getJDTableModel().isVisible(i));
                        mi.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                getJDTableModel().setVisible(j, mi.isSelected());
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

        });
        getTableHeader().setReorderingAllowed(true);
        getTableHeader().setResizingAllowed(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoscrolls(true);
        this.setRowHeight(ROWHEIGHT);
        getTableHeader().setPreferredSize(new Dimension(getColumnModel().getTotalColumnWidth(), 19));
        // This method is 1.6 only
        if (JDUtilities.getJavaVersion() >= 1.6) this.setFillsViewportHeight(true);

        getColumnModel().addColumnModelListener(new TableColumnModelListener() {

            public void columnAdded(TableColumnModelEvent e) {
            }

            public void columnMarginChanged(ChangeEvent e) {
            }

            public void columnMoved(TableColumnModelEvent e) {
                if (e == null) return;
                if (e.getFromIndex() == e.getToIndex()) return;
                TableColumnModel tcm = getColumnModel();
                for (int i = 0; i < tcm.getColumnCount(); i++) {
                    tableconfig.setProperty("POS_COL_" + i, getJDTableModel().getJDTableColumn(tcm.getColumn(i).getModelIndex()).getID());
                }
                tableconfig.save();
            }

            public void columnRemoved(TableColumnModelEvent e) {
            }

            public void columnSelectionChanged(ListSelectionEvent e) {
            }

        });
    }

    public ArrayList<JDRowHighlighter> getJDRowHighlighter() {
        return model.getJDRowHighlighter();
    }

    public void addJDRowHighlighter(JDRowHighlighter high) {
        model.addJDRowHighlighter(high);
    }

    public JDTableModel getJDTableModel() {
        return model;
    }

    private JDTextField searchField;
    private JButton reset;

    public JPanel getFilterField() {
        final String text = "Search after Link";

        JPanel panel = new JPanel(new MigLayout("ins 0"));

        panel.add(searchField = new JDTextField(true), "growx, w 200!");
        searchField.setText(text);

        panel.add(reset = Factory.createButton(null, JDTheme.II("gui.images.undo", 16, 16), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchField.setForeground(Color.LIGHT_GRAY);
                searchField.setText(text);
            }
        }));
        reset.setBorder(null);

        return panel;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int col) {
        return model.getJDTableColumn(convertColumnIndexToModel(col));
    }

    @Override
    public TableCellEditor getCellEditor(int row, int col) {
        return model.getJDTableColumn(convertColumnIndexToModel(col));
    }

    private void createColumns() {
        setAutoCreateColumnsFromModel(false);
        TableColumnModel tcm = getColumnModel();
        while (tcm.getColumnCount() > 0) {
            tcm.removeColumn(tcm.getColumn(0));
        }
        LinkedHashMap<String, TableColumn> columns = new LinkedHashMap<String, TableColumn>();
        for (int i = 0; i < getModel().getColumnCount(); ++i) {
            final int j = i;

            TableColumn tableColumn = new TableColumn(i);
            tableColumn.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("width")) {
                        tableconfig.setProperty("WIDTH_COL_" + model.getJDTableColumn(j).getID(), evt.getNewValue());
                        tableconfig.save();
                    }
                }
            });
            tableColumn.setPreferredWidth(tableconfig.getIntegerProperty("WIDTH_COL_" + model.getJDTableColumn(j).getID(), tableColumn.getWidth()));
            if (!model.isVisible(i)) continue;
            columns.put(model.getJDTableColumn(j).getID(), tableColumn);
        }
        int index = 0;
        while (true) {
            if (columns.isEmpty()) break;
            if (index < getModel().getColumnCount()) {
                String id = tableconfig.getStringProperty("POS_COL_" + index, null);
                index++;
                if (id != null) {
                    TableColumn item = columns.remove(id);
                    if (item != null) addColumn(item);
                }
            } else {
                for (TableColumn ritem : columns.values()) {
                    addColumn(ritem);
                }
                break;
            }
        }
    }

    public int realColumnAtPoint(Point point) {
        int x = columnAtPoint(point);
        return convertColumnIndexToModel(x);
    }

    public void addSortItem(JPopupMenu menu, int colindex, Object obj, String desc) {
        if (menu == null) return;
        JDTableColumn col = model.getJDTableColumn(colindex);
        if (col.isSortable(obj)) {
            defaultSortMenuItem.set(col, obj, desc);
            menu.add(defaultSortMenuItem);
        }
    }

    public Point getPointinCell(Point x) {
        int row = rowAtPoint(x);
        if (row == -1) return null;
        Rectangle cellPosition = getCellRect(row, columnAtPoint(x), true);
        Point p = new Point();
        p.setLocation(x.getX() - cellPosition.getX(), x.getY() - cellPosition.getY());
        return p;
    }

}
