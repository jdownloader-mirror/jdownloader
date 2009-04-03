//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.skins.simple.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.PackageManager;
import jd.gui.skins.simple.components.JLinkButton;
import jd.update.PackageData;
import jd.update.WebUpdater;
import jd.utils.JDLocale;
import net.miginfocom.swing.MigLayout;

/**
 * @author JD-Team
 */
public class SubPanelRessources extends ConfigPanel implements ActionListener, PropertyChangeListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDLocale.L("gui.config.packagemanager.column_name", "Paket");
            case 1:
                return JDLocale.L("gui.config.packagemanager.column_category", "Kategorie");
            case 2:
                return JDLocale.L("gui.config.packagemanager.column_info", "Info.");
            case 3:
                return JDLocale.L("gui.config.packagemanager.column_latestVersion", "Akt. Version");
            case 4:
                return JDLocale.L("gui.config.packagemanager.column_installedVersion", "Inst. Version");
            case 5:
                return JDLocale.L("gui.config.packagemanager.column_select", "Auswählen");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return packageData.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PackageData element = packageData.get(rowIndex);

            switch (columnIndex) {
            case 0:
                return element.getStringProperty("name");
            case 1:
                return element.getStringProperty("category");
            case 2:
                return new JLinkButton(JDLocale.L("gui.config.packagemanager.table.info", "Info"), element.getStringProperty("infourl"));
            case 3:
                return element.getStringProperty("version");
            case 4:
                return String.valueOf(element.getInstalledVersion());
            case 5:
                return element.isSelected();
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2 || columnIndex == 5;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 5) {
                PackageData element = packageData.get(row);
                element.setSelected(!element.isSelected());
            }
        }
    }

    private static final long serialVersionUID = 1L;

    private JButton btnReset;

    private ConfigEntriesPanel cep;

    private ArrayList<PackageData> packageData = new ArrayList<PackageData>();

    private JTable table;

    private InternalTableModel tableModel;

    public SubPanelRessources(Configuration configuration) {
        super();
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnReset) {
            for (PackageData pkg : packageData) {
                pkg.setInstalledVersion(0);
                pkg.setUpdating(false);
                pkg.setDownloaded(false);
            }
            tableModel.fireTableDataChanged();
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == cep) {
            cep.save();
        }
    }

    @Override
    public void initPanel() {
        this.setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow][fill]"));

        packageData = new PackageManager().getPackageData();
        Collections.sort(packageData, new Comparator<PackageData>() {
            public int compare(PackageData a, PackageData b) {
                return (a.getStringProperty("category") + " " + a.getStringProperty("name")).compareToIgnoreCase(b.getStringProperty("category") + " " + b.getStringProperty("name"));
                // return ((Integer) a.getSortID()).compareTo((Integer)
                // b.getSortID());
            }
        });

        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

            private static final long serialVersionUID = 1L;
            private Color bgNormal = null;
            private Color bgSelected = null;

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (bgNormal == null) bgNormal = getBackground();
                if (bgSelected == null && isSelected) bgSelected = getBackground();

                PackageData pd = packageData.get(row);
                if (isSelected) {
                    c.setBackground(bgSelected);
                } else if (column == 0 && (pd.getInstalledVersion() != 0 || pd.isSelected()) && pd.getInstalledVersion() < Integer.valueOf(pd.getStringProperty("version"))) {
                    c.setBackground(Color.GREEN);
                } else {
                    c.setBackground(bgNormal);
                }

                return c;
            }

        });

        TableColumn column = null;
        for (int c = 0; c < tableModel.getColumnCount(); ++c) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(250);
                break;
            case 1:
                column.setPreferredWidth(100);
                column.setMaxWidth(150);
                break;
            case 2:
                column.setPreferredWidth(70);
                column.setMaxWidth(70);
                column.setMinWidth(70);
                column.setCellRenderer(JLinkButton.getJLinkButtonRenderer());
                column.setCellEditor(JLinkButton.getJLinkButtonEditor());
                break;
            case 3:
                column.setPreferredWidth(80);
                column.setMaxWidth(100);
                break;
            case 4:
                column.setPreferredWidth(80);
                column.setMaxWidth(100);
                break;
            case 5:
                column.setPreferredWidth(70);
                column.setMaxWidth(70);
                column.setMinWidth(70);
                break;
            }
        }

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(600, 300));

        btnReset = new JButton(JDLocale.L("gui.config.packagemanager.reset", "Addons neu herunterladen"));
        btnReset.addActionListener(this);

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        bpanel.add(btnReset);

        ConfigContainer container = new ConfigContainer(this);
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, WebUpdater.getConfig("JDU"), "SUPPORT_JD", JDLocale.L("gui.config.packagemanager.supportJD", "Support JD by downloading pumped-up-addons")).setDefaultValue(true));
        this.add(cep = new ConfigEntriesPanel(container));
        cep.addPropertyChangeListener(this);
        this.add(scrollpane, "spanx,height :900:,gapleft 10, gapright 10");
        this.add(bpanel, "spanx,gapleft 10, gapright 10");
    }

    @Override
    public void load() {
    }

    @Override
    public void save() {
        cep.save();
    }

}
