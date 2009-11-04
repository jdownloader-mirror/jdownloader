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

package jd.plugins;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.CaptchaController;
import jd.controlling.DownloadController;
import jd.controlling.JDLogger;
import jd.gui.UserIF;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.JDImage;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Dies ist die Oberklasse für alle Plugins, die von einem Anbieter Dateien
 * herunterladen können
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {

    public PluginForHost(PluginWrapper wrapper) {
        super(wrapper);
        config.setIcon(getHosterIcon());
    }

    protected String getCaptchaCode(String captchaAddress, DownloadLink downloadLink) throws IOException, PluginException {
        return getCaptchaCode(getHost(), captchaAddress, downloadLink);
    }

    @Override
    public String getVersion() {
        return wrapper.getVersion();
    }

    protected String getCaptchaCode(String method, String captchaAddress, DownloadLink downloadLink) throws IOException, PluginException {
        if (captchaAddress == null) {
            logger.severe("Captcha Adresse nicht definiert");
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        File captchaFile = getLocalCaptchaFile();
        try {
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaAddress));
        } catch (Exception e) {
            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String captchaCode = getCaptchaCode(method, captchaFile, downloadLink);
        return captchaCode;
    }

    protected String getCaptchaCode(File captchaFile, DownloadLink downloadLink) throws PluginException {
        return getCaptchaCode(getHost(), captchaFile, downloadLink);
    }

    protected String getCaptchaCode(String methodname, File captchaFile, DownloadLink downloadLink) throws PluginException {
        return getCaptchaCode(methodname, captchaFile, 0, downloadLink, null, null);
    }

    protected String getCaptchaCode(String method, File file, int flag, DownloadLink link, String defaultValue, String explain) throws PluginException {
        String status = link.getLinkStatus().getStatusText();
        try {
            link.getLinkStatus().addStatus(LinkStatus.WAITING_USERIO);
            link.getLinkStatus().setStatusText(JDL.L("gui.downloadview.statustext.jac", "Captcha recognition"));
            try {
                BufferedImage img = ImageIO.read(file);
                link.getLinkStatus().setStatusIcon(JDImage.getScaledImageIcon(img, 16, 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
            DownloadController.getInstance().fireDownloadLinkUpdate(link);
            String cc = new CaptchaController(getHost(), method, file, defaultValue, explain).getCode(flag);

            if (cc == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            return cc;
        } finally {
            link.getLinkStatus().removeStatus(LinkStatus.WAITING_USERIO);
            link.getLinkStatus().setStatusText(status);
            link.getLinkStatus().setStatusIcon(null);
            DownloadController.getInstance().fireDownloadLinkUpdate(link);
        }
    }

    private static final String AGB_CHECKED = "AGB_CHECKED";
    private static final String CONFIGNAME = "pluginsForHost";
    private static int currentConnections = 0;

    public static final String PARAM_MAX_RETRIES = "MAX_RETRIES";
    protected DownloadInterface dl = null;
    private int maxConnections = 50;

    private static HashMap<String, Long> LAST_CONNECTION_TIME = new HashMap<String, Long>();
    private static HashMap<String, Long> LAST_STARTED_TIME = new HashMap<String, Long>();
    private Long WAIT_BETWEEN_STARTS = 0L;

    private boolean enablePremium = false;

    private boolean accountWithoutUsername = false;

    private String premiumurl = null;

    private ImageIcon hosterIcon;
    private MenuAction premiumAction;

    public boolean checkLinks(DownloadLink[] urls) {
        return false;
    }

    @Override
    public void clean() {
        dl = null;
        super.clean();
    }

    protected int waitForFreeConnection(DownloadLink downloadLink) throws InterruptedException {
        int free;
        while ((free = getMaxConnections() - getCurrentConnections()) <= 0) {
            Thread.sleep(1000);
            downloadLink.getLinkStatus().setStatusText(JDL.LF("download.system.waitForconnection", "Cur. %s/%s connections...waiting", getCurrentConnections() + "", getMaxConnections() + ""));
            downloadLink.requestGuiUpdate();
        }
        return free;
    }

    protected void setBrowserExclusive() {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getID() == 1) {
            UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, config);

            return;
        }
        if (e.getID() == 2) {

            UserIF.getInstance().requestPanel(UserIF.Panels.PREMIUMCONFIG, null);
            ActionController.getToolBarAction("action.premiumview.addacc").actionPerformed(new ActionEvent(this, 0, "addaccount"));
            return;
        }

        if (e.getID() == 3) {

            UserIF.getInstance().requestPanel(UserIF.Panels.PREMIUMCONFIG, null);

            try {
                JLink.openURL(getBuyPremiumUrl());
            } catch (Exception ex) {
            }

            return;
        }
        ArrayList<Account> accounts = getPremiumAccounts();
        if (e.getID() >= 200) {
            int accountID = e.getID() - 200;
            Account account = accounts.get(accountID);
            UserIF.getInstance().requestPanel(UserIF.Panels.PREMIUMCONFIG, account);

        } else if (e.getID() >= 100) {
            int accountID = e.getID() - 100;
            Account account = accounts.get(accountID);
            account.setEnabled(!account.isEnabled());
        }
    }

    /** default fetchAccountInfo, set account valid to true */
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        account.setValid(true);
        return null;
    }

    public boolean getAccountwithoutUsername() {
        return accountWithoutUsername;
    }

    public void setAccountwithoutUsername(boolean b) {
        accountWithoutUsername = b;
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {

        if (!enablePremium) return null;
        ArrayList<MenuAction> menuList = new ArrayList<MenuAction>();
        MenuAction account;
        MenuAction m;

        if (config != null && config.getEntries().size() > 0) {
            m = new MenuAction("plugins.configs", 1);
            m.setActionListener(this);
            menuList.add(m);
            menuList.add(new MenuAction(Types.SEPARATOR));
        }

        if (config != null) config.setGroup(new ConfigGroup(getHost(), getHosterIcon()));

        if (premiumAction == null) {
            premiumAction = new MenuAction("accounts", 0);
            premiumAction.setType(Types.CONTAINER);
            ArrayList<Account> accounts = getPremiumAccounts();

            int i = 1;
            int c = 0;
            for (Account a : accounts) {
                if (a == null) continue;
                try {
                    c++;
                    if (getAccountwithoutUsername()) {
                        if (a.getPass() == null || a.getPass().trim().length() == 0) continue;
                        account = new MenuAction("account." + getHost() + "." + i, 0);
                        account.setTitle(i++ + ". " + "Account " + (i - 1));
                        account.setType(ToolBarAction.Types.CONTAINER);
                    } else {
                        if (a.getUser() == null || a.getUser().trim().length() == 0) continue;
                        account = new MenuAction("account." + getHost() + "." + i, 0);
                        account.setTitle(i++ + ". " + a.getUser());
                        account.setType(ToolBarAction.Types.CONTAINER);
                    }
                    m = AccountMenuItemSyncer.getInstance().get(a);

                    if (m == null) {
                        m = new MenuAction("plugins.PluginForHost.enable_premium", 100 + c - 1);
                    }
                    m.setActionID(100 + c - 1);
                    m.setSelected(a.isEnabled());
                    m.setActionListener(this);
                    account.addMenuItem(m);

                    AccountMenuItemSyncer.getInstance().map(a, m);

                    m = new MenuAction("plugins.PluginForHost.premiumInfo", 200 + c - 1);
                    m.setActionListener(this);
                    account.addMenuItem(m);
                    premiumAction.addMenuItem(account);

                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }

        }
        if (premiumAction.getSize() != 0) {
            menuList.add(premiumAction);
        } else {
            menuList.add(m = new MenuAction("plugins.menu.noaccounts", 2));
            m.setActionListener(this);
        }
        menuList.add(m = new MenuAction("plugins.menu.buyaccount", 3));
        m.setActionListener(this);

        return menuList;

    }

    public abstract String getAGBLink();

    protected void enablePremium() {
        enablePremium(null);
    }

    protected void enablePremium(String url) {
        premiumurl = url;
        enablePremium = true;
    }

    public static synchronized int getCurrentConnections() {
        return currentConnections;
    }

    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text
     * gesucht. Gefundene Links werden dann in einem ArrayList zurückgeliefert
     * 
     * @param data
     *            Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein ArrayList mit den gefundenen Downloadlinks
     */
    public ArrayList<DownloadLink> getDownloadLinks(String data, FilePackage fp) {

        ArrayList<DownloadLink> links = null;
        String[] hits = new Regex(data, getSupportedLinks()).getColumn(-1);
        if (hits != null && hits.length > 0) {
            links = new ArrayList<DownloadLink>();
            for (String file : hits) {
                while (file.charAt(0) == '"') {
                    file = file.substring(1);
                }
                while (file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }

                try {
                    // Zwecks Multidownload braucht jeder Link seine eigene
                    // Plugininstanz
                    PluginForHost plg = (PluginForHost) wrapper.getNewPluginInstance();
                    DownloadLink link = new DownloadLink(plg, file.substring(file.lastIndexOf("/") + 1, file.length()), getHost(), file, true);
                    links.add(link);
                    if (fp != null) {
                        link.setFilePackage(fp);
                    }

                } catch (IllegalArgumentException e) {
                    JDLogger.exception(e);
                } catch (SecurityException e) {
                    JDLogger.exception(e);
                }
            }
        }
        return links;
    }

    /** überschreiben falls die downloadurl erst rekonstruiert werden muss */
    public void correctDownloadLink(DownloadLink link) throws Exception {
    }

    /**
     * Holt Informationen zu einem Link. z.B. dateigröße, Dateiname,
     * verfügbarkeit etc.
     * 
     * @param parameter
     * @return true/false je nach dem ob die Datei noch online ist (verfügbar)
     * @throws IOException
     */
    public abstract AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception;

    /**
     * Gibt einen String mit den Dateiinformationen zurück. Die Defaultfunktion
     * gibt nur den dateinamen zurück. Allerdings Sollte diese Funktion
     * überschrieben werden. So kann ein Plugin zusatzinfos zu seinen Links
     * anzeigen (Nach dem aufruf von getFileInformation()
     * 
     * @param downloadLink
     * @return
     */
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + Formatter.formatReadable(downloadLink.getDownloadSize()) + ")";
    }

    public synchronized int getFreeConnections() {
        return Math.max(1, getMaxConnections() - currentConnections);
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getMaxRetries() {
        return SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(PARAM_MAX_RETRIES, 3);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public int getMaxSimultanDownloadNum() {
        int max;
        if (isPremiumDownload()) {
            max = getMaxSimultanPremiumDownloadNum();
        } else {
            max = getMaxSimultanFreeDownloadNum();
        }
        if (max < 0) return Integer.MAX_VALUE;
        return max;
    }

    public boolean isPremiumDownload() {
        if (!enablePremium || !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) return false;
        if (AccountController.getInstance().getValidAccount(this) == null) return false;
        return true;
    }

    public synchronized long getLastTimeStarted() {
        if (!LAST_STARTED_TIME.containsKey(getHost())) { return 0; }
        return Math.max(0, (LAST_STARTED_TIME.get(getHost())));
    }

    public synchronized void putLastTimeStarted(long time) {
        LAST_STARTED_TIME.put(getHost(), time);
    }

    public synchronized long getLastConnectionTime() {
        if (!LAST_CONNECTION_TIME.containsKey(getHost())) { return 0; }
        return Math.max(0, (LAST_CONNECTION_TIME.get(getHost())));
    }

    public synchronized void putLastConnectionTime(long time) {
        LAST_CONNECTION_TIME.put(getHost(), time);
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        link.getLinkStatus().addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.getLinkStatus().setErrorMessage(JDL.L("plugins.hoster.nopremiumsupport", "Plugin has no handlePremium Method!"));
    }

    public abstract void handleFree(DownloadLink link) throws Exception;

    public void handle(DownloadLink downloadLink) throws Exception {
        downloadLink.getTransferStatus().usePremium(false);
        downloadLink.getTransferStatus().setResumeSupport(false);
        try {
            while (waitForNextStartAllowed(downloadLink)) {
            }
        } catch (InterruptedException e) {
            return;
        }
        putLastTimeStarted(System.currentTimeMillis());
        if (!isAGBChecked()) {
            logger.severe("AGB not signed : " + this.getWrapper().getID());
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_AGB_NOT_SIGNED);
            return;
        }

        Account account = null;
        if (enablePremium) account = AccountController.getInstance().getValidAccount(this);
        if (account != null) {
            long before = downloadLink.getDownloadCurrent();
            try {
                downloadLink.getTransferStatus().usePremium(true);
                handlePremium(downloadLink, account);
                if (dl != null && dl.getConnection() != null) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (Exception e) {
                    }
                }
            } catch (PluginException e) {
                e.fillLinkStatus(downloadLink.getLinkStatus());
                logger.info(downloadLink.getLinkStatus().getLongErrorMessage());
            }

            long traffic = Math.max(0, downloadLink.getDownloadCurrent() - before);
            boolean throwupdate = false;
            synchronized (AccountController.ACCOUNT_LOCK) {
                AccountInfo ai = account.getAccountInfo();
                if (traffic > 0 && ai != null && !ai.isUnlimitedTraffic()) {
                    long left = Math.max(0, ai.getTrafficLeft() - traffic);
                    ai.setTrafficLeft(left);
                    if (left == 0 && ai.isSpecialTraffic()) {
                        logger.severe("Premium Account " + account.getUser() + ": Traffic Limit could be reached, but SpecialTraffic might be available!");
                    } else if (left == 0) {
                        logger.severe("Premium Account " + account.getUser() + ": Traffic Limit reached");
                        account.setTempDisabled(true);
                    }
                    throwupdate = true;
                }
            }
            if (throwupdate) AccountController.getInstance().throwUpdateEvent(this, account);
            if (downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_PREMIUM)) {
                if (downloadLink.getLinkStatus().getValue() == PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                    logger.severe("Premium Account " + account.getUser() + ": Traffic Limit reached");
                    account.setTempDisabled(true);
                    if (account.getAccountInfo() != null) account.getAccountInfo().setStatus(downloadLink.getLinkStatus().getErrorMessage());
                } else if (downloadLink.getLinkStatus().getValue() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
                    account.setEnabled(false);
                    if (account.getAccountInfo() != null) account.getAccountInfo().setStatus(downloadLink.getLinkStatus().getErrorMessage());
                    logger.severe("Premium Account " + account.getUser() + ": expired:" + downloadLink.getLinkStatus().getLongErrorMessage());
                } else {
                    account.setEnabled(false);
                    if (account.getAccountInfo() != null) account.getAccountInfo().setStatus(downloadLink.getLinkStatus().getErrorMessage());
                    logger.severe("Premium Account " + account.getUser() + ":" + downloadLink.getLinkStatus().getLongErrorMessage());
                }
            } else {
                if (account.getAccountInfo() != null) account.getAccountInfo().setStatus(JDL.L("plugins.hoster.premium.status_ok", "Account is ok"));
            }
        } else {
            try {
                handleFree(downloadLink);
                if (dl != null && dl.getConnection() != null) {
                    try {
                        dl.getConnection().disconnect();
                    } catch (Exception e) {
                    }
                }
            } catch (PluginException e) {
                e.fillLinkStatus(downloadLink.getLinkStatus());
                logger.info(downloadLink.getLinkStatus().getLongErrorMessage());
            }
        }
        return;
    }

    public boolean isAGBChecked() {
        if (!getPluginConfig().hasProperty(AGB_CHECKED)) {
            // this is just so complicated to preserv compatibility
            getPluginConfig().setProperty(AGB_CHECKED, SubConfiguration.getConfig(CONFIGNAME).getBooleanProperty("AGBS_CHECKED_" + getPluginID(), false) || SubConfiguration.getConfig(CONFIGNAME).getBooleanProperty("AGB_CHECKED_" + getHost(), false));
            getPluginConfig().save();
        }
        return getPluginConfig().getBooleanProperty(AGB_CHECKED, false);
    }

    /**
     * Stellt das Plugin in den Ausgangszustand zurück (variablen intialisieren
     * etc)
     */
    public abstract void reset();

    public abstract void resetDownloadlink(DownloadLink link);

    public void resetPluginGlobals() {
    }

    public void setAGBChecked(boolean value) {
        getPluginConfig().setProperty(AGB_CHECKED, value);
        getPluginConfig().save();
    }

    public static synchronized void setCurrentConnections(int CurrentConnections) {
        currentConnections = CurrentConnections;
    }

    public int getTimegapBetweenConnections() {
        return 50;
    }

    public void setStartIntervall(long interval) {
        WAIT_BETWEEN_STARTS = interval;
    }

    public boolean waitForNextStartAllowed(DownloadLink downloadLink) throws InterruptedException {
        long time = Math.max(0, WAIT_BETWEEN_STARTS - (System.currentTimeMillis() - getLastTimeStarted()));
        if (time > 0) {
            try {
                sleep(time, downloadLink);
            } catch (PluginException e) {

                // downloadLink.getLinkStatus().setStatusText(null);
                throw new InterruptedException();
            }
            // downloadLink.getLinkStatus().setStatusText(null);
            return true;
        } else {
            // downloadLink.getLinkStatus().setStatusText(null);
            return false;
        }
    }

    public boolean waitForNextConnectionAllowed() throws InterruptedException {
        long time = Math.max(0, getTimegapBetweenConnections() - (System.currentTimeMillis() - getLastConnectionTime()));
        if (time > 0) {
            Thread.sleep(time);
            return true;
        } else {
            return false;
        }
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void sleep(long i, DownloadLink downloadLink) throws PluginException {
        sleep(i, downloadLink, "");
    }

    public void sleep(long i, DownloadLink downloadLink, String message) throws PluginException {
        try {
            while (i > 0 && downloadLink.getDownloadLinkController() != null && !downloadLink.getDownloadLinkController().isAborted()) {
                i -= 1000;
                downloadLink.getLinkStatus().setStatusText(message + JDL.LF("gui.download.waittime_status2", "Wait %s", Formatter.formatSeconds(i / 1000)));
                downloadLink.requestGuiUpdate();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            throw new PluginException(LinkStatus.TODO);
        }
        downloadLink.getLinkStatus().setStatusText(null);
    }

    public boolean isAborted(DownloadLink downloadLink) {
        return (downloadLink.getDownloadLinkController() != null && downloadLink.getDownloadLinkController().isAborted());
    }

    public Browser getBrowser() {
        return br;
    }

    public void setDownloadInterface(DownloadInterface dl2) {
        this.dl = dl2;
    }

    /**
     * Gibt die Url zurück, unter welcher ein PremiumAccount gekauft werden kann
     * 
     * @return
     */
    public String getBuyPremiumUrl() {
        if (premiumurl != null) return "http://jdownloader.org/r.php?u=" + Encoding.urlEncode(premiumurl);
        return premiumurl;
    }

    public boolean isPremiumEnabled() {
        return enablePremium;
    }

    public ArrayList<Account> getPremiumAccounts() {
        return AccountController.getInstance().getAllAccounts(this);
    }

    /**
     * returns hosterspecific infos. for example the downloadserver
     * 
     * @return
     */
    public String getSessionInfo() {
        return "";
    }

    public ImageIcon getHosterIcon() {
        if (hosterIcon == null) hosterIcon = initHosterIcon();
        return hosterIcon;
    }

    private final ImageIcon initHosterIcon() {
        Image image = JDImage.getImage("hosterlogos/" + getHost());
        if (image == null) image = createDefaultIcon();
        if (image != null) return new ImageIcon(image);
        return null;
    }

    private final String cleanString(String host) {
        return host.replaceAll("[a-z0-9\\-\\.]", "");
    }

    /**
     * Creates a dummyHosterIcon
     */
    private final Image createDefaultIcon() {
        int w = 16;
        int h = 16;
        int size = 9;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);
        Graphics2D g = image.createGraphics();
        String host = getHost();
        String dummy = cleanString(host);
        if (dummy.length() < 2) dummy = cleanString(getClass().getSimpleName());
        if (dummy.length() < 2) dummy = host.toUpperCase();
        if (dummy.length() > 2) dummy = dummy.substring(0, 2);
        g.setFont(new Font("Arial", Font.BOLD, size));
        int ww = g.getFontMetrics().stringWidth(dummy);
        // g.setColor(Color.BLACK);
        // g.drawRect(0, 0, w - 1, h - 1);

        g.setColor(Color.WHITE);
        g.fillRect(1, 1, w - 2, h - 2);
        g.setColor(Color.BLACK);
        g.drawString(dummy, (w - ww) / 2, 2 + size);

        g.dispose();
        try {
            File imageFile = JDUtilities.getResourceFile("jd/img/hosterlogos/" + getHost() + ".png", true);
            ImageIO.write(image, "png", imageFile);
        } catch (Exception e) {
            JDLogger.exception(e);
        }
        return image;
    }

    /**
     * Caches the HosterInfo object for this hoster
     * 
     * @see HosterInfo
     * @see #getHosterInfo()
     */
    private HosterInfo hi = null;
    
    private boolean gotHosterInfo = false;

    /**
     * Returns the {@link HosterInfo} object for this hoster which will be
     * served via the {@link HosterInfoServer#getHosterInfoInternal()} method if
     * the hoster implements this interface
     * 
     * @return the cached {@link HosterInfo} object
     */
    public final HosterInfo getHosterInfo() {
        if (gotHosterInfo) return hi;
        gotHosterInfo = true;
        return hi = getHosterInfoInternal();
    }
    
    protected HosterInfo getHosterInfoInternal() {
        return null;
    }
}
