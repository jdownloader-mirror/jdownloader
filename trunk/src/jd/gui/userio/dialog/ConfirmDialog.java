package jd.gui.userio.dialog;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTextPane;

public class ConfirmDialog extends AbstractDialog {

    private JTextPane textField;
    private String message;

    public ConfirmDialog(int flag, String title, String message, ImageIcon icon, String okOption, String cancelOption) {
        super(flag, title, icon, okOption, cancelOption);
        this.message = message;
        init();
    }

    /**
     * 
     */
    private static final long serialVersionUID = -7647771640756844691L;

    public void contentInit(JPanel cp) {
        textField = new JTextPane();
        textField.setBorder(null);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.setText(this.message);
        textField.setEditable(false);
        cp.add(textField);
    }

    public Integer getReturnID() {
        // TODO Auto-generated method stub
        return (Integer) super.getReturnValue();
    }

}
