package jd.gui.swing.jdgui.settings.panels.premium.Columns;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;

import jd.controlling.AccountController;
import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.settings.panels.premium.HostAccounts;
import jd.gui.swing.laf.LookAndFeelController;
import jd.plugins.Account;

class BooleanRenderer extends JCheckBox implements TableCellRenderer, UIResource {

    private static final long serialVersionUID = 5635326369148415608L;

    public BooleanRenderer() {
        super();
        setHorizontalAlignment(JLabel.CENTER);
        setBorderPainted(false);
        setOpaque(true);
        if (LookAndFeelController.isSubstance()) this.setOpaque(false);
        this.setFocusable(false);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }
        setSelected((value != null && ((Boolean) value).booleanValue()));

        return this;
    }
}

public class EnabledColumn extends JDTableColumn implements ActionListener {
    private static final long serialVersionUID = -1043261559739746995L;
    private Component co;
    private Component coedit;
    private BooleanRenderer boolrend;
    private JCheckBox checkbox;
    boolean enabled = false;

    public EnabledColumn(String name, JDTableModel table) {
        super(name, table);
        boolrend = new BooleanRenderer();
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
        if (value instanceof Account) {
            Account ac = (Account) value;
            value = ac.isEnabled();
            co = boolrend.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            HostAccounts ha = (HostAccounts) value;
            value = ha.isEnabled();
            co = boolrend.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            co.setBackground(table.getBackground().darker());
        }
        return co;
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