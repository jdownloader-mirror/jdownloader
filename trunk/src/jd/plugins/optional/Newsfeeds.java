package jd.plugins.optional;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.parser.Regex;
import jd.plugins.HTTP;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class Newsfeeds extends PluginOptional implements ListSelectionListener {

    private String version = "0.1.0";
    private static final String PROPERTY_ENABLED = "PROPERTY_ENABLED";
    private static final String PROPERTY_FEEDS = "PROPERTY_FEEDS";
    private static final String PROPERTY_ABOS = "PROPERTY_ABOS";
    private SubConfiguration subConfig;

    private JFrame frame;
    private JButton btnDownload;
    private JButton btnDownload2;
    private JButton btnAddFeed;
    private JButton btnDeleteFeed;
    private JButton btnAddAbo;
    private JButton btnDeleteAbo;
    private JTextField filterText;
    private JList list;
    private JList feedList;
    private JList aboList;
    private JList subscribedList;
    private JLabel statusLabelManageFeeds;
    private JLabel statusLabelGetSubscribed;

    private Vector<String[]> entriesManageFeeds = new Vector<String[]>();
    private Vector<String[]> entriesGetSubscribed = new Vector<String[]>();
    private Vector<String[]> feeds = new Vector<String[]>();
    private Vector<String[]> abos = new Vector<String[]>();
    private String serienjunkiesCookie = "";
    private int feedListSelectedIndex = -1;

    public Newsfeeds() {
    }

    public static int getAddonInterfaceVersion() {
        return 0;
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public boolean initAddon() {
        subConfig = JDUtilities.getSubConfig("ADDONS_NEWSFEEDS");
        return true;
    }

    @Override
    public void onExit() {
    }

    @Override
    public String getCoder() {
        return "jD-Team";
    }

    @Override
    public String getPluginID() {
        return getPluginName() + " " + version;
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.newsfeeds.pluginTitle", "Newsfeed Check");
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {

        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        if (!JDUtilities.getSubConfig("ADDONS_NEWSFEEDS").getBooleanProperty(PROPERTY_ENABLED, false)) {

            menu.add(new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.newsfeeds.manageFeeds", "Manage Feeds"), 0).setActionListener(this));
            menu.add(new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.newsfeeds.manageDownloadSubscriptions", "Manage Download Subscriptions"), 1).setActionListener(this));
            menu.add(new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.newsfeeds.getLatestSubscribedDownloads", "Get latest subscribed Downloads"), 2).setActionListener(this));

        }

        return menu;

    }

    /*
     * GUIs
     */

    private void showManageFeedsGui() {

        frame = new JFrame();
        frame.setTitle(JDLocale.L("plugins.optional.newsfeeds.manageFeeds", "Manage Feeds"));
        frame.setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.jd_logo")));
        frame.setPreferredSize(new Dimension(700, 500));
        frame.setName("ADDON_NEWSFEED_1");
        LocationListener listener = new LocationListener();
        frame.addComponentListener(listener);
        frame.addWindowListener(listener);
        frame.setLayout(new BorderLayout(5, 5));

        list = new JList();
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        feedList = new JList();
        feedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        feedList.addListSelectionListener(this);

        btnDownload = new JButton(JDLocale.L("plugins.optional.newsfeeds.downloadSelected", "Download Selected"));
        btnDownload.addActionListener(this);
        btnAddFeed = new JButton(" + ");
        btnAddFeed.addActionListener(this);
        btnDeleteFeed = new JButton("  -  ");
        btnDeleteFeed.addActionListener(this);

        JPanel main = new JPanel();
        JPanel left = new JPanel();
        JPanel leftTop = new JPanel();
        JPanel leftBottom1 = new JPanel();
        JPanel leftBottom2 = new JPanel();
        JPanel right = new JPanel();
        JPanel rightTop = new JPanel();
        JPanel rightBottom = new JPanel();
        main.setBorder(new EmptyBorder(10, 10, 10, 10));
        main.setLayout(new BorderLayout(5, 5));
        left.setLayout(new BorderLayout(5, 5));
        leftTop.setLayout(new BorderLayout(5, 5));
        leftBottom1.setLayout(new BorderLayout(5, 5));
        leftBottom2.setLayout(new BorderLayout(5, 5));
        right.setLayout(new BorderLayout(5, 5));
        rightTop.setLayout(new BorderLayout(5, 5));
        rightBottom.setLayout(new BorderLayout(5, 5));

        JScrollPane scrollPane = new JScrollPane(list);
        JScrollPane feedScrollPane = new JScrollPane(feedList);

        JLabel filterLabel = new JLabel("Filter:");
        statusLabelManageFeeds = new JLabel(JDLocale.L("plugins.optional.newsfeeds.pleaseAddFeed", "Please add Feeds."));

        filterText = new JTextField();
        filterText.addActionListener(this);

        frame.add(main);

        main.add(left, BorderLayout.LINE_START);
        main.add(right, BorderLayout.CENTER);

        left.add(leftTop, BorderLayout.PAGE_START);
        left.add(feedScrollPane, BorderLayout.CENTER);
        left.add(leftBottom1, BorderLayout.PAGE_END);

        leftBottom1.add(btnAddFeed, BorderLayout.LINE_END);
        leftBottom1.add(leftBottom2, BorderLayout.CENTER);
        leftBottom2.add(btnDeleteFeed, BorderLayout.LINE_END);

        right.add(rightTop, BorderLayout.PAGE_START);
        right.add(scrollPane, BorderLayout.CENTER);
        right.add(rightBottom, BorderLayout.PAGE_END);

        rightTop.add(filterLabel, BorderLayout.LINE_START);
        rightTop.add(filterText, BorderLayout.CENTER);

        rightBottom.add(btnDownload, BorderLayout.LINE_END);
        rightBottom.add(statusLabelManageFeeds, BorderLayout.LINE_START);

        loadFeeds();

        frame.setResizable(true);
        frame.pack();
        SimpleGUI.restoreWindow(null, null, frame);
        frame.setVisible(true);

    }

    private void showManageDownloadSubscriptionsGui() {

        frame = new JFrame();
        frame.setTitle(JDLocale.L("plugins.optional.newsfeeds.manageDownloadSubscriptions", "Manage Download Subscriptions"));
        frame.setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.jd_logo")));
        frame.setPreferredSize(new Dimension(300, 200));
        frame.setName("ADDON_NEWSFEED_2");
        LocationListener listener = new LocationListener();
        frame.addComponentListener(listener);
        frame.addWindowListener(listener);
        frame.setLayout(new BorderLayout(5, 5));

        aboList = new JList();
        aboList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        btnDeleteAbo = new JButton("  -  ");
        btnDeleteAbo.addActionListener(this);
        btnAddAbo = new JButton(" + ");
        btnAddAbo.addActionListener(this);

        JPanel main = new JPanel(new BorderLayout(5, 5));
        JPanel buttons1 = new JPanel(new BorderLayout(5, 5));
        JPanel buttons2 = new JPanel(new BorderLayout(5, 5));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(aboList);

        main.add(scrollPane, BorderLayout.CENTER);
        main.add(buttons1, BorderLayout.PAGE_END);

        buttons1.add(buttons2, BorderLayout.CENTER);
        buttons1.add(btnAddAbo, BorderLayout.LINE_END);

        buttons2.add(btnDeleteAbo, BorderLayout.LINE_END);

        loadAbos();
        setAboList();
        if (abos.size() > 0) aboList.setSelectedIndex(0);

        frame.add(main);
        frame.setResizable(true);
        frame.pack();
        SimpleGUI.restoreWindow(null, null, frame);
        frame.setVisible(true);

    }

    private void showGetLatestSubscribedDownloadsGui() {

        frame = new JFrame();
        frame.setTitle(JDLocale.L("plugins.optional.newsfeeds.getLatestSubscribedDownloads", "Get latest subscribed Downloads"));
        frame.setIconImage(JDUtilities.getImage(JDTheme.V("gui.images.jd_logo")));
        frame.setPreferredSize(new Dimension(300, 200));
        frame.setName("ADDON_NEWSFEED_3");
        LocationListener listener = new LocationListener();
        frame.addComponentListener(listener);
        frame.addWindowListener(listener);
        frame.setLayout(new BorderLayout(5, 5));

        subscribedList = new JList();
        subscribedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        btnDownload2 = new JButton(JDLocale.L("plugins.optional.newsfeeds.downloadSelected", "Download Selected"));
        btnDownload2.addActionListener(this);

        statusLabelGetSubscribed = new JLabel(JDLocale.L("plugins.optional.newsfeeds.pleaseCreateSubscription", "Please create Download Subscriptions."));

        JPanel main = new JPanel(new BorderLayout(5, 5));
        JPanel buttons = new JPanel(new BorderLayout(5, 5));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(subscribedList);

        main.add(scrollPane, BorderLayout.CENTER);
        main.add(buttons, BorderLayout.PAGE_END);

        buttons.add(btnDownload2, BorderLayout.LINE_END);
        buttons.add(statusLabelGetSubscribed, BorderLayout.LINE_START);

        setSubscribedList();

        int[] indices = new int[entriesGetSubscribed.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }

        subscribedList.setSelectedIndices(indices);

        frame.add(main);
        frame.setResizable(true);
        frame.pack();
        SimpleGUI.restoreWindow(null, null, frame);
        frame.setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 0) {

            showManageFeedsGui();

        } else if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 1) {

            showManageDownloadSubscriptionsGui();

        } else if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 2) {

            showGetLatestSubscribedDownloadsGui();

        } else if (e.getSource().equals(btnDownload)) {

            int[] indices = list.getSelectedIndices();
            Vector<String> links = new Vector<String>();

            for (int index : indices) {

                for (String link : specialFeedWorkarounds(entriesManageFeeds.get(index))) {
                    links.add(link);
                }

            }

            String linksString = "";

            for (String link : links) {
                linksString += link + "\n";
            }

            DistributeData distributeData = new DistributeData(linksString);
            distributeData.addControlListener(JDUtilities.getController());
            distributeData.start();

        } else if (e.getSource().equals(filterText)) {

            setEntryList(filterText.getText());

        } else if (e.getSource().equals(btnAddFeed)) {

            AddFeedDialog dialog = new AddFeedDialog(frame);
            String title = dialog.title;
            String url = dialog.url;

            if (title != null && url != null) {

                feeds.add(new String[] { url, title });
                setFeedList();
                feedList.setSelectedIndex(feeds.size() - 1);
                saveFeeds();

            }

        } else if (e.getSource().equals(btnDeleteFeed)) {

            int i = feedList.getSelectedIndex();
            feeds.remove(i);

            if (i < feeds.size()) {
                feedList.setSelectedIndex(i);
            } else if (i - 1 > -1 && i - 1 < feeds.size()) {
                feedList.setSelectedIndex(i - 1);
            }

            setFeedList();
            saveFeeds();

        } else if (e.getSource().equals(btnAddAbo)) {

            AddAboDialog dialog = new AddAboDialog(frame);

            if (dialog.url != null && dialog.title != null && dialog.filter != null) {

                abos.add(new String[] { dialog.url, dialog.title, dialog.filter });
                setAboList();
                aboList.setSelectedIndex(abos.size() - 1);
                saveAbos();

            }

        } else if (e.getSource().equals(btnDeleteAbo)) {

            int i = aboList.getSelectedIndex();
            abos.remove(i);

            if (i < abos.size()) {
                aboList.setSelectedIndex(i);
            } else if (i - 1 > -1 && i - 1 < abos.size()) {
                aboList.setSelectedIndex(i - 1);
            }

            setAboList();
            saveAbos();

        } else if (e.getSource().equals(btnDownload2)) {

            int[] indices = subscribedList.getSelectedIndices();
            Vector<String> links = new Vector<String>();

            for (int index : indices) {

                for (String link : specialFeedWorkarounds(entriesGetSubscribed.get(index))) {
                    links.add(link);
                }

            }

            String linksString = "";

            for (String link : links) {
                linksString += link + "\n";
            }

            DistributeData distributeData = new DistributeData(linksString);
            distributeData.addControlListener(JDUtilities.getController());
            distributeData.start();

        }

    }

    public void valueChanged(ListSelectionEvent e) {

        if (e.getSource().equals(feedList) && feedList.getSelectedIndex() != feedListSelectedIndex) {

            feedListSelectedIndex = feedList.getSelectedIndex();
            setEntryList(filterText.getText());

        }

    }

    /*
     * List setters
     */

    private void setEntryList(final String filter) {

        if (feedList.getSelectedIndex() != -1) {

            list.removeAll();
            statusLabelManageFeeds.setText(JDLocale.L("plugins.optional.newsfeeds.loading", "Loading..."));

            Thread thread = new Thread(new Runnable() {
                public void run() {

                    String url = feeds.get(feedList.getSelectedIndex())[0];
                    entriesManageFeeds.clear();
                    entriesManageFeeds = getEntries(url, filter);
                    Vector<String> titles = new Vector<String>();

                    for (String[] entry : entriesManageFeeds) {
                        titles.add(entry[0].replaceAll("\\.", " "));
                    }

                    list.setListData(titles);
                    statusLabelManageFeeds.setText("");

                }
            });

            thread.start();

        }

    }

    private void setFeedList() {

        feedList.removeAll();
        Vector<String> titles = new Vector<String>();

        for (String[] feed : feeds) {

            titles.add(feed[1]);

        }

        feedList.setListData(titles);

    }

    private void setAboList() {

        aboList.removeAll();
        Vector<String> data = new Vector<String>();

        for (String[] abo : abos) {
            data.add(abo[1] + " : " + abo[2]);
        }

        aboList.setListData(data);

    }

    private void setSubscribedList() {

        loadAbos();

        if (abos.size() > 0) {

            subscribedList.removeAll();
            statusLabelGetSubscribed.setText(JDLocale.L("plugins.optional.newsfeeds.loading", "Loading..."));

            Thread thread = new Thread(new Runnable() {
                public void run() {

                    Vector<String[]> newEntries = new Vector<String[]>();
                    Vector<String> titles = new Vector<String>();

                    for (String[] abo : abos) {

                        entriesGetSubscribed.clear();
                        entriesGetSubscribed = getEntries(abo[0], abo[2]);
                        newEntries.addAll(entriesGetSubscribed);

                    }

                    for (String[] entry : newEntries) {

                        titles.add(entry[0]);

                    }

                    entriesGetSubscribed = newEntries;
                    subscribedList.setListData(titles);

                    if (entriesGetSubscribed.size() > 0) {
                        statusLabelGetSubscribed.setText("");
                    } else {
                        statusLabelGetSubscribed.setText(JDLocale.L("plugins.optional.newsfeeds.nowSubscribedFound", "No subscribed Downloads were found."));
                    }

                }
            });

            thread.start();

        }

    }

    /*
     * RSS/ATOM
     */

    private Vector<String[]> getEntries(final String url, final String filter) {

        Vector<String[]> vector = new Vector<String[]>();

        try {

            logger.info("loading " + url);

            requestInfo = HTTP.getRequest(new URL(url));

            String patEntry = "";
            String patTitle = "";
            String patLink = "";

            if (requestInfo.containsHTML("<feed")) {

                patEntry = "<entry>(.*?)</entry>";
                patTitle = "<title.*?>(.*?)</title>";
                patLink = "<link.*?href='|\"(.*?)'|\".*?/>";

            } else if (requestInfo.containsHTML("<rss")) {

                patEntry = "<item>(.*?)</item>";
                patTitle = "<title>(.*?)</title>";
                patLink = "<link>(.*?)</link>";

            }

            String[] matches = requestInfo.getRegexp(patEntry).getMatches(1);

            for (String match : matches) {

                String title = new Regex(Pattern.compile(patTitle).matcher(match)).getFirstMatch(1);
                String link = new Regex(Pattern.compile(patLink).matcher(match)).getFirstMatch(1);

                if (title != null && link != null) {

                    String[] temp = new String[2];
                    temp[0] = JDUtilities.htmlDecode(title);
                    temp[1] = JDUtilities.htmlDecode(link);

                    if (temp[0].toLowerCase().contains(filter.toLowerCase())) vector.add(temp);

                }

            }

            logger.info("finished!");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return vector;

    }

    public Vector<String> specialFeedWorkarounds(String[] parameter) {

        Vector<String> links = new Vector<String>();

        if (parameter[1].contains("http://serienjunkies.org/?cat=")) {

            try {

                requestInfo = HTTP.getRequest(new URL(parameter[1]), serienjunkiesCookie, null, true);

                if (requestInfo.containsHTML("Wenn Sie nicht weitergeleitet werden")) {

                    System.out.println("Serienjunkies forewarding..");

                    requestInfo = HTTP.getRequest(new URL("http://serienjunkies.org/enter"), serienjunkiesCookie, parameter[1], true);
                    serienjunkiesCookie = requestInfo.getCookie();
                    requestInfo = HTTP.getRequest(new URL(parameter[1]), serienjunkiesCookie, "http://serienjunkies.org/enter", true);

                }

                Regex regex = requestInfo.getRegexp("Pages \\(([0-9]+)\\):");
                String pagesString = regex.getFirstMatch(1);
                int pages = 1;
                if (pagesString != null) pages = Integer.parseInt(pagesString);

                boolean found = false;

                for (int i = 1; i <= pages; i++) {

                    if (found) break;
                    if (pages > 1) requestInfo = HTTP.getRequest(new URL(parameter[1] + "&paged=" + i));

                    regex = requestInfo.getRegexp("<p><strong>(.*?)</strong>(.*?)</p>");
                    String[][] matches2 = regex.getMatches();

                    for (String[] match2 : matches2) {

                        String id1 = new Regex(Pattern.compile("(S[0-9]+){0,1}E[0-9]+").matcher(match2[0])).getFirstMatch(0);
                        String id2 = new Regex(Pattern.compile("(S[0-9]+){0,1}E[0-9]+").matcher(parameter[0])).getFirstMatch(0);

                        if (parameter[0].contains(match2[0]) || (id1 != null && id2 != null && id1.equals(id2))) {

                            found = true;

                            regex = new Regex(Pattern.compile("href=\"(http://download\\.serienjunkies\\.org/.*?\\.html)\"").matcher(match2[1]));

                            for (String link : regex.getMatches(1)) {
                                links.add(link);
                            }

                            break;

                        }

                    }

                }

                if (!found) JDUtilities.getGUI().showMessageDialog("Download \"" + parameter[0] + "\" konnte nicht gefunden werden.");

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {

            links.add(parameter[1]);

        }

        return links;

    }

    /*
     * Load/Save
     */
    /**
     * load und save funktionen wurden über die subConfigs gelöst. Eine zentrale
     * Klasse zum ablegen von lokalen daten ist besser
     */
    private void saveFeeds() {

        // String content = "";
        //		
        // for ( String[] feed : feeds ) {
        //    		
        // content += feed[0] + ";" + feed[1] + "\n";
        //    		
        // }
        subConfig.setProperty(PROPERTY_FEEDS, feeds);
        subConfig.save();
        // JDUtilities.writeLocalFile(JDUtilities.getResourceFile("feeds.conf"),
        // content);

    }

    @SuppressWarnings("unchecked")
    private void loadFeeds() {
        Object f = subConfig.getProperty(PROPERTY_FEEDS);
        if (f == null) {
            String content = JDUtilities.getLocalFile(JDUtilities.getResourceFile("feeds.conf"));
            Regex regex = new Regex(Pattern.compile("(.*?);(.*?)\n").matcher(content));

            feeds.clear();

            for (String match[] : regex.getMatches()) {

                feeds.add(new String[] { match[0], match[1] });

            }
            saveFeeds();
        } else {
            JDUtilities.getResourceFile("feeds.conf").deleteOnExit();
            feeds = (Vector<String[]>) f;
            if (feeds == null) feeds = new Vector<String[]>();

        }
        setFeedList();
        if (feeds.size() > 0) feedList.setSelectedIndex(0);

    }

    private void saveAbos() {

        // String content = "";
        //		
        // for ( String[] abo : abos ) {
        //    		
        // content += abo[0] + ";" + abo[1] + ";" + abo[2] + "\n";
        //    		
        // }
        subConfig.setProperty(PROPERTY_ABOS, abos);
        subConfig.save();
        // JDUtilities.writeLocalFile(JDUtilities.getResourceFile("abos.conf"),
        // content);

    }

    @SuppressWarnings("unchecked")
    private void loadAbos() {
        Object f = subConfig.getProperty(PROPERTY_ABOS);
        if (f == null) {
            String content = JDUtilities.getLocalFile(JDUtilities.getResourceFile("abos.conf"));
            Regex regex = new Regex(Pattern.compile("(.*?);(.*?);(.*?)\n").matcher(content));

            abos.clear();

            for (String match[] : regex.getMatches()) {

                abos.add(new String[] { match[0], match[1], match[2] });

            }
            saveAbos();
        } else {
            JDUtilities.getResourceFile("feeds.conf").deleteOnExit();
            abos = (Vector<String[]>) f;
            if (abos == null) abos = new Vector<String[]>();

        }

    }

    /*
     * Dialogs
     */

    private class AddFeedDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        private JButton btnOK;
        private JButton btnCancel;
        private JFrame owner;
        private JTextField titleField = new JTextField();
        private JTextField urlField = new JTextField();
        private JLabel titleLabel = new JLabel("Titel:");
        private JLabel urlLabel = new JLabel("URL:");

        public String title = "";
        public String url = "";

        public AddFeedDialog(JFrame owner) {

            super(owner);
            this.owner = owner;

            setModal(true);
            setLayout(new BorderLayout(5, 5));
            setTitle(JDLocale.L("plugins.optional.newsfeeds.addFeedDialogTitle", "Add Feed"));
            getRootPane().setDefaultButton(btnOK);

            btnOK = new JButton(JDLocale.L("gui.btn_add", "Add"));
            btnOK.addActionListener(this);
            btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
            btnCancel.addActionListener(this);

            JPanel main = new JPanel(new BorderLayout(5, 5));
            JPanel titlePanel = new JPanel(new BorderLayout(5, 5));
            JPanel urlPanel = new JPanel(new BorderLayout(5, 5));
            JPanel buttonPanel1 = new JPanel(new BorderLayout(5, 5));
            JPanel buttonPanel2 = new JPanel(new BorderLayout(5, 5));
            main.setBorder(new EmptyBorder(10, 10, 10, 10));

            main.add(titlePanel, BorderLayout.PAGE_START);
            main.add(urlPanel, BorderLayout.CENTER);
            main.add(buttonPanel1, BorderLayout.PAGE_END);

            titlePanel.add(titleLabel, BorderLayout.LINE_START);
            titlePanel.add(titleField, BorderLayout.CENTER);
            urlPanel.add(urlLabel, BorderLayout.LINE_START);
            urlPanel.add(urlField, BorderLayout.CENTER);

            buttonPanel1.add(btnOK, BorderLayout.LINE_END);
            buttonPanel1.add(buttonPanel2, BorderLayout.CENTER);
            buttonPanel2.add(btnCancel, BorderLayout.LINE_END);

            setContentPane(main);
            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == btnOK) {

                title = titleField.getText();
                url = urlField.getText();
                dispose();
                owner.setVisible(true);

            } else if (e.getSource() == btnCancel) {

                title = null;
                url = null;
                dispose();
                owner.setVisible(true);

            }

        }

    }

    private class AddAboDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        private JButton btnOK;
        private JButton btnCancel;
        private JFrame owner;
        private JTextField filterField = new JTextField();

        public String title;
        public String url;
        public String filter;

        public AddAboDialog(JFrame owner) {

            super(owner);
            this.owner = owner;

            setModal(true);
            setLayout(new BorderLayout(5, 5));
            setTitle(JDLocale.L("plugins.optional.newsfeeds.addAboDialogTitle", "Add Subscription"));
            getRootPane().setDefaultButton(btnOK);

            feedList = new JList();
            feedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            loadFeeds();

            JScrollPane feedScrollPane = new JScrollPane(feedList);

            JLabel chooseFeedLabel = new JLabel(JDLocale.L("plugins.optional.newsfeeds.addAboDialogSelectFeed", "Please select Feed") + ":");
            JLabel keywordLabeld = new JLabel(JDLocale.L("plugins.optional.newsfeeds.addAboDialogKeyword", "Keyword") + ":");

            btnOK = new JButton(JDLocale.L("gui.btn_add", "Add"));
            btnOK.addActionListener(this);
            btnCancel = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
            btnCancel.addActionListener(this);

            JPanel main = new JPanel(new BorderLayout(5, 5));
            JPanel bottom = new JPanel(new BorderLayout(5, 5));
            JPanel buttonPanel1 = new JPanel(new BorderLayout(5, 5));
            JPanel buttonPanel2 = new JPanel(new BorderLayout(5, 5));
            main.setBorder(new EmptyBorder(10, 10, 10, 10));

            main.add(chooseFeedLabel, BorderLayout.PAGE_START);
            main.add(feedScrollPane, BorderLayout.CENTER);
            main.add(bottom, BorderLayout.PAGE_END);

            bottom.add(keywordLabeld, BorderLayout.LINE_START);
            bottom.add(filterField, BorderLayout.CENTER);
            bottom.add(buttonPanel1, BorderLayout.PAGE_END);

            buttonPanel1.add(buttonPanel2, BorderLayout.CENTER);
            buttonPanel1.add(btnOK, BorderLayout.LINE_END);

            buttonPanel2.add(btnCancel, BorderLayout.LINE_END);

            setContentPane(main);
            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == btnOK) {

                url = feeds.get(feedList.getSelectedIndex())[0];
                title = feeds.get(feedList.getSelectedIndex())[1];
                filter = filterField.getText();

                dispose();
                owner.setVisible(true);

            } else if (e.getSource() == btnCancel) {

                title = null;
                url = null;
                filter = null;

                dispose();
                owner.setVisible(true);

            }

        }

    }

}