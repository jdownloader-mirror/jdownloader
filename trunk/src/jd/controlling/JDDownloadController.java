package jd.controlling;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.Timer;

import jd.Main;
import jd.config.Configuration;
import jd.event.JDBroadcaster;
import jd.event.JDEvent;
import jd.event.JDListener;
import jd.gui.skins.simple.components.DownloadView.DownloadTreeTable;
import jd.nutils.io.JDIO;
import jd.plugins.BackupLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageEvent;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.update.PackageData;
import jd.utils.JDUtilities;

public class JDDownloadController extends JDBroadcaster implements JDListener, ActionListener {

    private static JDDownloadController INSTANCE = null;

    private Vector<FilePackage> packages = new Vector<FilePackage>();
    private Logger logger = null;

    private JDController controller;

    private Timer Save_Async; /*
                               * Async-Save, Linkliste wird verzögert
                               * gespeichert
                               */

    public synchronized static JDDownloadController getDownloadController() {
        /* darf erst nachdem der JDController init wurde, aufgerufen werden */
        if (INSTANCE == null) {
            INSTANCE = new JDDownloadController();
        }
        return INSTANCE;
    }

    private JDDownloadController() {
        logger = jd.controlling.JDLogger.getLogger();
        controller = JDUtilities.getController();
        initDownloadLinks();
        Save_Async = new Timer(2000, this);
        Save_Async.setInitialDelay(2000);
        Save_Async.setRepeats(false);
        addJDListener(this);/* erst nachdem die packages geinit wurden! */
    }

    /**
     * Lädt zum Start das erste Mal alle Links aus einer Datei
     * 
     * @return true/False je nach Erfolg
     */
    private void initDownloadLinks() {
        try {
            packages = loadDownloadLinks();
        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().severe("" + e.getStackTrace());
            packages = null;
        }
        if (packages == null) {
            packages = new Vector<FilePackage>();
            File file = JDUtilities.getResourceFile("backup/links.linkbackup");
            if (file.exists()) {
                logger.severe("Strange: No Linklist,Try to restore from backup file");
                controller.loadContainerFile(file);
            }
            return;
        } else if (packages.size() == 0 && Main.returnedfromUpdate()) {
            File file = JDUtilities.getResourceFile("backup/links.linkbackup");
            if (file.exists() && file.lastModified() >= System.currentTimeMillis() - 10 * 60 * 1000l) {
                logger.severe("Strange: Empty Linklist,Try to restore from backup file");
                controller.loadContainerFile(file);
            }
            return;
        }
        for (FilePackage filePackage : packages) {
            filePackage.getJDBroadcaster().addJDListener(this);
            for (DownloadLink downloadLink : filePackage.getDownloadLinks()) {
                downloadLink.setProperty(DownloadTreeTable.PROPERTY_SELECTED, false);
            }
        }
        return;
    }

    public void saveDownloadLinksAsync() {
        Save_Async.restart();
    }

    /**
     * Speichert die Linksliste ab
     * 
     * @param file
     *            Die Datei, in die die Links gespeichert werden sollen
     */
    public void saveDownloadLinksSync() {
        synchronized (packages) {
            System.out.println("DOWNLOADCONTROLLER: Save LinkList (Sync)");
            JDUtilities.getDatabaseConnector().saveLinks(packages);
        }
    }

    public void backupDownloadLinks() {
        synchronized (packages) {
            Vector<DownloadLink> links = getAllDownloadLinks();
            Iterator<DownloadLink> it = links.iterator();
            ArrayList<BackupLink> ret = new ArrayList<BackupLink>();
            while (it.hasNext()) {
                DownloadLink next = it.next();
                BackupLink bl;
                if (next.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                    bl = (new BackupLink(JDUtilities.getResourceFile(next.getContainerFile()), next.getContainerIndex(), next.getContainer()));

                } else {
                    bl = (new BackupLink(next.getDownloadURL()));
                }
                bl.setProperty("downloaddirectory", next.getFilePackage().getDownloadDirectory());
                bl.setProperty("packagename", next.getFilePackage().getName());
                bl.setProperty("plugin", next.getPlugin().getClass().getSimpleName());
                bl.setProperty("name", new File(next.getFileOutput()).getName());
                bl.setProperty("properties", next.getProperties());
                bl.setProperty("enabled", next.isEnabled());

                ret.add(bl);
            }
            if (ret.size() == 0) return;
            File file = JDUtilities.getResourceFile("backup/links.linkbackup");
            if (file.exists()) {
                File old = JDUtilities.getResourceFile("backup/links_" + file.lastModified() + ".linkbackup");

                file.getParentFile().mkdirs();
                if (file.exists()) {
                    file.renameTo(old);
                }
                file.delete();
            } else {
                file.getParentFile().mkdirs();
            }
            JDIO.saveObject(null, ret, file, "links.linkbackup", "linkbackup", false);
        }
    }

