//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org  http://jdownloader.org
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
//

package jd;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jd.captcha.JACController;
import jd.captcha.JAntiCaptcha;
import jd.controlling.ClipboardMonitoring;
import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyEvent;
import jd.controlling.proxy.ProxyInfo;
import jd.gui.UserIF;
import jd.gui.swing.MacOSApplicationAdapter;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.events.EDTEventQueue;
import jd.gui.swing.laf.LookAndFeelController;
import jd.http.Browser;
import jd.http.ext.security.JSPermissionRestricter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;

import org.appwork.app.launcher.parameterparser.CommandSwitch;
import org.appwork.app.launcher.parameterparser.CommandSwitchListener;
import org.appwork.app.launcher.parameterparser.ParameterParser;
import org.appwork.controlling.SingleReachableState;
import org.appwork.shutdown.ShutdownController;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.update.inapp.RlyExitListener;
import org.appwork.update.inapp.WebupdateSettings;
import org.appwork.update.updateclient.InstallLogList;
import org.appwork.update.updateclient.InstalledFile;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.singleapp.AnotherInstanceRunningException;
import org.appwork.utils.singleapp.InstanceMessageListener;
import org.appwork.utils.singleapp.SingleAppInstance;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.api.ExternInterface;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.dynamic.Dynamic;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.gui.uiserio.JDSwingUserIO;
import org.jdownloader.gui.uiserio.NewUIO;
import org.jdownloader.images.NewTheme;
import org.jdownloader.jdserv.stats.StatsManager;
import org.jdownloader.logging.LogController;
import org.jdownloader.logging.LogSource;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.settings.AutoDownloadStartOption;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.translate._JDT;
import org.jdownloader.update.JDUpdater;

public class Launcher {
    static {
        try {

            statics();
        } catch (Throwable e) {
            e.printStackTrace();
            org.jdownloader.controlling.JDRestartController.getInstance().restartViaUpdater(false);
            // TODO: call Updater.jar
        }
    }

    private static LogSource           LOG;
    private static boolean             instanceStarted            = false;
    public static SingleAppInstance    SINGLE_INSTANCE_CONTROLLER = null;

    public static SingleReachableState INIT_COMPLETE              = new SingleReachableState("INIT_COMPLETE");
    public static SingleReachableState GUI_COMPLETE               = new SingleReachableState("GUI_COMPLETE");
    public static ParameterParser      PARAMETERS;
    public final static long           startup                    = System.currentTimeMillis();

    // private static JSonWrapper webConfig;

    /**
     * Sets special Properties for MAC
     */
    private static void initMACProperties() {
        // set DockIcon (most used in Building)
        try {
            com.apple.eawt.Application.getApplication().setDockIconImage(NewTheme.I().getImage("logo/jd_logo_128_128", -1));
        } catch (final Throwable e) {
            /* not every mac has this */
            Launcher.LOG.info("Error Initializing  Mac Look and Feel Special: " + e);
            Launcher.LOG.log(e);
        }

        // Use ScreenMenu in every LAF
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // native Mac just if User Choose Aqua as Skin
        if (LookAndFeelController.getInstance().getPlaf().getName().equals("Apple Aqua")) {
            // Mac Java from 1.3
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
            System.setProperty("com.apple.hwaccel", "true");

            // Mac Java from 1.4
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.showGrowBox", "true");
        }

        try {
            MacOSApplicationAdapter.enableMacSpecial();
        } catch (final Throwable e) {
            Launcher.LOG.info("Error Initializing  Mac Look and Feel Special: " + e);
            Launcher.LOG.log(e);
        }

    }

