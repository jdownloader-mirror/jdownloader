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

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

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

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class Main {

    public static int BOTHRESIZE = GridBagConstraints.BOTH;
    public static Insets INSETS = new Insets(5, 5, 5, 5);
    public static int NORESIZE = GridBagConstraints.NONE;
    public static int NORTHWEST = GridBagConstraints.NORTHWEST;
    public static int REL = GridBagConstraints.RELATIVE;
    public static int REM = GridBagConstraints.REMAINDER;

    /**
     * Genau wie add, aber mit den Standardwerten iPadX,iPadY=0
     * 
     * @param cont
     *            Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp
     *            Die Komponente, die hinzugefuegt werden soll
     * @param x
     *            X-Position innerhalb des GriBagLayouts
     * @param y
     *            Y-Position innerhalb des GriBagLayouts
     * @param width
     *            Anzahl der Spalten, ueber die sich diese Komponente erstreckt
     * @param height
     *            Anzahl der Reihen, ueber die sich diese Komponente erstreckt
     * @param weightX
     *            Verteilung von zur Verfuegung stehendem Platz in X-Richtung
     * @param weightY
     *            Verteilung von zur Verfuegung stehendem Platz in Y-Richtung
     * @param insets
     *            Abstände der Komponente
     * @param fill
     *            Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor
     *            Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp, int x, int y, int width, int height, int weightX, int weightY, Insets insets, int fill, int anchor) {
        if (cont == null) return;
        if (comp == null) return;
        Main.addToGridBag(cont, comp, x, y, width, height, weightX, weightY, insets, 0, 0, fill, anchor);
    }

    /**
     * Diese Klasse fuegt eine Komponente einem Container hinzu
     * 
     * @param cont
     *            Der Container, dem eine Komponente hinzugefuegt werden soll
     * @param comp
     *            Die Komponente, die hinzugefuegt werden soll
     * @param x
     *            X-Position innerhalb des GriBagLayouts
     * @param y
     *            Y-Position innerhalb des GriBagLayouts
     * @param width
     *            Anzahl der Spalten, ueber die sich diese Komponente erstreckt
     * @param height
     *            Anzahl der Reihen, ueber die sich diese Komponente erstreckt
     * @param weightX
     *            Verteilung von zur Verfuegung stehendem Platz in X-Richtung
     * @param weightY
     *            Verteilung von zur Verfuegung stehendem Platz in Y-Richtung
     * @param insets
     *            Abständer der Komponente
     * @param iPadX
     *            Leerraum zwischen einer GridBagZelle und deren Inhalt
     *            (X-Richtung)
     * @param iPadY
     *            Leerraum zwischen einer GridBagZelle und deren Inhalt
     *            (Y-Richtung)
     * @param fill
     *            Verteilung der Komponente innerhalb der zugewiesen Zelle/n
     * @param anchor
     *            Positionierung der Komponente innerhalb der zugewiesen Zelle/n
     */
    public static void addToGridBag(Container cont, Component comp, int x, int y, int width, int height, int weightX, int weightY, Insets insets, int iPadX, int iPadY, int fill, int anchor) {
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = x;
        cons.gridy = y;
        cons.gridwidth = width;
        cons.gridheight = height;
        cons.weightx = weightX;
        cons.weighty = weightY;
        cons.fill = fill;
        cons.anchor = anchor;
        if (insets != null) {
            cons.insets = insets;
        }
        cons.ipadx = iPadX;
        cons.ipady = iPadY;
        cont.add(comp, cons);
    }

    private static void log(StringBuilder log, String string) {
        log.append(string);
        System.out.println(string);

    }

    @SuppressWarnings("unchecked")
    public static void main(String args[]) {
        final StringBuilder log = new StringBuilder();
        boolean OSFilter = true;
        boolean IgnorePlugins = true;
        boolean clone = false;
        String clonePrefix = null;

        for (String p : args) {
            if (p.trim().equalsIgnoreCase("-noosfilter")) {
                OSFilter = false;
            } else if (p.trim().equalsIgnoreCase("-allplugins")) {
                IgnorePlugins = false;
            } else if (p.trim().equalsIgnoreCase("-full")) {
                IgnorePlugins = false;
                OSFilter = false;
            } else if (p.trim().equalsIgnoreCase("-clone")) {
                IgnorePlugins = false;
                OSFilter = false;
                clone = true;
            } else if (p.trim().equalsIgnoreCase("/nofilter")) {
                OSFilter = false;
            } else if (clone && clonePrefix == null) {
                clonePrefix = p.trim();
            }
        }

        if (clone && clonePrefix != null) {
            Main.log(log, "Starting...");
            for (int i = 0; i < args.length; i++) {
                Main.log(log, "Parameter " + i + " " + args[i] + " " + System.getProperty("line.separator"));
            }
            WebUpdater updater = new WebUpdater();
            updater.setOSFilter(OSFilter);
            updater.ignorePlugins(!SubConfiguration.getSubConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_DISABLE", false));
            updater.ignorePlugins(IgnorePlugins);
            updater.setprimaryUpdatePrefix(clonePrefix);
            updater.setsecondaryUpdatePrefix(clonePrefix);
            updater.setLogger(log);
            Main.trace("Start Webupdate");
            Vector<Vector<String>> files;
            try {
                files = updater.getAvailableFiles();
            } catch (Exception e) {
                Main.trace("Update failed");
                Main.log(log, "Update failed");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
                files = new Vector<Vector<String>>();
            }

            if (files != null) {
                updater.filterAvailableUpdates(files);
                updater.updateFiles(files);
            }
            Main.trace("End Webupdate with " + updater.getErrors() + " Errors");
            if (new File(WebUpdater.getJDDirectory(), "webcheck.tmp").exists()) {
                new File(WebUpdater.getJDDirectory(), "webcheck.tmp").delete();
            }
            System.exit(updater.getErrors());
        }
        UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
        SubConfiguration guiConfig = SubConfiguration.getSubConfig("WEBUPDATE");
        String paf = guiConfig.getStringProperty("PLAF", null);
        boolean plafisSet = false;

        log.append(SubConfiguration.getSubConfig("WEBUPDATE").getProperties() + "\r\n");
        System.out.println(SubConfiguration.getSubConfig("WEBUPDATE").getProperties() + "\r\n");
        System.out.println(SubConfiguration.getSubConfig("PACKAGEMANAGER").getProperties() + "\r\n");
        log.append(SubConfiguration.getSubConfig("PACKAGEMANAGER").getProperties() + "\r\n");
        /* Http-Proxy einstellen */
        if (SubConfiguration.getSubConfig("WEBUPDATE").getBooleanProperty("USE_PROXY", false)) {

            String host = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_HOST", "");
            String port = new Integer(SubConfiguration.getSubConfig("WEBUPDATE").getIntegerProperty("PROXY_PORT", 8080)).toString();
            String user = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_USER", "");
            String pass = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_PASS", "");

            System.setProperty("http.proxySet", "true");
            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", port);
            System.setProperty("http.proxyUserName", user);
            System.setProperty("http.proxyPassword", pass);

            Main.log(log, "http-proxy: enabled" + System.getProperty("line.separator"));
        } else {
            System.setProperty("http.proxySet", "false");
            System.setProperty("http.proxyHost", "");
            Main.log(log, "http-proxy: disabled" + System.getProperty("line.separator"));
        }
        /* Socks-Proxy einstellen */
        if (SubConfiguration.getSubConfig("WEBUPDATE").getBooleanProperty("USE_SOCKS", false)) {

            String user = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_USER_SOCKS", "");
            String pass = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("PROXY_PASS_SOCKS", "");
            String host = SubConfiguration.getSubConfig("WEBUPDATE").getStringProperty("SOCKS_HOST", "");
            String port = new Integer(SubConfiguration.getSubConfig("WEBUPDATE").getIntegerProperty("SOCKS_PORT", 1080)).toString();

            System.setProperty("socksProxySet", "true");
            System.setProperty("socksProxyHost", host);
            System.setProperty("socksProxyPort", port);
            System.setProperty("socksProxyUserName", user);
            System.setProperty("socksProxyPassword", pass);
            System.setProperty("socks.useProxy", "true");
            System.setProperty("socks.proxyHost", host);
            System.setProperty("socks.proxyPort", port);
            System.setProperty("socks.proxyUserName", user);
            System.setProperty("socks.proxyPassword", pass);

            Main.log(log, "socks-proxy: enabled" + System.getProperty("line.separator"));
        } else {
            System.setProperty("socksProxySet", "false");
            System.setProperty("socks.useProxy", "false");
            System.setProperty("socks.proxyHost", "");
            System.setProperty("socksProxyHost", "");
            Main.log(log, "socks-proxy: disabled" + System.getProperty("line.separator"));
        }

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
        File file = new File(WebUpdater.getJDDirectory(), "webupdater.jar");
        if (file.exists()) {
            file.deleteOnExit();
        }
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("JD Update");
        frame.setLayout(new GridBagLayout());
        final JProgressBar progresslist = new JProgressBar();
        progresslist.setMaximum(100);
        progresslist.setStringPainted(true);
        final JProgressBar progressload = new JProgressBar();
        progressload.setMaximum(100);
        progressload.setStringPainted(true);
        final JTextArea logWindow = new JTextArea(30, 120);
        JScrollPane scrollPane = new JScrollPane(logWindow);
        scrollPane.setAutoscrolls(true);
        logWindow.setEditable(false);
        logWindow.setAutoscrolls(true);

        Main.addToGridBag(frame, new JLabel("Webupdate is running..."), REL, REL, REM, 1, 0, 0, INSETS, NORESIZE, NORTHWEST);
        Main.addToGridBag(frame, new JLabel("List files: "), REL, REL, REL, 1, 0, 0, INSETS, NORESIZE, NORTHWEST);
        Main.addToGridBag(frame, progresslist, REL, REL, REM, 1, 1, 0, INSETS, BOTHRESIZE, NORTHWEST);
        Main.addToGridBag(frame, new JLabel("Download: "), REL, REL, REL, 1, 0, 0, INSETS, NORESIZE, NORTHWEST);
        Main.addToGridBag(frame, progressload, REL, REL, REM, 1, 1, 0, INSETS, BOTHRESIZE, NORTHWEST);
        Main.log(log, "Starting...");
        logWindow.setText(log.toString());
        Main.addToGridBag(frame, scrollPane, REL, REL, REM, 1, 1, 1, INSETS, BOTHRESIZE, NORTHWEST);

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

        for (int i = 0; i < args.length; i++) {
            Main.log(log, "Parameter " + i + " " + args[i] + " " + System.getProperty("line.separator"));
            logWindow.setText(log.toString());
        }
        new Thread() {
            public void run() {
                while (true) {
                    logWindow.setText(log.toString());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }.start();
        WebUpdater updater = new WebUpdater();
        updater.setOSFilter(OSFilter);
        updater.ignorePlugins(!SubConfiguration.getSubConfig("WEBUPDATE").getBooleanProperty("WEBUPDATE_DISABLE", false));
        updater.ignorePlugins(IgnorePlugins);
        String warnHash = updater.getLocalHash(new File(WebUpdater.getJDDirectory(), "updatewarnings.html"));

        updater.downloadBinary(new File(WebUpdater.getJDDirectory(), "updatewarnings.html").getAbsolutePath(), "http://service.jdownloader.org/messages/updatewarning.html", null);
        String hash2 = updater.getLocalHash(new File("updatewarnings.html"));
        if (hash2 != null && !hash2.equals(warnHash)) {
            String str;
            if (JOptionPane.showConfirmDialog(frame, str = utils.getLocalFile(new File("updatewarnings.html")), "UPDATE WARNINGS", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
                Main.log(log, "Abort due to warnings " + str);

                new File(WebUpdater.getJDDirectory(), "updatewarnings.html").delete();
                new File(WebUpdater.getJDDirectory(), "updatewarnings.html").deleteOnExit();
                if (new File(WebUpdater.getJDDirectory(), "webcheck.tmp").exists()) {
                    new File(WebUpdater.getJDDirectory(), "webcheck.tmp").delete();
                }
                Main.log(log, "Local: " + new File("").getAbsolutePath());
                Main.log(log, "Start java -jar -Xmx512m JDownloader.jar in " + WebUpdater.getJDDirectory().getAbsolutePath());

                Main.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar" }, WebUpdater.getJDDirectory().getAbsolutePath(), 0);

                logWindow.setText(log.toString());
                Main.writeLocalFile(new File(WebUpdater.getJDDirectory(), "updateLog.txt"), log.toString());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                System.exit(0);
                return;
            }
        }
        updater.setLogger(log);
        updater.setListProgress(progresslist);
        updater.setDownloadProgress(progressload);
        Main.trace("Start Webupdate");
        Vector<Vector<String>> files;
        try {
            files = updater.getAvailableFiles();
        } catch (Exception e) {
            Main.trace("Update failed");
            Main.log(log, "Update failed");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
            }
            files = new Vector<Vector<String>>();
        }

        if (files != null) {
            updater.filterAvailableUpdates(files);
            progresslist.setValue(100);
            updater.updateFiles(files);
        }

        SubConfiguration jdus = SubConfiguration.getSubConfig("JDU");
        ArrayList<PackageData> data = (ArrayList<PackageData>) jdus.getProperty("PACKAGEDATA", new ArrayList<PackageData>());

        for (PackageData pa : data) {
            if (!pa.isDownloaded()) continue;
            File zip = new File(pa.getStringProperty("LOCALPATH"));

            Main.log(log, "Install: " + zip + System.getProperty("line.separator") + System.getProperty("line.separator"));

            UnZip u = new UnZip(zip, WebUpdater.getJDDirectory());
            File[] efiles;
            try {
                efiles = u.extract();
                if (files != null) {

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

                    zip.delete();
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
        File afile[] = (new File(WebUpdater.getJDDirectory(), "packages")).listFiles();
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
        Main.trace(updater.getLogger().toString());
        Main.trace("End Webupdate");
        logWindow.setText(log.toString());
        Main.trace(new File(WebUpdater.getJDDirectory(), "updateLog.txt").getAbsoluteFile());

        if (new File(WebUpdater.getJDDirectory(), "webcheck.tmp").exists()) {
            new File(WebUpdater.getJDDirectory(), "webcheck.tmp").delete();
        }
        Main.log(log, "Local: " + WebUpdater.getJDDirectory().getAbsolutePath());

        Main.log(log, "Start java -jar -Xmx512m JDownloader.jar in " + WebUpdater.getJDDirectory().getAbsolutePath());
        Main.runCommand("java", new String[] { "-Xmx512m", "-jar", "JDownloader.jar" }, WebUpdater.getJDDirectory().getAbsolutePath(), 0);

        logWindow.setText(log.toString());
        Main.writeLocalFile(new File(WebUpdater.getJDDirectory(), "updateLog.txt"), log.toString());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        System.exit(0);
    }

    /**
     * Führt einen Externen befehl aus.
     * 
     * @param command
     * @param parameter
     * @param runIn
     * @param waitForReturn
     * @return null oder die rückgabe des befehls falls waitforreturn == true
     *         ist
     */
    public static String runCommand(String command, String[] parameter, String runIn, int waitForReturn) {

        if (parameter == null) {
            parameter = new String[] {};
        }
        String[] params = new String[parameter.length + 1];
        params[0] = command;
        System.arraycopy(parameter, 0, params, 1, parameter.length);
        Vector<String> tmp = new Vector<String>();
        String par = "";
        for (String element : params) {
            if (element != null && element.trim().length() > 0) {
                par += element + " ";
                tmp.add(element.trim());
            }
        }

        params = tmp.toArray(new String[] {});
        ProcessBuilder pb = new ProcessBuilder(params);

        if (runIn != null && runIn.length() > 0) {
            if (new File(runIn).exists()) {
                pb.directory(new File(runIn));
            } else {
                Main.trace("Working drectory " + runIn + " does not exist!");
            }
        }
        Process process;

        try {
            Main.trace("Start " + par + " in " + runIn + " wait " + waitForReturn);
            process = pb.start();
            if (waitForReturn > 0) {
                long t = System.currentTimeMillis();
                while (true) {
                    try {
                        process.exitValue();
                        break;
                    } catch (Exception e) {
                        if (System.currentTimeMillis() - t > waitForReturn * 1000) {
                            Main.trace(command + ": Prozess ist nach " + waitForReturn + " Sekunden nicht beendet worden. Breche ab.");
                            process.destroy();
                        }
                    }
                }
                Scanner s = new Scanner(process.getInputStream()).useDelimiter("\\Z");
                String ret = "";
                while (s.hasNext()) {
                    ret += s.next();
                }
                return ret;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            Main.trace("Error executing " + command + ": " + e.getLocalizedMessage());
            return null;
        }
    }

    public static void trace(Object arg) {
        try {
            System.out.println(arg.toString());
        } catch (Exception e) {
            System.out.println(arg);
        }
    }

    /**
     * Schreibt content in eine Lokale textdatei
     * 
     * @param file
     * @param content
     * @return true/False je nach Erfolg des Schreibvorgangs
     */
    public static boolean writeLocalFile(File file, String content) {
        try {
            if (file.isFile()) {
                if (!file.delete()) {

                return false; }
            }
            if (file.getParent() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            BufferedWriter f = new BufferedWriter(new FileWriter(file));
            f.write(content);
            f.close();
            return true;
        } catch (Exception e) {
            // e.printStackTrace();
            return false;
        }
    }

}