    /**
     * Lädt eine LinkListe
     * 
     * @param file
     *            Die Datei, aus der die Links gelesen werden
     * @return Ein neuer Vector mit den DownloadLinks
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Vector<FilePackage> loadDownloadLinks() throws Exception {
        Object obj = JDUtilities.getDatabaseConnector().getLinks();
        if (obj != null && obj instanceof Vector && (((Vector) obj).size() == 0 || ((Vector) obj).size() > 0 && ((Vector) obj).get(0) instanceof FilePackage)) {
            Vector<FilePackage> packages = (Vector<FilePackage>) obj;
            Iterator<FilePackage> iterator = packages.iterator();
            DownloadLink localLink;
            PluginForHost pluginForHost = null;
            PluginsC pluginForContainer = null;
            Iterator<DownloadLink> it;
            FilePackage fp;
            while (iterator.hasNext()) {
                fp = iterator.next();
                if (fp.getDownloadLinks().size() == 0) {
                    iterator.remove();
                    continue;
                }
                it = fp.getDownloadLinks().iterator();
                while (it.hasNext()) {
                    localLink = it.next();
                    if (localLink.getLinkType() == DownloadLink.LINKTYPE_JDU && (localLink.getProperty("JDU") == null || !(localLink.getProperty("JDU") instanceof PackageData))) {
                        iterator.remove();
                        continue;
                    }
                    if (!localLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        localLink.getLinkStatus().reset();
                    }
                    if (localLink.getLinkStatus().hasStatus(LinkStatus.FINISHED) && JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION) == 1) {
                        it.remove();
                        if (fp.getDownloadLinks().size() == 0) {
                            iterator.remove();
                            continue;
                        }
                    } else {
                        // Anhand des Hostnamens aus dem DownloadLink
                        // wird ein passendes Plugin gesucht
                        try {
                            pluginForHost = JDUtilities.getNewPluginForHostInstanz(localLink.getHost());
                        } catch (Exception e) {
                            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
                        }
                        // Gibt es einen Names für ein Containerformat,
                        // wird ein passendes Plugin gesucht
                        try {
                            if (localLink.getContainer() != null) {
                                pluginForContainer = JDUtilities.getPluginForContainer(localLink.getContainer(), localLink.getContainerFile());
                                if (pluginForContainer == null) {
                                    localLink.setEnabled(false);
                                }
                            }
                        } catch (NullPointerException e) {
                            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
                        }
                        if (pluginForHost != null) {
                            localLink.setLoadedPlugin(pluginForHost);
                        }
                        if (pluginForContainer != null) {
                            localLink.setLoadedPluginForContainer(pluginForContainer);
                        }
                        if (pluginForHost == null) {
                            logger.severe("couldn't find plugin(" + localLink.getHost() + ") for this DownloadLink." + localLink.getName());
                        }
                    }
                }
            }
            return packages;
        }
        throw new Exception("Linklist incompatible");
    }

    public Vector<FilePackage> getPackages() {
        return packages;
    }

    public void addPackage(FilePackage fp) {
        if (fp == null) return;
        boolean added = false;
        synchronized (packages) {
            if (!packages.contains(fp)) {
                added = true;
                fp.getJDBroadcaster().addJDListener(this);
                packages.add(fp);
                if (added) fireJDEvent(new JDDownloadControllerEvent(this, JDDownloadControllerEvent.ADD_FP));
            }
        }
    }

    public void addPackageAt(FilePackage fp, int index) {
        if (fp == null) return;
        synchronized (packages) {
            if (packages.size() == 0) {
                addPackage(fp);
                return;
            }
            if (index > packages.size() - 1) {
                index = packages.size() - 1;
            }
            if (index < 0) {
                index = 0;
            }
            if (packages.contains(fp)) {
                packages.remove(fp);
            }
            packages.add(index, fp);
        }
        fireJDEvent(new JDDownloadControllerEvent(this, JDDownloadControllerEvent.UPDATE));
    }

    public void removePackage(FilePackage fp2) {
        if (fp2 == null) return;
        synchronized (packages) {
            fp2.abortDownload();
            fp2.getJDBroadcaster().removeJDListener(this);
            packages.remove(fp2);
            fireJDEvent(new JDDownloadControllerEvent(this, JDDownloadControllerEvent.REMOVE_FP));
        }
    }

    /**
     * Liefert alle DownloadLinks zurück
     * 
     * @return Alle DownloadLinks zurück
     */
    public Vector<DownloadLink> getAllDownloadLinks() {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        synchronized (packages) {
            for (FilePackage fp : packages) {
                ret.addAll(fp.getDownloadLinks());
            }
        }
        return ret;
    }

