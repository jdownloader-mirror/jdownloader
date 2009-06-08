//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSE the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://gnu.org/licenses/>.

package jd.plugins.optional.jdunrar;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.filechooser.FileFilter;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.PasswordListController;
import jd.controlling.ProgressController;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.nutils.Executer;
import jd.nutils.Formatter;
import jd.nutils.JDHash;
import jd.nutils.OSDetector;
import jd.nutils.jobber.Jobber;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.plugins.PluginProgress;
import jd.utils.JDHexUtils;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDUnrar extends PluginOptional implements ControlListener, UnrarListener {

    private static final String DUMMY_HOSTER = "dum.my";

    public static int getAddonInterfaceVersion() {
        return 3;
    }

    /**
     * Wird als reihe für anstehende extracthjobs verwendet
     */
    private Jobber queue;
    private ConfigContainer passwordConfig;
    static String CODEPAGE = OSDetector.isWindows() ? "ISO-8859-1" : "UTF-8";

    // private ConfigEntry pwField;

    public JDUnrar(PluginWrapper wrapper) {
        super(wrapper);
        this.queue = new Jobber(1);
        // this.waitQueue = (ArrayList<DownloadLink>)
        // this.getPluginConfig().getProperty
        // (JDUnrarConstants.CONFIG_KEY_WAITLIST, new
        // ArrayList<DownloadLink>());
        checkUnrarCommand();

        initConfig();
    }

    /**
     * das controllevent fängt heruntergeladene file ab und wertet sie aus
     */
    @SuppressWarnings("unchecked")
    // @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);
        DownloadLink link;
        switch (event.getID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (this.getPluginConfig().getBooleanProperty("ACTIVATED", true)) {
                if (!(event.getSource() instanceof PluginForHost)) { return; }
                link = ((SingleDownloadController) event.getParameter()).getDownloadLink();
                link = findStartLink(link);
                if (link == null) return;
                if (link.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    if (link.getFilePackage().isExtractAfterDownload() || link.getFilePackage() == FilePackage.getDefaultFilePackage()) {

                        if (isArchiveComplete(link)) {
                            this.addToQueue(link);
                        }

                        // else {
                        // this.addToWaitQueue(link);
                        // }
                    }

                }
            }
            break;
        case ControlEvent.CONTROL_ON_FILEOUTPUT:
            if (this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_DEEP_EXTRACT, true)) {
                try {
                    File[] list = (File[]) event.getParameter();

                    for (File archiveStartFile : list) {
                        if (getArchivePartType(archiveStartFile) == JDUnrarConstants.NO_RAR_ARCHIVE || getArchivePartType(archiveStartFile) == JDUnrarConstants.NO_START_PART) continue;
                        link = JDUtilities.getController().getDownloadLinkByFileOutput(archiveStartFile, LinkStatus.FINISHED);

                        if (link == null) {
                            link = new DownloadLink(null, archiveStartFile.getName(), DUMMY_HOSTER, "", true);
                            link.setDownloadSize(archiveStartFile.length());
                            FilePackage fp = FilePackage.getInstance();
                            fp.setDownloadDirectory(archiveStartFile.getParent());
                            link.setFilePackage(fp);
                        }
                        link = this.findStartLink(link);
                        if (link == null) {
                            continue;
                        }
                        final DownloadLink finalLink = link;
                        System.out.print("queued to extract: " + archiveStartFile);
                        new Thread() {
                            public void run() {
                                addToQueue(finalLink);
                            }
                        }.start();

                    }

                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }
            break;

        case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:
            ArrayList<MenuItem> items = (ArrayList<MenuItem>) event.getParameter();
            MenuItem m;
            MenuItem container = new MenuItem(MenuItem.CONTAINER, JDLocale.L("plugins.optional.jdunrar.linkmenu.container", "JDUnrar"), 0);
            items.add(container);
            if (event.getSource() instanceof DownloadLink) {

                link = (DownloadLink) event.getSource();

                container.addMenuItem(m = new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.jdunrar.linkmenu.extract", "Extract"), 1000).setActionListener(this));
                m.setEnabled(false);
                boolean isLocalyAvailable = (new File(link.getFileOutput()).exists() || new File(link.getStringProperty(DownloadLink.STATIC_OUTPUTFILE, link.getFileOutput())).exists());
                if (isLocalyAvailable && link.getName().matches(".*rar$")) m.setEnabled(true);
                m.setProperty("LINK", link);
                container.addMenuItem(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.optional.jdunrar.linkmenu.autoextract", "Autoextract"), 1005).setActionListener(this));
                m.setSelected(link.getFilePackage().isExtractAfterDownload());
                m.setProperty("LINK", link);
                container.addMenuItem(m = new MenuItem(MenuItem.SEPARATOR));
                container.addMenuItem(m = new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.jdunrar.linkmenu.setextract", "Set Extract to..."), 1003).setActionListener(this));

                m.setProperty("LINK", link);
                File dir = this.getExtractToPath(link);
                while (dir != null && !dir.exists()) {
                    if (dir.getParentFile() == null) break;
                    dir = dir.getParentFile();
                }
                container.addMenuItem(m = new MenuItem(MenuItem.NORMAL, JDLocale.LF("plugins.optional.jdunrar.linkmenu.openextract2", "Open directory (%s)", dir.getAbsolutePath()), 1002).setActionListener(this));
                m.setEnabled(dir != null);
                link.setProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH + "2", dir.getAbsolutePath());
                m.setProperty("LINK", link);

            } else {
                FilePackage fp = (FilePackage) event.getSource();
                container.addMenuItem(m = new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.jdunrar.linkmenu.package.extract", "Extract package"), 1001).setActionListener(this));
                m.setProperty("PACKAGE", fp);
                container.addMenuItem(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.optional.jdunrar.linkmenu.package.autoextract", "Autoextract"), 1006).setActionListener(this));
                m.setSelected(fp.isExtractAfterDownload());
                m.setProperty("PACKAGE", fp);
            }

            break;

        }

    }

    // /**
    // * prüft die Warteschlange ob nun archive komplett sind und entpackt
    // werden
    // * können.
    // *
    // */
    // private void checkWaitQueue() {
    // synchronized (waitQueue) {
    // for (int i = waitQueue.size() - 1; i >= 0; i--) {
    // if (archiveIsComplete(waitQueue.get(i))) {
    // this.addToQueue(waitQueue.remove(i));
    // this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_WAITLIST,
    // queue);
    // }
    // }
    // }
    //
    // }

    // /**
    // * Fügt downloadlinks, bei denen der startart zwar schon geladen ist, aber
    // * die folgeparts noch nicht zu einer wartequeue
    // *
    // * @param link
    // */
    // private void addToWaitQueue(DownloadLink link) {
    // synchronized (waitQueue) {
    // waitQueue.add(link);
    // this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_WAITLIST,
    // waitQueue);
    // this.getPluginConfig();
    // }
    // }

    /**
     * Prüft im zugehörigem Filepackage, ob noch downloadlinks vom archiv
     * ungeladen sind.
     * 
     * @param link
     * @return
     */
    private boolean isArchiveComplete(DownloadLink link) {
        String pattern = link.getFileOutput().replaceAll("\\.part[0-9]+.rar$", "");
        pattern = pattern.replaceAll("\\.rar$", "");
        pattern = pattern.replaceAll("\\.r\\d+$", "");
        pattern = "^" + Regex.escape(pattern) + ".*";
        ArrayList<DownloadLink> matches = JDUtilities.getController().getDownloadLinksByPathPattern(pattern);
        for (DownloadLink l : matches) {
            if (!new File(l.getFileOutput()).exists()) return false;
            if (!l.getLinkStatus().hasStatus(LinkStatus.FINISHED) && l.isEnabled()) return false;
        }
        return true;
    }

    /**
     * prüft um welchen archivtyp es sich handelt. Es wird
     * JDUnrarConstants.MULTIPART_START_PART
     * JDUnrarConstants.SINGLE_PART_ARCHIVE JDUnrarConstants.NO_RAR_ARCHIVE
     * JDUnrarConstants.NO_START_PART
     * 
     * @param link
     * @return
     */
    private int getArchivePartType(DownloadLink link) {
        return getArchivePartType(new File(link.getFileOutput()));
    }

    private int getArchivePartType(File file) {
        if (file.getName().matches(".*part\\d+.rar$")) return JDUnrarConstants.MULTIPART_START_PART;
        if (file.getName().matches(".*.rar$")) {
            String filename = new Regex(file, "(.*)\\.rar$").getMatch(0);
            if ((new File(filename + ".r0")).exists()) {
                return JDUnrarConstants.MULTIPART_START_PART_V2;
            } else if ((new File(filename + ".r00")).exists()) {
                return JDUnrarConstants.MULTIPART_START_PART_V2;
            } else if ((new File(filename + ".r000")).exists()) {
                return JDUnrarConstants.MULTIPART_START_PART_V2;
            } else if ((new File(filename + ".r0000")).exists()) { return JDUnrarConstants.MULTIPART_START_PART_V2; }
        }
        if (file.getName().matches(".*rar$")) { return JDUnrarConstants.SINGLE_PART_ARCHIVE; }
        if (!file.getName().matches(".*rar$")) { return JDUnrarConstants.NO_RAR_ARCHIVE; }
        return JDUnrarConstants.NO_START_PART;
    }

    private DownloadLink findStartLink(DownloadLink link) {
        int type = getArchivePartType(link);
        switch (type) {
        case JDUnrarConstants.MULTIPART_START_PART:
        case JDUnrarConstants.MULTIPART_START_PART_V2:
            break;
        case JDUnrarConstants.SINGLE_PART_ARCHIVE:
            return link;
        case JDUnrarConstants.NO_RAR_ARCHIVE:
            return null;
        }
        File file = null;
        String filename = null;
        if (type == JDUnrarConstants.MULTIPART_START_PART) {
            filename = new Regex(link.getFileOutput(), "(.*)\\.part[0-9]+.rar$").getMatch(0);
            if ((file = new File(filename + ".part1.rar")).exists()) {
            } else if ((file = new File(filename + ".part01.rar")).exists()) {
            } else if ((file = new File(filename + ".part001.rar")).exists()) {
            } else if ((file = new File(filename + ".part0001.rar")).exists()) {
            } else if ((file = new File(filename + ".part000.rar")).exists()) {
            } else {
                return null;
            }
        } else if (type == JDUnrarConstants.MULTIPART_START_PART_V2) {
            filename = new Regex(link.getFileOutput(), "(.*)\\.r(\\d+|ar)$").getMatch(0);
            if (!(file = new File(filename + ".rar")).exists()) { return null; }
        }

        DownloadLink dlink = JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED);
        if (dlink == null) {
            System.out.print("DLink nicht gefunden.. erstelle Dummy");
            dlink = new DownloadLink(null, file.getName(), DUMMY_HOSTER, "", true);
            dlink.getLinkStatus().setStatus(link.getLinkStatus().getStatus());
            FilePackage fp = FilePackage.getInstance();
            fp.setDownloadDirectory(file.getParent());
            dlink.setFilePackage(fp);

        }
        return dlink;
    }

    private String getArchiveName(DownloadLink link) {
        String match = new Regex(new File(link.getFileOutput()).getName(), "(.*)\\.part[0]*[1].rar$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(.*)\\.part[0-9]+.rar$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(.*)\\.rar$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link.getFileOutput()).getName(), "(.*)\\.r\\d+$").getMatch(0);
        return match;
    }

    /**
     * Startet das abwarbeiten der extractqueue
     */
    private void addToQueue(final DownloadLink link) {
        if (getPluginConfig().getStringProperty(JDUnrarConstants.UNRAR_HASH, null) == null) {
            logger.warning("JDUnrar: no valid binary found!");
            return;
        }
        if (!new File(link.getFileOutput()).exists()) return;
        link.getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
        link.getLinkStatus().setErrorMessage(null);
        File dl = this.getExtractToPath(link);
        if (link.getHost().equals(DUMMY_HOSTER)) {
            ProgressController progress = new ProgressController(JDLocale.LF("plugins.optional.jdunrar.progress.extractfile", "Extract %s", link.getFileOutput()), 100);
            link.setProperty("PROGRESSCONTROLLER", progress);
        }
        UnrarWrapper wrapper = new UnrarWrapper(link);

        wrapper.addUnrarListener(this);
        wrapper.setExtractTo(dl);

        wrapper.setRemoveAfterExtract(this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_REMVE_AFTER_EXTRACT, false));

        wrapper.setOverwrite(this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_OVERWRITE, true));
        wrapper.setUnrarCommand(getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND));
        ArrayList<String> pwList = new ArrayList<String>();
        String[] linkPws = JDUtilities.passwordStringToArray(link.getFilePackage().getPassword());
        for (String pw : linkPws) {
            pwList.add(pw);
        }
        pwList.addAll(PasswordListController.getInstance().getPasswordList());
        // Fügt den Archivnamen und dan dateinamen ans ende der passwortliste
        pwList.add(this.getArchiveName(link));
        pwList.add(new File(link.getFileOutput()).getName());
        wrapper.setPasswordList(pwList.toArray(new String[] {}));

        queue.add(wrapper);
        queue.start();
        ArrayList<DownloadLink> list = this.getArchiveList(link);
        for (DownloadLink l : list) {
            if (l == null) continue;
            l.setProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH, dl.getAbsolutePath());
        }

    }

    /**
     * Bestimmt den Pfad in den das Archiv entpackt werden soll
     * 
     * @param link
     * @return
     */
    private File getExtractToPath(DownloadLink link) {

        if (link.getProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH) != null) return (File) link.getProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH);
        if (link.getHost().equals(DUMMY_HOSTER)) { return new File(link.getFileOutput()).getParentFile(); }
        String path;

        if (!getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_USE_EXTRACT_PATH, false)) {
            path = new File(link.getFileOutput()).getParent();
        } else {
            path = this.getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_UNRARPATH, JDUtilities.getConfiguration().getDefaultDownloadDirectory());
        }

        File ret = new File(path);
        if (!this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, false)) { return ret; }

        path = this.getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_SUBPATH, "%PACKAGENAME%");

        try {
            if (link.getFilePackage().getName() != null) {
                path = path.replace("%PACKAGENAME%", link.getFilePackage().getName());
            } else {
                path = path.replace("%PACKAGENAME%", "");
                logger.severe("link.getFilePackage().getName() ==null");
            }
            if (getArchiveName(link) != null) {
                path = path.replace("%ARCHIVENAME%", getArchiveName(link));
            } else {
                logger.severe("getArchiveName(link) ==null");
            }
            if (link.getHost() != null) {
                path = path.replace("%HOSTER%", link.getHost());

            } else {
                logger.severe("link.getFilePackage().getName() ==null");
            }

            String dif = new File(JDUtilities.getConfiguration().getDefaultDownloadDirectory()).getAbsolutePath().replace(new File(link.getFileOutput()).getParent(), "");
            if (new File(dif).isAbsolute()) {
                dif = "";
            }
            path = path.replace("%SUBFOLDER%", dif);

            path = path.replaceAll("[/]+", "\\\\");
            path = path.replaceAll("[\\\\]+", "\\\\");

            return new File(ret, path);
        } catch (Exception e) {
            JDLogger.exception(e);
            return ret;
        }
    }

    public String getIconKey() {
        return "gui.images.addons.unrar";
    }

    // @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;
        menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.optional.jdunrar.menu.toggle", "Activate"), 1).setActionListener(this));
        m.setSelected(this.getPluginConfig().getBooleanProperty("ACTIVATED", true));

        menu.add(new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.jdunrar.menu.extract.singlefils", "Extract archive(s)"), 21).setActionListener(this));

        return menu;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem) {
            actionPerformedOnMenuItem(e, (MenuItem) e.getSource());
        }
    }

    private void actionPerformedOnMenuItem(ActionEvent e, MenuItem source) {
        SubConfiguration cfg = this.getPluginConfig();
        DownloadLink link;
        switch (source.getActionID()) {
        case 1:
            cfg.setProperty("ACTIVATED", !cfg.getBooleanProperty("ACTIVATED", true));
            cfg.save();
            break;
        case 21:
            JDFileChooser fc = new JDFileChooser("_JDUNRAR_");
            fc.setMultiSelectionEnabled(true);
            FileFilter ff = new FileFilter() {

                public boolean accept(File pathname) {
                    if (pathname.getName().matches(".*part[0]*[1].rar$")) return true;
                    if (!pathname.getName().matches(".*part[0-9]+.rar$") && pathname.getName().matches(".*rar$")) { return true; }
                    if (pathname.isDirectory()) return true;
                    return false;
                }

                // @Override
                public String getDescription() {
                    return JDLocale.L("plugins.optional.jdunrar.filefilter", "Rar-Startvolumes");
                }

            };
            fc.setFileFilter(ff);
            if (fc.showOpenDialog(SimpleGUI.CURRENTGUI) == JDFileChooser.APPROVE_OPTION) {
                File[] list = fc.getSelectedFiles();
                if (list == null) return;
                for (File archiveStartFile : list) {
                    link = JDUtilities.getController().getDownloadLinkByFileOutput(archiveStartFile, LinkStatus.FINISHED);

                    if (link == null) {
                        link = new DownloadLink(null, archiveStartFile.getName(), DUMMY_HOSTER, "", true);
                        link.setDownloadSize(archiveStartFile.length());
                        FilePackage fp = FilePackage.getInstance();
                        fp.setDownloadDirectory(archiveStartFile.getParent());
                        link.setFilePackage(fp);
                    }
                    link = this.findStartLink(link);
                    if (link == null) {
                        continue;
                    }
                    final DownloadLink finalLink = link;
                    System.out.print("queued to extract: " + archiveStartFile);
                    new Thread() {
                        public void run() {

                            addToQueue(finalLink);
                        }
                    }.start();
                }
            }
            break;

        case 1000:

            link = this.findStartLink((DownloadLink) source.getProperty("LINK"));
            if (link == null) { return; }
            final DownloadLink finalLink = link;
            System.out.print("queued to extract: " + link);
            new Thread() {
                public void run() {
                    addToQueue(finalLink);
                }
            }.start();
            break;

        case 1001:

            FilePackage fp = (FilePackage) source.getProperty("PACKAGE");
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            for (DownloadLink l : fp.getDownloadLinkList()) {
                if (l.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    if (l.getName().matches(".*part[0]*[1].rar$") || (!l.getName().matches(".*part[0-9]+.rar$") && l.getName().matches(".*rar$"))) {
                        links.add(l);
                    }

                }

            }
            if (links.size() <= 0) return;

            for (DownloadLink link0 : links) {
                link = link0;

                link = this.findStartLink(link);
                if (link == null) {
                    continue;
                }
                final DownloadLink finalLink0 = link;
                System.out.print("queued to extract: " + link);
                new Thread() {
                    public void run() {
                        addToQueue(finalLink0);
                    }
                }.start();

            }
            break;

        case 1002:

            link = (DownloadLink) source.getProperty("LINK");
            if (link == null) { return; }
            String path = link.getStringProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH + "2");

            if (!new File(path).exists()) {
                JDUtilities.getGUI().showMessageDialog(JDLocale.LF("plugins.optional.jdunrar.messages", "The path %s does not exist.", path));
            } else {
                JDUtilities.openExplorer(new File(path));
            }

            break;

        case 1003:

            link = (DownloadLink) source.getProperty("LINK");
            ArrayList<DownloadLink> list = this.getArchiveList(link);
            fc = new JDFileChooser("_JDUNRAR_");
            fc.setMultiSelectionEnabled(false);
            fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
            ff = new FileFilter() {

                public boolean accept(File pathname) {
                    if (pathname.isDirectory()) return true;
                    return false;
                }

                // @Override
                public String getDescription() {
                    return JDLocale.L("plugins.optional.jdunrar.filefilter.extractto", "Extract Directory");
                }

            };
            fc.setFileFilter(ff);
            File extractto = this.getExtractToPath(link);
            while (extractto != null && !extractto.isDirectory())
                extractto = extractto.getParentFile();
            fc.setCurrentDirectory(extractto);
            if (fc.showOpenDialog(SimpleGUI.CURRENTGUI) == JDFileChooser.APPROVE_OPTION) {

                File dl = fc.getSelectedFile();
                if (dl == null) { return; }
                for (DownloadLink l : list) {
                    l.setProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH, dl);
                }

            }

            break;

        case 1005:

            link = (DownloadLink) source.getProperty("LINK");
            if (link == null) { return; }
            link.getFilePackage().setExtractAfterDownload(!link.getFilePackage().isExtractAfterDownload());

            break;

        case 1006:

            fp = (FilePackage) source.getProperty("PACKAGE");
            if (fp == null) { return; }
            fp.setExtractAfterDownload(!fp.isExtractAfterDownload());

            break;
        }

    }

    // @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.jdunrar.name", "JD-Unrar");
    }

    // @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public boolean initAddon() {
        JDUtilities.getController().addControlListener(this);
        return true;
    }

    public void initConfig() {
        SubConfiguration subConfig = getPluginConfig();
        ConfigEntry ce;
        ConfigEntry conditionEntry;
        String hash = this.getPluginConfig().getStringProperty(JDUnrarConstants.UNRAR_HASH, null);
        if (hash == null) {
            config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, JDLocale.L("gui.config.unrar.cmd", "UnRAR command")));
        }

        config.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_USE_EXTRACT_PATH, JDLocale.L("gui.config.unrar.use_extractto", "Use customized extract path")));
        conditionEntry.setDefaultValue(false);

        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, subConfig, JDUnrarConstants.CONFIG_KEY_UNRARPATH, JDLocale.L("gui.config.unrar.path", "Extract to")));
        ce.setDefaultValue(JDUtilities.getConfiguration().getDefaultDownloadDirectory());
        ce.setEnabledCondidtion(conditionEntry, "==", true);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_REMVE_AFTER_EXTRACT, JDLocale.L("gui.config.unrar.remove_after_extract", "Delete archives after suc. extraction?")));
        ce.setDefaultValue(false);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_OVERWRITE, JDLocale.L("gui.config.unrar.overwrite", "Overwrite existing files?")));
        ce.setDefaultValue(false);

        this.passwordConfig = new ConfigContainer(JDLocale.L("plugins.optional.jdunrar.config.passwordtab", "List of passwords"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, passwordConfig));

        passwordConfig.addEntry(new ConfigEntry(ConfigContainer.TYPE_UNRARPASSWORDS, SubConfiguration.getConfig(PasswordListController.PASSWORDCONTROLLER), "LIST", JDLocale.LF("plugins.optional.jdunrar.config.passwordlist2", "List of all passwords. Each line one password. Available passwords: %s", "")));

        ConfigContainer ext = new ConfigContainer(JDLocale.L("plugins.optional.jdunrar.config.advanced", "Advanced settings"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, ext));

        ext.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, JDLocale.L("gui.config.unrar.use_subpath", "Use subpath")));
        conditionEntry.setDefaultValue(false);

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, JDUnrarConstants.CONFIG_KEY_SUBPATH, JDLocale.L("gui.config.unrar.subpath", "Subpath")));
        ce.setDefaultValue("%PACKAGENAME%");
        ce.setEnabledCondidtion(conditionEntry, "==", true);
        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, JDUnrarConstants.CONFIG_KEY_SUBPATH_MINNUM, JDLocale.L("gui.config.unrar.subpath_minnum", "Only use subpath if archive contains more than x files"), 0, 600).setDefaultValue(0));
        // ce.setEnabledCondidtion(conditionEntry, "==", true);
        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_ASK_UNKNOWN_PASS, JDLocale.L("gui.config.unrar.ask_path", "Ask for unknown passwords?")));
        ce.setDefaultValue(true);
        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_DEEP_EXTRACT, JDLocale.L("gui.config.unrar.deep_extract", "Deep-Extraction")));
        ce.setDefaultValue(true);
        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_REMOVE_INFO_FILE, JDLocale.L("gui.config.unrar.remove_infofile", "Delete Infofile after extraction")));
        ce.setDefaultValue(false);
    }

    /**
     * Diese Funktion wird momentan nicht benötigt. sie sucht nach dem Richtigen
     * Encoding.
     * 
     * @return
     */
    @SuppressWarnings("unused")
    private String getCodepage() {
        Executer exec = new Executer(this.getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND));
        exec.addParameter("v");
        exec.addParameter("-v");
        exec.addParameter("-c-");
        exec.addParameter(JDUtilities.getResourceFile("plugins/jdunrar/aeoeue.rar").getAbsolutePath());
        exec.setWaitTimeout(-1);
        exec.start();
        exec.waitTimeout();
        byte[] b = exec.getInputStreamBuffer().getSub(280, 330);
        Iterator<Entry<String, Charset>> it = Charset.availableCharsets().entrySet().iterator();
        String found = null;
        System.out.println(JDHexUtils.getHexString(b));
        while (it.hasNext()) {
            Entry<String, Charset> n = it.next();
            try {
                if (new String(b, n.getKey()).contains("Øaeäoeöueü")) {
                    System.err.println(n.getKey() + " -->" + new String(b, n.getKey()));
                    if (found == null) found = n.getKey();
                } else {
                    // System.out.println(n.getKey()+" : "+new String(b,
                    // n.getKey()));
                }
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }

        exec = new Executer(this.getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND));
        exec.addParameter("v");
        exec.setCodepage(found);
        exec.addParameter("-v");
        exec.addParameter("-c-");
        exec.addParameter(JDUtilities.getResourceFile("plugins/jdunrar/aeoeue.rar").getAbsolutePath());
        exec.setWaitTimeout(-1);
        exec.start();
        exec.waitTimeout();
        System.err.println(exec.getInputStreamBuffer().toString());
        return found;
    }

    private void chmodUnrar(String path) {
        Executer exec = new Executer("chmod");
        exec.addParameter("+x");
        exec.addParameter(path);
        exec.setWaitTimeout(-1);
        exec.start();
        exec.waitTimeout();
    }

    /**
     * Überprüft den eingestellten UNrarbefehl und setzt ihn notfalls neu.
     */
    private void checkUnrarCommand() {
        String path = this.getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, null);
        String hash = this.getPluginConfig().getStringProperty(JDUnrarConstants.UNRAR_HASH, null);
        if (hash == null || hash.length() == 0) {
            path = null;
        } else {
            if (path != null && path.length() != 0) {
                String curhash = JDHash.getMD5(path);
                if (curhash.equalsIgnoreCase(hash)) return;
            }
            path = null;
            hash = null;
        }
        if (path == null || path.length() == 0) {
            if (OSDetector.isWindows()) {
                path = JDUtilities.getResourceFile("tools\\windows\\unrarw32\\unrar.exe").getAbsolutePath();
                this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(path));
                this.getPluginConfig().save();
                return;
            } else {
                if (OSDetector.isLinux()) {
                    path = JDUtilities.getResourceFile("tools/linux/unrar/unrar").getAbsolutePath();
                    chmodUnrar(path);
                    if (isUnrarCommandValid(path)) {
                        this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                        this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(path));
                        this.getPluginConfig().save();
                        return;
                    }
                }
                if (OSDetector.isMac()) {
                    path = JDUtilities.getResourceFile("tools/mac/unrar2/unrar").getAbsolutePath();
                    chmodUnrar(path);
                    if (isUnrarCommandValid(path)) {
                        this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                        this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(path));
                        this.getPluginConfig().save();
                        return;
                    }
                }
                if (isUnrarCommandValid("unrar")) {
                    path = "unrar";
                    this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                    this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(path));
                    this.getPluginConfig().save();
                    return;
                }
                if (isUnrarCommandValid("rar")) {
                    path = "rar";
                    this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                    this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(path));
                    this.getPluginConfig().save();
                    return;
                }
                try {
                    String[] charset = System.getenv("PATH").split(":");
                    for (String element : charset) {
                        File fi = new File(element, "unrar");
                        File fi2 = new File(element, "rar");
                        if (fi.isFile() && isUnrarCommandValid(fi.getAbsolutePath())) {
                            path = fi.getAbsolutePath();
                            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                            this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(path));
                            this.getPluginConfig().save();
                            return;
                        } else if (fi2.isFile() && isUnrarCommandValid(fi2.getAbsolutePath())) {
                            path = fi2.getAbsolutePath();
                            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                            this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(path));
                            this.getPluginConfig().save();
                            return;
                        }
                    }
                } catch (Throwable e) {
                }
                path = "please install unrar";
                this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, null);
                this.getPluginConfig().save();
            }
        }
    }

    /**
     * Prüft ob ein bestimmter Unrarbefehl gültig ist
     * 
     * @param path
     * @return
     */
    private boolean isUnrarCommandValid(String path) {
        return UnrarWrapper.isUnrarCommandValid(path);
    }

    // @Override
    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

    public void onUnrarEvent(int id, UnrarWrapper wrapper) {
        LinkStatus ls = wrapper.getDownloadLink().getLinkStatus();
        // Falls der link entfernt wird während dem entpacken
        if (wrapper.getDownloadLink().getFilePackage() == FilePackage.getDefaultFilePackage() && wrapper.getDownloadLink().getProperty("PROGRESSCONTROLLER") == null) {
            logger.warning("LINK GOT REMOVED_: " + wrapper.getDownloadLink());
            ProgressController progress = new ProgressController(JDLocale.LF("plugins.optional.jdunrar.progress.extractfile", "Extract %s", wrapper.getDownloadLink().getFileOutput()), 100);
            wrapper.getDownloadLink().setProperty("PROGRESSCONTROLLER", progress);
        }

        if (wrapper.getDownloadLink().getProperty("PROGRESSCONTROLLER") != null) {
            onUnrarDummyEvent(id, wrapper);
            return;
        }
        // int min;
        switch (id) {
        case JDUnrarConstants.INVALID_BINARY:
            logger.severe("Invalid unrar binary!");
            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, "please install unrar");
            this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, null);
            break;
        case JDUnrarConstants.WRAPPER_EXTRACTION_FAILED:

            ArrayList<DownloadLink> list = this.getArchiveList(wrapper.getDownloadLink());

            for (DownloadLink link : list) {

                if (link == null) continue;
                LinkStatus lls = link.getLinkStatus();

                if (wrapper.getException() != null) {
                    lls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    lls.setErrorMessage("Extract failed: " + wrapper.getException().getMessage());
                    link.requestGuiUpdate();
                } else {
                    lls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    lls.setErrorMessage("Extract failed");
                    link.requestGuiUpdate();
                }
            }
            this.onFinished(wrapper);

            break;
        case JDUnrarConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE:

            wrapper.getDownloadLink().requestGuiUpdate();

            if (this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_ASK_UNKNOWN_PASS, true)) {
                String pass = JDUtilities.getGUI().showCountdownUserInputDialog(JDLocale.LF("plugins.optional.jdunrar.askForPassword", "Password for %s?", wrapper.getDownloadLink().getName()), null);
                if (pass == null) {
                    ls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    ls.setStatusText(JDLocale.L("plugins.optional.jdunrar.status.extractfailedpass", "Extract failed (password)"));
                    this.onFinished(wrapper);
                    break;
                }
                wrapper.setPassword(pass);
            }

            break;

        case JDUnrarConstants.WRAPPER_CRACK_PASSWORD:

            // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
            // ": " + "Cracking password");
            wrapper.getDownloadLink().getLinkStatus().setStatusText(JDLocale.L("plugins.optional.jdunrar.status.crackingpass", "Cracking password"));
            wrapper.getDownloadLink().requestGuiUpdate();
            break;
        case JDUnrarConstants.WRAPPER_NEW_STATUS:
            // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
            // ": " + "New status " + wrapper.getStatus());
            break;
        case JDUnrarConstants.WRAPPER_START_OPEN_ARCHIVE:
            wrapper.getDownloadLink().getLinkStatus().setStatusText(JDLocale.L("plugins.optional.jdunrar.status.openingarchive", "Opening archive"));
            wrapper.getDownloadLink().requestGuiUpdate();
            // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
            // ": " + "Start opening archive");
            break;
        case JDUnrarConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS:
            assignRealDownloadDir(wrapper);
            break;
        case JDUnrarConstants.WRAPPER_PASSWORD_FOUND:
            // progress.get(wrapper).setColor(Color.GREEN);
            wrapper.getDownloadLink().getLinkStatus().setStatusText(JDLocale.L("plugins.optional.jdunrar.status.passfound", "Password found"));
            wrapper.getDownloadLink().requestGuiUpdate();
            wrapper.getDownloadLink().setPluginProgress(null);

            // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
            // ": " + "Password found " + wrapper.getPassword());
            break;

        case JDUnrarConstants.WRAPPER_PASSWORT_CRACKING:
            wrapper.getDownloadLink().getLinkStatus().setStatusText(JDLocale.L("plugins.optional.jdunrar.status.crackingpass", "Cracking password"));

            if (wrapper.getDownloadLink().getPluginProgress() == null) {
                wrapper.getDownloadLink().setPluginProgress(new PluginProgress(wrapper.getCrackProgress(), 100, Color.GREEN.darker()));
                //                
            } else {
                wrapper.getDownloadLink().getPluginProgress().setCurrent(wrapper.getCrackProgress());
                //               
            }
            wrapper.getDownloadLink().requestGuiUpdate();

            break;
        case JDUnrarConstants.WRAPPER_ON_PROGRESS:
            // progress.get(wrapper).setRange(wrapper.getTotalSize());
            // progress.get(wrapper).setStatus(wrapper.getExtractedSize());
            wrapper.getDownloadLink().getLinkStatus().setStatusText(JDLocale.L("plugins.optional.jdunrar.status.extracting", "Extracting"));

            if (wrapper.getDownloadLink().getPluginProgress() == null) {
                wrapper.getDownloadLink().setPluginProgress(new PluginProgress(wrapper.getExtractedSize(), wrapper.getTotalSize(), Color.YELLOW.darker()));
                //                
            } else {
                wrapper.getDownloadLink().getPluginProgress().setCurrent(wrapper.getExtractedSize());
                //               
            }
            wrapper.getDownloadLink().requestGuiUpdate();
            // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
            // ": " + "Progress: " +
            // JDUtilities.getPercent(wrapper.getExtractedSize(),
            // wrapper.getTotalSize()));
            break;
        case JDUnrarConstants.WRAPPER_START_EXTRACTION:
            // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
            // ": " + "Extraction started");
            break;
        case JDUnrarConstants.WRAPPER_STARTED:
            // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
            // ": " + "Started Unrarprocess");
            break;
        case JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC:
            // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
            // ": " + "CRC Failure");
            // progress.get(wrapper).setColor(Color.RED);
            list = this.getArchiveList(wrapper.getDownloadLink());
            list.add(null);
            DownloadLink crc = null;
            if (wrapper.getCurrentVolume() > 0) {
                crc = list.size() >= wrapper.getCurrentVolume() ? list.get(wrapper.getCurrentVolume() - 1) : null;
            }
            if (crc != null) {
                // crc.reset();
                crc.getLinkStatus().removeStatus(LinkStatus.FINISHED);
                crc.getLinkStatus().addStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
                crc.getLinkStatus().setValue(LinkStatus.VALUE_FAILED_HASH);
                crc.getLinkStatus().setErrorMessage(JDLocale.LF("plugins.optional.jdunrar.crcerrorin", "Extract: failed (CRC in %s)", crc.getName()));

                crc.requestGuiUpdate();

            } else {
                for (DownloadLink link : list) {
                    if (link == null) continue;
                    link.getLinkStatus().setErrorMessage(JDLocale.L("plugins.optional.jdunrar.error.extrfailedcrc", "Extract: failed (CRC in unknown file)"));
                    link.requestGuiUpdate();
                }
            }
            this.onFinished(wrapper);

            break;

        case JDUnrarConstants.WRAPPER_PROGRESS_SINGLE_FILE_FINISHED:
            // progress.get(wrapper).setColor(Color.YELLOW);

            // progress.get(wrapper).setColor(Color.GREEN);
            // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
            // ": " + "Progress. SingleFile finished: " +
            // wrapper.getCurrentFile());
            break;
        case JDUnrarConstants.WRAPPER_FINISHED_SUCCESSFULL:
            // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
            // ": " + "SUCCESSFULL");
            // progress.get(wrapper).setColor(Color.GREEN);

            list = this.getArchiveList(wrapper.getDownloadLink());

            // this.deepExtract(wrapper);
            File[] files = new File[wrapper.getFiles().size()];
            int i = 0;
            for (ArchivFile af : wrapper.getFiles()) {
                files[i++] = af.getFile();
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(wrapper, ControlEvent.CONTROL_ON_FILEOUTPUT, files));

            for (DownloadLink link : list) {
                if (link == null) continue;
                link.getLinkStatus().addStatus(LinkStatus.FINISHED);
                link.getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
                link.getLinkStatus().setStatusText(JDLocale.L("plugins.optional.jdunrar.status.extractok", "Extract OK"));
                link.requestGuiUpdate();
            }
            if (this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_REMOVE_INFO_FILE, false)) {
                File fileOutput = new File(wrapper.getDownloadLink().getFileOutput());
                File infoFiles = new File(fileOutput.getParentFile(), fileOutput.getName().replaceFirst("(?i)(\\.part[0-9]+\\.rar|\\.rar)$", "") + ".info");
                if (infoFiles.exists() && infoFiles.delete()) {
                    logger.info(infoFiles.getName() + " removed");
                }
            }
            this.onFinished(wrapper);

            break;

        default:
            // System.out.println("id ");
        }
    }

    private void assignRealDownloadDir(UnrarWrapper wrapper) {
        // progress.get(wrapper).setStatusText(wrapper.getFile().getName() +
        // ": " + "Archive opened successfull");
        PasswordListController.getInstance().addPassword(wrapper.getPassword());

        int min = this.getPluginConfig().getIntegerProperty(JDUnrarConstants.CONFIG_KEY_SUBPATH_MINNUM, 0);
        if (min > 0) {

            ArrayList<ArchivFile> files = wrapper.getFiles();
            int i = 0;
            // get filenum without directories
            for (ArchivFile af : files) {
                if (af.getSize() > 0) i++;
            }
            Boolean usesub = this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, false);
            if (min >= i) {
                // reset extractdirectory to default
                this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, false);
            } else {
                this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, true);
            }
            File dl = this.getExtractToPath(wrapper.getDownloadLink());
            wrapper.setExtractTo(dl);
            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, usesub);

            ArrayList<DownloadLink> linkList = this.getArchiveList(wrapper.getDownloadLink());
            for (DownloadLink l : linkList) {
                if (l == null) continue;
                l.setProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH, dl.getAbsolutePath());
            }

        }

    }

    /**
     * Als Dummy wird ein downloadlink bezeicnet, der nicht ind er downloadliste
     * war, sondern nur angelegt wurde um als container für ein externes archiv
     * zu dienen. Zur Fortschrittsanzeige wird ein progresscontroller verwendet
     * 
     * @param id
     * @param wrapper
     */
    private void onUnrarDummyEvent(int id, UnrarWrapper wrapper) {
        ProgressController pc = (ProgressController) wrapper.getDownloadLink().getProperty("PROGRESSCONTROLLER");
        // int min;
        switch (id) {
        case JDUnrarConstants.INVALID_BINARY:
            logger.severe("Invalid unrar binary!");
            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, "please install unrar");
            this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, null);
            break;
        case JDUnrarConstants.WRAPPER_EXTRACTION_FAILED:

            if (wrapper.getException() != null) {

                pc.setStatusText(wrapper.getFile().getName() + ": " + JDLocale.L("plugins.optional.jdunrar.status.extractfailed", "Extract failed") + ": " + wrapper.getException().getMessage());

            } else {

                pc.setStatusText(wrapper.getFile().getName() + ": " + JDLocale.L("plugins.optional.jdunrar.status.extractfailed", "Extract failed"));

            }

            this.onFinished(wrapper);

            break;
        case JDUnrarConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE:

            pc.setStatusText(wrapper.getFile().getName() + ": " + JDLocale.L("plugins.optional.jdunrar.status.extractfailedpass", "Extract failed (password)"));

            if (this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_ASK_UNKNOWN_PASS, true)) {
                String pass = JDUtilities.getGUI().showCountdownUserInputDialog(JDLocale.LF("plugins.optional.jdunrar.askForPassword", "Password for %s?", wrapper.getDownloadLink().getName()), null);
                if (pass == null) {
                    this.onFinished(wrapper);
                    break;
                }
                wrapper.setPassword(pass);
            }

            break;
        case JDUnrarConstants.WRAPPER_PASSWORT_CRACKING:
            pc.setStatusText(wrapper.getFile().getName() + ": " + JDLocale.L("plugins.optional.jdunrar.status.crackingpass", "Cracking password"));
            pc.setRange(100);
            pc.setStatus(wrapper.getCrackProgress());

            break;
        case JDUnrarConstants.WRAPPER_CRACK_PASSWORD:
            break;
        case JDUnrarConstants.WRAPPER_NEW_STATUS:

            break;
        case JDUnrarConstants.WRAPPER_START_OPEN_ARCHIVE:
            pc.setStatusText(wrapper.getFile().getName() + ": " + JDLocale.L("plugins.optional.jdunrar.status.openingarchive", "Opening archive"));

            break;
        case JDUnrarConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS:
            assignRealDownloadDir(wrapper);
            break;

        case JDUnrarConstants.WRAPPER_PASSWORD_FOUND:

            pc.setStatusText(wrapper.getFile().getName() + ": " + JDLocale.L("plugins.optional.jdunrar.status.passfound", "Password found"));
            break;
        case JDUnrarConstants.WRAPPER_ON_PROGRESS:

            pc.setStatusText(wrapper.getFile().getName() + ": " + JDLocale.L("plugins.optional.jdunrar.status.extracting", "Extracting"));
            pc.setRange(wrapper.getTotalSize());
            pc.setStatus(wrapper.getExtractedSize());

            break;
        case JDUnrarConstants.WRAPPER_START_EXTRACTION:

            break;
        case JDUnrarConstants.WRAPPER_STARTED:

            break;
        case JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC:

            pc.setStatusText(wrapper.getFile().getName() + ": " + JDLocale.L("plugins.optional.jdunrar.status.extractfailedcrc", "Extract failed (CRC error)"));

            this.onFinished(wrapper);

            break;

        case JDUnrarConstants.WRAPPER_PROGRESS_SINGLE_FILE_FINISHED:

            break;
        case JDUnrarConstants.WRAPPER_FINISHED_SUCCESSFULL:

            File[] files = new File[wrapper.getFiles().size()];
            int i = 0;
            for (ArchivFile af : wrapper.getFiles()) {
                files[i++] = af.getFile();
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(wrapper, ControlEvent.CONTROL_ON_FILEOUTPUT, files));

            pc.setStatusText(wrapper.getFile().getName() + ": " + JDLocale.L("plugins.optional.jdunrar.status.extractok", "Extract OK"));

            if (this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_REMOVE_INFO_FILE, false)) {
                File fileOutput = new File(wrapper.getDownloadLink().getFileOutput());
                File infoFiles = new File(fileOutput.getParentFile(), fileOutput.getName().replaceFirst("(?i)(\\.part[0-9]+\\.rar|\\.rar)$", "") + ".info");
                if (infoFiles.exists() && infoFiles.delete()) {
                    logger.info(infoFiles.getName() + " removed");
                }
            }
            this.onFinished(wrapper);

            break;

        default:
            // System.out.println("id ");
        }

    }

    // /**
    // * LIest alle von wrapper w entapcken files und prüft ob es weitere
    // archive
    // * zum entpacken gibt.
    // *
    // * @param w
    // */
    // private void deepExtract(UnrarWrapper w) {
    //
    // for (ArchivFile file : w.getFiles()) {
    // File f = file.getFile();
    //
    // if (f.exists() && (f.getName().matches(".*part[0]*[1].rar$") ||
    // (!f.getName().matches(".*part[0-9]+.rar$") &&
    // f.getName().matches(".*rar$")))) {
    //
    // logger.info("Deep extract: " + f);
    // UnrarWrapper wrapper = new UnrarWrapper(w.getDownloadLink(), f);
    // wrapper.addUnrarListener(this);
    // wrapper.setExtractTo(f.getParentFile());
    // wrapper.setRemoveAfterExtract(this.getPluginConfig().getBooleanProperty(
    // JDUnrarConstants.CONFIG_KEY_REMVE_AFTER_EXTRACT, false));
    //
    // wrapper.setOverwrite(this.getPluginConfig().getBooleanProperty(
    // JDUnrarConstants.CONFIG_KEY_OVERWRITE, true));
    // wrapper.setUnrarCommand(getPluginConfig().getStringProperty(
    // JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND));
    // wrapper.setPasswordList(PasswordList.getPasswordList().toArray(new
    // String[] {}));
    //
    // wrapper.start();
    //
    // }
    //
    // }
    //
    // }

    /**
     * Gibt alle downloadlinks zum übergebenen link zurück. d.h. alle links die
     * zu dem archiv gehören
     * 
     * @param downloadLink
     * @return
     */
    private ArrayList<DownloadLink> getArchiveList(DownloadLink downloadLink) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        File file;
        int type = this.getArchivePartType(downloadLink);
        String name = null;
        int nums = 0;
        int i = 0;
        switch (type) {
        case JDUnrarConstants.NO_START_PART:
        case JDUnrarConstants.MULTIPART_START_PART:
        case JDUnrarConstants.MULTIPART_START_PART_V2:
            name = this.getArchiveName(downloadLink);
            String test = null;
            if ((test = new Regex(downloadLink.getFileOutput(), "part(\\d*?)\\.").getMatch(0)) != null) {
                nums = test.length();
                i = 1;
                while ((file = new File(new File(downloadLink.getFileOutput()).getParentFile(), name + ".part" + Formatter.fillString(i + "", "0", "", nums) + ".rar")).exists() || JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED) != null) {
                    ret.add(JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED));
                    i++;
                }
                break;
            } else if ((test = new Regex(downloadLink.getFileOutput(), "(.*)\\.rar$").getMatch(0)) != null) {
                ret.add(downloadLink);
                i = 0;
                nums = -1;
                for (int a = 5; a > 0; a--) {
                    String len = ".r";
                    for (int b = a; b > 0; b--) {
                        len = len + "0";
                    }
                    if (new File(test + len).exists()) {
                        nums = a;
                        break;
                    }
                }
                if (nums != -1) {
                    while ((file = new File(new File(downloadLink.getFileOutput()).getParentFile(), name + ".r" + Formatter.fillString(i + "", "0", "", nums))).exists() || JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED) != null) {
                        ret.add(JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED));
                        i++;
                    }
                }
            }
            break;
        case JDUnrarConstants.SINGLE_PART_ARCHIVE:
            ret.add(downloadLink);
            break;

        }
        return ret;

    }

    private void onFinished(UnrarWrapper wrapper) {
        // progress.get(wrapper).finalize(3000l);
        // wrapper.getDownloadLink().getLinkStatus().setStatusText(null);
        wrapper.getDownloadLink().setPluginProgress(null);
        if (wrapper.getDownloadLink().getProperty("PROGRESSCONTROLLER") != null) {
            ((ProgressController) wrapper.getDownloadLink().getProperty("PROGRESSCONTROLLER")).finalize(8000);
        }

    }

    // @Override
    public Object interact(String command, Object parameter) {
        if (command.equals("isWorking")) {
            return queue.isAlive();
        } else {
            return null;
        }
    }
}