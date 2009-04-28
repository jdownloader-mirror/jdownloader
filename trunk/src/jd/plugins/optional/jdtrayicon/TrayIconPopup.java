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

package jd.plugins.optional.jdtrayicon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JWindow;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;

public class TrayIconPopup extends JWindow implements MouseListener, MouseMotionListener, ChangeListener {

    private static final int ACTION_ADD = 4;

    private static final int ACTION_EXIT = 11;
    private static final int ACTION_LOAD = 0;
    private static final int ACTION_PAUSE = 3;
    private static final int ACTION_RECONNECT = 8;
    private static final int ACTION_START = 1;
    private static final int ACTION_STOP = 2;
    private static final int ACTION_TOGGLE_CLIPBOARD = 9;
    private static final int ACTION_TOGGLE_RECONNECT = 10;
    private static final int ACTION_TOGGLE_PREMIUM = 12;
    private static final int ACTION_UPDATE = 5;
    private static final int ANCHOR_NORTH_WEST = GridBagConstraints.NORTHWEST;
    private static final int ANCHOR_WEST = GridBagConstraints.WEST;
    private static final Color BACKGROUNDCOLOR = Color.WHITE;
    private static final Color DISABLED_COLOR = Color.GRAY;
    private static final int FILL_NONE = GridBagConstraints.NONE;
    private static final Color HIGHLIGHT_COLOR = Color.BLUE;
    private static final Insets INSETS = new Insets(1, 1, 1, 1);
    private static final int MARGIN = 2;
    private static final int MENUENTRY_HEIGHT = 16;
    private static final int MENUENTRY_ICON_WIDTH = MENUENTRY_HEIGHT + 12;
    private static final int MENUENTRY_LABEL_WIDTH = 220;

    private static final long serialVersionUID = 1L;
    private JPanel bottomPanel;
    private boolean enteredPopup;
    private ArrayList<Integer> entries = new ArrayList<Integer>();
    private JPanel leftPanel;
    private int midPanelCounter = 0;
    private int mouseOverRow;
    private Point point;
    private JPanel rightPanel;
    private JSpinner spMax;
    private JSpinner spMaxDls;
    private JPanel topPanel;

    public TrayIconPopup() {
        setLayout(new GridBagLayout());
        addMouseMotionListener(this);
        addMouseListener(this);
        toFront();
        setAlwaysOnTop(true);

        init();
        initTopPanel();

        addMenuEntry(ACTION_LOAD, JDTheme.II("gui.images.load"), JDLocale.L("plugins.trayicon.popup.menu.load", "Container laden"));

        switch (JDUtilities.getController().getDownloadStatus()) {
        case JDController.DOWNLOAD_NOT_RUNNING:
            addMenuEntry(ACTION_START, JDTheme.II("gui.images.next"), JDLocale.L("plugins.trayicon.popup.menu.start", "Download Starten"));
            break;
        case JDController.DOWNLOAD_RUNNING:
            addMenuEntry(ACTION_STOP, JDTheme.II("gui.images.stop"), JDLocale.L("plugins.trayicon.popup.menu.stop", "Download anhalten"));
            break;
        default:
            addDisabledMenuEntry(JDTheme.II("gui.images.next"), JDLocale.L("plugins.trayicon.popup.menu.start", "Download Starten"));
        }

        /*
         * if (JDUtilities.getController().getDownloadStatus() ==
         * JDController.DOWNLOAD_RUNNING) { addMenuEntry(ACTION_PAUSE,
         * JDTheme.II("gui.images.stop_after"),
         * JDLocale.L("plugins.trayicon.popup.menu.pause",
         * "Nach diesem Download anhalten")); }
         */

        addMenuEntry(ACTION_ADD, JDTheme.II("gui.images.add"), JDLocale.L("plugins.trayicon.popup.menu.add", "Downloads hinzufügen"));
        addMenuEntry(ACTION_UPDATE, JDTheme.II("gui.images.update_manager"), JDLocale.L("plugins.trayicon.popup.menu.update", "JD aktualisieren"));
        addMenuEntry(ACTION_RECONNECT, JDTheme.II("gui.images.reconnect"), JDLocale.L("plugins.trayicon.popup.menu.reconnect", "Reconnect durchführen"));
        addMenuEntry(ACTION_TOGGLE_PREMIUM, getPremiumImage(), JDLocale.L("plugins.trayicon.popup.menu.togglePremium", "Premium an/aus"));
        addMenuEntry(ACTION_TOGGLE_CLIPBOARD, getClipBoardImage(), JDLocale.L("plugins.trayicon.popup.menu.toggleClipboard", "Zwischenablage an/aus"));
        addMenuEntry(ACTION_TOGGLE_RECONNECT, getReconnectImage(), JDLocale.L("plugins.trayicon.popup.menu.toggleReconnect", "Reconnect an/aus"));
        addMenuEntry(ACTION_EXIT, JDTheme.II("gui.images.exit"), JDLocale.L("plugins.trayicon.popup.menu.exit", "Beenden"));

        initBottomPanel();
        setVisible(false);
        pack();
    }