    public void removeCompletedPackages() {
        Vector<FilePackage> packagestodelete = new Vector<FilePackage>();
        synchronized (packages) {
            for (FilePackage fp : packages) {
                if (fp.getLinksWithStatus(LinkStatus.FINISHED).size() == fp.size()) packagestodelete.add(fp);
            }
        }
        for (FilePackage fp : packagestodelete) {
            removePackage(fp);
        }
    }

    public void removeCompletedDownloadLinks() {
        Vector<DownloadLink> downloadstodelete = new Vector<DownloadLink>();
        synchronized (packages) {
            for (FilePackage fp : packages) {
                downloadstodelete.addAll(fp.getLinksWithStatus(LinkStatus.FINISHED));
            }
        }
        for (DownloadLink dl : downloadstodelete) {
            dl.getFilePackage().remove(dl);
        }
    }

    public DownloadLink getDownloadLinkAfter(DownloadLink lastElement) {
        synchronized (packages) {
            for (FilePackage fp : packages) {
                DownloadLink dl = fp.getLinkAfter(lastElement);
                if (dl != null) return dl;
            }
        }
        return null;
    }

    public DownloadLink getDownloadLinkBefore(DownloadLink lastElement) {
        synchronized (packages) {
            for (FilePackage fp : packages) {
                DownloadLink dl = fp.getLinkBefore(lastElement);
                if (dl != null) return dl;
            }
        }
        return null;
    }

    public DownloadLink getFirstDownloadLinkwithURL(String url) {
        if (url == null) return null;
        url = url.trim();
        synchronized (packages) {
            for (DownloadLink dl : getAllDownloadLinks()) {
                if (dl.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) continue;
                if (dl.getDownloadURL() != null && dl.getDownloadURL().equalsIgnoreCase(url)) { return dl; }
            }
        }
        return null;
    }

    public DownloadLink getFirstLinkThatBlocks(DownloadLink link) {
        synchronized (packages) {
            for (DownloadLink nextDownloadLink : getAllDownloadLinks()) {
                if (nextDownloadLink != link) {
                    if ((nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) {
                        if (new File(nextDownloadLink.getFileOutput()).exists()) {
                            /*
                             * fertige datei sollte auch auf der platte sein und
                             * nicht nur als fertig in der liste
                             */
                            logger.info("Link owner: " + nextDownloadLink.getHost() + nextDownloadLink);
                            return nextDownloadLink;
                        }
                    }
                    if ((nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS) || nextDownloadLink.getLinkStatus().isPluginActive()) && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) {
                        if (nextDownloadLink.getFinalFileName() != null) {
                            /* Dateiname muss fertig geholt sein */
                            logger.info("Link owner: " + nextDownloadLink.getHost() + nextDownloadLink);
                            return nextDownloadLink;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void receiveJDEvent(JDEvent event) {
        if (event instanceof JDDownloadControllerEvent) {
            switch (event.getID()) {
            case JDDownloadControllerEvent.ADD_FP:
                System.out.println("DOWNLOADCONTROLLER: FilePackage added, Throw Update Event");
                fireJDEvent(new JDDownloadControllerEvent(this, JDDownloadControllerEvent.UPDATE));
                break;
            case JDDownloadControllerEvent.REMOVE_FP:
                System.out.println("DOWNLOADCONTROLLER: FilePackage removed, Throw Update Event");
                fireJDEvent(new JDDownloadControllerEvent(this, JDDownloadControllerEvent.UPDATE));
                break;
            case JDDownloadControllerEvent.UPDATE:
                System.out.println("DOWNLOADCONTROLLER: Update Event, Save LinkList (Async)");
                this.saveDownloadLinksAsync();
                break;
            }
        }
        if (event instanceof FilePackageEvent) {
            switch (event.getID()) {
            case FilePackageEvent.DL_ADDED:
                System.out.println("DOWNLOADCONTROLLER: Filepackage, Link added, Throw Update Event");
                fireJDEvent(new JDDownloadControllerEvent(this, JDDownloadControllerEvent.UPDATE));
                break;
            case FilePackageEvent.DL_REMOVED:
                System.out.println("DOWNLOADCONTROLLER: Filepackage, Link removed, Throw Update Event");
                fireJDEvent(new JDDownloadControllerEvent(this, JDDownloadControllerEvent.UPDATE));
                break;
            case FilePackageEvent.FP_UPDATE:
                System.out.println("DOWNLOADCONTROLLER: forward Filepackage Update to GUI");
                fireJDEvent(new JDDownloadControllerEvent(this, JDDownloadControllerEvent.UPDATE));
                break;
            case FilePackageEvent.FP_EMPTY:
                System.out.println("DOWNLOADCONTROLLER: remove FilePackage, Update GUI");
                this.removePackage((FilePackage) event.getSource());
                fireJDEvent(new JDDownloadControllerEvent(this, JDDownloadControllerEvent.UPDATE));
                break;
            }
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == Save_Async) {
            this.saveDownloadLinksSync();
        }
    }
}