    public static void statics() {

        try {
            Dynamic.runPreStatic();
        } catch (Throwable e) {
            e.printStackTrace();

        }

        // USe Jacksonmapper in this project
        JSonStorage.setMapper(new JacksonMapper());
        // do this call to keep the correct root in Application Cache

        NewUIO.setUserIO(new JDSwingUserIO());
        RlyExitListener.getInstance().setEnabled(true);
        org.jdownloader.controlling.JDRestartController.getInstance().setApp("JDownloader.app");
        org.jdownloader.controlling.JDRestartController.getInstance().setExe("JDownloader.exe");
        org.jdownloader.controlling.JDRestartController.getInstance().setJar("JDownloader.jar");
        org.jdownloader.controlling.JDRestartController.getInstance().setUpdaterJar("Updater.jar");
    }

    /**
     * Checks if the user uses a correct java version
     */
    private static void javaCheck() {
        if (Application.getJavaVersion() < Application.JAVA15) {
            Launcher.LOG.warning("Javacheck: Wrong Java Version! JDownloader needs at least Java 1.5 or higher!");
            System.exit(0);
        }
        if (Application.isOutdatedJavaVersion(true)) {
            try {
                Dialog.getInstance().showConfirmDialog(Dialog.BUTTONS_HIDE_CANCEL, _JDT._.gui_javacheck_newerjavaavailable_title(Application.getJavaVersion()), _JDT._.gui_javacheck_newerjavaavailable_msg(), NewTheme.I().getIcon("warning", 32), null, null);
                CrossSystem.openURLOrShowMessage("http://jdownloader.org/download/index?updatejava=1");
            } catch (DialogNoAnswerException e) {
            }
        }
    }

    /**
     * Lädt ein Dynamicplugin.
     * 
     * @throws IOException
     */