    private void addDisabledMenuEntry(ImageIcon i, String l) {
        JLabel b;
        JLabel icon;
        JDUtilities.addToGridBag(leftPanel, icon = new JLabel(new ImageIcon(i.getImage().getScaledInstance(MENUENTRY_HEIGHT, MENUENTRY_HEIGHT, Image.SCALE_SMOOTH))), 0, midPanelCounter, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_NORTH_WEST);

        JDUtilities.addToGridBag(rightPanel, b = new JLabel(l), 0, midPanelCounter, 1, 1, 0, 1, new Insets(1, 4, 1, 1), FILL_NONE, ANCHOR_NORTH_WEST);
        entries.add(null);
        midPanelCounter++;
        b.setHorizontalAlignment(SwingConstants.LEFT);

        b.setOpaque(false);
        icon.setOpaque(false);
        b.setPreferredSize(new Dimension(MENUENTRY_LABEL_WIDTH, MENUENTRY_HEIGHT));
        icon.setPreferredSize(new Dimension(MENUENTRY_ICON_WIDTH, MENUENTRY_HEIGHT));
        icon.setForeground(Color.GRAY);
        b.setForeground(Color.GRAY);
        icon.setEnabled(false);
        b.setEnabled(false);
    }

    private void addMenuEntry(Integer id, ImageIcon i, String l) {
        JLabel b;
        JLabel icon;
        JDUtilities.addToGridBag(leftPanel, icon = new JLabel(new ImageIcon(i.getImage().getScaledInstance(MENUENTRY_HEIGHT, MENUENTRY_HEIGHT, Image.SCALE_SMOOTH))), 0, midPanelCounter, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_NORTH_WEST);

        JDUtilities.addToGridBag(rightPanel, b = new JLabel(l), 0, midPanelCounter, 1, 1, 0, 1, new Insets(1, 4, 1, 1), FILL_NONE, ANCHOR_NORTH_WEST);
        entries.add(id);
        midPanelCounter++;
        b.setHorizontalAlignment(SwingConstants.LEFT);

        b.setOpaque(false);
        icon.setOpaque(false);
        b.setPreferredSize(new Dimension(MENUENTRY_LABEL_WIDTH, MENUENTRY_HEIGHT));
        icon.setPreferredSize(new Dimension(MENUENTRY_ICON_WIDTH, MENUENTRY_HEIGHT));
    }

    // Checken ob es ein neues Update verf�gbar ist
    private boolean checkUpdate(Point p) {
        if (mouseOverRow != getRow(p)) {
            mouseOverRow = getRow(p);
            paint();
            return true;
        }
        return false;
    }

    private ImageIcon getClipBoardImage() {
        if (ClipboardHandler.getClipboard().isEnabled()) {
            return JDTheme.II("gui.images.clipboard_enabled");
        } else {
            return JDTheme.II("gui.images.clipboard_disabled");
        }
    }

