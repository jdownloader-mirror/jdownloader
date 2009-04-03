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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jd.HostPluginWrapper;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.controlling.reconnect.HTTPLiveHeader;
import jd.controlling.reconnect.ReconnectMethod;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.skins.simple.JDMenu;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.Progressor;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.BrowseFile;
import jd.gui.skins.simple.components.ConfirmCheckBoxDialog;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.components.MiniLogDialog;
import jd.http.Encoding;
import jd.nutils.JDImage;
import jd.plugins.PluginForHost;
import jd.router.GetRouterInfo;
import jd.router.RInfo;
import jd.router.reconnectrecorder.JDRRGui;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class FengShuiConfigPanel extends JTabbedPanel implements ActionListener {

    private static final long serialVersionUID = 1715405893428812995L;
    private static final String WRAP_BETWEEN_ROWS = ", wrap 10!";
    private static final String WRAP = ", wrap 20";
    private static final String GAPLEFT = "gapleft 10!, ";
    private static final String GAPRIGHT = "gapright 26!, ";
    private static final String PUSHGAP = " :70";

    private JButton btnRR, btnPremium, btnAutoConfig, btnSelectRouter, btnTestReconnect;
    private JComboBox languages;
    private Configuration config = JDUtilities.getConfiguration();
    private BrowseFile downloadDirectory;
    private String ddir = null;
    private JTextField username, password, ip;
    public JTextField routername;
    private String routerIp = null;
    private Progressor prog;
    private JPanel progresspanel;
    private Thread routerselect = null;
    private JProgressBar progress = null;
    public String Reconnectmethode = null;
    public String ReconnectmethodeClr = null;
    private String wikiurl = JDLocale.L("gui.fengshuiconfig.wikiurl", "http://wiki.jdownloader.org/index.php?title=DE:fengshui:");

    public FengShuiConfigPanel() {
        super();
        this.setName("FENGSHUICONFIG");

        initPanel();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnRR) {
            JDRRGui jd = new JDRRGui(SimpleGUI.CURRENTGUI, ip.getText());
            jd.setModal(true);
            jd.setVisible(true);
            if (jd.saved) {
                ip.setText(jd.RouterIP);
                if (jd.user != null) username.setText(jd.user);
                if (jd.pass != null) password.setText(jd.pass);
                Reconnectmethode = jd.methode;
                routername.setText("Reconnect Recorder Methode");
            }
        } else if (e.getSource() == btnPremium) {
            JPopupMenu popup = new JPopupMenu(JDLocale.L("gui.menu.plugins.phost", "Premium Hoster"));
            for (Iterator<HostPluginWrapper> it = JDUtilities.getPluginsForHost().iterator(); it.hasNext();) {
                HostPluginWrapper wrapper = it.next();

                if (wrapper.isLoaded()) {
                    final PluginForHost helpplugin = wrapper.getPlugin();
                    if (helpplugin.createMenuitems() != null) {
                        JMenu item;
                        popup.add(item = new JMenu(helpplugin.getHost()));
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);

                        if (item != null) {
                            popup.add(item);

                            item.addMenuListener(new MenuListener() {
                                public void menuCanceled(MenuEvent e) {
                                }

                                public void menuDeselected(MenuEvent e) {
                                }

                                public void menuSelected(MenuEvent e) {
                                    JMenu m = (JMenu) e.getSource();
                                    JMenuItem c;
                                    m.removeAll();
                                    for (MenuItem menuItem : helpplugin.createMenuitems()) {
                                        c = JDMenu.getJMenuItem(menuItem);
                                        if (c == null) {
                                            m.addSeparator();
                                        } else {
                                            m.add(c);
                                        }

                                    }

                                }

                            });
                        } else {
                            popup.addSeparator();
                        }
                    }
                }
            }

            popup.show(((JButton) e.getSource()), 100, 25);
        } else if (e.getSource() == btnTestReconnect) {
            save();

            final MiniLogDialog mld = new MiniLogDialog("Reconnect");
            mld.getBtnOK().setEnabled(false);
            mld.getBtnOK().setText(JDLocale.L("gui.warning.reconnect.pleaseWait", "Bitte Warten...Reconnect läuft"));
            mld.getProgress().setMaximum(100);
            mld.getProgress().setValue(2);

            JDUtilities.getLogger().info("Start Reconnect");
            JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RETRIES, 0);

            new Thread(new Runnable() {

                public void run() {
                    boolean tmp = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, false);
                    if (Reconnecter.waitForNewIP(1)) {
                        mld.setText(JDLocale.L("gui.warning.reconnectSuccess", "Reconnect successfull") + "\r\n\r\n\r\n" + mld.getText());
                    } else {

                        mld.setText(JDLocale.L("gui.warning.reconnectFailed", "Reconnect failed!") + "\r\n\r\n\r\n" + mld.getText());
                        if (JDUtilities.getController().getRunningDownloadNum() > 0) {
                            mld.setText(JDLocale.L("gui.warning.reconnectFailedRunningDownloads", "Please stop all running Downloads first!") + "\r\n\r\n\r\n" + mld.getText());
                        }
                    }
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, tmp);
                    mld.getProgress().setValue(100);
                    mld.getBtnOK().setEnabled(true);
                    mld.getBtnOK().setText(JDLocale.L("gui.warning.reconnect.close", "Fenster schließen"));
                }

            }).start();
        } else if (e.getSource() == btnAutoConfig) {
            GetRouterInfo.autoConfig(password, username, ip, this);
        } else if (e.getSource() == btnSelectRouter) {
            if (routerselect != null && !routerselect.isAlive()) {
                routerselect = null;
            } else if (routerselect == null) {
                routerselect = new Thread(new Runnable() {
                    public void run() {

                        final Vector<RInfo> scripts = new Vector<RInfo>();

                        Thread th = new Thread(new Runnable() {
                            public void run() {
                                Vector<RInfo> routers = new GetRouterInfo(showProgressbar()).getRouterInfos();
                                scripts.addAll(routers);
                                synchronized (this) {
                                    notify();
                                }
                            }
                        });
                        th.start();
                        Vector<String[]> scripts2 = new HTTPLiveHeader().getLHScripts();

                        Collections.sort(scripts2, new Comparator<String[]>() {
                            public int compare(String[] a, String[] b) {
                                return (a[0] + " " + a[1]).compareToIgnoreCase(b[0] + " " + b[1]);
                            }

                        });

                        HashMap<String, Boolean> ch = new HashMap<String, Boolean>();
                        for (int i = scripts2.size() - 1; i >= 0; i--) {
                            String[] sc = scripts2.get(i);
                            if (ch.containsKey(sc[0] + sc[1] + sc[2])) {
                                scripts2.remove(i);
                            } else {

                                ch.put(sc[0] + sc[1] + sc[2], true);
                            }
                        }
                        ch.clear();
                        if (th.isAlive()) {
                            synchronized (th) {
                                try {
                                    th.wait(15000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                        final String[] d = new String[scripts.size() + scripts2.size()];
                        for (int i = 0; i < scripts.size(); i++) {
                            RInfo sc = (RInfo) scripts.get(i);
                            d[i] = i + ". ODB:" + Encoding.htmlDecode(sc.getRouterName());
                        }
                        int i = scripts.size();
                        for (String[] strings : scripts2) {
                            RInfo sc = new RInfo();
                            sc.setReconnectMethode(strings[2]);
                            sc.setRouterName(Encoding.htmlDecode(strings[0] + " : " + strings[1]));
                            scripts.add(sc);
                            d[i] = i + ". " + Encoding.htmlDecode(sc.getRouterName());
                            i++;
                        }

                        JPanel panel = new JPanel(new BorderLayout(10, 10));
                        final DefaultListModel defaultListModel = new DefaultListModel();
                        final String text = JDLocale.L("gui.config.fengshui.searchroutermodel", "Search Router Model");
                        final JTextField searchField = new JTextField();
                        searchField.setForeground(Color.lightGray);
                        final JList list = new JList(defaultListModel);
                        searchField.getDocument().addDocumentListener(new DocumentListener() {
                            public void changedUpdate(DocumentEvent e) {
                            }

                            public void insertUpdate(DocumentEvent e) {
                                refreshList();
                            }

                            private void refreshList() {
                                String search = searchField.getText().toLowerCase();
                                String[] hits = search.split(" ");
                                defaultListModel.removeAllElements();
                                for (int i = 0; i < d.length; i++) {
                                    for (int j = 0; j < hits.length; j++) {
                                        if (!d[i].toLowerCase().contains(hits[j])) {
                                            break;
                                        }
                                        if (j == hits.length - 1) {
                                            defaultListModel.addElement(d[i]);
                                        }
                                    }
                                }
                                list.setModel(defaultListModel);
                            }

                            public void removeUpdate(DocumentEvent e) {
                                refreshList();
                            }
                        });
                        searchField.addFocusListener(new FocusAdapter() {
                            boolean onInit = true;

                            @Override
                            public void focusGained(FocusEvent e) {
                                if (onInit) {
                                    onInit = !onInit;
                                    return;
                                }
                                searchField.setForeground(Color.black);
                                if (searchField.getText().equals(text)) {
                                    searchField.setText("");
                                }
                            }
                        });

                        // Eclipse Clear Console Icon should be used
                        ImageIcon imageIcon = new ImageIcon(JDImage.getImage(JDTheme.V("gui.images.exit")));
                        imageIcon = new ImageIcon(imageIcon.getImage().getScaledInstance(16, -1, Image.SCALE_SMOOTH));
                        JButton reset = new JButton(imageIcon);
                        reset.setBorder(null);
                        reset.setOpaque(false);
                        reset.setContentAreaFilled(false);
                        reset.setBorderPainted(false);
                        reset.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                searchField.setForeground(Color.lightGray);
                                searchField.setText(text);
                                for (String element : d) {
                                    defaultListModel.addElement(element);
                                }
                            }
                        });
                        searchField.setText(text);
                        // !!! Lupen-Icon should be used
                        Icon icon = new ImageIcon(JDImage.getImage(JDTheme.V("gui.images.update_manager")));
                        JPanel p = new JPanel(new BorderLayout(5, 5));
                        p.add(searchField, BorderLayout.CENTER);
                        p.add(reset, BorderLayout.EAST);
                        JLabel example = new JLabel(JDLocale.L("gui.config.fengshui.example", "Example:") + "3Com ADSL");
                        example.setForeground(Color.gray);
                        p.add(example, BorderLayout.SOUTH);
                        for (String element : d) {
                            defaultListModel.addElement(element);
                        }

                        JScrollPane scrollPane = new JScrollPane(list);
                        panel.add(p, BorderLayout.NORTH);
                        panel.add(scrollPane, BorderLayout.CENTER);
                        panel.setPreferredSize(new Dimension(400, 500));
                        int n = 10;
                        panel.setBorder(new EmptyBorder(n, n, n, n));

                        JOptionPane op = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, icon);
                        JDialog dialog = op.createDialog(SimpleGUI.CURRENTGUI, JDLocale.L("gui.config.liveHeader.dialog.importRouter", "Router importieren"));
                        dialog.add(op);
                        dialog.setModal(true);
                        dialog.setPreferredSize(new Dimension(400, 500));
                        dialog.pack();
                        dialog.setLocationRelativeTo(null);
                        dialog.setVisible(true);
                        int answer = ((Integer) op.getValue()).intValue();
                        if (answer != JOptionPane.CANCEL_OPTION && list.getSelectedValue() != null) {
                            String selected = (String) list.getSelectedValue();
                            int id = Integer.parseInt(selected.split("\\.")[0]);
                            RInfo info = scripts.get(id);
                            routername.setText(info.getRouterName());
                            Reconnectmethode = info.getReconnectMethode();
                            ReconnectmethodeClr = info.getReconnectMethodeClr();

                        }

                    }
                });
                routerselect.start();
            }
        }
    }

    private void addComponents(String label, JComponent component, String comp_constraints) {
        JLabel l = getLabel(label);
        this.add(l, "gapleft 22, growx 0");
        this.add(component, comp_constraints + ", growx");
    }

    private JLabel getLabel(String label) {
        return new JLabel("<html><b color=\"#4169E1\">" + label);
    }

    private void addSeparator(String title, Icon icon, String help) {
        try {
            JLinkButton label = new JLinkButton("<html><u><b  color=\"#006400\">" + title, icon, new URL(wikiurl + (title.replaceAll("\\s", "_"))));
            label.setIconTextGap(8);
            label.setBorder(null);
            this.add(label, "align left, split 2");
            this.add(new JSeparator(), "gapleft 10, spanx, pushx, growx");
            this.add(new JSeparator(), "span 3, pushx, growx");

            JLabel tip = new JLabel(JDTheme.II("gui.images.config.tip", 16, 16));
            tip.setToolTipText(help);
            this.add(tip, GAPLEFT + "w pref!" + WRAP);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    private String getDownloadDirectory() {
        if (ddir == null) {
            ddir = config.getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY);
            if (ddir == null) ddir = JDUtilities.getResourceFile("downloads").getAbsolutePath();
        }
        return ddir;
    }

    private Progressor showProgressbar() {
        progress = new JProgressBar();
        progresspanel.add(progress);
        prog = new Progressor() {
            private static final long serialVersionUID = 1L;

            public int getMaximum() {
                return progress.getMaximum();
            }

            public String getMessage() {
                return null;
            }

            public int getMinimum() {
                return progress.getMinimum();
            }

            public String getString() {
                return progress.getString();
            }

            public int getValue() {
                return progress.getValue();
            }

            public void setMaximum(int value) {
                progress.setMaximum(value);
            }

            public void setMessage(String txt) {
            }

            public void setMinimum(int value) {
                progress.setMinimum(value);
            }

            public void setString(String txt) {
                progress.setString(txt);
            }

            public void setStringPainted(boolean v) {
                progress.setStringPainted(v);
            }

            public void setThread(Thread th) {
            }

            public void setValue(final int value) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        progress.setValue(value);
                        if (value == 100) {
                            hideProgressbar();
                        }
                    }

                });
            }
        };
        return prog;
    }

    private void hideProgressbar() {
        progresspanel.remove(progress);
        progress.removeAll();
        progress = null;

        progresspanel.add(new JSeparator(), "pushx, growx");
        progresspanel.revalidate();
        progresspanel.repaint();
    }

    private void initPanel() {
        this.setLayout(new MigLayout("ins 20", "[right, pref!]0[grow,fill]0[]"));
        routerIp = config.getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
        Reconnectmethode = config.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null);
        addSeparator(JDLocale.L("gui.config.general.name", "Allgemein"), JDTheme.II("gui.images.configuration", 32, 32), JDLocale.L("gui.fengshuiconfig.general.tooltip", "<html>You can set the Downloadpath and the language here"));

        languages = new JComboBox(JDLocale.getLocaleIDs().toArray(new String[] {}));
        languages.setSelectedItem(JDUtilities.getSubConfig(JDLocale.CONFIG).getProperty(JDLocale.LOCALE_ID, Locale.getDefault()));

        downloadDirectory = new BrowseFile();
        downloadDirectory.setEditable(true);
        downloadDirectory.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        downloadDirectory.setText(getDownloadDirectory());

        addComponents(JDLocale.L("gui.config.general.downloadDirectory", "Downloadverzeichnis"), downloadDirectory, GAPLEFT + GAPRIGHT + ", spanx" + WRAP_BETWEEN_ROWS);
        addComponents(JDLocale.L("gui.config.gui.language", "Language"), languages, GAPLEFT + GAPRIGHT + ", w pref!, wrap " + PUSHGAP);

        btnPremium = new JButton(JDLocale.L("gui.config.fengshui.settings", "Settings"));
        btnPremium.addActionListener(this);

        addSeparator(JDLocale.L("gui.config.plugin.host.name", "Host Plugins"), JDTheme.II("gui.images.next", 32, 32), JDLocale.L("gui.fengshuiconfig.plugin.host.tooltip", "<html>If you have a Premium Account for a hoster you can enter you login<br> password here and JD will use them automatically henceforth<br> if you download files with that hoster"));
        addComponents(JDLocale.L("gui.menu.plugins.phost", "Premium Hoster"), btnPremium, GAPLEFT + GAPRIGHT + ", w pref!, wrap" + PUSHGAP);

        String titl = JDLocale.L("gui.config.reconnect.name", "Reconnect");
        try {
            JLinkButton label = new JLinkButton("<html><u><b  color=\"#006400\">" + titl, JDTheme.II("gui.images.reconnect", 32, 32), new URL(wikiurl + (titl.replaceAll("\\s", "_"))));
            label.setIconTextGap(8);
            label.setBorder(null);
            this.add(label, "align left, split 2");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        this.add(new JSeparator(), "gapleft 10, spanx, pushx, growx");
        progresspanel = new JPanel(new MigLayout("ins 0, aligny 49%", "fill, grow"));
        this.add(progresspanel, "pushx, span 3, growx");
        if (routerIp == null || routerIp.matches("^[\\s]*$")) {
            showProgressbar();
        } else {
            progresspanel.add(new JSeparator(), "span 3, pushx, growx");
        }

        JLabel tip = new JLabel(JDTheme.II("gui.images.config.tip", 16, 16));
        tip.setToolTipText(JDLocale.L("gui.fengshuiconfig.reconnect.tooltip", "<html>Somtimes you need to change your ip via reconnect, to skip the waittime!"));
        this.add(tip, GAPLEFT + "w pref!" + WRAP);

        btnTestReconnect = new JButton(JDLocale.L("modules.reconnect.testreconnect", "Test reconnect"));
        btnTestReconnect.addActionListener(this);
        btnRR = new JButton(JDLocale.L("modules.reconnect.rr", "Record Reconnect"));
        btnRR.addActionListener(this);
        btnAutoConfig = new JButton(JDLocale.L("gui.config.fengshui.rdautomatic", "Automatic"));
        btnAutoConfig.addActionListener(this);
        btnSelectRouter = new JButton(JDLocale.L("gui.config.fengshui.rdmanual", "Manual"));
        btnSelectRouter.addActionListener(this);
        JPanel routerPanel = new JPanel(new MigLayout("ins 0", "[grow,fill]10![grow,fill]10![grow,fill]"));
        routerPanel.add(btnAutoConfig);
        routerPanel.add(btnSelectRouter);
        routerPanel.add(btnRR);
        addComponents(JDLocale.L("gui.config.fengshui.rdname", "Router detection"), routerPanel, GAPLEFT + ", w pref!" + WRAP_BETWEEN_ROWS);

        int n = 10;
        ip = new JTextField(n);
        routername = new JTextField(n);
        username = new JTextField(n);
        password = new JPasswordField(n);

        ip.setText(routerIp);
        routername.setEnabled(false);
        routername.setEditable(false);

        routername.setText(config.getStringProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, ""));
        username.setText(config.getStringProperty(Configuration.PARAM_HTTPSEND_USER, ""));
        password.setText(config.getStringProperty(Configuration.PARAM_HTTPSEND_PASS, ""));

        String constr = GAPLEFT + ", w pref!, growx, spanx" + WRAP_BETWEEN_ROWS;
        addComponents(JDLocale.L("gui.fengshuiconfig.routerip", "RouterIP") + ":", subpanel(ip, JDLocale.L("gui.fengshuiconfig.routername", "Routername") + ":", routername), constr);
        addComponents(JDLocale.L("gui.fengshuiconfig.username", "Username") + ":", subpanel(username, JDLocale.L("gui.fengshuiconfig.routerpassword", "Password") + ":", password), constr);
        addComponents(JDLocale.L("gui.fengshuiconfig.testsettings", "Einstellungen testen") + ":", btnTestReconnect, GAPLEFT + ", w pref!, wrap" + PUSHGAP + ":push");

        getRouterIp();
    }

    private JComponent subpanel(JComponent c1, String l2, JComponent c2) {
        JPanel subpanel = new JPanel(new MigLayout("ins 0", "[grow,fill]10[right, 70!]10[grow,fill]"));
        subpanel.add(c1);
        subpanel.add(getLabel(l2));
        subpanel.add(c2);
        return subpanel;
    }

    private void getRouterIp() {
        new Thread(new Runnable() {

            public void run() {
                if (config.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) == ReconnectMethod.LIVEHEADER) {
                    boolean reachable = false;
                    if (routerIp != null && !routerIp.matches("^[\\s]*$")) {
                        try {
                            reachable = InetAddress.getByName(routerIp).isReachable(1500);
                        } catch (IOException e) {
                        }
                    }

                    if (routerIp == null || routerIp.matches("^[\\s]*$") || !reachable) {
                        InetAddress ia = new GetRouterInfo(prog).getAdress();
                        if (ia != null) ip.setText(ia.getHostName());
                    }
                    if (Reconnectmethode == null || Reconnectmethode.matches("^[\\s]*$")) {
                        if (GetRouterInfo.isFritzbox(ip.getText())) {
                            String tit = JDLocale.L("gui.config.fengshui.fritzbox.title", "Fritz!Box erkannt");
                            if (GetRouterInfo.isUpnp(ip.getText(), "49000")) {
                                if (config.getBooleanProperty("FENGSHUI_UPNPACTIVE", true)) {
                                    ConfirmCheckBoxDialog con = new ConfirmCheckBoxDialog(tit, JDLocale.L("gui.config.fengshui.fritzbox.upnpactive", "Sie haben eine Fritz!Box, der Reconnect über Upnp ist möglich.<br> möchten sie den Upnp Reconnect nutzen?"), JDLocale.L("gui.config.fengshui.fritzbox.upnp.checkbox", "Mitteilung nichtmehr zeigen"), true);
                                    if (con.isOk) {
                                        Reconnectmethode = "[[[HSRC]]]\r\n" + "[[[STEP]]]\r\n" + "[[[REQUEST]]]\r\n" + "POST /upnp/control/WANIPConn1 HTTP/1.1\r\n" + "Host: %%%routerip%%%:49000\r\n" + "Content-Type: text/xml; charset=\"utf-8\"\r\n" + "SoapAction:urn:schemas-upnp-org:service:WANIPConnection:1#ForceTermination\r\n" +

                                        "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:ForceTermination xmlns:u='urn:schemas-upnp-org:service:WANIPConnection:1' /> </s:Body> </s:Envelope>\r\n" + "[[[/REQUEST]]]\r\n" + "[[[/STEP]]]\r\n" + "[[[/HSRC]]]\r\n";
                                        routername.setText("!FRITZ BOX (All via UPNP)");
                                    }
                                    if (con.isChecked) {
                                        config.setProperty("FENGSHUI_UPNPACTIVE", false);
                                    }
                                }
                            } else {
                                if (config.getBooleanProperty("FENGSHUI_UPNPDEACTIVE", true)) {
                                    ConfirmCheckBoxDialog con = new ConfirmCheckBoxDialog(tit, JDLocale.LF("gui.config.fengshui.fritzbox.upnpinactive", "Bitte aktivieren sie Upnp bei ihrer Fritz!Box <br><a href=\"http://%s\">zur Fritz!Box</a><br><a href=\"http://wiki.jdownloader.org/index.php?title=Fritz!Box_Upnp\">Wikiartikel: Fritz!Box Upnp</a>", ip.getText()), JDLocale.L("gui.config.fengshui.fritzbox.upnp.checkbox", "Mitteilung nichtmehr zeigen"), true);
                                    if (con.isChecked) {
                                        config.setProperty("FENGSHUI_UPNPDEACTIVE", false);
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }).start();
    }

    private boolean hasChanged() {
        if (!languages.getSelectedItem().equals(JDUtilities.getSubConfig(JDLocale.CONFIG).getProperty(JDLocale.LOCALE_ID, Locale.getDefault()))) return true;
        if (!downloadDirectory.getText().equals(getDownloadDirectory())) return true;
        if (!ip.getText().equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_IP, ""))) return true;
        if (!username.getText().equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_USER, ""))) return true;
        if (!password.getText().equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_PASS, ""))) return true;
        if (Reconnectmethode != null && !Reconnectmethode.equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null))) return true;
        if (ReconnectmethodeClr != null && !ReconnectmethodeClr.equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR, null))) return true;
        if (routername.getText() != null && !routername.getText().equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, ""))) return true;
        return false;
    }

    private boolean restartRequired() {
        return !JDUtilities.getSubConfig(JDLocale.CONFIG).getProperty(JDLocale.LOCALE_ID, Locale.getDefault()).equals(languages.getSelectedItem());
    }

    private void save() {
        JDUtilities.getSubConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, languages.getSelectedItem());
        JDUtilities.getSubConfig(JDLocale.CONFIG).save();

        config.setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, downloadDirectory.getText());
        config.setProperty(Configuration.PARAM_HTTPSEND_IP, ip.getText());
        config.setProperty(Configuration.PARAM_HTTPSEND_USER, username.getText());
        config.setProperty(Configuration.PARAM_HTTPSEND_PASS, password.getText());
        if (Reconnectmethode != null && !Reconnectmethode.equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null))) {
            config.setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER);
            config.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS, Reconnectmethode);
        } else if (ReconnectmethodeClr != null && !ReconnectmethodeClr.equals(config.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR, null))) {
            config.setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.CLR);
            config.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR, Reconnectmethode);
        }
        config.setProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, routername.getText());
        config.save();
    }

    @Override
    public void onDisplay() {
    }

    @Override
    public void onHide() {
        if (hasChanged()) {
            if (JDUtilities.getGUI().showConfirmDialog(JDLocale.L("gui.config.save.doyourealywant", "Do you want to save your changes?"), JDLocale.L("gui.config.save.doyourealywant.title", "Changes"))) {
                this.save();
                if (restartRequired()) {
                    if (JDUtilities.getGUI().showConfirmDialog(JDLocale.L("gui.config.save.restart", "Your changes need a restart of JDownloader to take effect.\r\nRestart now?"), JDLocale.L("gui.config.save.restart.title", "JDownloader restart requested"))) {
                        JDUtilities.restartJD();
                    }
                }
            }
        }
    }
}
