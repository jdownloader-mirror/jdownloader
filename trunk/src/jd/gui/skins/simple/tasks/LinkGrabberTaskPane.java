package jd.gui.skins.simple.tasks;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JSeparator;

import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.JDAction;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberV2TreeTableAction;
import jd.gui.skins.simple.components.Linkgrabber.UpdateEvent;
import jd.gui.skins.simple.components.Linkgrabber.UpdateListener;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class LinkGrabberTaskPane extends TaskPanel implements ActionListener, ControlListener, UpdateListener {

    private JButton panel_add_links;
    private JButton panel_add_containers;
    private JButton lg_add_all;
    private JButton lg_add_selected;
    private JButton lg_clear;
    private JSeparator sep = new JSeparator();
    private SimpleGUI simplegui;
    boolean lg_buttons_visible = false;

    public LinkGrabberTaskPane(String string, ImageIcon ii) {
        super(string, ii, "linkgrabber");
        this.setLayout(new MigLayout("ins 0, wrap 1", "[fill]"));
        JDUtilities.getController().addControlListener(this);
        simplegui = SimpleGUI.CURRENTGUI;
        lg_buttons_visible = false;
        initGUI();
    }

    private void initGUI() {
        this.panel_add_links = addButton(this.createButton(JDLocale.L("gui.linkgrabberv2.addlinks", "Add Links"), JDTheme.II("gui.images.add", 16, 16)));
        this.panel_add_containers = addButton(this.createButton(JDLocale.L("gui.linkgrabberv2.addcontainers", "Add Containers"), JDTheme.II("gui.images.load", 16, 16)));
    }

    private JButton addButton(JButton bt) {
        bt.addActionListener(this);
        bt.setHorizontalAlignment(JButton.LEFT);
        add(bt, "alignx leading");
        return bt;
    }

    private JButton addButton(JButton bt, int pos) {
        bt.addActionListener(this);
        bt.setHorizontalAlignment(JButton.LEFT);
        add(bt, "alignx leading", pos);
        return bt;
    }

    private void removeButton(JButton bt) {
        if (bt == null) return;
        bt.removeActionListener(this);
        remove(bt);
    }

    /**
     * 
     */
    private static final long serialVersionUID = -7720749076951577192L;

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == panel_add_links) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.ITEMS_ADD, null));
            return;
        }
        if (e.getSource() == panel_add_containers) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_LOAD_DLC, null));
            return;
        }
        if (e.getSource() == lg_add_all) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.ADD_ALL, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == lg_add_selected) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.ADD_SELECTED, ((JButton) e.getSource()).getName()));
            return;
        }
        if (e.getSource() == lg_clear) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberV2TreeTableAction.CLEAR, ((JButton) e.getSource()).getName()));
            return;
        }
    }

    public void controlEvent(ControlEvent event) {

    }

    public synchronized void UpdateEvent(final UpdateEvent event) {
        if (event.getID() == UpdateEvent.EMPTY_EVENT) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    remove(sep);
                    removeButton(lg_add_all);
                    removeButton(lg_add_selected);
                    removeButton(lg_clear);
                }
            });
            lg_buttons_visible = false;
        }
        if (event.getID() == UpdateEvent.UPDATE_EVENT && lg_buttons_visible == false) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    add(sep, 2);
                    lg_add_all = addButton(createButton(JDLocale.L("gui.linkgrabberv2.lg.addall", "Add all packages"), JDTheme.II("gui.images.add", 16, 16)), 3);
                    lg_add_selected = addButton(createButton(JDLocale.L("gui.linkgrabberv2.lg.addselected", "Add selected package(s)"), JDTheme.II("gui.images.add", 16, 16)), 4);
                    lg_clear = addButton(createButton(JDLocale.L("gui.linkgrabberv2.lg.clear", "Clear List"), JDTheme.II("gui.images.delete", 16, 16)), 5);
                }
            });
            lg_buttons_visible = true;
        }
    }
}
