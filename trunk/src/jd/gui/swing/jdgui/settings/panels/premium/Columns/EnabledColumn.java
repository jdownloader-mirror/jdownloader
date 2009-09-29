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

package jd.gui.swing.jdgui.settings.panels.premium.Columns;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JTable;

import jd.controlling.AccountController;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.settings.panels.premium.HostAccounts;
import jd.plugins.Account;

import org.jdesktop.swingx.renderer.JRendererCheckBox;

public class EnabledColumn extends JDTableColumn implements ActionListener {
    private static final long serialVersionUID = -1043261559739746995L;
    private Component co;
    private Component coedit;
    private JRendererCheckBox boolrend;
    private JCheckBox checkbox;
    boolean enabled = false;

    public EnabledColumn(String name, JDTableModel table) {
        super(name, table);
        boolrend = new JRendererCheckBox();
        boolrend.setHorizontalAlignment(JCheckBox.CENTER);
        checkbox = new JCheckBox();
        checkbox.setHorizontalAlignment(JCheckBox.CENTER);
    }

    @Override
    public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value instanceof Account) {
            enabled = ((Account) value).isEnabled();
        } else {
            enabled = ((HostAccounts) value).isEnabled();
        }
        checkbox.removeActionListener(this);
        checkbox.setSelected(enabled);
        checkbox.addActionListener(this);
        coedit = checkbox;
        return coedit;
    }

    @Override
    public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        co = boolrend;
        if (value instanceof Account) {
            Account ac = (Account) value;
            boolrend.setSelected(ac.isEnabled());
        } else {
            HostAccounts ha = (HostAccounts) value;
            boolrend.setSelected(ha.isEnabled());
        }
        return co;
    }

    @Override
    public void postprocessCell(Component c, JTable table, Object value, boolean isSelected, int row, int column) {
        if (!(value instanceof Account)) {
            c.setBackground(table.getBackground().darker());
        }
    }

    @Override
    public boolean isEditable(Object obj) {
        return true;
    }

    @Override
    public void setValue(Object value, Object o) {
        boolean b = (Boolean) value;
        if (o instanceof Account) {
            ((Account) o).setEnabled(b);
        } else if (o instanceof HostAccounts) {
            ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(((HostAccounts) o).getHost());
            if (accs == null) return;
            for (Account acc : accs) {
                acc.setEnabled(b);
            }
        }
    }

    public Object getCellEditorValue() {
        if (coedit == null) return null;
        return ((JCheckBox) coedit).isSelected();

    }

    public void actionPerformed(ActionEvent e) {
        checkbox.removeActionListener(this);
        this.fireEditingStopped();

    }

    @Override
    public boolean isSortable(Object obj) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Account) return ((Account) obj).isEnabled();
        if (obj instanceof HostAccounts) return ((HostAccounts) obj).isEnabled();
        return true;
    }

}
