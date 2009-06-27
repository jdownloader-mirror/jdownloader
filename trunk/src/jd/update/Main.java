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

package jd.update;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;

import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.nutils.zip.UnZip;
import jd.utils.JDUtilities;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class Main {

    private static int BOTHRESIZE = GridBagConstraints.BOTH;
    private static Insets INSETS = new Insets(5, 5, 5, 5);
    private static int NORESIZE = GridBagConstraints.NONE;
    private static int NORTHWEST = GridBagConstraints.NORTHWEST;
    private static int REL = GridBagConstraints.RELATIVE;
    private static int REM = GridBagConstraints.REMAINDER;
    private static SubConfiguration guiConfig;
    private static StringBuilder log;
    private static JFrame frame;
    private static JTextArea logWindow;
    public static ArrayList<Server> clonePrefix = new ArrayList<Server>();
    public static boolean clone = false;
    private static JProgressBar progressload;

    private static void log(StringBuilder log, String string) {
        if(log!=null)log.append(string);
        System.out.println(string);

    }

    public static void main(String args[]) {
        try {
            log = new StringBuilder();

            boolean OSFilter = true;
            boolean loadAllPlugins = false;

            File cfg;
            if ((cfg = JDUtilities.getResourceFile("jdownloader.config")).exists()) {

                JDUtilities.getResourceFile("backup/").mkdirs();

                cfg.renameTo(JDUtilities.getResourceFile("backup/jdownloader.config.outdated"));

            }

            for (int i = 0; i < args.length; i++) {
                String p = args[i];
                if (p.trim().equalsIgnoreCase("-noosfilter")) {
                    OSFilter = false;
                } else if (p.trim().equalsIgnoreCase("-allplugins")) {
                    loadAllPlugins = true;
                } else if (p.trim().equalsIgnoreCase("-full")) {
                    loadAllPlugins = true;
                    OSFilter = false;
                } else if (p.trim().equalsIgnoreCase("-brdebug")) {
                    Browser.setVerbose(true);

                } else if (p.trim().equalsIgnoreCase("-branch")) {
                    String br = args[++i];
                    if (br.equalsIgnoreCase("reset")) br = null;
                    WebUpdater.getConfig("WEBUPDATE").setProperty("BRANCH", br);
                    WebUpdater.getConfig("WEBUPDATE").save();
                    System.out.println("Switched branch: " + br);
                } else if (p.trim().equalsIgnoreCase("-clone")) {
                    loadAllPlugins = true;
                    OSFilter = false;
                    clone = true;
                } else if (clone && clonePrefix.size() == 0) {
                    clonePrefix.add(new Server(100, p.trim()));
                }
            }

            Browser.init();

            guiConfig = WebUpdater.getConfig("WEBUPDATE");

            log.append("Update JDownloader  at " + JDUtilities.getResourceFile(".") + "\r\n");
            log.append(WebUpdater.getConfig("WEBUPDATE").getProperties() + "\r\n");
            System.out.println(WebUpdater.getConfig("WEBUPDATE").getProperties() + "\r\n");
            System.out.println(WebUpdater.getConfig("PACKAGEMANAGER").getProperties() + "\r\n");
            log.append(WebUpdater.getConfig("PACKAGEMANAGER").getProperties() + "\r\n");

            if (!clone) initGUI();

            for (int i = 0; i < args.length; i++) {
                Main.log(log, "Parameter " + i + " " + args[i] + " " + System.getProperty("line.separator"));
                if (!clone) logWindow.setText(log.toString());
            }
            WebUpdater updater = new WebUpdater();
            updater.setOSFilter(OSFilter);
            Main.log(log, "Current Date:" + new Date() + "\r\n");
            if (!clone) checkBackup();
            updater.ignorePlugins(!WebUpdater.getConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_DISABLE", false));
            if (loadAllPlugins) updater.ignorePlugins(false);
            if (!clone) checkUpdateMessage();
            updater.setLogger(log);

            updater.setDownloadProgress(progressload);
            Main.trace("Start Webupdate");
            ArrayList<FileUpdate> files;
            try {
                files = updater.getAvailableFiles();
            } catch (Exception e) {
                Main.trace("Update failed");
                Main.log(log, "Update failed");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
                files = new ArrayList<FileUpdate>();
            }

            if (files != null) {

                updater.filterAvailableUpdates(files);

                JDUpdateUtils.backupDataBase();
                updater.updateFiles(files, null);
            }

            if (!clone) installAddons(JDUtilities.getResourceFile("."));
            Main.trace(updater.getLogger().toString());
            Main.trace("End Webupdate");

            if (!clone) logWindow.setText(log.toString());
            Main.trace(JDUtilities.getResourceFile("updateLog.txt").getAbsoluteFile());

            if (JDUtilities.getResourceFile("webcheck.tmp").exists()) {
                JDUtilities.getResourceFile("webcheck.tmp").delete();
            }
            Main.log(log, "Local: " + JDUtilities.getResourceFile(".").getAbsolutePath());

            Main.log(log, "Start java -jar -Xmx512m JDownloader.jar in " + JDUtilities.getResourceFile(".").getAbsolutePath());
            JDUtilities.getDatabaseConnector().shutdownDatabase();
            if (!clone) JDUtilities.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar", "-rfu" }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0);

            if (!clone) logWindow.setText(log.toString());
            JDIO.writeLocalFile(JDUtilities.getResourceFile("updateLog.txt"), log.toString());
            Main.log(log, "Errors: " + updater.getErrors());
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();

            Main.log(log, "ERROR " + e.getLocalizedMessage());

        }
    }

    public static void installAddons(File root) {
        SubConfiguration jdus = WebUpdater.getConfig("JDU");
        ArrayList<PackageData> data = jdus.getGenericProperty("PACKAGEDATA", new ArrayList<PackageData>());

        for (PackageData pa : data) {
            if (!pa.isDownloaded()) continue;
            File zip = new File(pa.getStringProperty("LOCALPATH"));

            Main.log(log, "Install: " + zip + System.getProperty("line.separator") + System.getProperty("line.separator"));

            UnZip u = new UnZip(zip, root);
            File[] efiles;
            try {
                efiles = u.extract();
                if (efiles != null) {

                    for (File element : efiles) {
                        Main.log(log, "       extracted: " + element + System.getProperty("line.separator"));
                        if (element.getAbsolutePath().endsWith("readme.html")) {
                            pa.setProperty("README", element.getAbsolutePath());

                        }
                    }
                    pa.setInstalled(true);
                    pa.setUpdating(false);
                    pa.setDownloaded(false);
                    pa.setInstalledVersion(Integer.parseInt(pa.getStringProperty("version")));

                    Main.log(log, "Installation successfull: " + zip + System.getProperty("line.separator"));

                    System.out.println("Delete " + zip.delete());
                    zip.deleteOnExit();

                }
            } catch (Exception e) {

                e.printStackTrace();

                StackTraceElement[] trace = e.getStackTrace();
                for (int i = 0; i < trace.length; i++)
                    Main.log(log, "\tat " + trace[i] + "\r\n");

                zip.delete();
                zip.deleteOnExit();

                pa.setInstalled(true);
                pa.setUpdating(false);
                pa.setDownloaded(false);

            }

        }
        jdus.save();
        File afile[] = (JDUtilities.getResourceFile("packages")).listFiles();
        if (afile != null) {
            for (int l = 0; l < afile.length; l++) {
                File jdu = afile[l];
                if (jdu.getName().toLowerCase().endsWith("jdu")) {
                    jdu.delete();
                    jdu.deleteOnExit();
                    log(log, (new StringBuilder("delete: ")).append(jdu).toString());
                }
            }
        }

    }

    private static void checkUpdateMessage() throws IOException {
        try {
            if (JDUtilities.getResourceFile("updateLog.txt").exists()) {
                String warnHash = JDHash.getMD5(JDUtilities.getResourceFile("updatewarnings.html"));

                Browser.download(JDUtilities.getResourceFile("updatewarnings.html"), "http://update1.jdownloader.org/messages/updatewarning.html");
                String hash2 = JDHash.getMD5(JDUtilities.getResourceFile("updatewarnings.html"));
                if (hash2 != null && !hash2.equals(warnHash)) {
                    String str;
                    str = JDIO.getLocalFile(new File("updatewarnings.html"));
                    if (str.trim().length() > 0) {
                        if (JOptionPane.showConfirmDialog(frame, str, "UPDATE WARNINGS", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
                            Main.log(log, "Abort due to warnings " + str);

                            JDUtilities.getResourceFile("updatewarnings.html").delete();
                            JDUtilities.getResourceFile("updatewarnings.html").deleteOnExit();
                            if (JDUtilities.getResourceFile("webcheck.tmp").exists()) {
                                JDUtilities.getResourceFile("webcheck.tmp").delete();
                            }
                            Main.log(log, "Local: " + new File("").getAbsolutePath());
                            Main.log(log, "Start java -jar -Xmx512m JDownloader.jar in " + JDUtilities.getResourceFile(".").getAbsolutePath());

                            JDUtilities.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar", "-rfu" }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0);

                            logWindow.setText(log.toString());
                            JDIO.writeLocalFile(JDUtilities.getResourceFile("updateLog.txt"), log.toString());
                            System.exit(0);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void checkBackup() {
        if (JDUtilities.getResourceFile("updateLog.txt").exists()) {
            if (!JDUtilities.getResourceFile("/backup/").exists()) {
                JDUtilities.getResourceFile("/backup/").mkdirs();
                /*
                 * Ordner wurde doch gerade angelegt, warum dann Fehlermeldung
                 * dass er nicht angelegt werden konnte? - Greeny
                 */
                Main.log(log, "Not found: " + (JDUtilities.getResourceFile("/backup/").getAbsolutePath()) + "\r\n");
                UserIO.getInstance().requestMessageDialog("JDownloader could not create a backup. Please make sure that\r\n " + JDUtilities.getResourceFile("/backup/").getAbsolutePath() + " exists and is writable before starting the update");
                JDUtilities.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar", "-rfu" }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0);
                System.exit(0);
                return;
            }

            File lastBackup = JDUtilities.getResourceFile("/backup/links.linkbackup");

            Main.log(log, "Backup found. date:" + new Date(lastBackup.lastModified()) + "\r\n");
            if (!lastBackup.exists() || (System.currentTimeMillis() - lastBackup.lastModified()) > 5 * 60 * 1000) {

                JDUtilities.getResourceFile("/backup/").mkdirs();
                String msg = "";

                if (!lastBackup.exists()) {

                    msg = "Do you want to continue without a backup? Your queue may get lost.\r\nLatest backup found: NONE!\r\nNote: You can ignore this message if this is a fresh JD-Installation and your linklist is empty anyway";
                } else {
                    msg = "Do you want to continue without a backup? Your queue may get lost.\r\nLatest backup found: " + new Date(lastBackup.lastModified()) + "\r\nin " + lastBackup.getAbsolutePath();
                }

                if (JOptionPane.showConfirmDialog(frame, msg, "There is no backup of your current Downloadqueue", JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
                    JDUtilities.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar", "-rfu" }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0);
                    System.exit(0);
                    return;
                }

            }
        }

    }

    private static void initGUI() {
        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
        String paf = guiConfig.getStringProperty("PLAF", null);
        boolean plafisSet = false;

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

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("JD Update");
        frame.setLayout(new GridBagLayout());

        progressload = new JProgressBar();
        progressload.setMaximum(100);
        progressload.setStringPainted(true);
        logWindow = new JTextArea(30, 120);
        JScrollPane scrollPane = new JScrollPane(logWindow);
        scrollPane.setAutoscrolls(true);
        logWindow.setEditable(false);
        logWindow.setAutoscrolls(true);

        JDUtilities.addToGridBag(frame, new JLabel("Webupdate is running..."), REL, REL, REM, 1, 0, 0, INSETS, NORESIZE, NORTHWEST);

        JDUtilities.addToGridBag(frame, new JLabel("Download: "), REL, REL, REL, 1, 0, 0, INSETS, NORESIZE, NORTHWEST);
        JDUtilities.addToGridBag(frame, progressload, REL, REL, REM, 1, 1, 0, INSETS, BOTHRESIZE, NORTHWEST);
        Main.log(log, "Starting...");
        logWindow.setText(log.toString());
        JDUtilities.addToGridBag(frame, scrollPane, REL, REL, REM, 1, 1, 1, INSETS, BOTHRESIZE, NORTHWEST);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        int n = 5;
        ((JComponent) frame.getContentPane()).setBorder(new EmptyBorder(n, n, n, n));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread() {
            public void run() {
                while (true) {
                    logWindow.setText(log.toString());
                    logWindow.setCaretPosition(logWindow.getText().length());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }.start();

    }

    public static void trace(Object arg) {
        try {
            System.out.println(arg.toString());
        } catch (Exception e) {
            System.out.println(arg);
        }
    }

}
