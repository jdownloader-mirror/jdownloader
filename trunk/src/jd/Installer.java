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

package jd;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.skins.simple.Factory;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.components.BrowseFile;
import jd.gui.userio.dialog.AbstractDialog;
import jd.gui.userio.dialog.ContainerDialog;
import jd.http.Browser;
import jd.nutils.Executer;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.utils.JDFileReg;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

/**
 * Der Installer erscheint nur beim ersten mal Starten der Webstartversion und
 * beim neuinstallieren der webstartversion der User kann Basiceinstellungen
 * festlegen
 * 
 * @author JD-Team
 */
public class Installer {

    private static final long serialVersionUID = 8764525546298642601L;

    private boolean aborted = false;

    private String language;

    private String languageid;

    private boolean error;

    public Installer() {

        language = "us";
        try {
            /* determine real country id */
            Browser br = new Browser();
            br.setConnectTimeout(10000);
            br.setReadTimeout(10000);
            language = br.getPage("http://jdownloader.net:8081/advert/getLanguage.php");
            if (!br.getRequest().getHttpConnection().isOK()) language = null;
            if (language != null) {
                language = language.trim();
                SubConfiguration.getConfig(JDLocale.CONFIG).setProperty("DEFAULTLANGUAGE", language);
                SubConfiguration.getConfig(JDLocale.CONFIG).save();
            } else {
                language = "us";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        languageid = "english";
        if (language.equalsIgnoreCase("de")) {
            languageid = "german";
        } else if (language.equalsIgnoreCase("es")) {
            languageid = "Spanish";
        } else if (language.equalsIgnoreCase("ar")) {
            languageid = "Spanish";
        } else if (language.equalsIgnoreCase("it")) {
            languageid = "Italiano";
        } else if (language.equalsIgnoreCase("pl")) {
            languageid = "Polski";
        } else if (language.equalsIgnoreCase("fr")) {
            languageid = "French";
        } else if (language.equalsIgnoreCase("tr")) {
            languageid = "Turkish";
        } else if (language.equalsIgnoreCase("ru")) {
            languageid = "Russian";
        }
        SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, null);

        showConfig();

        if (JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY) == null) {
            JDLogger.getLogger().severe("downloaddir not set");
            this.aborted = true;
            return;
        }
        AbstractDialog.setDefaultDimension(new Dimension(550, 400));

        int answer = (Integer) new GuiRunnable<Object>() {

            private ContainerDialog dialog;

            @Override
            public Object runSave() {
                JPanel c = new JPanel(new MigLayout("ins 10,wrap 1", "[grow,fill]", "[][][grow,fill]"));

                JLabel lbl = new JLabel(JDLocale.L("installer.gui.message", "After Installation, JDownloader will update to the latest version."));

                if (OSDetector.isWindows()) {
                    JDUtilities.getResourceFile("downloads");

                }
                c.add(lbl, "pushx,growx,split 2");

                Font f = lbl.getFont();
                f = f.deriveFont(f.getStyle() ^ Font.BOLD);

                lbl.setFont(f);
                c.add(new JLabel(JDImage.getScaledImageIcon(JDImage.getImage("logo/jd_logo_54_54"), 32, 32)), "alignx right");
                c.add(new JSeparator(), "pushx,growx,gapbottom 5");

                c.add(lbl = new JLabel(JDLocale.L("installer.firefox.message", "Do you want to integrate JDownloader to Firefox?")), "growy,pushy");
                lbl.setVerticalAlignment(SwingConstants.TOP);
                lbl.setHorizontalAlignment(SwingConstants.LEFT);

                new ContainerDialog(UserIO.NO_COUNTDOWN, JDLocale.L("installer.firefox.title", "Install firefox integration?"), c, null, null) {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -7983868276841947499L;

                    protected void packed() {
                        dialog = this;
                        this.setIconImage(JDImage.getImage("logo/jd_logo_54_54"));
                        this.setSize(550, 400);
                    }

                    protected void setReturnValue(boolean b) {
                        super.setReturnValue(b);

                    }
                };

                return dialog.getReturnValue();
            }

        }.getReturnValue();

        if (JDFlags.hasAllFlags(answer, UserIO.RETURN_OK)) installFirefoxaddon();
        JDFileReg.registerFileExts();
        JDUtilities.getConfiguration().save();

        if (OSDetector.isWindows()) {
            String lng = SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty("DEFAULTLANGUAGE", "DE");
            if (lng.equalsIgnoreCase("de") || lng.equalsIgnoreCase("us")) {
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        new KikinDialog();
                        return null;
                    }

                }.waitForEDT();
            }
        }

        AbstractDialog.setDefaultDimension(null);
    }

    private void showConfig() {
        new GuiRunnable<Object>() {

            private ContainerDialog dialog;

            @Override
            public Object runSave() {
                JPanel p = getInstallerPanel();
                JPanel content = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]"));
                p.add(content);
                content.add(Factory.createHeader(JDLocale.L("gui.config.gui.language", "Language"), JDTheme.II("gui.splash.languages", 24, 24)), "growx,pushx");
                final JList list;
                content.add(new JScrollPane(list = new JList(new AbstractListModel() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -7645376943352687975L;
                    private ArrayList<String> ids;

                    private ArrayList<String> getIds() {
                        if (ids == null) {
                            ids = JDLocale.getLocaleIDs();
                        }

                        return ids;
                    }

                    public Object getElementAt(int index) {
                        return getIds().get(index);
                    }

                    public int getSize() {
                        // TODO Auto-generated method stub
                        return getIds().size();
                    }

                })), "growx,pushx,gapleft 40,gapright 10");
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                if (error) list.setEnabled(false);
                list.setSelectedValue(SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID, languageid), true);
                list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                    public void valueChanged(ListSelectionEvent e) {
                        String lng = list.getSelectedValue().toString();
                        SubConfiguration.getConfig(JDLocale.CONFIG).setProperty(JDLocale.LOCALE_ID, lng);
                        JDLocale.setLocale(SubConfiguration.getConfig(JDLocale.CONFIG).getStringProperty(JDLocale.LOCALE_ID, "english"));
                        SubConfiguration.getConfig(JDLocale.CONFIG).save();
                        dialog.dispose();
                        showConfig();
                    }

                });
                content.add(Factory.createHeader(JDLocale.L("gui.config.general.downloaddirectory", "Download directory"), JDTheme.II("gui.images.taskpanes.download", 24, 24)), " growx,pushx,gaptop 10");

                final BrowseFile br;
                content.add(br = new BrowseFile(), "growx,pushx,gapleft 40,gapright 10");
                br.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (error) br.setEnabled(false);
                content.add(new JSeparator(), "growx,pushx,gaptop 5");
                if (OSDetector.isMac()) {
                    br.setCurrentPath(new File(System.getProperty("user.home") + "/Downloads"));

                } else if (OSDetector.isWindows() && new File(System.getProperty("user.home") + "/Downloads").exists()) {
                    br.setCurrentPath(new File(System.getProperty("user.home") + "/Downloads"));
                } else if (OSDetector.isWindows() && new File(System.getProperty("user.home") + "/Download").exists()) {
                    br.setCurrentPath(new File(System.getProperty("user.home") + "/Download"));
                } else {
                    br.setCurrentPath(JDUtilities.getResourceFile("downloads"));
                }
                new ContainerDialog(UserIO.NO_COUNTDOWN, JDLocale.L("installer.gui.title", "JDownloader Installation"), p, null, null) {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = 4685519683324833575L;

                    protected void packed() {
                        dialog = this;
                        this.setIconImage(JDImage.getImage("logo/jd_logo_54_54"));
                        this.setSize(550, 400);
                        if (error) {
                            this.btnOK.setEnabled(false);
                        }
                    }

                    protected void setReturnValue(boolean b) {
                        super.setReturnValue(b);
                        if (b) {
                            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, br.getCurrentPath());

                        } else {
                            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, null);
                        }
                    }
                };
                return null;
            }

        }.waitForEDT();
    }

    public static void installFirefoxaddon() {
        String path = null;

        if (OSDetector.isWindows()) {

            if (new File("C:\\Program Files\\Mozilla Firefox\\firefox.exe").exists()) {

                path = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";
            } else if (new File("C:\\Programme\\Mozilla Firefox\\firefox.exe").exists()) {
                path = "C:\\Programme\\Mozilla Firefox\\firefox.exe";
            }
            if (path != null) {
                Executer exec = new Executer(path);
                exec.addParameters(new String[] { JDUtilities.getResourceFile("tools/jdownff.xpi").getAbsolutePath() });

                exec.setWaitTimeout(180);
                exec.start();
                String res = exec.getOutputStream() + " \r\n " + exec.getErrorStream();

                System.out.println(res);
            }
        } else if (OSDetector.isMac()) {

            if (new File("/Applications/Firefox.app").exists()) {
                path = "/Applications/Firefox.app " + JDUtilities.getResourceFile("tools/jdownff.xpi");

                Executer exec = new Executer("open");
                exec.addParameters(new String[] { path });

                exec.setWaitTimeout(180);
                exec.start();
            }

        } else if (OSDetector.isLinux()) {

            Executer exec = new Executer("firefox");
            exec.addParameters(new String[] { JDUtilities.getResourceFile("tools/jdownff.xpi").getAbsolutePath() });

            exec.setWaitTimeout(180);
            exec.start();

        }

    }

    public JPanel getInstallerPanel() {
        JPanel c = new JPanel(new MigLayout("ins 10,wrap 1", "[grow,fill]", "[][grow,fill]"));

        JLabel lbl = new JLabel(JDLocale.L("installer.gui.message", "After Installation, JDownloader will update to the latest version."));

        if (OSDetector.getOSID() == OSDetector.OS_WINDOWS_VISTA) {
            String dir = JDUtilities.getResourceFile("downloads").getAbsolutePath().substring(3).toLowerCase();

            if (dir.startsWith("programme\\") || dir.startsWith("program files\\")) {
                lbl.setText(JDLocale.LF("installer.vistaDir.warning", "Warning! JD is installed in %s. This causes errors.", JDUtilities.getResourceFile("downloads")));
                lbl.setForeground(Color.RED);
                lbl.setBackground(Color.RED);
                error = true;
            }

        }
        c.add(lbl, "pushx,growx,split 2");

        Font f = lbl.getFont();
        f = f.deriveFont(f.getStyle() ^ Font.BOLD);

        lbl.setFont(f);
        c.add(new JLabel(JDImage.getScaledImageIcon(JDImage.getImage("logo/jd_logo_54_54"), 32, 32)), "alignx right");
        // c.add(new JSeparator(), "pushx,growx,gapbottom 5");
        return c;
    }

    // public static void showConfigDialog(final JFrame parent, final
    // ConfigContainer configContainer, final boolean alwaysOnTop) {
    // // logger.info("ConfigDialog");
    // new GuiRunnable<Object>() {
    //
    // // @Override
    // public Object runSave() {
    //
    // ConfigEntriesPanel p = new ConfigEntriesPanel(configContainer);
    // JPanel panel = new JPanel(new BorderLayout());
    // JPanel con;
    // panel.add(con = new JPanel(new MigLayout("ins 10,wrap 1")),
    // BorderLayout.NORTH);
    // panel.add(p, BorderLayout.CENTER);
    // JLabel lbl;
    // con.add(lbl=new
    // JLabel("JDownloader Installation"),"pushx,growx,split 2");
    // Font f = lbl.getFont();
    // f=f.deriveFont(f.getStyle() ^ Font.BOLD);
    //
    // // bold
    // lbl.setFont(f);
    // con.add(new
    // JLabel(JDImage.getScaledImageIcon(JDImage.getImage("logo/jd_logo_54_54"),
    // 48, 48)),"alignx right");
    // con.add(new JSeparator(), "pushx,growx,gapbottom 15");
    // ConfigurationPopup pop = new ConfigurationPopup(parent, p, panel);
    // pop.setSize(550, 400);
    // pop.setModal(true);
    // pop.setAlwaysOnTop(alwaysOnTop);
    // pop.setLocation(Screen.getCenterOfComponent(parent, pop));
    // pop.setVisible(true);
    //
    // return null;
    // }
    //
    // }.waitForEDT();
    // }

    public boolean isAborted() {
        return aborted;
    }

}
