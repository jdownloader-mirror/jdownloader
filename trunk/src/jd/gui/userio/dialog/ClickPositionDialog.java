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

package jd.gui.userio.dialog;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.SimpleGUI;
import jd.nutils.Screen;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class ClickPositionDialog extends JCountdownDialog implements ActionListener, MouseListener {

    private static final long serialVersionUID = 5540481255364141955L;

    private JButton btnBAD;

    private Point result = null;

    private File imagefile;

    private String title;

    private String explain;

    public ClickPositionDialog(File imagefile, String title, String explain) {
        super(SimpleGUI.CURRENTGUI);
        this.imagefile = imagefile;
        this.title = title;
        this.explain = explain;
        this.init();
    }

    public void init() {

        this.setModal(true);
        this.setTitle(title);
        this.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]"));

        ImageIcon imageIcon = null;

        if (imagefile != null && imagefile.exists()) {
            imageIcon = new ImageIcon(this.imagefile.getAbsolutePath());
        } else {
            imageIcon = JDTheme.II("gui.images.config.ocr");
        }

        btnBAD = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
        btnBAD.addActionListener(this);

        JLabel captcha = new JLabel(imageIcon);
        captcha.addMouseListener(this);
        captcha.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        captcha.setToolTipText(explain);

        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        if (explain != null) {
            JTextPane tf = new JTextPane();
            tf.setBorder(null);
            tf.setBackground(null);
            tf.setContentType("text/html");
            tf.setOpaque(false);
            tf.setText(explain);
            tf.setEditable(false);
            add(tf, "");
        }
        add(captcha, "w pref!, h pref!, alignx center");
        add(this.countDownLabel, "split 2,growx");
        add(btnBAD, "alignx right");
        this.setMinimumSize(new Dimension(300, -1));
        this.pack();
        this.setResizable(false);
        if (SimpleGUI.CURRENTGUI == null || SimpleGUI.CURRENTGUI.getExtendedState() == JFrame.ICONIFIED || !SimpleGUI.CURRENTGUI.isVisible() || !SimpleGUI.CURRENTGUI.isActive()) {
            this.setLocation(Screen.getDockBottomRight(this));
        } else {
            this.setLocation(Screen.getCenterOfComponent(SimpleGUI.CURRENTGUI, this));
        }
        this.toFront();
        this.setAlwaysOnTop(true);
        this.requestFocus();

        this.countdown(Math.max(2, SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.JAC_SHOW_TIMEOUT, 20)));

        this.setVisible(true);
        this.toFront();

    }

    public void actionPerformed(ActionEvent e) {
        mouseEntered(null);
        dispose();
    }

    public Point getPoint() {
        return result;
    }

    // @Override
    protected void onCountdown() {
        this.dispose();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        this.interrupt();
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        this.result = e.getPoint();
        dispose();
    }

}
