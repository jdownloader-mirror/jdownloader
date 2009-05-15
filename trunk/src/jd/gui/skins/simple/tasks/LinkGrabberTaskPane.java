package jd.gui.skins.simple.tasks;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.Timer;

import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberFilePackage;
import jd.gui.skins.simple.components.Linkgrabber.LinkGrabberTreeTableAction;
import jd.nutils.Formatter;
import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class LinkGrabberTaskPane extends TaskPanel implements ActionListener, LinkGrabberControllerListener {

    private static final long serialVersionUID = -7720749076951577192L;
    private JButton panel_add_links;
    private JButton panel_add_containers;
    private JButton lg_add_all;
    private JButton lg_add_selected;
    private JButton lg_clear;

    private boolean linkgrabberButtonsEnabled = false;

    private JLabel linkgrabber;

    private JLabel downloadlinks;
    private JLabel filteredlinks;
    private JLabel packages;
    private JLabel totalsize;
    private Timer fadeTimer;
    private Vector<LinkGrabberFilePackage> fps;
    private LinkGrabberController lgi;
    private JCheckBox topOrBottom;
    private JCheckBox startAFteradding;

    public LinkGrabberTaskPane(String string, ImageIcon ii) {
        super(string, ii, "linkgrabber");
        lgi = LinkGrabberController.getInstance();
        fps = lgi.getPackages();
        lgi.getBroadcaster().addListener(this);
        linkgrabberButtonsEnabled = false;
        initGUI();
        fadeTimer = new Timer(2000, this);
        fadeTimer.setInitialDelay(0);
        fadeTimer.start();
    }

    private void initListStatGUI() {

        linkgrabber = (new JLabel(JDLocale.L("gui.taskpanes.download.linkgrabber", "Packagestats")));
        linkgrabber.setIcon(JDTheme.II("gui.images.taskpanes.linkgrabber", 16, 16));

        packages = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.packages", "%s Package(s)", 0)));
        downloadlinks = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.downloadLinks", "%s Link(s)", 0)));
        filteredlinks = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.filteredLinks", "%s filtered Link(s)", 0)));
        totalsize = (new JLabel(JDLocale.LF("gui.taskpanes.download.linkgrabber.size", "Total size: %s", 0)));
        add(linkgrabber, D1_LABEL_ICON);
        add(packages, D2_LABEL);
        add(downloadlinks, D2_LABEL);
        add(filteredlinks, D2_LABEL);
        add(totalsize, D2_LABEL);
    }

    private void update() {/* TODO: soll man über events aktuallisiert werden */
        packages.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.packages", "%s Packages", fps.size()));
        long tot = 0;
        long links = 0;
        synchronized (fps) {
            for (LinkGrabberFilePackage fp : fps) {
                tot += fp.getDownloadSize(false);
                links += fp.getDownloadLinks().size();
            }
        }
        downloadlinks.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Links", links));
        filteredlinks.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.filteredLinks", "%s filtered Link(s)", lgi.getFILTERPACKAGE().size()));
        totalsize.setText(JDLocale.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", Formatter.formatReadable(tot)));
    }

    private void initGUI() {

        this.panel_add_links = (this.createButton(JDLocale.L("gui.linkgrabberv2.addlinks", "Add Links"), JDTheme.II("gui.images.add", 16, 16)));
        this.panel_add_containers = (this.createButton(JDLocale.L("gui.linkgrabberv2.addcontainers", "Open Containers"), JDTheme.II("gui.images.load", 16, 16)));

        lg_add_all = (createButton(JDLocale.L("gui.linkgrabberv2.lg.addall", "Add all packages"), JDTheme.II("gui.images.add_all", 16, 16)));
        lg_add_selected = (createButton(JDLocale.L("gui.linkgrabberv2.lg.addselected", "Add selected package(s)"), JDTheme.II("gui.images.add_package", 16, 16)));
        lg_clear = (createButton(JDLocale.L("gui.linkgrabberv2.lg.clear", "Clear List"), JDTheme.II("gui.images.clear", 16, 16)));

        add(panel_add_links, D1_BUTTON_ICON);
        add(panel_add_containers, D1_BUTTON_ICON);
        add(new JSeparator());
        add(lg_add_all, D1_BUTTON_ICON);
        add(lg_add_selected, D1_BUTTON_ICON);
        add(lg_clear, D1_BUTTON_ICON);
        lg_add_all.setEnabled(false);
        lg_add_selected.setEnabled(false);
        lg_clear.setEnabled(false);

        add(new JSeparator());
        initListStatGUI();
        add(new JSeparator());
        initQuickConfig();
    }

    private void initQuickConfig() {
        JLabel config = (new JLabel(JDLocale.L("gui.taskpanes.download.linkgrabber.config", "Settings")));
        config.setIcon(JDTheme.II("gui.images.taskpanes.configuration", 16, 16));

        topOrBottom = new JCheckBox(JDLocale.L("gui.taskpanes.download.linkgrabber.config.addattop", "Add at top"));

        topOrBottom.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SimpleGuiConstants.GUI_CONFIG.setProperty(SimpleGuiConstants.PARAM_INSERT_NEW_LINKS_AT, topOrBottom.isSelected());
                SimpleGuiConstants.GUI_CONFIG.save();
            }

        });
        if (SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
            topOrBottom.setSelected(true);
        }
        
        
       
        startAFteradding = new JCheckBox(JDLocale.L("gui.taskpanes.download.linkgrabber.config.startofter", "Start after adding"));
        startAFteradding.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SimpleGuiConstants.GUI_CONFIG.setProperty(SimpleGuiConstants.PARAM_START_AFTER_ADDING_LINKS, startAFteradding.isSelected());
                SimpleGuiConstants.GUI_CONFIG.save();
            }

        });
        if (SimpleGuiConstants.GUI_CONFIG.getBooleanProperty(SimpleGuiConstants.PARAM_START_AFTER_ADDING_LINKS, true)) {
            startAFteradding.setSelected(true);
        }
        add(config, D1_LABEL_ICON);
        
        startAFteradding.setToolTipText(JDLocale.L("gui.tooltips.linkgrabber.startlinksafteradd","Is selected, download starts after adding new links"));
        add(startAFteradding, TaskPanel.D2_CHECKBOX);
        startAFteradding.setToolTipText(JDLocale.L("gui.tooltips.linkgrabber.topOrBottom","if selected, new links will be added at top of your downloadlist"));
        
        add(topOrBottom, TaskPanel.D2_CHECKBOX);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == fadeTimer) {
            if (!this.isCollapsed()) update();
            return;
        }
        if (e.getSource() == panel_add_links) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.GUI_ADD, null));
            return;
        }

        if (e.getSource() == panel_add_containers) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.GUI_LOAD, null));
            return;
        }

        if (e.getSource() == lg_add_all) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.ADD_ALL, null));
            return;
        }
        if (e.getSource() == lg_add_selected) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.ADD_SELECTED, null));
            return;
        }
        if (e.getSource() == lg_clear) {
            this.broadcastEvent(new ActionEvent(this, LinkGrabberTreeTableAction.CLEAR, null));
            return;
        }
    }

    public void setPanelID(int i) {
        SimpleGUI.CURRENTGUI.getContentPane().display(getPanel(i));
        return;
        // switch (i) {
        // case 0:
        // lg_add_all.setEnabled(false);
        // lg_add_selected.setEnabled(false);
        // lg_clear.setEnabled(false);
        // panel_add_links.setEnabled(false);
        //
        // linkgrabber.setEnabled(false);
        // packages.setEnabled(false);
        // downloadlinks.setEnabled(false);
        // totalsize.setEnabled(false);
        // break;
        // case 1:
        // linkgrabber.setEnabled(true);
        // packages.setEnabled(true);
        // downloadlinks.setEnabled(true);
        // totalsize.setEnabled(true);
        // panel_add_links.setEnabled(true);
        // if (linkgrabberButtonsEnabled) {
        // lg_add_all.setEnabled(true);
        // lg_add_selected.setEnabled(true);
        // lg_clear.setEnabled(true);
        // }
        // break;
        // }

    }

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        switch (event.getID()) {
        case LinkGrabberControllerEvent.REFRESH_STRUCTURE:
            if (linkgrabberButtonsEnabled) return;
            linkgrabberButtonsEnabled = true;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    lg_add_all.setEnabled(true);
                    lg_add_selected.setEnabled(true);
                    lg_clear.setEnabled(true);
                    revalidate();
                }
            });
            break;
        case LinkGrabberControllerEvent.EMPTY:
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    lg_add_all.setEnabled(false);
                    lg_add_selected.setEnabled(false);
                    lg_clear.setEnabled(false);
                    revalidate();
                    linkgrabberButtonsEnabled = false;
                }
            });
            break;
        default:
            break;
        }
    }

}