    public static void mainStart(final String args[]) {

        try {
            Dynamic.runMain(args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Launcher.LOG = LogController.GL;
        // Mac OS specific
        if (CrossSystem.isMac()) {
            // Set MacApplicationName
            // Must be in Main
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JDownloader");
            Launcher.initMACProperties();
        }
        /* hack for ftp plugin to use new ftp style */
        System.setProperty("ftpStyle", "new");
        /* random number: eg used for cnl2 without asking dialog */
        System.setProperty("jd.randomNumber", "" + (System.currentTimeMillis() + new Random().nextLong()));
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.swing.enableImprovedDragGesture", "true");
        // only use ipv4, because debian changed default stack to ipv6
        System.setProperty("java.net.preferIPv4Stack", "true");
        // Disable the GUI rendering on the graphic card
        System.setProperty("sun.java2d.d3d", "false");
        try {
            // log source revision infos
            Launcher.LOG.info(IO.readFileToString(Application.getResource("build.json")));
        } catch (Throwable e1) {
            Launcher.LOG.log(e1);
        }
        final Properties pr = System.getProperties();
        final TreeSet<Object> propKeys = new TreeSet<Object>(pr.keySet());
        for (final Object it : propKeys) {
            final String key = it.toString();
            Launcher.LOG.finer(key + "=" + pr.get(key));
        }
        Launcher.LOG.info("JDownloader");
        PARAMETERS = new ParameterParser(args);
        PARAMETERS.getEventSender().addListener(new CommandSwitchListener() {

            @Override
            public void executeCommandSwitch(CommandSwitch event) {
                if ("debug".equalsIgnoreCase(event.getSwitchCommand())) {

                    Launcher.LOG.info("Activated JD Debug Mode");
                } else if ("brdebug".equalsIgnoreCase(event.getSwitchCommand())) {
                    Browser.setGlobalVerbose(true);
                    Launcher.LOG.info("Activated Browser Debug Mode");
                } else if ("update".equalsIgnoreCase(event.getSwitchCommand())) {
                    JDInitFlags.REFRESH_CACHE = true;
                    Launcher.LOG.info("RefreshCache=true");
                }
            }
        });
        PARAMETERS.parse(null);
        checkSessionInstallLog();
        org.jdownloader.controlling.JDRestartController.getInstance().setStartArguments(PARAMETERS.getRawArguments());
        boolean jared = Application.isJared(Launcher.class);
        String revision = JDUtilities.getRevision();
        if (!jared) {
            /* always enable debug and cache refresh in developer version */
            Launcher.LOG.info("Not Jared Version(" + revision + "): RefreshCache=true");
            JDInitFlags.REFRESH_CACHE = true;
        } else {
            Launcher.LOG.info("Jared Version(" + revision + ")");
        }

        Launcher.preInitChecks();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-branch")) {
                if (args[i + 1].equalsIgnoreCase("reset")) {
                    JDUpdater.getInstance().setForcedBranch(null);
                    Launcher.LOG.info("Switching back to default JDownloader branch");
                } else {
                    JDUpdater.getInstance().setForcedBranch(args[i + 1]);
                    Launcher.LOG.info("Switching to " + args[i + 1] + " JDownloader branch");
                }
                i++;
            } else if (args[i].equals("-prot")) {
                Launcher.LOG.finer(args[i] + " " + args[i + 1]);
                i++;
            } else if (args[i].equals("--help") || args[i].equals("-h")) {
                ParameterManager.showCmdHelp();
                System.exit(0);
            } else if (args[i].equals("--captcha") || args[i].equals("-c")) {

                if (args.length > i + 2) {
                    Launcher.LOG.setLevel(Level.OFF);
                    final String captchaValue = JAntiCaptcha.getCaptcha(args[i + 1], args[i + 2]);
                    System.out.println(captchaValue);
                    System.exit(0);
                } else {
                    System.out.println("Error: Please define filepath and JAC method");
                    System.out.println("Usage: java -jar JDownloader.jar --captcha /path/file.png example.com");
                    System.exit(0);
                }
            }
        }
        if (PARAMETERS.hasCommandSwitch("show") || PARAMETERS.hasCommandSwitch("s")) {
            Launcher.LOG.info("Show Captcha (JAC)");
            JACController.showDialog(false);
            System.exit(0);
        } else if (PARAMETERS.hasCommandSwitch("train") || PARAMETERS.hasCommandSwitch("t")) {
            Launcher.LOG.info("Train Captcha (JAC)");
            JACController.showDialog(true);
            System.exit(0);
        }
        try {
            Launcher.SINGLE_INSTANCE_CONTROLLER = new SingleAppInstance("JD", JDUtilities.getJDHomeDirectoryFromEnvironment());
            Launcher.SINGLE_INSTANCE_CONTROLLER.setInstanceMessageListener(new InstanceMessageListener() {
                public void parseMessage(final String[] args) {
                    ParameterManager.processParameters(args);
                }
            });
            Launcher.SINGLE_INSTANCE_CONTROLLER.start();
            Launcher.instanceStarted = true;
        } catch (final AnotherInstanceRunningException e) {
            Launcher.LOG.info("existing jD instance found!");
            Launcher.instanceStarted = false;
        } catch (final Exception e) {
            Launcher.LOG.log(e);
            Launcher.LOG.severe("Instance Handling not possible!");
            Launcher.instanceStarted = true;
        }
        if (Launcher.instanceStarted) {
            Launcher.start(args);
        } else if (PARAMETERS.hasCommandSwitch("n")) {
            Launcher.LOG.severe("Forced to start new instance!");
            Launcher.start(args);
        } else {
            if (args.length > 0) {
                Launcher.LOG.info("Send parameters to existing jD instance and exit");
                Launcher.SINGLE_INSTANCE_CONTROLLER.sendToRunningInstance(args);
            } else {
                Launcher.LOG.info("There is already a running jD instance");
                Launcher.SINGLE_INSTANCE_CONTROLLER.sendToRunningInstance(new String[] { "--focus" });
            }
            System.exit(0);
        }

    }

