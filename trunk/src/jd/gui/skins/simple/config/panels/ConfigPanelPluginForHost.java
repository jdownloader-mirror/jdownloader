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

package jd.gui.skins.simple.config.panels;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.HostPluginWrapper;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.config.ConfigPanel;
import jd.nutils.Colors;
import jd.nutils.JDFlags;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.painter.MattePainter;

public class ConfigPanelPluginForHost extends ConfigPanel implements ActionListener, MouseListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDL.L("gui.column_host", "Host");
            case 1:
                return JDL.L("gui.column_version", "Version");
            case 2:
                return JDL.L("gui.column_agbChecked", "akzeptieren");
            case 3:
                return JDL.L("gui.column_usePlugin", "verwenden");
            case 4:
                return JDL.L("gui.column_agb", "AGB");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return pluginsForHost.size();
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            switch (columnIndex) {
            case 0:
                return pluginsForHost.get(rowIndex).getHost();
            case 1:
                return pluginsForHost.get(rowIndex).getVersion();
            case 2:
                return pluginsForHost.get(rowIndex).isAGBChecked();
            case 3:
                return pluginsForHost.get(rowIndex).usePlugin();
            case 4:
                JLinkButton ret = new JLinkButton(new AbstractAction(JDL.L("gui.config.plugin.host.readAGB", "AGB")) {

                    private static final long serialVersionUID = 5915595466511261075L;

                    public void actionPerformed(ActionEvent e) {
                        try {
                            JLinkButton.openURL(pluginsForHost.get(rowIndex).getPlugin().getAGBLink());
                        } catch (Exception e1) {
                            JDLogger.exception(e1);
                        }

                    }
                });
                ret.setLinkColor(null);
                ret.setOpaque(true);
                return ret;
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex >= 2;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 2) {
                if ((Boolean) value) {
                    String ttl = JDL.L("userio.countdownconfirm", "Please confirm");
                    String msg = JDL.L("gui.config.plugin.host.desc", "Das JD Team übernimmt keine Verantwortung für die Einhaltung der AGB <br> der Hoster. Bitte lesen Sie die AGB aufmerksam und aktivieren Sie das Plugin nur,\r\nfalls Sie sich mit diesen Einverstanden erklären!\r\nDie Reihenfolge der Plugins bestimmt die Prioritäten der automatischen Mirrorauswahl\n\rBevorzugte Hoster sollten oben stehen!") + "\r\n\r\n" + JDL.LF("gui.config.plugin.abg_confirm", "Ich habe die AGB/TOS/FAQ von %s gelesen und erkläre mich damit einverstanden!", pluginsForHost.get(row).getHost());
                    if (JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(0, ttl, msg, UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), null, null), UserIO.RETURN_OK)) {
                        pluginsForHost.get(row).setAGBChecked((Boolean) value);
                    }
                } else {
                    pluginsForHost.get(row).setAGBChecked((Boolean) value);
                }
            } else if (col == 3) {
                pluginsForHost.get(row).setUsePlugin((Boolean) value);
            }
        }
    }

    private static final long serialVersionUID = -5219586497809869375L;

    private JButton btnEdit;

    private JButton btnLoad;

    private ArrayList<HostPluginWrapper> pluginsForHost;

    private JXTable table;

    private InternalTableModel tableModel;

    public ConfigPanelPluginForHost(Configuration configuration) {
        super();
        pluginsForHost = JDUtilities.getPluginsForHost();
        Collections.sort(pluginsForHost);
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnEdit) {
            editEntry();
        } else if (e.getSource() == btnLoad) {
            loadEntry();
        }
    }

    private void editEntry(HostPluginWrapper hpw) {
        hpw.getPlugin().getConfig().setGroup(new ConfigGroup(hpw.getPlugin().getHost(), JDTheme.II("gui.images.taskpanes.premium", 24, 24)));
        SimpleGUI.displayConfig(hpw.getPlugin().getConfig(), false);
    }

    private void editEntry() {
        editEntry(pluginsForHost.get(table.getSelectedRow()));
    }

    private void loadEntry(HostPluginWrapper hpw) {
        int cur = table.getSelectedRow();
        hpw.getPlugin();
        tableModel.fireTableRowsUpdated(cur, cur);
        btnEdit.setEnabled(hpw.hasConfig());
        btnLoad.setEnabled(false);
    }

    private void loadEntry() {
        loadEntry(pluginsForHost.get(table.getSelectedRow()));
    }

    @Override
    public void initPanel() {
        tableModel = new InternalTableModel();
        table = new JXTable(tableModel);
        table.setSortable(false);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedRow() < 0) return;
                HostPluginWrapper hpw = pluginsForHost.get(table.getSelectedRow());
                btnEdit.setEnabled(hpw.hasConfig());
                btnLoad.setEnabled(!hpw.isLoaded());
            }
        });
        table.getTableHeader().setReorderingAllowed(false);

        PainterHighlighter highlighter = new PainterHighlighter(new HighlightPredicate() {
            public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                return pluginsForHost.get(adapter.row).hasConfig();
            }
        });
        highlighter.setPainter(new MattePainter(Colors.getColor(getBackground().darker(), 50)));

        table.addHighlighter(highlighter);
        table.addHighlighter(new PainterHighlighter(HighlightPredicate.ROLLOVER_ROW, new MattePainter(Colors.getColor(getBackground().brighter(), 50))));

        TableColumn column = null;

        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(150);
                break;
            case 1:
                column.setPreferredWidth(60);
                column.setMinWidth(60);
                break;
            case 2:
                column.setPreferredWidth(90);
                column.setMaxWidth(90);
                column.setMinWidth(90);
                break;
            case 3:
                column.setPreferredWidth(100);
                break;
            case 4:
                column.setPreferredWidth(70);
                column.setMaxWidth(70);
                column.setMinWidth(70);
                column.setCellRenderer(JLinkButton.getJLinkButtonRenderer());
                column.setCellEditor(JLinkButton.getJLinkButtonEditor());
                break;
            }
        }

        btnEdit = new JButton(JDL.L("gui.btn_settings", "Einstellungen"));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);

        btnLoad = new JButton(JDL.L("gui.config.plugin.host.btn_load", "Load Plugin"));
        btnLoad.setEnabled(false);
        btnLoad.addActionListener(this);

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        bpanel.add(btnEdit);
        bpanel.add(btnLoad);

        setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]", "[fill,grow][]"));
        add(new JScrollPane(table));
        add(bpanel, "w pref!");
    }

    @Override
    public void load() {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            int row = table.getSelectedRow();
            HostPluginWrapper hpw = pluginsForHost.get(row);
            if (!hpw.isLoaded()) {
                loadEntry(hpw);
            } else if (hpw.hasConfig()) {
                editEntry(hpw);
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

    @Override
    public void save() {
    }

    @Override
    public PropertyType hasChanges() {
        return PropertyType.NONE;
    }

}