    private ImageIcon getReconnectImage() {
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false)) {
            return JDTheme.II("gui.images.reconnect_enabled");
        } else {
            return JDTheme.II("gui.images.reconnect_disabled");
        }
    }

    private ImageIcon getPremiumImage() {
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, false)) {
            return JDTheme.II("gui.images.premium_enabled");
        } else {
            return JDTheme.II("gui.images.premium_disabled");
        }
    }

    private int getRow(Point e) {
        int y = e.y;
        y -= rightPanel.getY();
        if (y < 0) { return -1; }
        y /= rightPanel.getHeight() / midPanelCounter;

        return y;
    }

    private void init() {
        JPanel p;
        p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JDUtilities.addToGridBag(this, p, 0, 0, 1, 1, 1, 1, new Insets(0, 0, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.SOUTHEAST);

        topPanel = new JPanel(new GridBagLayout());
        leftPanel = new JPanel(new GridBagLayout());
        rightPanel = new JPanel(new GridBagLayout());
        bottomPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(BACKGROUNDCOLOR);
        leftPanel.setBackground(BACKGROUNDCOLOR);
        rightPanel.setBackground(BACKGROUNDCOLOR);
        bottomPanel.setBackground(BACKGROUNDCOLOR);

        topPanel.setOpaque(false);
        leftPanel.setOpaque(false);
        rightPanel.setOpaque(false);
        bottomPanel.setOpaque(false);
        JDUtilities.addToGridBag(p, topPanel, 0, 0, 2, 1, 0, 0, new Insets(MARGIN, MARGIN, 0, MARGIN), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(p, leftPanel, 0, 1, 1, 1, 0, 0, new Insets(0, MARGIN, 0, 0), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(p, rightPanel, 1, 1, 1, 1, 0, 0, new Insets(0, 0, 0, MARGIN), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(p, bottomPanel, 0, 2, 2, 1, 0, 0, new Insets(0, MARGIN, MARGIN, MARGIN), GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.LIGHT_GRAY));
    }

    private void initBottomPanel() {
        int maxspeed = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);

        spMax = new JSpinner();
        spMax.setModel(new SpinnerNumberModel(maxspeed, 0, Integer.MAX_VALUE, 50));
        spMax.setPreferredSize(new Dimension(60, 20));
        spMax.setToolTipText(JDLocale.L("gui.tooltip.statusbar.speedlimiter", "Geschwindigkeitsbegrenzung festlegen(kb/s) [0:unendlich]"));
        spMax.addChangeListener(this);

        spMaxDls = new JSpinner();
        spMaxDls.setModel(new SpinnerNumberModel(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 20, 1));
        spMaxDls.setPreferredSize(new Dimension(60, 20));
        spMaxDls.setToolTipText(JDLocale.L("gui.tooltip.statusbar.simultan_downloads", "Max. gleichzeitige Downloads"));
        spMaxDls.addChangeListener(this);

        JDUtilities.addToGridBag(bottomPanel, new JLabel(JDLocale.L("plugins.trayicon.popup.bottom.speed", "Geschwindigkeitsbegrenzung")), 0, 0, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_WEST);
        JDUtilities.addToGridBag(bottomPanel, spMax, 1, 0, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_NORTH_WEST);
        JDUtilities.addToGridBag(bottomPanel, new JLabel(JDLocale.L("plugins.trayicon.popup.bottom.simDls", "Gleichzeitige Downloads")), 0, 1, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_WEST);
        JDUtilities.addToGridBag(bottomPanel, spMaxDls, 1, 1, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_NORTH_WEST);
    }

    private void initTopPanel() {
        // ImageIcon logo = new ImageIcon(JDImage.getImage("logo/logo_32_32"));
        JDUtilities.addToGridBag(topPanel, new JLabel(JDLocale.L("plugins.trayicon.popup.title", "JDownloader") + " 0." + JDUtilities.getRevision(), null, SwingConstants.LEFT), 0, 0, 1, 1, 0, 0, INSETS, FILL_NONE, ANCHOR_NORTH_WEST);
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        point = e.getPoint();
        checkUpdate(point);
    }

    public void mouseEntered(MouseEvent e) {
        enteredPopup = true;
    }

    public void mouseExited(MouseEvent e) {
        if (e.getSource() == this && enteredPopup && !this.contains(e.getPoint())) {
            dispose();
        }
    }

    public void mouseMoved(MouseEvent e) {
        point = e.getPoint();
        checkUpdate(point);
    }

    public void mousePressed(MouseEvent e) {
        point = e.getPoint();
        checkUpdate(point);
    }

    public void mouseReleased(MouseEvent e) {
        point = e.getPoint();
        checkUpdate(point);
        if (mouseOverRow < 0 || entries.get(mouseOverRow) != null) {
            onAction(mouseOverRow);
        }
    }

    private void onAction(int row) {
        SimpleGUI simplegui = SimpleGUI.CURRENTGUI;
        if (row < 0) {
            simplegui.setVisible(!simplegui.isVisible());
            return;
        }
        JDLogger.getLogger().info("Action " + entries.get(row));
        switch (entries.get(row)) {

        case TrayIconPopup.ACTION_ADD:
            /**
             * TODO
             */
            // simplegui.actionPerformed(new ActionEvent(this,
            // MenuAction.ITEMS_ADD, null));
            break;
        case TrayIconPopup.ACTION_LOAD:
            /**
             * TODO
             */
            // simplegui.actionPerformed(new ActionEvent(this,
            // MenuAction.APP_LOAD_DLC, null));
            break;
        case TrayIconPopup.ACTION_PAUSE:
            JDUtilities.getController().pauseDownloads(true);
            break;
        case TrayIconPopup.ACTION_RECONNECT:
            simplegui.doManualReconnect();
            break;
        case TrayIconPopup.ACTION_START:
        case TrayIconPopup.ACTION_STOP:
            JDUtilities.getController().toggleStartStop();
            break;
        case TrayIconPopup.ACTION_TOGGLE_CLIPBOARD:
            /**
             * TODO
             */
            // simplegui.actionPerformed(new ActionEvent(this,
            // JDAction.MenuAction, null));
            break;
        case TrayIconPopup.ACTION_TOGGLE_RECONNECT:
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));
            JDUtilities.getConfiguration().save();
            break;
        case TrayIconPopup.ACTION_UPDATE:
            new WebUpdate().doWebupdate(true);
            break;
        case TrayIconPopup.ACTION_EXIT:
            JDUtilities.getController().exit();
            break;
        case TrayIconPopup.ACTION_TOGGLE_PREMIUM:
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, false));
            break;
        }
        dispose();
    }

    private void paint() {
        Graphics g = getContentPane().getParent().getGraphics();
        getContentPane().setBackground(BACKGROUNDCOLOR);
        Point p = rightPanel.getLocation();

        int y = 0;

        for (int i = 0; i < midPanelCounter; i++) {
            y = i * (rightPanel.getHeight() / midPanelCounter) + p.y;

            if (mouseOverRow >= 0 && point.y >= y && point.y < (i + 1) * (rightPanel.getHeight() / midPanelCounter) + p.y) {
                if (entries.get(mouseOverRow) != null) {
                    g.setColor(HIGHLIGHT_COLOR);
                } else {
                    g.setColor(DISABLED_COLOR);
                }
            } else {
                g.setColor(BACKGROUNDCOLOR);
            }
            g.drawRect(2, y, getWidth() - 4, rightPanel.getHeight() / midPanelCounter - 1);
        }
    }

    public void stateChanged(ChangeEvent e) {
        int max = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);

        if (e.getSource() == spMax) {
            int value = (Integer) spMax.getValue();

            if (max != value) {
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, value);
                SubConfiguration.getConfig("DOWNLOAD").save();
            }
        } else if (e.getSource() == spMaxDls) {
            int value = (Integer) spMaxDls.getValue();

            if (max != value) {
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, value);
                SubConfiguration.getConfig("DOWNLOAD").save();
            }
        }
    }
}