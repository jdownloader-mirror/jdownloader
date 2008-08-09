//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jd.JDFileFilter;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.gui.skins.simple.components.HTMLDialog;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JHelpDialog;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.gui.skins.simple.components.TwoTextFieldDialog;
import jd.gui.skins.simple.config.ConfigurationDialog;
import jd.gui.skins.simple.config.jdUnrarPasswordListDialog;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForContainer;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.unrar.UnZip;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTitledSeparator;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class SimpleGUI implements UIInterface, ActionListener, UIListener, WindowListener {
    /**
     * Toggled das MenuItem fuer die Ansicht des Log Fensters
     * 
     * @author Tom
     */
    private final class LogDialogWindowAdapter extends WindowAdapter {

        public void windowClosed(WindowEvent e) {
            if (menViewLog != null) {
                menViewLog.setSelected(false);
            }
        }

        public void windowOpened(WindowEvent e) {
            if (menViewLog != null) {
                menViewLog.setSelected(true);
            }
        }
    }

    /**
     * Diese Klasse realisiert eine StatusBar
     * 
     * @author astaldo
     */
    private class StatusBar extends JPanel implements ChangeListener, ControlListener {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 3676496738341246846L;

        private JCheckBox chbPremium;

        private JLabel lblMessage;
        private JLabel lblSimu;
        // private JLabel lblPluginHostActive;
        //
        // private JLabel lblPluginDecryptActive;

        private JLabel lblSpeed;
        protected JSpinner spMax;

        protected JSpinner spMaxDls;

        public StatusBar() {
            setLayout(new BorderLayout());
            // int n = 10;
            // JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            // add(bundle(left, right));
            JPanel panel = new JPanel(new BorderLayout(0, 0));
            add(panel, BorderLayout.WEST);
            add(right, BorderLayout.EAST);

            // TODO: Please replace with proper Icon that catches the users eye.
            // Icon could even change in case of a warning or error.
            // gruener Haken - everything ok
            // oranges Warnschild - ohoh
            // roter Kreis - we are roally f#$%cked!
            ImageIcon statusIcon = JDUtilities.getscaledImageIcon(JDTheme.V("gui.images.jd_logo"), 16, -1);

            lblMessage = new JLabel(JDLocale.L("sys.message.welcome"));
            lblMessage.setIcon(statusIcon);
            chbPremium = new JCheckBox(JDLocale.L("gui.statusbar.premium", "Premium"));
            chbPremium.setToolTipText(JDLocale.L("gui.tooltip.statusbar.premium", "Aus/An schalten des Premiumdownloads"));
            chbPremium.addChangeListener(this);
            JDUtilities.getController().addControlListener(this);
            chbPremium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));
            lblSpeed = new JLabel(JDLocale.L("gui.statusbar.speed", "Max. Speed"));
            lblSimu = new JLabel(JDLocale.L("gui.statusbar.sim_ownloads", "Max.Dls."));
            int maxspeed = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);

            spMax = new JSpinner();
            spMax.setModel(new SpinnerNumberModel(maxspeed, 0, Integer.MAX_VALUE, 50));
            spMax.setPreferredSize(new Dimension(60, 20));
            spMax.setToolTipText(JDLocale.L("gui.tooltip.statusbar.speedlimiter", "Geschwindigkeitsbegrenzung festlegen(kb/s) [0:unendlich]"));
            spMax.addChangeListener(this);

            spMaxDls = new JSpinner();
            spMaxDls.setModel(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2), 1, 20, 1));
            spMaxDls.setPreferredSize(new Dimension(60, 20));
            spMaxDls.setToolTipText(JDLocale.L("gui.tooltip.statusbar.simultan_downloads", "Max. gleichzeitige Downloads"));
            spMaxDls.addChangeListener(this);

            // lblPluginHostActive = new JLabel(imgInactive);
            // lblPluginDecryptActive = new JLabel(imgInactive);
            // lblPluginDecryptActive.setToolTipText(JDLocale.L(
            // "gui.tooltip.plugin_decrypt"));
            // lblPluginHostActive.setToolTipText(JDLocale.L(
            // "gui.tooltip.plugin_host"));

            panel.add(lblMessage);
            // JLinkButton linkButton = new
            // JLinkButton("http://jdownloader.org");
            // left.add(linkButton);
            right.add(chbPremium);
            // right.add(bundle(lblSimu, spMaxDls));
            addItem(right, bundle(lblSimu, spMaxDls));
            // right.add(bundle(lblSpeed, spMax));
            addItem(right, bundle(lblSpeed, spMax));

            colorizeSpinnerSpeed(maxspeed);
            // JDUtilities.addToGridBag(this, lblMessage, 0, 0, 1, 1, 1, 1, new
            // Insets(0, 5,
            // 0, 0), GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
            // JDUtilities.addToGridBag(this, lblSimu, 1, 0, 1, 1, 0, 0, new
            // Insets(0, 2, 0,
            // 0), GridBagConstraints.NONE, GridBagConstraints.WEST);
            // JDUtilities.addToGridBag(this, spMaxDls, 2, 0, 1, 1, 0, 0, new
            // Insets(0, 2,
            // 0, 0), GridBagConstraints.NONE, GridBagConstraints.WEST);
            //
            // JDUtilities.addToGridBag(this, chbPremium, 3, 0, 1, 1, 0, 0, new
            // Insets(0, 2,
            // 0, 0), GridBagConstraints.NONE, GridBagConstraints.WEST);
            // JDUtilities.addToGridBag(this, new
            // JSeparator(JSeparator.VERTICAL), 4, 0, 1,
            // 1, 0, 0, new Insets(2, 2, 2, 2), GridBagConstraints.BOTH,
            // GridBagConstraints.WEST);
            //
            // JDUtilities.addToGridBag(this, lblSpeed, 5, 0, 1, 1, 0, 0, new
            // Insets(0, 2,
            // 0, 0), GridBagConstraints.NONE, GridBagConstraints.WEST);
            //
            // JDUtilities.addToGridBag(this, spMax, 6, 0, 1, 1, 0, 0, new
            // Insets(0, 2, 0,
            // 0), GridBagConstraints.NONE, GridBagConstraints.WEST);

            // JDUtilities.addToGridBag(this, lblPluginHostActive, 5, 0, 1, 1,
            // 0, 0, new Insets(0, 5, 0, 0), GridBagConstraints.NONE,
            // GridBagConstraints.EAST);
            // JDUtilities.addToGridBag(this, lblPluginDecryptActive, 6, 0, 1,
            // 1, 0, 0, new Insets(0, 0, 0, 0), GridBagConstraints.NONE,
            // GridBagConstraints.EAST);
        }

        void addItem(JComponent where, Component component) {
            int n = 10;
            Dimension d = new Dimension(n, 0);
            JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
            separator.setPreferredSize(new Dimension(n, n));
            where.add(new Box.Filler(d, d, d));
            where.add(separator);
            where.add(component);
        }

        private Component bundle(Component c1, Component c2) {
            JPanel panel = new JPanel(new BorderLayout(2, 0));
            panel.add(c1, BorderLayout.WEST);
            panel.add(c2, BorderLayout.EAST);
            return panel;
        }

        // /**
        // * Ändert das genutzte Bild eines Labels, um In/Aktivität anzuzeigen
        // *
        // * @param lbl Das zu ändernde Label
        // * @param active soll es als aktiv oder inaktiv gekennzeichnet werden
        // */
        // private void setPluginActive(JLabel lbl, boolean active) {
        // if (active)
        // lbl.setIcon(imgActive);
        // else
        // lbl.setIcon(imgInactive);
        // }
        private void colorizeSpinnerSpeed(Integer Speed) {
            /* färbt den spinner ein, falls speedbegrenzung aktiv */
            JSpinner.DefaultEditor spMaxEditor = (JSpinner.DefaultEditor) spMax.getEditor();
            Color warning = JDTheme.C("gui.color.statusbar.maxspeedhighlight", "ff0c03");
            if (Speed > 0) {
                lblSpeed.setForeground(warning);
                spMaxEditor.getTextField().setForeground(Color.red);
            } else {
                lblSpeed.setForeground(Color.black);
                spMaxEditor.getTextField().setForeground(Color.black);
            }
        }

        public void controlEvent(ControlEvent event) {
            Property p;
            switch (event.getID()) {
            case ControlEvent.CONTROL_JDPROPERTY_CHANGED:
                p = (Property) event.getSource();
                if (spMax != null && event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SPEED)) {

                    spMax.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
                    colorizeSpinnerSpeed(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));

                } else if (event.getParameter().equals(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN)) {
                    spMaxDls.setValue(p.getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2));
                } else if (p == JDUtilities.getConfiguration() && event.getParameter().equals(Configuration.PARAM_USE_GLOBAL_PREMIUM)) {
                    chbPremium.setSelected(p.getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true));

                }
                // else if (event.getParameter().equals(CESClient.UPDATE)) {
                // p = (Property) event.getSource();
                // CESClient ces = (CESClient)
                // p.getProperty(CESClient.LASTEST_INSTANCE);
                // if (ces != null) {
                // setText(ces.getAccountInfoString());
                // }
                //
                // if (!CES.isEnabled()) {
                // btnCes.setIcon(CES.getImage());
                // setText("");
                // }
                // }
                break;
            }

        }

        // /**
        // * Zeigt, ob die Plugins zum Downloaden von einem Anbieter arbeiten
        // *
        // * @param active wahr, wenn Downloads aktiv sind
        // */
        // public void setPluginForHostActive(boolean active) {
        // setPluginActive(lblPluginHostActive, active);
        // }
        //
        // /**
        // * Zeigt an, ob die Plugins zum Entschlüsseln von Links arbeiten
        // *
        // * @param active wahr, wenn soeben Links entschlüsselt werden
        // */
        // public void setPluginForDecryptActive(boolean active) {
        // setPluginActive(lblPluginDecryptActive, active);
        // }

        /**
         * Setzt die Downloadgeschwindigkeit
         * 
         * @param speed
         *            bytes pro sekunde
         */
        public void setSpeed(Integer speed) {
            if (speed <= 0) {
                lblSpeed.setText(JDLocale.L("gui.statusbar.speed", "Max. Speed"));
                return;
            }

            if (speed > 1024) {
                lblSpeed.setText("(" + speed / 1024 + JDLocale.L("gui.download.kbps", "kb/s") + ")");
            } else {
                lblSpeed.setText("(" + speed + JDLocale.L("gui.download.bps", "bytes/s") + ")");
            }
        }

        public void setSpinnerSpeed(Integer speed) {
            spMax.setValue(speed);
            colorizeSpinnerSpeed(speed);
        }

        public void setText(String text) {
            if (text == null) {
                text = JDLocale.L("sys.message.welcome");
            }
            lblMessage.setText(text);
        }

        public void stateChanged(ChangeEvent e) {
            int max = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);

            if (e.getSource() == spMax) {
                int value = (Integer) spMax.getValue();
                colorizeSpinnerSpeed(value);
                if (max != value) {
                    JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, value);
                    JDUtilities.getSubConfig("DOWNLOAD").save();
                }

            }
            if (e.getSource() == spMaxDls) {
                int value = (Integer) spMaxDls.getValue();
                if (max != value) {
                    JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, value);
                    JDUtilities.getSubConfig("DOWNLOAD").save();
                }

            }
            if (e.getSource() == chbPremium) {
                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) != chbPremium.isSelected()) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, chbPremium.isSelected());
                    JDUtilities.saveConfig();
                }
            }

        }
    }

    public static SimpleGUI CURRENTGUI = null;
    public static final String GUICONFIGNAME = "simpleGUI";
    private static SubConfiguration guiConfig = JDUtilities.getSubConfig(GUICONFIGNAME);

    public static final String PARAM_BROWSER = "BROWSER";

    public static final String PARAM_BROWSER_VARS = "BROWSER_VARS";

    public static final String PARAM_DISABLE_CONFIRM_DIALOGS = "DISABLE_CONFIRM_DIALOGS";

    public static final String PARAM_JAC_LOG = "JAC_DOLOG";
    public static final String PARAM_LOCALE = "LOCALE";
    public static final String PARAM_PLAF = "PLAF";

    public static final String PARAM_SHOW_SPLASH = "SHOW_SPLASH";

    public static final String PARAM_START_DOWNLOADS_AFTER_START = "START_DOWNLOADS_AFTER_START";

    public static final String PARAM_THEME = "THEME";

    public static final String SELECTED_CONFIG_TAB = "SELECTED_CONFIG_TAB";

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3966433144683787356L;

    private static boolean uiInitated = false;

    /**
     * factory method for menu items
     * 
     * @param action
     *            action for the menu item
     * @return the new menu item
     */
    private static JMenuItem createMenuItem(JDAction action) {
        JMenuItem menuItem = new JMenuItem(action);
        if (menuItem.getIcon() instanceof ImageIcon) {
            ImageIcon icon = (ImageIcon) menuItem.getIcon();
            menuItem.setIcon(JDUtilities.getscaledImageIcon(icon, 16, -1));
        }
        if (action.getAccelerator() != null) {
            menuItem.setAccelerator(action.getAccelerator());
        }
        return menuItem;
    }

    // private JDAction removeFinished;

    public static JMenuItem getJMenuItem(final MenuItem mi) {
        switch (mi.getID()) {
        case MenuItem.SEPARATOR:
            return null;
        case MenuItem.NORMAL:
            JMenuItem m = new JMenuItem(new JDMenuAction(mi));

            return m;

        case MenuItem.TOGGLE:
            JCheckBoxMenuItem m2 = new JCheckBoxMenuItem(new JDMenuAction(mi));
            JDUtilities.getLogger().info("SELECTED: " + mi.isSelected());
            if (mi.isSelected()) {
                m2.setIcon(JDTheme.II("gui.images.selected"));
            } else {
                m2.setIcon(JDTheme.II("gui.images.unselected"));
            }
            // m2.setIcon(JDTheme.I("gui.images.selected"));

            // m2.addActionListener(mi.getActionListener());
            // m2.setSelected(mi.isSelected());
            return m2;
        case MenuItem.CONTAINER:
            JMenu m3 = new JMenu(mi.getTitle());

            m3.addMenuListener(new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    JMenu m = (JMenu) e.getSource();
                    m.removeAll();
                    JMenuItem c;
                    if (mi.getSize() == 0) m.setEnabled(false);
                    for (int i = 0; i < mi.getSize(); i++) {
                        c = SimpleGUI.getJMenuItem(mi.get(i));
                        if (c == null) {
                            m.addSeparator();
                        } else {
                            m.add(c);
                        }

                    }
                }

            });

            return m3;
        }
        return null;
    }

    public static Dimension getLastDimension(Component child, String key) {
        if (key == null) {
            key = child.getName();
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        Object loc = guiConfig.getProperty("DIMENSION_OF_" + key);
        if (loc != null && loc instanceof Dimension) {
            if (((Dimension) loc).width > width) {
                ((Dimension) loc).width = width;
            }
            if (((Dimension) loc).height > height) {
                ((Dimension) loc).height = height;
            }
            return (Dimension) loc;
        }

        return null;
    }

    public static Point getLastLocation(Component parent, String key, Component child) {
        if (key == null) {
            key = child.getName();
        }
        Object loc = guiConfig.getProperty("LOCATION_OF_" + key);
        // JDUtilities.getLogger().info("Get dim of " + "LOCATION_OF_" + key + "
        // : " + loc);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        if (loc != null && loc instanceof Point && ((Point) loc).getX() < width && ((Point) loc).getY() < height) {
            if (((Point) loc).getX() < 0) {
                ((Point) loc).x = 0;
            }
            if (((Point) loc).getY() < 0) {
                ((Point) loc).y = 0;
            }
            return (Point) loc;
        }
        return JDUtilities.getCenterOfComponent(parent, child);
    }

    public static void restoreWindow(JFrame parent, Object object, Component component) {
        if (parent == null) {
            parent = CURRENTGUI.getFrame();
        }
        // JDUtilities.getLogger().info("Restore Position of "+component);
        Point point = SimpleGUI.getLastLocation(parent, null, component);
        if (point.y < 0) {
            point.y = 0;
        }
        if (point.x < 0) {
            point.x = 0;
        }
        component.setLocation(point);
        if (SimpleGUI.getLastDimension(component, null) != null) {
            Dimension dim = SimpleGUI.getLastDimension(component, null);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            dim.width = Math.min(dim.width, screenSize.width);
            dim.height = Math.min(dim.height, screenSize.height);
            component.setSize(dim);

            // JDUtilities.getLogger().info("Default size:
            // "+SimpleGUI.getLastDimension(component, null));
        } else {
            // JDUtilities.getLogger().info("Default dim");
            component.validate();
        }

    }

    public static void saveLastDimension(Component child, String key) {
        if (key == null) {
            key = child.getName();
        }
        guiConfig.setProperty("DIMENSION_OF_" + key, child.getSize());
        guiConfig.save();
        // JDUtilities.getLogger().info("DIMEN VOR: " + "DIMENSION_OF_" + key +
        // " : " + child.getSize());

    }

    public static void saveLastLocation(Component parent, String key) {
        if (key == null) {
            key = parent.getName();
        }

        if (parent.isShowing()) {
            guiConfig.setProperty("LOCATION_OF_" + key, parent.getLocationOnScreen());
            guiConfig.save();
            // JDUtilities.getLogger().info("LOCATION_OF_ VOR: " +
            // "LOCATION_OF_" + key + " : " + parent.getLocationOnScreen());

        }

    }

    // private JDAction actionItemsTop;

    // private JDAction actionItemsUp;

    // private JDAction actionItemsDown;

    // private JDAction actionItemsBottom;

    public static void setUIManager() {
        if (uiInitated) { return; }
        uiInitated = true;
        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();

        boolean plafisSet = false;
        String paf = guiConfig.getStringProperty(PARAM_PLAF, null);
        if (paf != null) {
            for (LookAndFeelInfo element : info) {
                if (element.getName().equals(paf)) {
                    try {
                        UIManager.setLookAndFeel(element.getClassName());
                        plafisSet = true;
                        break;
                    } catch (UnsupportedLookAndFeelException e) {
                    } catch (ClassNotFoundException e) {

                    } catch (InstantiationException e) {

                    } catch (IllegalAccessException e) {

                    }
                }
            }
        } else {
            for (int i = 0; i < info.length; i++) {
                if (!info[i].getName().matches("(?is).*(metal|motif).*")) {
                    try {
                        UIManager.setLookAndFeel(info[i].getClassName());
                        plafisSet = true;
                        break;
                    } catch (UnsupportedLookAndFeelException e) {
                    } catch (ClassNotFoundException e) {

                    } catch (InstantiationException e) {

                    } catch (IllegalAccessException e) {

                    }
                }
            }
        }
        if (!plafisSet) {
            try {
                UIManager.setLookAndFeel(new WindowsLookAndFeel());
            } catch (UnsupportedLookAndFeelException e) {
            }
        }
        // try {
        // System.setProperty("nimrodlf.themeFile", "arena.theme");
        // System.setProperty("nimrodlf.themeFile", "arena2.theme");
        // System.setProperty("nimrodlf.themeFile", "ash.theme");
        // System.setProperty("nimrodlf.themeFile", "night.theme");
        // UIManager.setLookAndFeel(new NimRODLookAndFeel());
        // } catch (UnsupportedLookAndFeelException e1) {
        // e1.printStackTrace();
        // }
    }

    private JDAction actionAbout;

    // private JDAction actionCes;

    private JDAction actionClipBoard;

    private JDAction actionConfig;

    private JDAction actionDnD;

    private JDAction actionExit;

    private JDAction actionHelp;

    // private JDAction actionSearch;

    private JDAction actioninstallJDU;

    private JDAction actionItemsAdd;

    private JDAction actionItemsBottom;

    private JDAction actionItemsDelete;

    private JDAction actionItemsDown;

    private JDAction actionItemsTop;

    private JDAction actionItemsUp;

    private JDAction actionLoadDLC;

    private JDAction actionLog;

    private JDAction actionPasswordlist;

    private JDAction actionPause;

    private JDAction actionReconnect;
    private JDAction actionSaveDLC;
    private JDAction actionStartStopDownload;

    private JDAction actionTester;

    private JDAction actionUnrar;

    private JDAction actionUpdate;

    private JDAction actionWiki;

    // private JButton btnCes;

    private JButton btnClipBoard;

    private JButton btnPause;

    private JButton btnReconnect;

    /**
     * Ein Togglebutton zum Starten / Stoppen der Downloads
     */
    public JButton btnStartStop;

    // private SwingWorker warningWorker;

    private JDAction doReconnect;

    Dropper dragNDrop;

    /**
     * Das Hauptfenster
     */
    private JFrame frame;

    private LinkGrabber linkGrabber;

    /**
     * Komponente, die alle Downloads anzeigt
     */
    private DownloadLinksTreeTablePanel linkListPane;
    private LogDialog logDialog;

    private Logger logger = JDUtilities.getLogger();

    /**
     * Die Menüleiste
     */
    private JMenuBar menuBar;

    private JMenuItem menViewLog = null;

    /**
     * Komponente, die den Fortschritt aller Plugins anzeigt
     */
    private TabProgress progressBar;

    private JSplitPane splitpane;

    /**
     * TabbedPane
     */
    // private JTabbedPane tabbedPane;
    /**
     * Die Statusleiste für Meldungen
     */
    private StatusBar statusBar;

    /**
     * Toolleiste für Knöpfe
     */
    private JToolBar toolBar;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #fireUIEvent(UIEvent)} ein Event losgeschickt wird.
     */
    public Vector<UIListener> uiListener = null;

    private JLabel warning;

    private Thread warningWorker;

    /**
     * Das Hauptfenster wird erstellt
     */
    public SimpleGUI() {
        super();
        SimpleGUI.setUIManager();
        uiListener = new Vector<UIListener>();
        frame = new JFrame();
        // tabbedPane = new JTabbedPane();
        menuBar = new JMenuBar();
        toolBar = new JToolBar();
        // toolBar.setBackground(Color.orange);
        // toolBar.setOpaque(false);

        frame.addWindowListener(this);
        frame.setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.jd_logo")));
        frame.setTitle(JDUtilities.getJDTitle());
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        initActions();
        initMenuBar();
        buildUI();

        frame.setName("MAINFRAME");
        Dimension dim = SimpleGUI.getLastDimension(frame, null);
        if (dim == null) {
            dim = new Dimension(600, 600);
        }
        frame.setPreferredSize(dim);
        frame.setLocation(SimpleGUI.getLastLocation(null, null, frame));
        frame.pack();
        frame.setExtendedState(guiConfig.getIntegerProperty("MAXIMIZED_STATE", JFrame.NORMAL));
        frame.setVisible(true);
        // DND
        dragNDrop = new Dropper(new JFrame());
        dragNDrop.addUIListener(this);
        // Ruft jede sekunde ein UpdateEvent auf

        /*
         * jago: Fixed very bad style of coding which often is the cause of
         * fully frozen applications!!! After the JFrame is realized (e.g.
         * setVisible, see above) it is not allowed to access any Swing object
         * from outside of the EDT. Even worse in this case the code created a
         * new Thread that constantly executed interval() which accesses the EDT
         * from outside every second!
         * 
         * Look out for similar code in the rest of JD whenever you encounter
         * unreproducible, seemingly random crashes or an unresponsive/frozen
         * GUI.
         */
        new Thread("guiworker") {
            public void run() {
                while (true) {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            interval();
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

    }

    /**
     * Hier werden die Aktionen ausgewertet und weitergeleitet
     * 
     * @param e
     *            Die erwünschte Aktion
     */
    public void actionPerformed(ActionEvent e) {
        JDSounds.PT("sound.gui.clickToolbar");
        switch (e.getID()) {
        case JDAction.ITEMS_MOVE_UP:
        case JDAction.ITEMS_MOVE_DOWN:
        case JDAction.ITEMS_MOVE_TOP:
        case JDAction.ITEMS_MOVE_BOTTOM:
            linkListPane.moveSelectedItems(e.getID());
            break;
        case JDAction.APP_ALLOW_RECONNECT:
            logger.finer("Allow Reconnect");
            boolean checked = !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
            if (checked) {
                displayMiniWarning(JDLocale.L("gui.warning.reconnect.hasbeendisabled", "Reconnect deaktiviert!"), JDLocale.L("gui.warning.reconnect.hasbeendisabled.tooltip", "Um erfolgreich einen Reconnect durchführen zu können muss diese Funktion wieder aktiviert werden."), 10000);
            }

            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, checked);

            JDUtilities.saveConfig();

            /*
             * Steht hier, weil diese Funktion(toggleReconnect) direkt vom
             * Trayicon Addon aufgerufen wird und ich dennoch die Gui aktuell
             * halten will
             */

            // btnReconnect.setIcon(new
            // ImageIcon(JDUtilities.getImage(getDoReconnectImage())));
            break;
        case JDAction.APP_PAUSE_DOWNLOADS:
            btnPause.setSelected(!btnPause.isSelected());
            fireUIEvent(new UIEvent(this, UIEvent.UI_PAUSE_DOWNLOADS, btnPause.isSelected()));
            btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));
            break;
        case JDAction.APP_TESTER:
            logger.finer("Test trigger pressed");
            displayMiniWarning(JDLocale.L("interaction.trigger.testtrigger.shortWarn", "Testtrigger wurde ausgelöst"), JDLocale.L("interaction.trigger.testtrigger.toolTip", "Der Testauslöser für den Eventmanager wurde manuell ausgelöst."), 5000);
            Interaction.handleInteraction(Interaction.INTERACTION_TESTTRIGGER, false);

            break;
        case JDAction.APP_UNRAR:
            logger.finer("Unrar");
            JDUtilities.getController().getUnrarModule().interact(null);
            break;
        case JDAction.APP_CLIPBOARD:
            logger.finer("Clipboard");
            ClipboardHandler.getClipboard().toggleActivation();

            break;
        // case JDAction.APP_CES:
        // logger.finer("CES");
        // CES.toggleActivation();
        // btnCes.setIcon(CES.getImage());
        // break;
        case JDAction.APP_PASSWORDLIST:
            new jdUnrarPasswordListDialog(((SimpleGUI) JDUtilities.getController().getUiInterface()).getFrame()).setVisible(true);
            break;
        case JDAction.APP_START_STOP_DOWNLOADS:
            logger.finer("Start Stop Downloads");
            // btnStartStop.setSelected(!btnStartStop.isSelected());
            // if (!btnStartStop.isSelected()) btnStartStop.setEnabled(false);
            startStopDownloads();
            btnStartStop.setIcon(new ImageIcon(JDUtilities.getImage(getStartStopDownloadImage())));
            btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));
            break;
        case JDAction.APP_SAVE_DLC:
            JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
            fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
            fc.showSaveDialog(frame);
            File ret = fc.getSelectedFile();
            if (ret == null) { return; }
            if (JDUtilities.getFileExtension(ret) == null || !JDUtilities.getFileExtension(ret).equalsIgnoreCase("dlc")) {

                ret = new File(ret.getAbsolutePath() + ".dlc");
            }
            if (ret != null) {
                fireUIEvent(new UIEvent(this, UIEvent.UI_SAVE_LINKS, ret));
            }
            break;
        case JDAction.APP_LOAD_DLC:
            fc = new JDFileChooser("_LOADSAVEDLC");
            fc.setFileFilter(new JDFileFilter(null, ".dlc|.rsdf|.ccf|.linkbackup", true));
            fc.showOpenDialog(frame);
            ret = fc.getSelectedFile();
            if (ret != null) {
                fireUIEvent(new UIEvent(this, UIEvent.UI_LOAD_LINKS, ret));
            }
            break;
        case JDAction.APP_INSTALL_JDU:
            fc = new JDFileChooser("_INSTALLJDU");
            fc.setFileFilter(new JDFileFilter(null, ".jdu", true));
            fc.showOpenDialog(frame);
            ret = fc.getSelectedFile();
            if (ret != null) {

                UnZip u = new UnZip(ret, JDUtilities.getResourceFile("."));
                File[] files;
                try {
                    files = u.extract();
                    if (files != null) {
                        boolean c = false;
                        for (File element : files) {
                            if (element.getAbsolutePath().endsWith("readme.html")) {

                                String html = JDUtilities.getLocalFile(element);
                                if (Regex.matches(html, "src\\=\"(.*?)\"")) {
                                    html = new Regex(html, "src\\=\"(.*?)\"").getFirstMatch();
                                    html = JDLocale.L("modules.packagemanager.loadednewpackage.title", "Paket Update installiert") + "<hr><b>" + ret.getName() + "</b><hr><a href='" + html + "'>" + JDLocale.L("modules.packagemanager.loadednewpackage.more", "More Information & Installnotes") + "</a>";
                                }

                                JDUtilities.getGUI().showCountdownConfirmDialog(html, 30);
                                c = true;
                            }
                        }
                        if (!c) {
                            JDUtilities.getGUI().showCountdownConfirmDialog(JDLocale.L("modules.packagemanager.loadednewpackage.title", "Paket Update installiert") + "<hr><b>" + ret.getName() + "</b>", 15);
                        }

                    }
                } catch (Exception e3) {
                    e3.printStackTrace();
                }

            }
            break;
        case JDAction.APP_EXIT:
            frame.setVisible(false);
            frame.dispose();
            fireUIEvent(new UIEvent(this, UIEvent.UI_EXIT));
            break;
        case JDAction.APP_LOG:
            logDialog.setVisible(!logDialog.isVisible());
            menViewLog.setSelected(!logDialog.isVisible());
            break;
        case JDAction.APP_RECONNECT:
            doReconnect();
            break;
        case JDAction.APP_UPDATE:
            fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_UPDATE));

            break;
        case JDAction.ITEMS_REMOVE:
            if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (showConfirmDialog(JDLocale.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?"))) {
                    linkListPane.removeSelectedLinks();
                }
            } else {
                linkListPane.removeSelectedLinks();
            }
            break;
        case JDAction.ITEMS_DND:
            toggleDnD();
            break;
        case JDAction.ABOUT:
            JDAboutDialog.getDialog().setVisible(true);
            break;
        case JDAction.ITEMS_ADD:
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String cb = "";
            try {
                cb = (String) clipboard.getData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException e1) {
            } catch (IOException e1) {
            }
            LinkInputDialog inputDialog = new LinkInputDialog(frame, cb.trim());
            String data = inputDialog.getLinksString();
            if (data != null) {
                fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS, data));
            }
            break;
        // case JDAction.APP_SEARCH:
        // SearchDialog s = new SearchDialog(this.getFrame());
        // data = s.getText();
        // if (!data.endsWith(":::")) {
        // logger.info(data);
        // if (data != null) {
        // fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS,
        // data));
        // }
        // }
        // break;
        //                
        case JDAction.HELP:
            try {
                JLinkButton.openURL(JDLocale.L("gui.support.forumurl", "http://www.the-lounge.org/viewforum.php?f=291"));
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            break;

        case JDAction.WIKI:
            try {
                JLinkButton.openURL(JDLocale.L("gui.support.wikiurl", "http://jdownloader.org/wiki/index.php?title=JDownloader_Wiki:Portal_English"));
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            break;
        case JDAction.APP_CONFIGURATION:
            ConfigurationDialog.showConfig(frame, this);

            break;
        }

    }

    public synchronized void addLinksToGrabber(Vector<DownloadLink> links) {
        logger.info("GRAB");
        DownloadLink[] linkList = links.toArray(new DownloadLink[] {});
        if (linkGrabber != null && (!linkGrabber.isDisplayable() || !linkGrabber.isVisible())) {
            logger.info("Linkgrabber should be disposed");
            linkGrabber.dispose();
            linkGrabber = null;
        }
        if (linkGrabber == null) {
            logger.info("new linkgrabber");
            linkGrabber = new LinkGrabber(this, linkList);

        } else {
            logger.info("add to grabber");
            linkGrabber.addLinks(linkList);
        }
        dragNDrop.setText("Grabbed: " + linkList.length + " (+" + ((Vector<?>) links).size() + ")");
    }

    public void addUIListener(UIListener listener) {
        synchronized (uiListener) {
            uiListener.add(listener);
        }
    }

    /**
     * Hier wird die komplette Oberfläche der Applikation zusammengestrickt
     */
    private void buildUI() {
        // tabbedPane = new JTabbedPane();
        CURRENTGUI = this;
        linkListPane = new DownloadLinksTreeTablePanel(this);
        progressBar = new TabProgress();
        statusBar = new StatusBar();
        splitpane = new JSplitPane();
        splitpane.setBottomComponent(progressBar);
        // JPanel tmp=new JPanel(new BorderLayout());
        splitpane.setTopComponent(linkListPane);
        splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        btnStartStop = createMenuButton(actionStartStopDownload);
        btnPause = createMenuButton(actionPause);
        btnPause.setEnabled(false);
        btnPause.setSelected(false);
        btnReconnect = createMenuButton(doReconnect);

        btnClipBoard = createMenuButton(actionClipBoard);
        btnReconnect.setSelected(false);
        btnClipBoard.setSelected(false);
        // btnCes = createMenuButton(actionCes);
        toolBar.setFloatable(false);
        // toolBar.add(createMenuButton(this.actionLoadDLC));
        // toolBar.add(createMenuButton(this.actionSaveDLC));
        // toolBar.addSeparator();
        toolBar.add(btnStartStop);
        toolBar.add(btnPause);
        toolBar.add(createMenuButton(actionItemsAdd));
        toolBar.add(createMenuButton(actionItemsDelete));
        // toolBar.add(btnSearch);
        toolBar.addSeparator();

        toolBar.add(createMenuButton(actionItemsBottom));
        toolBar.add(createMenuButton(actionItemsDown));
        toolBar.add(createMenuButton(actionItemsUp));
        toolBar.add(createMenuButton(actionItemsTop));
        toolBar.addSeparator();
        toolBar.add(createMenuButton(actionConfig));
        toolBar.addSeparator();
        toolBar.add(btnReconnect);
        toolBar.add(createMenuButton(actionReconnect));
        toolBar.add(btnClipBoard);
        toolBar.addSeparator();
        toolBar.add(createMenuButton(actionUpdate));

        // if
        // (JDUtilities.getSubConfig("JAC").getBooleanProperty(Configuration.JAC_USE_CES,
        // false)) {
        // toolBar.add(btnCes);
        // }

        JPanel panel = new JPanel(new BorderLayout());
        int n = 2;
        toolBar.setBorder(new EmptyBorder(n, 0, n, 0));
        n = 5;
        panel.setBorder(new EmptyBorder(0, n, 0, n));
        JPanel toolbar2 = new JPanel(new BorderLayout(n, n));
        n = 3;
        statusBar.setBorder(new EmptyBorder(n, 0, n, 0));
        frame.setContentPane(panel);
        // frame.setLayout(new GridBagLayout());
        // JDUtilities.addToGridBag(frame, toolBar, 0, 0, 1, 1, 0, 0, null,
        // GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH);
        // JDUtilities.addToGridBag(frame, splitpane, 0, 1, 1, 1, 1, 1, null,
        // GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        // JDUtilities.addToGridBag(frame, statusBar, 0, 2, 1, 1, 0, 0, null,
        // GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        JLabel advert = new JLabel(JDUtilities.getscaledImageIcon(JDTheme.V("gui.images.jd_logo"), -1, 32));
        advert.setOpaque(true);
        toolbar2.setOpaque(true);
        advert.setBackground(Color.orange);
        advert.setBorder(new EmptyBorder(0, n, 0, n));
        toolbar2.setBackground(Color.orange);
        toolbar2.add(toolBar, BorderLayout.CENTER);
        // toolbar2.add(advert, BorderLayout.EAST);
        panel.add(toolbar2, BorderLayout.NORTH);
        panel.add(splitpane, BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);

        // Einbindung des Log Dialogs
        logDialog = new LogDialog(frame, logger);
        logDialog.addWindowListener(new LogDialogWindowAdapter());
    }

    public void controlEvent(final ControlEvent event) {
        // Moved the whole content of this method into a Runnable run by
        // invokeLater(). Ensures that everything inside is executed on the EDT.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                switch (event.getID()) {

                case ControlEvent.CONTROL_PLUGIN_ACTIVE:
                    logger.info("Plugin Aktiviert: " + event.getSource());
                    // setPluginActive((PluginForDecrypt) event.getParameter(),
                    // true);
                    if (event.getSource() instanceof Interaction) {
                        logger.info("Interaction start. ");
                        statusBar.setText("Interaction: " + ((Interaction) event.getSource()).getInteractionName());
                        frame.setTitle(JDUtilities.JD_TITLE + " |Aktion: " + ((Interaction) event.getSource()).getInteractionName());

                    }
                    break;
                case ControlEvent.CONTROL_SYSTEM_EXIT:
                    SimpleGUI.this.getFrame().setVisible(false);
                    SimpleGUI.this.getFrame().dispose();
                    break;
                case ControlEvent.CONTROL_PLUGIN_INACTIVE:
                    logger.info("Plugin Deaktiviert: " + event.getSource());
                    // setPluginActive((PluginForDecrypt) event.getParameter(),
                    // false);
                    if (event.getSource() instanceof Interaction) {
                        logger.info("Interaction zu ende. rest status");

                        statusBar.setText(null);
                        frame.setTitle(JDUtilities.getJDTitle());
                    }
                    break;
                case ControlEvent.CONTROL_ON_PROGRESS:

                    handleProgressController((ProgressController) event.getSource(), event.getParameter());

                    break;

                case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
                    // showTrayTip("Downloads", "All downloads finished");
                    logger.info("ALL FINISHED");
                    // btnStartStop.setSelected(false);
                    btnStartStop.setEnabled(true);
                    btnPause.setEnabled(false);
                    btnPause.setSelected(false);
                    btnStartStop.setIcon(new ImageIcon(JDUtilities.getImage(getStartStopDownloadImage())));
                    btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));
                    break;

                case ControlEvent.CONTROL_DISTRIBUTE_FINISHED:
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_ACTIVE:
                    frame.setTitle(JDUtilities.getJDTitle() + " - Downloads werden abgebrochen");
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_TERMINATION_INACTIVE:
                    frame.setTitle(JDUtilities.getJDTitle());
                    break;
                case ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED:
                    if (event.getSource().getClass() == JDController.class) {
                    }
                    break;

                case ControlEvent.CONTROL_JDPROPERTY_CHANGED:
                    Property p = (Property) event.getSource();
                    if (p == JDUtilities.getConfiguration() && event.getParameter().equals(Configuration.PARAM_DISABLE_RECONNECT)) {
                        String img = getDoReconnectImage();
                        logger.info("JJJ" + img);
                        Image img2 = JDUtilities.getImage(img);
                        // boolean sel = btnReconnect.isSelected();
                        btnReconnect.setIcon(new ImageIcon(img2));
                        // btnReconnect.setSelected(p.getBooleanProperty(
                        // Configuration.PARAM_DISABLE_RECONNECT));
                        // btnReconnect.setSelected(true);
                    }

                    if (p == JDUtilities.getConfiguration() && event.getParameter().equals(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE)) {
                        btnClipBoard.setIcon(new ImageIcon(JDUtilities.getImage(getClipBoardImage())));
                        // btnClipBoard.setSelected(p.getBooleanProperty(
                        // Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE));

                    }
                    break;
                case ControlEvent.CONTROL_DOWNLOAD_START:
                    // only in this way the button state is correctly set
                    // controller.startDownloads() is called by button itself so
                    // it
                    // cannot handle this

                    btnStartStop.setEnabled(true);
                    btnPause.setEnabled(true);
                    // btnStartStop.setSelected(true);
                    // btnStartStop.setSelected(!btnStartStop.isSelected());
                    // if (!btnStartStop.isSelected())
                    // btnStartStop.setEnabled(false);
                    // this.startStopDownloads();
                    btnStartStop.setIcon(new ImageIcon(JDUtilities.getImage(getStartStopDownloadImage())));
                    btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));
                    break;

                case ControlEvent.CONTROL_DOWNLOAD_STOP:
                    btnStartStop.setEnabled(true);
                    btnPause.setEnabled(true);
                    // btnStartStop.setSelected(false);
                    btnStartStop.setIcon(new ImageIcon(JDUtilities.getImage(getStartStopDownloadImage())));
                    btnPause.setIcon(new ImageIcon(JDUtilities.getImage(getPauseImage())));
                    break;
                }
            }
        });
    }

    private JButton createMenuButton(JDAction action) {
        JButton bt = new JButton(action);
        bt.setFocusPainted(false);
        bt.setBorderPainted(false);
        bt.setOpaque(false);
        bt.setText(null);
        return bt;
    }

    public void displayMiniWarning(final String shortWarn, final String toolTip, final int showtime) {
        if (shortWarn == null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    warning.setVisible(false);

                    warning.setText("");
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    warning.setVisible(true);

                    warning.setText(shortWarn);
                    warning.setToolTipText(toolTip);
                }
            });
            if (warningWorker != null) {
                warningWorker.interrupt();
            }
            warningWorker = new Thread() {

                public void run() {
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(300);
                        } catch (Exception e) {
                        }
                        if (this.isInterrupted()) { return; }
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                warning.setEnabled(false);
                            }
                        });
                        if (this.isInterrupted()) { return; }
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                        }
                        if (this.isInterrupted()) { return; }
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                warning.setEnabled(true);
                            }
                        });
                        if (this.isInterrupted()) { return; }

                    }

                }
            };
            warningWorker.start();

            if (showtime > 0) {
                new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(showtime);
                        } catch (InterruptedException e) {

                            e.printStackTrace();
                        }
                        displayMiniWarning(null, null, 0);
                    }
                }.start();
            }

        }
    }

    public void doReconnect() {
        // statusBar.setText("Interaction: HTTPReconnect");
        // dragNDrop.setText("Reconnect....");
        // frame.setTitle(JDUtilities.JD_TITLE+" |Aktion:
        // HTTPReconnect");
        boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, true);
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
        if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
            int confirm = JOptionPane.showConfirmDialog(frame, JDLocale.L("gui.reconnect.confirm", "Wollen Sie sicher eine neue Verbindung aufbauen?"));
            if (confirm == JOptionPane.OK_OPTION) {
                fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_RECONNECT));
            }
        } else {
            fireUIEvent(new UIEvent(this, UIEvent.UI_INTERACT_RECONNECT));
        }
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);

        // statusBar.setText(null);
        // frame.setTitle(JDUtilities.JD_TITLE);
        // dragNDrop.setText("");
    }

    public void fireUIEvent(UIEvent uiEvent) {
        synchronized (uiListener) {
            Iterator<UIListener> recIt = uiListener.iterator();
            while (recIt.hasNext()) {
                ((UIListener) recIt.next()).uiEvent(uiEvent);
            }
        }
    }

    //
    // /**
    // * Delligiert die Pluginevents weiter an das host/decryptpanel.
    // * CHangedEvents werden abgefangen und im sekundeninterval weitergegeben.
    // */
    // public void delegatedPluginEvent(PluginEvent event) {
    //     
    // if (event.getSource() instanceof PluginForHost) {
    // //linkListPane.pluginEvent(event);
    // return;
    // }
    // if (event.getSource() instanceof PluginForDecrypt) {
    // // progressBar.pluginEvent(event);
    // splitpane.setDividerLocation(0.8);
    // return;
    // }
    // if (event.getSource() instanceof PluginOptional) {
    // JDAction actionToDo = null;
    // switch (event.getEventID()) {
    // case PluginEvent.PLUGIN_CONTROL_DND:
    // actionToDo = actionDnD;
    // break;
    // case PluginEvent.PLUGIN_CONTROL_EXIT:
    // actionToDo = actionExit;
    // break;
    // case PluginEvent.PLUGIN_CONTROL_RECONNECT:
    // actionToDo = actionReconnect;
    // break;
    // case PluginEvent.PLUGIN_CONTROL_SHOW_CONFIG:
    // actionToDo = actionConfig;
    // break;
    // case PluginEvent.PLUGIN_CONTROL_SHOW_UI:
    // frame.setVisible((Boolean) event.getParameter1());
    // break;
    // case PluginEvent.PLUGIN_CONTROL_START_STOP:
    // actionToDo = actionStartStopDownload;
    // break;
    // }
    // if (actionToDo != null) actionPerformed(new ActionEvent(this,
    // actionToDo.getActionID(), ""));
    //
    // }
    // }

    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress, String def) {
        CaptchaDialog captchaDialog = new CaptchaDialog(frame, plugin, captchaAddress, def);
        // frame.toFront();

        captchaDialog.setVisible(true);
        logger.info("Returned: " + captchaDialog.getCaptchaText());
        return captchaDialog.getCaptchaText();
    }

    // private void showTrayTip(String header, String msg) {
    // if (tray == null) return;
    // this.tray.showTip(header, msg);
    // }
    // public Vector<DownloadLink> getDownloadLinks() {
    // if (linkListPane != null) return linkListPane.getLinks();
    // return null;
    // }

    // public void setDownloadLinks(Vector<DownloadLink> links) {
    // if (linkListPane != null) {
    // linkListPane.setDownloadLinks(links.toArray(new DownloadLink[] {}));
    // }
    // }

    // public void addDownloadLinks(Vector<DownloadLink> links) {
    // DownloadLink[] linkList = links.toArray(new DownloadLink[] {});
    // if (linkListPane != null) {
    // linkListPane.setDownloadLinks(linkList);
    // }
    // }

    private String getClipBoardImage() {
        if (ClipboardHandler.getClipboard().isEnabled()) {
            return JDTheme.V("gui.images.clipboardon");
        } else {
            return JDTheme.V("gui.images.clipboardoff");
        }

    }

    // public void setPluginActive(Plugin plugin, boolean isActive) {
    // if (plugin instanceof PluginForDecrypt) {
    // statusBar.setPluginForDecryptActive(isActive);
    // }
    // else {
    // statusBar.setPluginForHostActive(isActive);
    // }
    // }

    private String getDoReconnectImage() {
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false)) {
            return JDTheme.V("gui.images.reconnect_ok");
        } else {
            return JDTheme.V("gui.images.reconnect_bad");
        }
    }

    public JFrame getFrame() {
        return frame;
    }

    private String getPauseImage() {
        if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
            if (btnPause.isSelected()) {
                return JDTheme.V("gui.images.stop_after_active");
            } else {
                return JDTheme.V("gui.images.stop_after");
            }
        } else {
            return JDTheme.V("gui.images.stop_after");
        }

    }

    private String getStartStopDownloadImage() {
        if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING) {
            return JDTheme.V("gui.images.next");
        } else {
            return JDTheme.V("gui.images.stop");
        }
    }

    private void handleProgressController(ProgressController source, Object parameter) {

        if (!progressBar.hasController(source) && !source.isFinished()) {
            // logger.info("addController " + source);
            progressBar.addController(source);
        } else if (source.isFinished()) {
            // logger.info("removeController " + source);
            progressBar.removeController(source);
        } else {
            // logger.info("updateController: " + source);
            progressBar.updateController(source);
        }

        if (progressBar.getControllers().size() > 0) {
            splitpane.setDividerLocation(0.8);

        }
    }

    /**
     * Die Aktionen werden initialisiert
     */
    public void initActions() {
        actionStartStopDownload = new JDAction(this, getStartStopDownloadImage(), "action.start", JDAction.APP_START_STOP_DOWNLOADS);
        actionPause = new JDAction(this, getPauseImage(), "action.pause", JDAction.APP_PAUSE_DOWNLOADS);
        actionItemsAdd = new JDAction(this, JDTheme.V("gui.images.add"), "action.add", JDAction.ITEMS_ADD);
        actionDnD = new JDAction(this, JDTheme.V("gui.images.clipboard"), "action.dnd", JDAction.ITEMS_DND);
        actioninstallJDU = new JDAction(this, JDTheme.V("gui.load"), "action.install", JDAction.APP_INSTALL_JDU);
        actionLoadDLC = new JDAction(this, JDTheme.V("gui.images.load"), "action.load", JDAction.APP_LOAD_DLC);
        actionSaveDLC = new JDAction(this, JDTheme.V("gui.images.save"), "action.save", JDAction.APP_SAVE_DLC);
        actionExit = new JDAction(this, JDTheme.V("gui.images.exit"), "action.exit", JDAction.APP_EXIT);
        actionLog = new JDAction(this, JDTheme.V("gui.images.terminal"), "action.viewlog", JDAction.APP_LOG);
        actionTester = new JDAction(this, null, "action.tester", JDAction.APP_TESTER);
        actionUnrar = new JDAction(this, null, "action.unrar", JDAction.APP_UNRAR);
        actionClipBoard = new JDAction(this, getClipBoardImage(), "action.clipboard", JDAction.APP_CLIPBOARD);
        // actionCes = new JDAction(this, CES.getCesImageString(), "action.ces",
        // JDAction.APP_CES);
        actionPasswordlist = new JDAction(this, null, "action.passwordlist", JDAction.APP_PASSWORDLIST);
        actionConfig = new JDAction(this, JDTheme.V("gui.images.configuration"), "action.configuration", JDAction.APP_CONFIGURATION);
        actionReconnect = new JDAction(this, JDTheme.V("gui.images.reconnect"), "action.reconnect", JDAction.APP_RECONNECT);
        actionUpdate = new JDAction(this, JDTheme.V("gui.images.update_manager"), "action.update", JDAction.APP_UPDATE);
        actionItemsDelete = new JDAction(this, JDTheme.V("gui.images.delete"), "action.edit.items_remove", JDAction.ITEMS_REMOVE);
        actionItemsTop = new JDAction(this, JDTheme.V("gui.images.go_top"), "action.edit.items_top", JDAction.ITEMS_MOVE_TOP);
        actionItemsUp = new JDAction(this, JDTheme.V("gui.images.top"), "action.edit.items_up", JDAction.ITEMS_MOVE_UP);
        actionItemsDown = new JDAction(this, JDTheme.V("gui.images.down"), "action.edit.items_down", JDAction.ITEMS_MOVE_DOWN);
        actionItemsBottom = new JDAction(this, JDTheme.V("gui.images.go_bottom"), "action.edit.items_bottom", JDAction.ITEMS_MOVE_BOTTOM);
        doReconnect = new JDAction(this, getDoReconnectImage(), "action.doReconnect", JDAction.APP_ALLOW_RECONNECT);
        actionHelp = new JDAction(this, JDTheme.V("gui.images.help"), "action.help", JDAction.HELP);
        actionWiki = new JDAction(this, null, "action.wiki", JDAction.WIKI);
        actionAbout = new JDAction(this, JDTheme.V("gui.images.jd_logo"), "action.about", JDAction.ABOUT);
    }

    /**
     * Das Menü wird hier initialisiert
     */
    public void initMenuBar() {
        JMenu menFile = new JMenu(JDLocale.L("gui.menu.file"));
        JMenu menExtra = new JMenu(JDLocale.L("gui.menu.extra"));
        JMenu menAddons = new JMenu(JDLocale.L("gui.menu.addons", "Addons"));
        JMenu menHelp = new JMenu(JDLocale.L("gui.menu.plugins.help", "?"));

        menViewLog = SimpleGUI.createMenuItem(actionLog);
        if (actionLog.getAccelerator() != null) {
            menViewLog.setAccelerator(actionLog.getAccelerator());
        }

        // add menus to parents

        // Adds the menus form the Addons
        HashMap<String, PluginOptional> addons = JDUtilities.getPluginsOptional();
       
        Iterator<String> e = addons.keySet().iterator();
        JMenuItem mi;

        while (e.hasNext()) {
            final PluginOptional helpplugin = addons.get(e.next());

            if (helpplugin.createMenuitems() != null && JDUtilities.getConfiguration().getBooleanProperty("OPTIONAL_PLUGIN_" + helpplugin.getPluginName(), false)) {

                MenuItem m = new MenuItem(MenuItem.CONTAINER, helpplugin.getPluginName(), 0);
                m.setItems(helpplugin.createMenuitems());
                mi = SimpleGUI.getJMenuItem(m);
                if (mi != null) {
                    menAddons.add(mi);

                    ((JMenu) mi).removeMenuListener(((JMenu) mi).getMenuListeners()[0]);
                    ((JMenu) mi).addMenuListener(new MenuListener() {
                        public void menuCanceled(MenuEvent e) {
                        }

                        public void menuDeselected(MenuEvent e) {
                        }

                        public void menuSelected(MenuEvent e) {
                            JMenu m = (JMenu) e.getSource();

                            m.removeAll();
                            for (MenuItem menuItem : helpplugin.createMenuitems()) {
                                m.add(SimpleGUI.getJMenuItem(menuItem));

                            }
                        }

                    });
                } else {
                    menAddons.addSeparator();
                }
            }
        }

        if (menAddons.getItemCount() == 0) {
            menAddons.setEnabled(false);
        }
        // Adds the menus form the plugins
        JMenu menPlugins = new JMenu(JDLocale.L("gui.menu.plugins", "Plugins"));
        JMenu helpHost = new JMenu(JDLocale.L("gui.menu.plugins.phost", "Premium Hoster"));
        JMenu helpDecrypt = new JMenu(JDLocale.L("gui.menu.plugins.decrypt", "Decrypter"));
        JMenu helpContainer = new JMenu(JDLocale.L("gui.menu.plugins.container", "Container"));

        menPlugins.add(helpHost);
        menPlugins.add(helpDecrypt);
        menPlugins.add(helpContainer);

        for (Iterator<PluginForHost> it = JDUtilities.getPluginsForHost().iterator(); it.hasNext();) {
            final Plugin helpplugin = it.next();
            if (helpplugin.createMenuitems() != null) {
                MenuItem m = new MenuItem(MenuItem.CONTAINER, helpplugin.getPluginName(), 0);
                // m.setItems(helpplugin.createMenuitems());
                mi = SimpleGUI.getJMenuItem(m);
                if (mi != null) {
                    helpHost.add(mi);

                    ((JMenu) mi).removeMenuListener(((JMenu) mi).getMenuListeners()[0]);
                    ((JMenu) mi).addMenuListener(new MenuListener() {
                        public void menuCanceled(MenuEvent e) {
                        }

                        public void menuDeselected(MenuEvent e) {
                        }

                        public void menuSelected(MenuEvent e) {
                            JMenu m = (JMenu) e.getSource();
                            JMenuItem c;
                            m.removeAll();
                            for (MenuItem menuItem : helpplugin.createMenuitems()) {
                                c = SimpleGUI.getJMenuItem(menuItem);
                                if (c == null) {
                                    m.addSeparator();
                                } else {
                                    m.add(c);
                                }

                            }

                        }

                    });
                } else {
                    helpHost.addSeparator();
                }
            }
        }
        if (helpHost.getItemCount() == 0) {
            helpHost.setEnabled(false);
        }

        for (Iterator<PluginForDecrypt> it = JDUtilities.getPluginsForDecrypt().iterator(); it.hasNext();) {
            final Plugin helpplugin = it.next();
            if (helpplugin.createMenuitems() != null) {
                MenuItem m = new MenuItem(MenuItem.CONTAINER, helpplugin.getPluginName(), 0);

                mi = SimpleGUI.getJMenuItem(m);
                if (mi != null) {
                    helpDecrypt.add(mi);

                    ((JMenu) mi).removeMenuListener(((JMenu) mi).getMenuListeners()[0]);
                    ((JMenu) mi).addMenuListener(new MenuListener() {
                        public void menuCanceled(MenuEvent e) {
                        }

                        public void menuDeselected(MenuEvent e) {
                        }

                        public void menuSelected(MenuEvent e) {
                            JMenu m = (JMenu) e.getSource();
                            m.removeAll();
                            for (MenuItem menuItem : helpplugin.createMenuitems()) {
                                m.add(SimpleGUI.getJMenuItem(menuItem));

                            }

                        }

                    });
                } else {
                    helpDecrypt.addSeparator();
                }
            }
        }
        if (helpDecrypt.getItemCount() == 0) {
            helpDecrypt.setEnabled(false);
        }

        for (Iterator<PluginForContainer> it = JDUtilities.getPluginsForContainer().iterator(); it.hasNext();) {
            final Plugin helpplugin = it.next();
            if (helpplugin.createMenuitems() != null) {
                MenuItem m = new MenuItem(MenuItem.CONTAINER, helpplugin.getPluginName(), 0);

                mi = SimpleGUI.getJMenuItem(m);
                if (mi != null) {
                    helpContainer.add(mi);

                    ((JMenu) mi).removeMenuListener(((JMenu) mi).getMenuListeners()[0]);
                    ((JMenu) mi).addMenuListener(new MenuListener() {
                        public void menuCanceled(MenuEvent e) {
                        }

                        public void menuDeselected(MenuEvent e) {
                        }

                        public void menuSelected(MenuEvent e) {
                            JMenu m = (JMenu) e.getSource();
                            m.removeAll();
                            for (MenuItem menuItem : helpplugin.createMenuitems()) {
                                m.add(SimpleGUI.getJMenuItem(menuItem));

                            }

                        }

                    });
                } else {
                    helpContainer.addSeparator();
                }
            }
        }
        if (helpContainer.getItemCount() == 0) {
            helpContainer.setEnabled(false);
        }

        menFile.add(SimpleGUI.createMenuItem(actionLoadDLC));
        menFile.add(SimpleGUI.createMenuItem(actionSaveDLC));
        menFile.addSeparator();
        menFile.add(SimpleGUI.createMenuItem(actioninstallJDU));
        menFile.addSeparator();
        menFile.add(SimpleGUI.createMenuItem(actionExit));

        menExtra.add(menViewLog);
        menExtra.add(SimpleGUI.createMenuItem(actionTester));
        menExtra.add(SimpleGUI.createMenuItem(actionConfig));
        menExtra.add(SimpleGUI.createMenuItem(actionDnD));
        menExtra.add(SimpleGUI.createMenuItem(actionUnrar));
        menExtra.addSeparator();
        menExtra.add(SimpleGUI.createMenuItem(actionPasswordlist));

        menHelp.add(SimpleGUI.createMenuItem(actionHelp));
        menHelp.add(SimpleGUI.createMenuItem(actionWiki));
        menHelp.addSeparator();
        menHelp.add(SimpleGUI.createMenuItem(actionAbout));

        menuBar.setLayout(new GridBagLayout());
        Insets insets = new Insets(1, 1, 1, 1);

        int m = 0;
        JDUtilities.addToGridBag(menuBar, menFile, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(menuBar, menExtra, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(menuBar, menAddons, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(menuBar, menPlugins, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(menuBar, menHelp, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(menuBar, new JLabel(""), m++, 0, 1, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        warning = new JLabel("", JDTheme.II("gui.images.warning", 16, 16), SwingConstants.RIGHT);
        warning.setVisible(false);
        warning.setIconTextGap(10);
        warning.setHorizontalTextPosition(SwingConstants.RIGHT);
        JDUtilities.addToGridBag(menuBar, warning, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);

        try {
            ImageIcon icon = JDUtilities.getscaledImageIcon(JDTheme.V("gui.images.update_manager"), 16, -1);
            JLinkButton linkButton = new JLinkButton(JDLocale.L("jdownloader.org", "jDownloader.org"), icon, new URL("http://jdownloader.org"));
            linkButton.setHorizontalTextPosition(SwingConstants.LEFT);
            linkButton.setBorder(null);
            Dimension d = new Dimension(10, 0);
            JDUtilities.addToGridBag(menuBar, new Box.Filler(d, d, d), m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
            JDUtilities.addToGridBag(menuBar, linkButton, m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
            d = new Dimension(1, 0);
            JDUtilities.addToGridBag(menuBar, new Box.Filler(d, d, d), m++, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        } catch (MalformedURLException e1) {
        }

        frame.setJMenuBar(menuBar);

    }

    // Funktion wird jede Sekunde aufgerufen
    /**
     * Diese Funktion wird in einem 1000 ms interval aufgerufen und kann dazu
     * verwendet werden die GUI zu aktuelisieren
     */
    private void interval() {

        if (JDUtilities.getController() != null) {

            statusBar.setSpeed(JDUtilities.getController().getSpeedMeter());

        }

        // linkListPane.pluginEvent(new PluginEvent(new
        // Rapidshare(),PluginEvent.PLUGIN_DATA_CHANGED, null));
        // linkListPane.refresh();

        progressBar.updateController(null);

        // hostPluginDataChanged = null;
        frame.setTitle(JDUtilities.getJDTitle());
    }

    public void onJDInitComplete() {
        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_START_DOWNLOADS_AFTER_START, false)) {
            // btnStartStop.setSelected(true);
            startStopDownloads();
        }
        frame.setTitle(JDUtilities.getJDTitle());

    }

    public void removeUIListener(UIListener listener) {
        synchronized (uiListener) {
            uiListener.remove(listener);
        }
    }

    /**
     * Setzt den text im DropTargets
     * 
     * @param text
     */
    public void setDropTargetText(String text) {
        dragNDrop.setText(text);
    }

    public void setGUIStatus(int id) {
        switch (id) {
        case UIInterface.WINDOW_STATUS_TRAYED:

            break;
        case UIInterface.WINDOW_STATUS_MAXIMIZED:
            frame.setState(JFrame.MAXIMIZED_BOTH);
            break;
        case UIInterface.WINDOW_STATUS_MINIMIZED:
            frame.setState(JFrame.ICONIFIED);
            break;

        case UIInterface.WINDOW_STATUS_NORMAL:
            frame.setState(JFrame.NORMAL);
            frame.setVisible(true);

            break;
        case UIInterface.WINDOW_STATUS_FOREGROUND:
            frame.setState(JFrame.NORMAL);
            frame.setFocusableWindowState(false);
            frame.setVisible(true);
            frame.toFront();
            frame.setFocusableWindowState(true);
            break;
        }

    }

    /**
     * Setzt die Geschwindigkeit in der Statusbar
     * 
     * @param speed
     */
    public void setSpeedStatusBar(Integer speed) {
        statusBar.setSpinnerSpeed(speed);
    }

    /**
     * Zeigt einen Confirm Dialog an
     * 
     * @param string
     */
    public boolean showConfirmDialog(String string) {
        logger.info("confirmdialog");
        return JOptionPane.showConfirmDialog(frame, string, "", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION;
    }

    public boolean showCountdownConfirmDialog(String string, int sec) {
        return CountdownConfirmDialog.showCountdownConfirmDialog(frame, string, sec);
    }

    public int showHelpMessage(String title, String message, String url) {
        logger.info("helpmessagedialog");
        try {
            return JHelpDialog.showHelpMessage(frame, title, "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">" + message + "</font>", new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean showHTMLDialog(String title, String htmlQuestion) {
        logger.info("HTMLDIALOG");
        return HTMLDialog.showDialog(getFrame(), title, htmlQuestion);

    }

    /**
     * Zeigt einen Messagedialog an
     */
    public void showMessageDialog(String string) {
        logger.info("messagedialog");
        JOptionPane.showMessageDialog(frame, string);
    }

    public String showTextAreaDialog(String title, String question, String def) {
        logger.info("Textareadialog");
        return TextAreaDialog.showDialog(getFrame(), title, question, def);

    }

    public String showUserInputDialog(String string) {
        logger.info("userinputdialog");
        return JOptionPane.showInputDialog(frame, string);
    }

    public String showUserInputDialog(String string, String def) {
        logger.info("userinputdialog");
        return JOptionPane.showInputDialog(frame, string, def);
    }

    public void startStopDownloads() {
        btnStartStop.setEnabled(false);
        btnPause.setEnabled(true);
        JDUtilities.getController().toggleStartStop();
        // if (btnStartStop.isSelected() &&
        // JDUtilities.getController().getDownloadStatus() ==
        // JDController.DOWNLOAD_NOT_RUNNING) {
        // btnPause.setEnabled(true);
        //
        // fireUIEvent(new UIEvent(this, UIEvent.UI_START_DOWNLOADS));
        // } else if (!btnStartStop.isSelected() &&
        // JDUtilities.getController().getDownloadStatus() ==
        // JDController.DOWNLOAD_RUNNING) {
        // final SimpleGUI _this = this;
        // // Dieser Thread muss sein, weil die Funktionen zum anhalten
        // // der Downloads blockiere und sonst die GUI einfriert bis
        // // alle downloads angehalten wurden. Start/stop vorgänge
        // // sind während dieser zeit nicht möglich da
        // // getDownloadStatus() in der Ausführungszeit auf
        // // JDController.DOWNLOAD_TERMINATION_IN_PROGRESS steht
        // new Thread() {
        // public void run() {
        // btnPause.setEnabled(false);
        // btnPause.setSelected(false);
        // fireUIEvent(new UIEvent(_this, UIEvent.UI_STOP_DOWNLOADS));
        // }
        // }.start();
        // }
    }

    public void toggleDnD() {
        if (dragNDrop.isVisible()) {
            dragNDrop.setVisible(false);
        } else {
            dragNDrop.setVisible(true);
            dragNDrop.setText(JDLocale.L("gui.droptarget.label", "Ziehe Links auf mich!"));
        }
    }

    public void uiEvent(UIEvent uiEvent) {
        switch (uiEvent.getID()) {
        case UIEvent.UI_DRAG_AND_DROP:
            if (uiEvent.getParameter() instanceof String) {
                fireUIEvent(new UIEvent(this, UIEvent.UI_LINKS_TO_PROCESS, uiEvent.getParameter()));
            } else {
                fireUIEvent(new UIEvent(this, UIEvent.UI_LOAD_LINKS, uiEvent.getParameter()));
            }
            break;
        }
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        if (e.getComponent() == getFrame()) {
            boolean doIt;
            if (!guiConfig.getBooleanProperty(PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                doIt = showConfirmDialog(JDLocale.L("sys.ask.rlyclose", "Wollen Sie jDownloader wirklich schließen?"));
            } else {
                doIt = true;
            }
            if (doIt) {
                SimpleGUI.saveLastLocation(e.getComponent(), null);
                SimpleGUI.saveLastDimension(e.getComponent(), null);
                guiConfig.setProperty("MAXIMIZED_STATE", getFrame().getExtendedState());
                JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).save();
                fireUIEvent(new UIEvent(this, UIEvent.UI_EXIT, null));
            }
        }
    }

    public void windowDeactivated(WindowEvent e) {
    }

    // public void updateStatusBar() {
    // int maxspeed =
    // JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.
    // PARAM_DOWNLOAD_MAX_SPEED,
    // 0);
    //
    // this.statusBar.spMaxDls.setModel(new
    // SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty
    // (Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN,
    // 3), 1, 20, 1));
    //
    // this.statusBar.spMax.setModel(new SpinnerNumberModel(maxspeed, 0,
    // Integer.MAX_VALUE, 50));
    //
    // this.statusBar.chbPremium.setSelected(JDUtilities.getConfiguration().
    // getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM));
    //
    // return;
    // }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public String[] showTextAreaDialog(String title, String questionOne, String questionTwo, String defaultOne, String defaultTwo) {
        logger.info("Textareadialog");
        return TextAreaDialog.showDialog(getFrame(), title, questionOne, questionTwo, defaultOne, defaultTwo);
    }

    public String[] showTwoTextFieldDialog(String title, String questionOne, String questionTwo, String defaultOne, String defaultTwo) {
        logger.info("TextFieldDialog");
        return TwoTextFieldDialog.showDialog(getFrame(), title, questionOne, questionTwo, defaultOne, defaultTwo);
    }

    public void showAccountInformation(final PluginForHost pluginForHost, final Account account) {
        new Thread() {
            public void run() {
                AccountInfo ai = pluginForHost.getAccountInformation(account);
                if (ai == null) {
                    SimpleGUI.this.showMessageDialog(String.format(JDLocale.L("plugins.host.premium.info.error", "The %s plugin does not support the Accountinfo feature yet."), pluginForHost.getHost()));
                    return;
                }
                JPanel panel = new JPanel(new MigLayout("ins 22", "[right]10[grow,fill]20:push[right]10[grow,fill][20]"));
                String def = String.format(JDLocale.L("plugins.host.premium.info.title", "Accountinformation from %s for %s"), account.getUser(), pluginForHost.getHost());
                String[] label = new String[] { JDLocale.L("plugins.host.premium.info.validUntil", "Valid until"), JDLocale.L("plugins.host.premium.info.trafficLeft", "Traffic left"), JDLocale.L("plugins.host.premium.info.files", "Files"), JDLocale.L("plugins.host.premium.info.rapidpoints", "Rapidpoints"), JDLocale.L("plugins.host.premium.info.usedSpace", "Used Space"), JDLocale.L("plugins.host.premium.info.trafficShareLeft", "Traffic Share left") };
                
            

                DateFormat formater;

                formater = DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.MEDIUM );
              

             

                
                String[] data = new String[] {(ai.isExpired()?"[expired] ":"")+formater.format(new Date(ai.getValidUntil())  ) + "", JDUtilities.formatBytesToMB(ai.getTrafficLeft()), ai.getFilesNum() + "", ai.getPremiumPoints() + "", JDUtilities.formatBytesToMB(ai.getUsedSpace()), JDUtilities.formatBytesToMB(ai.getTrafficShareLeft()) };
                panel.add(new JXTitledSeparator(def), "spanx, pushx, growx, gapbottom 15");
               
               
                for (int j = 0; j < data.length; j++) {
// panel.add(lbl=new JLabel(label[j]), "gapleft 20");
                    // panel.add(tf=new JTextField(data[j]), j!=0 && (j+1)%2 ==
                    // 0 ? "wrap":"");
                    
                    if(data[j]!=null&&!data[j].equals("-1")){
                        panel.add(new JLabel(label[j]), "gapleft 20");
                        panel.add(new JTextField(data[j]),"wrap");
                    }
                    
                }
                JOptionPane.showMessageDialog(null, panel, def, JOptionPane.INFORMATION_MESSAGE);
            }
        }.start();
    }
}
