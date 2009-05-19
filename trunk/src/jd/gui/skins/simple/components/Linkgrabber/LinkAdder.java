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

package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jd.controlling.ClipboardHandler;
import jd.gui.skins.simple.Factory;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.SimpleGUI;
import jd.parser.html.HTMLParser;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class LinkAdder extends JTabbedPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 3919268234331701967L;
    private JTextArea text;
    private Thread clipboardObserver;
    private JLabel lbl;
    private boolean watchclipboard = false;

    public LinkAdder() {
        super(new MigLayout("ins 0 0 8 8,wrap 1", "[]", "[][fill,grow][]"));

        add(Factory.createHeader(JDLocale.L("gui.linkgrabber.adder.links", "Add links"), JDTheme.II("gui.images.add", 32, 32)), "pushx,growx");
        add(new JScrollPane(text = new JTextArea()), "pushx,growx");

        reset();
        text.setLineWrap(true);
        text.setWrapStyleWord(false);
        text.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
                String[] links = HTMLParser.getHttpLinks(text.getText(), null);
                lbl.setText(JDLocale.LF("gui.linkgrabber.adder.links.status", "...contains %s link(s)", links.length));
            }

            public void insertUpdate(DocumentEvent e) {
                String[] links = HTMLParser.getHttpLinks(text.getText(), null);
                lbl.setText(JDLocale.LF("gui.linkgrabber.adder.links.status", "...contains %s link(s)", links.length));

            }

            public void removeUpdate(DocumentEvent e) {
                String[] links = HTMLParser.getHttpLinks(text.getText(), null);
                lbl.setText(JDLocale.LF("gui.linkgrabber.adder.links.status", "...contains %s link(s)", links.length));

            }

        });
        add(lbl = new JLabel(JDLocale.LF("gui.linkgrabber.adder.links.status", "...contains %s link(s)", 0)), "alignx left, split 3,growx");
        JButton bt;

        add(bt = new JButton(JDLocale.L("gui.linkgrabber.adder.links.cancel", "Cancel"), JDTheme.II("gui.images.abort", 16, 16)), "alignx right");
        bt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                reset();
                if (LinkGrabberPanel.getLinkGrabber().hasLinks()) {
                    SimpleGUI.CURRENTGUI.getLgTaskPane().setPanelID(1);
                }

            }

        });
        add(bt = new JButton(JDLocale.L("gui.linkgrabber.adder.links.confirm", "Continue"), JDTheme.II("gui.images.add", 16, 16)), "alignx right");
        bt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JDUtilities.getController().distributeLinks(text.getText());
                reset();
            }

        });

    }

    public boolean needsViewport() {
        return false;
    }

    // @Override
    public void onDisplay() {
        ClipboardHandler.getClipboard().setTempDisableD(true);
        setClipboard(true);
    }

    public void reset() {
        text.setText(JDLocale.L("gui.linkgrabber.adder.links.default", "Insert any text containing links here. This textfield grabs all links out of your clipboard"));
    }

    private void setClipboard(boolean b) {
        if (b) {
            watchclipboard = true;
            if (clipboardObserver != null && clipboardObserver.isAlive()) return;
            clipboardObserver = new Thread() {
                public void run() {
                    String old = null;
                    while (true && watchclipboard) {
                        try {
                            String newText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                            ClipboardHandler.getClipboard().setOldData(newText);
                            if (newText != null && !newText.equalsIgnoreCase(old)) {
                                old = newText;
                                String[] links = HTMLParser.getHttpLinks(newText, null);
                                if (links.length > 0) {
                                    addText(newText);
                                }
                            }
                        } catch (Exception e1) {
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }

            };
            clipboardObserver.start();
        } else {
            watchclipboard = false;
            clipboardObserver = null;
        }

    }

    public void addText(final String newText) {
        if (text.getText().contains(newText)) return;
        new GuiRunnable<Object>() {

            // @Override
            public Object runSave() {
                text.setText(text.getText() + "\r\n" + newText);
                return null;
            }
        }.waitForEDT();

    }

    // @Override
    public void onHide() {
        setClipboard(false);
        ClipboardHandler.getClipboard().setTempDisableD(false);
    }
}