    private static void checkSessionInstallLog() {
        File logFile = null;
        try {
            InstallLogList tmpInstallLog = new InstallLogList();
            logFile = Application.getResource(org.appwork.update.standalone.Main.SESSION_INSTALL_LOG_LOG);
            if (logFile.exists()) {
                Launcher.LOG.info("Check SessionInstallLog");
                tmpInstallLog = JSonStorage.restoreFrom(logFile, tmpInstallLog);

                for (InstalledFile iFile : tmpInstallLog) {
                    if (iFile.getRelPath().endsWith(".class")) {
                        // Updated plugins
                        JDInitFlags.REFRESH_CACHE = true;
                        Launcher.LOG.info("RefreshCache=true");
                        break;
                    }
                    if (iFile.getRelPath().startsWith("extensions") && iFile.getRelPath().endsWith(".jar")) {
                        // Updated extensions
                        JDInitFlags.REFRESH_CACHE = true;
                        Launcher.LOG.info("RefreshCache=true");
                        break;
                    }
                    if (iFile.getRelPath().endsWith(".class.backup")) {
                        // Updated plugins
                        JDInitFlags.REFRESH_CACHE = true;
                        Launcher.LOG.info("RefreshCache=true");
                        break;
                    }
                    if (iFile.getRelPath().startsWith("extensions") && iFile.getRelPath().endsWith(".jar.backup")) {
                        // Updated extensions
                        JDInitFlags.REFRESH_CACHE = true;
                        Launcher.LOG.info("RefreshCache=true");
                        break;
                    }
                }
            }

        } catch (Throwable e) {
            // JUst to be sure
            Launcher.LOG.log(e);
        } finally {
            if (logFile != null) {
                logFile.renameTo(new File(logFile.getAbsolutePath() + "." + System.currentTimeMillis()));

            }
        }
    }

    private static void preInitChecks() {
        Launcher.javaCheck();
    }

    private static void start(final String args[]) {
        go();
        for (final String p : args) {
            Launcher.LOG.finest("Param: " + p);
        }
        ParameterManager.processParameters(args);
    }

