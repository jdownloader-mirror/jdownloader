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

import javax.swing.JTextField;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.settings.panels.premium.HostAccounts;
import jd.plugins.Account;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class UserColumn extends JDTableColumn implements ActionListener {

    private static final long serialVersionUID = -5291590062503352550L;
    private JRendererLabel jlr;
    private JTextField user;

    public UserColumn(String name, JDTableModel table) {
        super(name, table);
        user = new JTextField();
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        setClickstoEdit(2);
    }

    public void actionPerformed(ActionEvent e) {
        user.removeActionListener(this);
        this.fireEditingStopped();
    }

    public Object getCellEditorValue() {
        return user.getText();
    }

    @Override
    public boolean isEditable(Object ob) {
        if (ob != null && ob instanceof Account) return true;
        return false;
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Account) return ((Account) obj).isEnabled();
        if (obj instanceof HostAccounts) return ((HostAccounts) obj).isEnabled();
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        user.removeActionListener(this);
        user.setText(((Account) value).getUser());
        user.addActionListener(this);
        return user;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Account) {
            Account ac = (Account) value;
            jlr.setText(ac.getUser());
        } else {
            jlr.setText("");
        }
        return jlr;
    }

    @Override
    public void setValue(Object value, Object o) {
        String pw = (String) value;
        if (o instanceof Account) ((Account) o).setUser(pw);
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

}
