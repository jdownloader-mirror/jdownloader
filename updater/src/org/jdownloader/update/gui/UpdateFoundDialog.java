package org.jdownloader.update.gui;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.update.translate.T;

public class UpdateFoundDialog extends ConfirmDialog {

    public static void main(final String[] args) throws DialogClosedException, DialogCanceledException {
        Dialog.getInstance().showDialog(new UpdateFoundDialog(null, null, 1));
    }

    private AbstractAction laterAction = null;
    private AbstractAction nowAction   = null;

    public UpdateFoundDialog(final Runnable later, final Runnable now, final int num) {
        super(Dialog.LOGIC_COUNTDOWN | Dialog.BUTTONS_HIDE_OK, T._.update_dialog_title_updates_available(), T._.update_dialog_msg_x_updates_available(num), ImageProvider.getImageIcon("logo", 32, 32), null, T._.update_dialog_cancel());
        this.setCountdownTime(60);
        if (later != null) {
            this.laterAction = new AbstractAction(T._.update_dialog_later()) {

                /**
             * 
             */
                private static final long serialVersionUID = 1L;

                public void actionPerformed(final ActionEvent e) {
                    UpdateFoundDialog.this.setReturnmask(true);
                    later.run();
                    UpdateFoundDialog.this.dispose();
                }

            };
        }
        if (now != null) {
            this.nowAction = new AbstractAction(T._.update_dialog_yes()) {

                /**
             * 
             */
                private static final long serialVersionUID = 1L;

                public void actionPerformed(final ActionEvent e) {
                    UpdateFoundDialog.this.setReturnmask(true);
                    now.run();
                    UpdateFoundDialog.this.dispose();
                }

            };
        }
    }

    protected JPanel getDefaultButtonPanel() {
        final JPanel ret = new JPanel(new MigLayout("ins 0", "[]", "0[]0"));

        if (this.laterAction != null) {
            ret.add(new JButton(this.laterAction), "alignx right,tag ok,sizegroup confirms,growx,pushx");
        }
        if (this.nowAction != null) {
            ret.add(new JButton(this.nowAction), "alignx right,tag ok,sizegroup confirms,growx,pushx");
        }

        return ret;

    }

    protected boolean isIgnoreSizeLimitations() {
        return true;
    }

    @Override
    public JComponent layoutDialogContent() {
        final JComponent txt = super.layoutDialogContent();
        final JPanel ret = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow, fill][]"));
        ret.add(txt);

        final JButton btn = new JButton(T._.update_dialog_news_button());
        btn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                UpdateFoundDialog.this.cancel();
                CrossSystem.openURLOrShowMessage(T._.update_dialog_news_button_url());

            }
        });
        btn.setContentAreaFilled(false);
        SwingUtils.toBold(btn);
        btn.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getBackground().darker()));
        ret.add(Box.createHorizontalGlue(), "split 2,growx,pushx");
        ret.add(btn, "");
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return ret;
    }
}
