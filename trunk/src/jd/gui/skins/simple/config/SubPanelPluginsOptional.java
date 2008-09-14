//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JLinkButton;
import jd.plugins.PluginOptional;
import jd.utils.GetExplorer;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 * 
 */
public class SubPanelPluginsOptional extends ConfigPanel implements ActionListener, MouseListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDLocale.L("gui.config.plugin.optional.column_status", "Status");
            case 1:
                return JDLocale.L("gui.config.plugin.optional.column_plugin", "Plugin");
            case 2:
                return JDLocale.L("gui.config.plugin.optional.column_version", "Version");
            case 3:
                return JDLocale.L("gui.config.plugin.optional.column_author", "Coder");
            case 4:
                return JDLocale.L("gui.config.plugin.optional.column_needs", "Needs");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return pluginsOptional.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return configuration.getBooleanProperty(getConfigParamKey(pluginsOptional.get(rowIndex).getPlugin()), false) ? JDLocale.L("gui.config.plugin.optional.statusActive", "An") : JDLocale.L("gui.config.plugin.optional.statusInactive", "Aus");
            case 1:
                return pluginsOptional.get(rowIndex).getPlugin().getHost();
            case 2:
                return pluginsOptional.get(rowIndex).getVersion();
            case 3:
                return pluginsOptional.get(rowIndex).getCoder();
            case 4:
                return pluginsOptional.get(rowIndex).getPlugin().getRequirements();
            }
            return null;
        }
    }

    private static final long serialVersionUID = 5794208138046480006L;

    private JButton btnEdit;

    private Configuration configuration;

    private JButton enableDisable;

    private JButton openPluginDir;

    private ArrayList<OptionalPluginWrapper> pluginsOptional;

    private JTable table;

    private InternalTableModel tableModel;

    public SubPanelPluginsOptional(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        pluginsOptional = OptionalPluginWrapper.getOptionalWrapper();
        Collections.sort(pluginsOptional);
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnEdit) {
            editEntry();
        } else if (e.getSource() == enableDisable) {
            int rowIndex = table.getSelectedRow();
            boolean b = configuration.getBooleanProperty(getConfigParamKey(pluginsOptional.get(rowIndex).getPlugin()), false);
            configuration.setProperty(getConfigParamKey(pluginsOptional.get(rowIndex).getPlugin()), !b);
            tableModel.fireTableRowsUpdated(rowIndex, rowIndex);
            table.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
        } else if (e.getSource() == openPluginDir) {
            try {
                new GetExplorer().openExplorer(JDUtilities.getResourceFile("plugins"));
            } catch (Exception ec) {
            }
        }
    }

    private void editEntry() {
        SimpleGUI.showPluginConfigDialog(JDUtilities.getParentFrame(this), pluginsOptional.get(table.getSelectedRow()).getPlugin());
    }

    private String getConfigParamKey(PluginOptional pluginOptional) {
        return "OPTIONAL_PLUGIN_" + pluginOptional.getHost();
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.plugin.optional.name", "Optional Plugins");
    }

    @Override
    public void initPanel() {
        this.setLayout(new BorderLayout());

        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                btnEdit.setEnabled((table.getSelectedRow() >= 0) && pluginsOptional.get(table.getSelectedRow()).hasConfig());
            }
        });
        // table.setDefaultRenderer(Object.class, new
        // PluginTableCellRenderer<PluginOptional>(pluginsOptional));

        TableColumn column = null;
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(70);
                break;
            case 1:
                column.setPreferredWidth(300);
                break;
            }
        }

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));

        btnEdit = new JButton(JDLocale.L("gui.config.plugin.optional.btn_settings", "Einstellungen"));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);

        enableDisable = new JButton(JDLocale.L("gui.config.plugin.optional.btn_toggleStatus", "An/Aus"));
        enableDisable.addActionListener(this);

        openPluginDir = new JButton(JDLocale.L("gui.config.plugin.optional.btn_openDir", "Addon Ordner öffnen"));
        openPluginDir.addActionListener(this);

        int n = 5;
        JPanel contentPanel = new JPanel(new BorderLayout(n, n));
        contentPanel.setBorder(new EmptyBorder(0, n, 0, n));
        contentPanel.add(new JLabel(JDLocale.L("gui.warning.restartNeeded", "JD-Restart needed after changes!")), BorderLayout.NORTH);
        contentPanel.add(scrollpane, BorderLayout.CENTER);

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        bpanel.add(btnEdit);
        bpanel.add(enableDisable);
        bpanel.add(openPluginDir);
        bpanel.add(new JLinkButton(JDLocale.L("gui.config.plugin.optional.linktext_help", "Hilfe"), JDLocale.L("gui.config.plugin.optional.link_help", "  http://jdownloader.org/page.php?id=122")));

        this.add(contentPanel);
        this.add(bpanel, BorderLayout.SOUTH);
    }

    @Override
    public void load() {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1 && pluginsOptional.get(table.getSelectedRow()).hasConfig()) {
            editEntry();
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

    @Override
    public void save() {
    }
}
