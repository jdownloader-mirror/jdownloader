//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.controlling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;

import jd.config.ConfigPropertyListener;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.JDBroadcaster;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkGrabberFilePackageEvent;
import jd.plugins.LinkGrabberFilePackageListener;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.hoster.HTTPAllgemein;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

class LinkGrabberControllerBroadcaster extends JDBroadcaster<LinkGrabberControllerListener, LinkGrabberControllerEvent> {

    // @Override
    protected void fireEvent(LinkGrabberControllerListener listener, LinkGrabberControllerEvent event) {
        listener.onLinkGrabberControllerEvent(event);
    }

}

public class LinkGrabberController implements LinkGrabberFilePackageListener, LinkGrabberControllerListener {

    public final static Object ControllerLock = new Object();

    public static final byte MOVE_BEFORE = 1;
    public static final byte MOVE_AFTER = 2;
    public static final byte MOVE_BEGIN = 3;
    public static final byte MOVE_END = 4;
    public static final byte MOVE_TOP = 5;
    public static final byte MOVE_BOTTOM = 6;

    public static final String PARAM_ONLINECHECK = "PARAM_ONLINECHECK";
    public static final String CONFIG = "LINKGRABBER";
    public static final String IGNORE_LIST = "IGNORE_LIST";
    public static final String DONTFORCEPACKAGENAME = "dontforcename";

    private static ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
    private static final HashSet<String> extensionFilter = new HashSet<String>();

    private static LinkGrabberController INSTANCE = null;
    private boolean lastSort = true;

    private LinkGrabberControllerBroadcaster broadcaster;

    private static String[] filter;

    private ConfigPropertyListener cpl;
    private LinkGrabberFilePackage FP_UNSORTED;
    private LinkGrabberFilePackage FP_UNCHECKED;
    private LinkGrabberFilePackage FP_UNCHECKABLE;
    private LinkGrabberFilePackage FP_OFFLINE;
    private LinkGrabberFilePackage FP_FILTERED;
    private LinkGrabberDistributeEvent distributer = null;

    private Logger logger;

    public synchronized static LinkGrabberController getInstance() {
        if (INSTANCE == null) INSTANCE = new LinkGrabberController();
        return INSTANCE;
    }

    public LinkGrabberFilePackage getFILTERPACKAGE() {
        return this.FP_FILTERED;
    }

    public void setDistributer(LinkGrabberDistributeEvent dist) {
        this.distributer = dist;
    }

    public void addLinks(ArrayList<DownloadLink> links, boolean hidegrabber, boolean autostart) {
        if (distributer != null) {
            distributer.addLinks(links, hidegrabber, autostart);
        } else {
            /*
             * TODO: evtl autopackaging auch hier, aber eigentlich net nötig, da
             * es sache des coders ist was genau er machen soll
             */
            JDLogger.getLogger().info("No Distributer set, using minimal version");
            ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
            FilePackage fp = FilePackage.getInstance();
            fp.setName("Added");
            for (DownloadLink link : links) {
                if (link.getFilePackage() == FilePackage.getDefaultFilePackage()) {
                    fp.add(link);
                    if (!fps.contains(fp)) fps.add(fp);
                } else {
                    if (!fps.contains(link.getFilePackage())) fps.add(link.getFilePackage());
                }
            }
            DownloadController.getInstance().addAllAt(fps, 0);
            if (autostart) JDController.getInstance().startDownloads();
        }
    }