    private static void go() {
        Launcher.LOG.info("Initialize JDownloader");
        try {
            Log.closeLogfile();
        } catch (final Throwable e) {
            Launcher.LOG.log(e);
        }
        try {
            for (Handler handler : Log.L.getHandlers()) {
                Log.L.removeHandler(handler);
            }
        } catch (final Throwable e) {
        }
        Log.L.setUseParentHandlers(true);
        Log.L.setLevel(Level.ALL);
        Log.L.addHandler(new Handler() {
            LogSource logger = LogController.getInstance().getLogger("OldLogL");

            @Override
            public void publish(LogRecord record) {
                logger.log(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });

        if (!PARAMETERS.hasCommandSwitch("console") && Application.isJared(Launcher.class)) {
            Launcher.LOG.info("Remove ConsoleHandler");
            LogController.getInstance().removeConsoleHandler();
        }
        /* these can be initiated without a gui */
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    CFG_GENERAL.BROWSER_COMMAND_LINE.getEventSender().addListener(new GenericConfigEventListener<String[]>() {

                        @Override
                        public void onConfigValidatorError(KeyHandler<String[]> keyHandler, String[] invalidValue, ValidationException validateException) {
                        }

                        @Override
                        public void onConfigValueModified(KeyHandler<String[]> keyHandler, String[] newValue) {
                            CrossSystem.setBrowserCommandLine(newValue);
                        }
                    });
                    CrossSystem.setBrowserCommandLine(CFG_GENERAL.BROWSER_COMMAND_LINE.getValue());
                    /* setup JSPermission */
                    try {
                        JSPermissionRestricter.init();
                    } catch (final Throwable e) {
                        Launcher.LOG.log(e);
                    }
                    /* set gloabel logger for browser */
                    Browser.setGlobalLogger(LogController.getInstance().getLogger("GlobalBrowser"));
                    /* init default global Timeouts */
                    Browser.setGlobalReadTimeout(JsonConfig.create(GeneralSettings.class).getHttpReadTimeout());
                    Browser.setGlobalConnectTimeout(JsonConfig.create(GeneralSettings.class).getHttpConnectTimeout());
                    /* init global proxy stuff */
                    Browser.setGlobalProxy(ProxyController.getInstance().getDefaultProxy());
                    /* add global proxy change listener */
                    ProxyController.getInstance().getEventSender().addListener(new DefaultEventListener<ProxyEvent<ProxyInfo>>() {

                        public void onEvent(ProxyEvent<ProxyInfo> event) {
                            if (event.getType().equals(ProxyEvent.Types.REFRESH)) {
                                HTTPProxy proxy = null;
                                if ((proxy = ProxyController.getInstance().getDefaultProxy()) != Browser._getGlobalProxy()) {
                                    Launcher.LOG.info("Set new DefaultProxy: " + proxy);
                                    Browser.setGlobalProxy(proxy);
                                }
                            }

                        }
                    });
                } catch (Throwable e) {
                    Launcher.LOG.log(e);
                    Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);

                    org.jdownloader.controlling.JDRestartController.getInstance().restartViaUpdater(false);
                }
            }
        };
        thread.start();
        final EDTHelper<Void> lafInit = new EDTHelper<Void>() {
            @Override
            public Void edtRun() {
                LookAndFeelController.getInstance().setUIManager();
                return null;
            }
        };
        lafInit.start();
        Locale.setDefault(Locale.ENGLISH);
        GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            ToolTipController.getInstance().setDelay(JsonConfig.create(GraphicalUserInterfaceSettings.class).getTooltipTimeout());
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Host Plugins");
                            HostPluginController.getInstance().ensureLoaded();
                            /* load links */
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init DownloadLinks");
                            DownloadController.getInstance().initDownloadLinks();
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Linkgrabber");
                            LinkCollector.getInstance().initLinkCollector();
                            /* start remote api */
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init RemoteAPI");
                            RemoteAPIController.getInstance();
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Extern INterface");
                            ExternInterface.getINSTANCE();
                            // GarbageController.getInstance();
                            /* load extensions */
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init Extensions");
                            ExtensionController.getInstance().init();
                            /* init clipboardMonitoring stuff */
                            if (org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED.isEnabled()) {
                                ClipboardMonitoring.getINSTANCE().startMonitoring();
                            }
                            org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

                                public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                                    if (Boolean.TRUE.equals(newValue) && ClipboardMonitoring.getINSTANCE().isMonitoring() == false) {
                                        ClipboardMonitoring.getINSTANCE().startMonitoring();
                                    } else {
                                        ClipboardMonitoring.getINSTANCE().stopMonitoring();
                                    }
                                }

                                public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
                                }
                            });
                            /* check for available updates */
                            // activate auto checker only if we are in jared
                            // mode
                            if ((JsonConfig.create(WebupdateSettings.class).isAutoUpdateCheckEnabled() && Application.isJared(Launcher.class)) || PARAMETERS.hasCommandSwitch("autoupdate")) {
                                JDUpdater.getInstance().startChecker();
                            }
                            /* start downloadwatchdog */
                            Thread.currentThread().setName("ExecuteWhenGuiReachedThread: Init DownloadWatchdog");
                            DownloadWatchDog.getInstance();
                            AutoDownloadStartOption doRestartRunninfDownloads = JsonConfig.create(GeneralSettings.class).getAutoStartDownloadOption();
                            boolean closedRunning = JsonConfig.create(GeneralSettings.class).isClosedWithRunningDownloads();
                            if (doRestartRunninfDownloads == AutoDownloadStartOption.ALWAYS || (closedRunning && doRestartRunninfDownloads == AutoDownloadStartOption.ONLY_IF_EXIT_WITH_RUNNING_DOWNLOADS)) {
                                IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                                    @Override
                                    protected Void run() throws RuntimeException {
                                        /*
                                         * we do this check inside IOEQ because initDownloadLinks also does its final init in IOEQ
                                         */
                                        List<DownloadLink> dlAvailable = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

                                            @Override
                                            public boolean isChildrenNodeFiltered(DownloadLink node) {
                                                return node.isEnabled() && node.getLinkStatus().hasStatus(LinkStatus.TODO);
                                            }

                                            @Override
                                            public int returnMaxResults() {
                                                return 1;
                                            }

                                        });
                                        if (dlAvailable.size() == 0) {
                                            /*
                                             * no downloadlinks available to autostart
                                             */
                                            return null;
                                        }
                                        new Thread("AutostartDialog") {
                                            @Override
                                            public void run() {
                                                if (!DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.IDLE_STATE)) {
                                                    // maybe downloads have been
                                                    // started by another
                                                    // instance
                                                    // or user input
                                                    return;
                                                }
                                                if (JsonConfig.create(GeneralSettings.class).isClosedWithRunningDownloads() && JsonConfig.create(GeneralSettings.class).isSilentRestart()) {

                                                    DownloadWatchDog.getInstance().startDownloads();
                                                } else {

                                                    if (JsonConfig.create(GeneralSettings.class).getAutoStartCountdownSeconds() > 0 && CFG_GENERAL.SHOW_COUNTDOWNON_AUTO_START_DOWNLOADS.isEnabled()) {
                                                        ConfirmDialog d = new ConfirmDialog(Dialog.LOGIC_COUNTDOWN, _JDT._.Main_run_autostart_(), _JDT._.Main_run_autostart_msg(), NewTheme.I().getIcon("start", 32), _JDT._.Mainstart_now(), null);
                                                        d.setCountdownTime(JsonConfig.create(GeneralSettings.class).getAutoStartCountdownSeconds());
                                                        try {
                                                            Dialog.getInstance().showDialog(d);
                                                            DownloadWatchDog.getInstance().startDownloads();
                                                        } catch (DialogNoAnswerException e) {
                                                            if (e.isCausedByTimeout()) {
                                                                DownloadWatchDog.getInstance().startDownloads();
                                                            }
                                                        }
                                                    } else {
                                                        DownloadWatchDog.getInstance().startDownloads();
                                                    }
                                                }
                                            }
                                        }.start();
                                        return null;
                                    }
                                });
                            }
                        } catch (Throwable e) {
                            Launcher.LOG.log(e);
                            Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);

                            org.jdownloader.controlling.JDRestartController.getInstance().restartViaUpdater(false);
                        }
                    }

                }.start();
            }

        });
        new EDTHelper<Void>() {
            @Override
            public Void edtRun() {
                /* init gui here */
                try {
                    lafInit.waitForEDT();
                    Launcher.LOG.info("InitGUI->" + (System.currentTimeMillis() - Launcher.startup));
                    JDGui.getInstance();

                    EDTEventQueue.initEventQueue();

                    Launcher.LOG.info("GUIDONE->" + (System.currentTimeMillis() - Launcher.startup));
                } catch (Throwable e) {
                    Launcher.LOG.log(e);
                    Dialog.getInstance().showExceptionDialog("Exception occured", "An unexpected error occured.\r\nJDownloader will try to fix this. If this happens again, please contact our support.", e);

                    org.jdownloader.controlling.JDRestartController.getInstance().restartViaUpdater(false);
                }
                return null;
            }
        }.waitForEDT();
        /* this stuff can happen outside edt */
        SwingGui.setInstance(JDGui.getInstance());
        UserIF.setInstance(SwingGui.getInstance());
        try {
            /* thread should be finished here */
            thread.join(10000);
        } catch (InterruptedException e) {
        }
        if (!JDGui.getInstance().getMainFrame().isVisible()) {
            ShutdownController.getInstance().requestShutdown(true);
            return;
        }
        Launcher.GUI_COMPLETE.setReached();
        Launcher.LOG.info("Initialisation finished");
        Launcher.INIT_COMPLETE.setReached();

        // init statsmanager
        StatsManager.I();
    }
}