    public void addListener(LinkGrabberControllerListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(LinkGrabberControllerListener l) {
        broadcaster.removeListener(l);
    }

    private LinkGrabberController() {
        logger = jd.controlling.JDLogger.getLogger();
        broadcaster = new LinkGrabberControllerBroadcaster();
        broadcaster.addListener(this);

        filter = getLinkFilterPattern();
        JDController.getInstance().addControlListener(this.cpl = new ConfigPropertyListener(IGNORE_LIST) {

            // @Override
            public void onPropertyChanged(Property source, String propertyName) {
                filter = getLinkFilterPattern();
            }

        });

        FP_UNSORTED = new LinkGrabberFilePackage(JDL.L("gui.linkgrabber.package.unsorted", "various"), this);
        FP_UNCHECKED = new LinkGrabberFilePackage(JDL.L("gui.linkgrabber.package.unchecked", "unchecked"), this);
        FP_UNCHECKABLE = new LinkGrabberFilePackage(JDL.L("gui.linkgrabber.package.uncheckable", "uncheckable"), this);
        FP_UNCHECKABLE.setIgnore(true);
        FP_OFFLINE = new LinkGrabberFilePackage(JDL.L("gui.linkgrabber.package.offline", "offline"), this);
        FP_OFFLINE.setIgnore(true);
        FP_FILTERED = new LinkGrabberFilePackage(JDL.L("gui.linkgrabber.package.filtered", "filtered"));
        FP_FILTERED.setIgnore(true);
    }

    public HashSet<String> getExtensionFilter() {
        return extensionFilter;
    }

    public boolean isLinkCheckEnabled() {
        return SubConfiguration.getConfig(CONFIG).getBooleanProperty(PARAM_ONLINECHECK, true);
    }

    public void clearExtensionFilter() {
        synchronized (extensionFilter) {
            extensionFilter.clear();
        }
        this.FP_FILTERED.setDownloadLinks(new ArrayList<DownloadLink>());
    }

    public void FilterExtension(String ext, boolean b) {
        boolean c = false;
        synchronized (extensionFilter) {
            if (!b) {
                if (!extensionFilter.contains(ext)) {
                    extensionFilter.add(ext);
                    c = true;
                } else {
                    return;
                }
            } else {
                if (extensionFilter.contains(ext)) {
                    extensionFilter.remove(ext);
                    c = true;
                } else {
                    return;
                }
            }
        }
        if (c) broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.FILTER_CHANGED));
    }

    public String[] getLinkFilterPattern() {
        String filter = SubConfiguration.getConfig(CONFIG).getStringProperty(IGNORE_LIST, null);
        if (filter == null || filter.length() == 0) return null;
        String[] lines = Regex.getLines(filter);
        ArrayList<String> ret = new ArrayList<String>();
        for (String line : lines) {
            if (line.trim().startsWith("#") || line.trim().length() == 0) continue;
            ret.add(line.trim());
        }
        return ret.toArray(new String[] {});
    }

    protected void finalize() {
        JDController.getInstance().removeControlListener(cpl);
        System.out.println("REMOVED LISTENER " + cpl);
    }

    public ArrayList<LinkGrabberFilePackage> getPackages() {
        return packages;
    }

    public int indexOf(LinkGrabberFilePackage fp) {
        return packages.indexOf(fp);
    }

    public boolean isExtensionFiltered(DownloadLink link) {
        synchronized (extensionFilter) {
            for (String ext : extensionFilter) {
                if (link.getName().endsWith(ext)) { return true; }
            }
        }
        return false;
    }

    public LinkGrabberFilePackage getFPwithName(String name) {
        synchronized (packages) {
            if (name == null) return null;
            for (LinkGrabberFilePackage fp : packages) {
                if (fp.getName().equalsIgnoreCase(name)) return fp;
            }
            if (FP_FILTERED.getName().equalsIgnoreCase(name)) return FP_FILTERED;
            return null;
        }
    }

    public LinkGrabberFilePackage getFPwithLink(DownloadLink link) {
        synchronized (packages) {
            if (link == null) return null;
            for (LinkGrabberFilePackage fp : packages) {
                if (fp.contains(link)) return fp;
            }
            if (FP_FILTERED.contains(link)) return FP_FILTERED;
            return null;
        }
    }

    public void postprocessing() {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(packages);
                for (LinkGrabberFilePackage fp : fps) {
                    boolean remove = false;
                    if (fp.countFailedLinks(true) == fp.size()) remove = true;
                    ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(fp.getDownloadLinks());
                    for (DownloadLink dl : links) {
                        if (dl.isAvailabilityStatusChecked() && dl.getAvailableStatus() == AvailableStatus.UNCHECKABLE && links.size() == 1) {
                            FP_UNCHECKABLE.add(dl);
                        } else if (dl.isAvailabilityStatusChecked() && !dl.isAvailable() && (links.size() == 1 || remove)) {
                            FP_OFFLINE.add(dl);
                        }
                    }
                    fp.sort(1, true);
                }
            }
        }
    }

    public boolean isDupe(DownloadLink link) {
        synchronized (packages) {
            if (link == null) return false;
            if (link.getBooleanProperty("ALLOW_DUPE", false)) return false;
            LinkGrabberFilePackage fp = null;
            DownloadLink dl = null;
            for (Iterator<LinkGrabberFilePackage> it = packages.iterator(); it.hasNext();) {
                fp = it.next();
                for (Iterator<DownloadLink> it2 = fp.getDownloadLinks().iterator(); it2.hasNext();) {
                    dl = it2.next();
                    if (dl.compareTo(link) == 0) return true;
                }
            }
            return false;
        }
    }

    public void addPackage(LinkGrabberFilePackage fp) {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                if (!packages.contains(fp)) {
                    packages.add(fp);
                    fp.addListener(this);
                    broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADD_FILEPACKAGE, fp));
                }
            }
        }
    }

    public void addAllAt(ArrayList<LinkGrabberFilePackage> links, int index) {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                int repos = 0;
                for (int i = 0; i < links.size(); i++) {
                    repos = addPackageAt(links.get(i), index + i, repos);
                }
            }
        }
    }

    public int addPackageAt(LinkGrabberFilePackage fp, int index, int repos) {
        if (fp == null) return repos;
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                if (packages.size() == 0) {
                    addPackage(fp);
                    return repos;
                }
                boolean newadded = false;
                if (packages.contains(fp)) {
                    int posa = this.indexOf(fp);
                    if (posa < index) {
                        index -= ++repos;
                    }
                    packages.remove(fp);
                    if (index > packages.size() - 1) {
                        packages.add(fp);
                    } else if (index < 0) {
                        packages.add(0, fp);
                    } else
                        packages.add(index, fp);
                } else {
                    if (index > packages.size() - 1) {
                        packages.add(fp);
                    } else if (index < 0) {
                        packages.add(0, fp);
                    } else
                        packages.add(index, fp);
                }
                if (newadded) {
                    fp.addListener(this);
                    broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADD_FILEPACKAGE, fp));
                } else {
                    broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE, fp));
                }
            }
        }
        return repos;
    }

    public void removePackage(LinkGrabberFilePackage fp) {
        if (fp == null) return;
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                if (fp != this.FP_FILTERED && fp != this.FP_OFFLINE && fp != this.FP_UNCHECKED && fp != this.FP_UNSORTED) fp.removeListener(this);
                packages.remove(fp);
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REMOVE_FILPACKAGE, fp));
            }
        }
    }

    public void sort(final int col) {
        lastSort = !lastSort;
        synchronized (packages) {
            if (col == 3) {
                for (LinkGrabberFilePackage fp : packages) {
                    fp.sort(3, lastSort);
                }
            } else {
                Collections.sort(packages, new Comparator<LinkGrabberFilePackage>() {
                    public int compare(LinkGrabberFilePackage a, LinkGrabberFilePackage b) {
                        LinkGrabberFilePackage aa = a;
                        LinkGrabberFilePackage bb = b;
                        if (lastSort) {
                            aa = b;
                            bb = a;
                        }
                        switch (col) {
                        case 0:
                            return aa.getName().compareToIgnoreCase(bb.getName());
                        case 1:
                            return aa.getDownloadSize(false) > bb.getDownloadSize(false) ? 1 : -1;
                        case 2:
                            return aa.getHoster().compareToIgnoreCase(bb.getHoster());
                        default:
                            return -1;
                        }
                    }
                });
            }
        }
        broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE));
    }

    public void FilterPackages() {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (packages) {
                ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(packages);
                fps.add(this.FP_FILTERED);
                for (LinkGrabberFilePackage fp : fps) {
                    if (fp == this.FP_UNCHECKED || fp == this.FP_OFFLINE || fp == this.FP_UNSORTED) continue;
                    ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(fp.getDownloadLinks());
                    for (DownloadLink dl : links) {
                        if (this.isExtensionFiltered(dl)) {
                            FP_FILTERED.add(dl);
                        } else {
                            attachToPackagesSecondStage(dl);
                        }
                    }
                }
            }
        }
    }

    public void attachToPackagesFirstStage(DownloadLink link) {
        synchronized (LinkGrabberController.ControllerLock) {
            String packageName;
            LinkGrabberFilePackage fp = null;
            if (this.isExtensionFiltered(link)) {
                fp = this.FP_FILTERED;
            } else {
                if (link.getFilePackage() != FilePackage.getDefaultFilePackage()) {
                    packageName = link.getFilePackage().getName();
                    fp = getFPwithName(packageName);
                    if (fp == null) {
                        fp = new LinkGrabberFilePackage(packageName, this);
                    }
                    fp.setDownloadDirectory(link.getFilePackage().getDownloadDirectory());
                    fp.setPassword(link.getFilePackage().getPassword());
                }
            }
            if (fp == null) {
                if (isLinkCheckEnabled()) {
                    fp = this.FP_UNCHECKED;
                } else {
                    fp = this.FP_UNSORTED;
                }
            }
            fp.add(link);
        }
    }

    public int size() {
        return packages.size();
    }

    public void attachToPackagesSecondStage(DownloadLink link) {
        synchronized (LinkGrabberController.ControllerLock) {
            String packageName;
            boolean autoPackage = false;
            if (this.isExtensionFiltered(link)) {
                this.FP_FILTERED.add(link);
                return;
            } else if (link.getFilePackage() != FilePackage.getDefaultFilePackage()) {
                if (link.getFilePackage().getStringProperty(DONTFORCEPACKAGENAME, null) != null) {
                    /* enable autopackaging even if filepackage is set */
                    autoPackage = true;
                    packageName = cleanFileName(link.getName());
                } else {
                    packageName = link.getFilePackage().getName();
                }
            } else {
                autoPackage = true;
                packageName = cleanFileName(link.getName());
            }
            int bestSim = 0;
            LinkGrabberFilePackage bestp = null;
            synchronized (packages) {
                for (int i = 0; i < packages.size(); i++) {
                    int sim = comparepackages(packages.get(i).getName(), packageName);
                    if (sim > bestSim) {
                        bestSim = sim;
                        bestp = packages.get(i);
                    }
                }
            }
            if (bestSim < 99) {
                LinkGrabberFilePackage fp = new LinkGrabberFilePackage(packageName, this);
                fp.setPassword(link.getFilePackage().getPassword());
                fp.setDownloadDirectory(link.getFilePackage().getDownloadDirectory());
                fp.add(link);
            } else {
                String newPackageName = autoPackage ? getSimString(bestp.getName(), packageName) : packageName;
                bestp.setName(newPackageName);
                bestp.add(link);
            }
        }
    }

    private String getSimString(String a, String b) {
        String aa = a.toLowerCase();
        String bb = b.toLowerCase();
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < Math.min(aa.length(), bb.length()); i++) {
            if (aa.charAt(i) == bb.charAt(i)) {
                ret.append(a.charAt(i));
            }
        }
        return ret.toString();
    }

    public static String cleanFileName(String name) {
        /** remove rar extensions */

        name = getNameMatch(name, "(.*)\\.part[0]*[1].rar$");
        name = getNameMatch(name, "(.*)\\.part[0-9]+.rar$");
        name = getNameMatch(name, "(.*)\\.rar$");
        name = getNameMatch(name, "(.*)\\.r\\d+$");
        name = getNameMatch(name, "(.*)(\\.|_)\\d+$");

        /**
         * remove 7zip and hjmerge extensions
         */

        name = getNameMatch(name, "(?is).*\\.7z\\.[\\d]+$");
        name = getNameMatch(name, "(.*)\\.a.$");

        name = getNameMatch(name, "(.*)(\\.|_)[\\d]+($|" + HTTPAllgemein.ENDINGS + "$)");

        int lastPoint = name.lastIndexOf(".");
        if (lastPoint <= 0) lastPoint = name.lastIndexOf("_");
        if (lastPoint > 0) {
            String extension = name.substring(name.length() - lastPoint + 1);
            if (extension.length() < 3) {
                name = name.substring(0, lastPoint);
            }
        }
        return JDUtilities.removeEndingPoints(name);
    }

    private static String getNameMatch(String name, String pattern) {
        String match = new Regex(name, pattern).getMatch(0);
        if (match != null) return match;
        return name;
    }

    private static int comparepackages(String a, String b) {
        int c = 0;
        String aa = a.toLowerCase();
        String bb = b.toLowerCase();
        for (int i = 0; i < Math.min(aa.length(), bb.length()); i++) {
            if (aa.charAt(i) == bb.charAt(i)) {
                c++;
            }
        }
        if (Math.min(aa.length(), bb.length()) == 0) { return 0; }
        return c * 100 / Math.max(aa.length(), bb.length());
    }

    public void handle_LinkGrabberFilePackageEvent(LinkGrabberFilePackageEvent event) {
        switch (event.getID()) {
        case LinkGrabberFilePackageEvent.EMPTY_EVENT:
            removePackage(((LinkGrabberFilePackage) event.getSource()));
            if (packages.size() == 0 && this.FP_FILTERED.size() == 0) {
                clearExtensionFilter();
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.EMPTY));
            }
            break;
        case LinkGrabberFilePackageEvent.ADD_LINK:
        case LinkGrabberFilePackageEvent.REMOVE_LINK:
            if (!packages.contains(((LinkGrabberFilePackage) event.getSource()))) {
                addPackage(((LinkGrabberFilePackage) event.getSource()));
            } else {
                broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE, event.getSource()));
            }
            break;
        case LinkGrabberFilePackageEvent.UPDATE_EVENT:
            broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE, event.getSource()));
            break;
        default:
            break;
        }
    }

    public void throwLinksAdded() {
        broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.ADDED));
    }

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        switch (event.getID()) {
        case LinkGrabberControllerEvent.ADD_FILEPACKAGE:
        case LinkGrabberControllerEvent.REMOVE_FILPACKAGE:
            broadcaster.fireEvent(new LinkGrabberControllerEvent(this, LinkGrabberControllerEvent.REFRESH_STRUCTURE));
            break;
        case LinkGrabberControllerEvent.FILTER_CHANGED:
            FilterPackages();
            break;
        default:
            break;
        }
    }

    public static boolean isFiltered(DownloadLink element) {
        if (filter == null || filter.length == 0) return false;
        synchronized (filter) {
            for (String f : filter) {
                if (element.getDownloadURL().matches(f) || element.getName().matches(f)) {
                    JDLogger.getLogger().finer("Filtered link: " + element.getName() + " due to filter entry " + f);
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean isFiltered(CryptedLink element) {
        if (filter == null || filter.length == 0) return false;
        synchronized (filter) {
            for (String f : filter) {
                String t = element.getCryptedUrl().replaceAll("httpviajd://", "http://").replaceAll("httpsviajd://", "https://");
                if (t.matches(f)) {
                    JDLogger.getLogger().finer("Filtered link: due to filter entry " + f);
                    return true;
                }
            }
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public void move(Object src2, Object dst, byte mode) {
        boolean type = false; /* false=downloadLink,true=LinkGrabberFilePackage */
        Object src = null;
        LinkGrabberFilePackage fp = null;
        if (src2 instanceof ArrayList<?>) {
            Object check = ((ArrayList<?>) src2).get(0);
            if (check == null) {
                logger.warning("Null src, cannot move!");
                return;
            }
            if (check instanceof DownloadLink) {
                src = src2;
                type = false;
            } else if (check instanceof LinkGrabberFilePackage) {
                src = src2;
                type = true;
            }
        } else if (src2 instanceof DownloadLink) {
            type = false;
            src = new ArrayList<DownloadLink>();
            ((ArrayList<DownloadLink>) src).add((DownloadLink) src2);
        } else if (src2 instanceof LinkGrabberFilePackage) {
            type = true;
            src = new ArrayList<LinkGrabberFilePackage>();
            ((ArrayList<LinkGrabberFilePackage>) src).add((LinkGrabberFilePackage) src2);
        }
        if (src == null) {
            logger.warning("Unknown src, cannot move!");
            return;
        }
        synchronized (ControllerLock) {
            synchronized (packages) {
                if (dst != null) {
                    if (!type) {
                        if (dst instanceof LinkGrabberFilePackage) {
                            /* src:DownloadLinks dst:LinkGrabberFilePackage */
                            switch (mode) {
                            case MOVE_BEGIN:
                                fp = ((LinkGrabberFilePackage) dst);
                                fp.addAllAt((ArrayList<DownloadLink>) src, 0);
                                return;
                            case MOVE_END:
                                fp = ((LinkGrabberFilePackage) dst);
                                fp.addAllAt((ArrayList<DownloadLink>) src, fp.size());
                                return;
                            default:
                                logger.warning("Unsupported mode, cannot move!");
                                return;
                            }
                        } else if (dst instanceof DownloadLink) {
                            /* src:DownloadLinks dst:DownloadLinks */
                            switch (mode) {
                            case MOVE_BEFORE:
                                fp = getFPwithLink((DownloadLink) dst);
                                fp.addAllAt((ArrayList<DownloadLink>) src, fp.indexOf((DownloadLink) dst));
                                return;
                            case MOVE_AFTER:
                                fp = getFPwithLink((DownloadLink) dst);
                                fp.addAllAt((ArrayList<DownloadLink>) src, fp.indexOf((DownloadLink) dst) + 1);
                                return;
                            default:
                                logger.warning("Unsupported mode, cannot move!");
                                return;
                            }
                        } else {
                            logger.warning("Unsupported dst, cannot move!");
                            return;
                        }
                    } else {
                        if (dst instanceof LinkGrabberFilePackage) {
                            /*
                             * src:LinkGrabberFilePackage
                             * dst:LinkGrabberFilePackage
                             */
                            switch (mode) {
                            case MOVE_BEFORE:
                                addAllAt((ArrayList<LinkGrabberFilePackage>) src, indexOf((LinkGrabberFilePackage) dst));
                                return;
                            case MOVE_AFTER:
                                addAllAt((ArrayList<LinkGrabberFilePackage>) src, indexOf((LinkGrabberFilePackage) dst) + 1);
                                return;
                            default:
                                logger.warning("Unsupported mode, cannot move!");
                                return;
                            }
                        } else if (dst instanceof DownloadLink) {
                            /* src:LinkGrabberFilePackage dst:DownloadLinks */
                            logger.warning("Unsupported mode, cannot move!");
                            return;
                        }
                    }
                } else {
                    /* dst==null, global moving */
                    if (type) {
                        /* src:LinkGrabberFilePackage */
                        switch (mode) {
                        case MOVE_TOP:
                            addAllAt((ArrayList<LinkGrabberFilePackage>) src, 0);
                            return;
                        case MOVE_BOTTOM:
                            addAllAt((ArrayList<LinkGrabberFilePackage>) src, size() + 1);
                            return;
                        default:
                            logger.warning("Unsupported mode, cannot move!");
                            return;
                        }
                    }
                }
            }
        }
    }

}
