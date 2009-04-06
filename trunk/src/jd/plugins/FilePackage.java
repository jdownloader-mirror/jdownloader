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

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import jd.config.Property;
import jd.event.JDBroadcaster;
import jd.nutils.io.JDIO;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse verwaltet Pakete
 * 
 * @author JD-Team
 */
public class FilePackage extends Property implements Serializable {

    // Zählt die instanzierungen durch um eine ID zu erstellen
    private static int counter = 0;

    private static final long serialVersionUID = -8859842964299890820L;

    private static final long UPDATE_INTERVAL = 2000;

    private String comment;

    private String downloadDirectory;

    private Vector<DownloadLink> downloadLinks;
    private transient static FilePackage FP = null;

    private transient JDBroadcaster bc = null;

    public JDBroadcaster getJDBroadcaster() {
        if (bc == null) bc = new JDBroadcaster();
        return bc;
    }

    public static FilePackage getDefaultFilePackage() {
        if (FP == null) FP = new FilePackage(JDLocale.L("controller.packages.defaultname", "various"));
        return FP;
    }

    // Eindeutige PaketID
    private String id;

    private boolean lastSort = false;

    private int linksFailed;

    private int linksFinished;

    private int linksInProgress;

    private String name = null;

    private String password;
    private boolean extractAfterDownload = true;

    private int totalBytesLoaded;

    private int totalDownloadSpeed;

    private int totalEstimatedPackageSize;

    private long updateTime;

    private long updateTime1;

    private boolean isFinished;

    public FilePackage() {
        downloadDirectory = JDUtilities.getConfiguration().getDefaultDownloadDirectory();
        counter++;
        id = System.currentTimeMillis() + "_" + counter;
        downloadLinks = new Vector<DownloadLink>();
    }

    public FilePackage(String name) {
        this();
        this.setName(name);
    }

    /**
     * Diese Methode speichert Paketinformationen ab (falls die Datei noch nicht
     * bereits besteht)
     */

    public void add(DownloadLink link) {
        synchronized (downloadLinks) {
            if (!downloadLinks.contains(link)) {
                downloadLinks.add(link);
                link.setFilePackage(this);
                getJDBroadcaster().fireJDEvent(new FilePackageEvent(this, FilePackageEvent.DL_ADDED));
            }
            link.setFilePackage(this);
        }
    }

    public void add(int index, DownloadLink link) {
        synchronized (downloadLinks) {
            if (downloadLinks.contains(link)) {
                downloadLinks.remove(link);
                downloadLinks.add(index, link);
                link.setFilePackage(this);
                getJDBroadcaster().fireJDEvent(new FilePackageEvent(this, FilePackageEvent.FP_UPDATE));
            } else {
                downloadLinks.add(index, link);
                link.setFilePackage(this);
                getJDBroadcaster().fireJDEvent(new FilePackageEvent(this, FilePackageEvent.DL_ADDED));
            }
        }
    }

    public void addAll(Vector<DownloadLink> links) {
        synchronized (downloadLinks) {
            for (DownloadLink dl : links) {
                add(dl);
            }
        }
    }

    public boolean isExtractAfterDownload() {
        return extractAfterDownload;
    }

    public void setExtractAfterDownload(boolean extractAfterDownload) {
        this.extractAfterDownload = extractAfterDownload;
    }

    public void addAllAt(Vector<DownloadLink> links, int index) {
        synchronized (downloadLinks) {
            for (int i = 0; i < links.size(); i++) {
                add(index + i, links.get(i));
            }
        }
    }

    public boolean contains(DownloadLink link) {
        synchronized (downloadLinks) {
            return downloadLinks.contains(link);
        }
    }

    public DownloadLink get(int index) {
        synchronized (downloadLinks) {
            return downloadLinks.get(index);
        }
    }

    /**
     * @return Gibt den Kommentar ab den der user im Linkgrabber zu diesem Paket
     *         abgegeben hat
     */
    public String getComment() {
        return comment == null ? "" : comment;
    }

    /**
     * @return Gibt den Downloadpfad zurück den der user für dieses paket
     *         festgelegt hat
     */
    public String getDownloadDirectory() {
        return downloadDirectory == null ? JDUtilities.getConfiguration().getDefaultDownloadDirectory() : downloadDirectory;
    }

    /**
     * @return Gibt nur den namen des Downloadverzeichnisses zurück. ACHTUNG! es
     *         wird nur der Directory-NAME zurückgegeben, nicht der ganze Pfad
     */
    public String getDownloadDirectoryName() {
        if (!hasDownloadDirectory()) { return "."; }
        return new File(downloadDirectory).getName();
    }

    public Vector<DownloadLink> getDownloadLinks() {
        synchronized (downloadLinks) {
            return downloadLinks;
        }
    }

    /**
     * Gibt die vorraussichtlich verbleibende Downloadzeit für dieses paket
     * zurück
     * 
     * @return
     */
    public int getETA() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        if (totalDownloadSpeed / 1024 == 0) { return -1; }
        return (Math.max(totalBytesLoaded, totalEstimatedPackageSize) - totalBytesLoaded) / (totalDownloadSpeed / 1024);
    }

    public String getId() {
        return id;
    }

    /**
     * Gibt die Anzahl der fehlerhaften Links zurück
     * 
     * @return
     */
    public int getLinksFailed() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }

        return linksFailed;
    }

    /**
     * Gibt die Anzahl der fertiggestellten Links zurück
     * 
     * @return
     */
    public int getLinksFinished() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return linksFinished;
    }

    /**
     * Gibt zurück wieviele Links gerade in Bearbeitung sind
     * 
     * @return
     */
    public int getLinksInProgress() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return linksInProgress;
    }

    public boolean isFinished() {
        if (System.currentTimeMillis() - updateTime1 > UPDATE_INTERVAL) {
            updateTime1 = System.currentTimeMillis();
            boolean value = true;
            if (linksFinished > 0) {
                synchronized (downloadLinks) {
                    for (DownloadLink lk : downloadLinks) {
                        if (!lk.getLinkStatus().hasStatus(LinkStatus.FINISHED) && lk.isEnabled()) {
                            value = false;
                            break;
                        }
                    }
                }
            } else {
                value = false;
            }
            isFinished = value;
        }
        return isFinished;
    }

    public String getName() {
        if (name == null) return "";
        return name;
    }

    /**
     * 
     * @return Gibt das Archivpasswort zurück das der User für dieses paket
     *         angegeben hat
     */
    public String getPassword() {
        return password == null ? "" : password;
    }

    /**
     * Diese Werte werden durch itterieren durch die downloadListe ermittelt. Um
     * dies nicht zu oft machen zu müssen geschiet das intervalartig
     * 
     * @return
     */
    /**
     * Gibt den Fortschritt des pakets in prozent zurück
     */
    public double getPercent() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }

        return 100.0 * totalBytesLoaded / Math.max(1, Math.max(totalBytesLoaded, totalEstimatedPackageSize));
    }

    /**
     * Gibt die Anzahl der Verbleibenden Links zurück. Wurden alle Links bereits
     * abgearbeitet gibt diese Methode 0 zurück Da die Methode alle Links
     * durchläuft sollte sie aus Performancegründen mit bedacht eingesetzt
     * werden
     */
    public int getRemainingLinks() {
        updateCollectives();
        return size() - linksFinished;

    }

    // Gibt die erste gefundene sfv datei im Paket zurück
    public DownloadLink getSFV() {
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if (dl.getFileOutput().toLowerCase().endsWith(".sfv")) return dl;
            }
        }
        return null;
    }

    /**
     * Gibt die aktuelle Downloadgeschwinigkeit des Pakets zurück
     * 
     * @return
     */
    public int getTotalDownloadSpeed() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }

        return totalDownloadSpeed;
    }

    /**
     * Gibt die geschätzte Gesamtgröße des Pakets zurück
     * 
     * @return
     */
    public long getTotalEstimatedPackageSize() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return Math.max(totalBytesLoaded, totalEstimatedPackageSize);
    }

    /**
     * Gibt zurück wieviele Bytes ingesamt schon in diesem Paket geladen wurden
     * 
     * @return
     */
    public int getTotalKBLoaded() {
        if (System.currentTimeMillis() - updateTime > UPDATE_INTERVAL) {
            updateCollectives();
        }
        return totalBytesLoaded;
    }

    /**
     * 
     * @return True/false, je nach dem ob ein Kommentar gespeichert ist
     */
    public boolean hasComment() {
        return comment != null && comment.length() > 0;
    }

    /**
     * @return True/false, je nach dem ob ein Downloadirectory festgelegt wurde
     */
    public boolean hasDownloadDirectory() {
        return downloadDirectory != null && downloadDirectory.length() > 0;
    }

    /**
     * @return true/false, je nachdem ob ein Passwort festgelegt wurde
     *         (archivpasswort)
     */
    public boolean hasPassword() {
        return password != null && password.length() > 0;
    }

    public int indexOf(DownloadLink link) {
        synchronized (downloadLinks) {
            return downloadLinks.indexOf(link);
        }
    }

    public DownloadLink getLinkAfter(DownloadLink link) {
        synchronized (downloadLinks) {
            int index = indexOf(link);
            if (index == -1 || index + 1 > downloadLinks.size() - 1) return null;
            return get(index + 1);
        }
    }

    public DownloadLink getLinkBefore(DownloadLink link) {
        synchronized (downloadLinks) {
            int index = indexOf(link);
            if (index == -1 || index - 1 < 0) return null;
            return get(index - 1);
        }
    }

    public DownloadLink lastElement() {
        synchronized (downloadLinks) {
            return downloadLinks.lastElement();
        }
    }

    public void remove(DownloadLink link) {
        if (link == null) return;
        synchronized (downloadLinks) {
            boolean ret = downloadLinks.remove(link);
            if (ret) {
                link.setFilePackage(null);
                getJDBroadcaster().fireJDEvent(new FilePackageEvent(this, FilePackageEvent.DL_REMOVED));
                if (downloadLinks.size() == 0) this.getJDBroadcaster().fireJDEvent(new FilePackageEvent(this, FilePackageEvent.FP_EMPTY));
            }
        }
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDownloadDirectory(String subFolder) {
        downloadDirectory = JDUtilities.removeEndingPoints(subFolder);
    }

    public void setDownloadLinks(Vector<DownloadLink> downloadLinks) {
        synchronized (downloadLinks) {
            this.downloadLinks = new Vector<DownloadLink>();
            if (downloadLinks.size() == 0) {
                this.getJDBroadcaster().fireJDEvent(new FilePackageEvent(this, FilePackageEvent.FP_EMPTY));
            }
            {
                this.addAll(downloadLinks);
            }
        }
    }

    public void setName(String name) {
        if (name == null || name.length() == 0) {
            this.name = JDUtilities.removeEndingPoints(getDefaultFilePackage().name);
        } else
            this.name = JDUtilities.removeEndingPoints(JDIO.validateFileandPathName(name));
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int size() {
        synchronized (downloadLinks) {
            return downloadLinks.size();
        }
    }

    public void abortDownload() {
        synchronized (downloadLinks) {
            for (DownloadLink downloadLink : downloadLinks) {
                downloadLink.setAborted(true);
            }
        }
    }

    public Vector<DownloadLink> getLinksWithStatus(int status) {
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                if (dl.getLinkStatus().hasStatus(status)) {
                    ret.add(dl);
                }
            }
        }
        return ret;
    }

    public String getHoster() {
        Set<String> hosterList = new HashSet<String>();
        synchronized (downloadLinks) {
            for (DownloadLink dl : downloadLinks) {
                hosterList.add(dl.getHost());
            }
        }
        return hosterList.toString();
    }

    public void sort(final int col) {
        lastSort = !lastSort;
        synchronized (downloadLinks) {

            Collections.sort(downloadLinks, new Comparator<DownloadLink>() {

                public int compare(DownloadLink a, DownloadLink b) {
                    if (a.getName().endsWith(".sfv")) { return -1; }
                    if (b.getName().endsWith(".sfv")) { return 1; }
                    DownloadLink aa = a;
                    DownloadLink bb = b;
                    if (lastSort) {
                        aa = b;
                        bb = a;
                    }
                    switch (col) {
                    case 1:
                        return aa.getName().compareToIgnoreCase(bb.getName());
                    case 2:
                        return aa.getHost().compareToIgnoreCase(bb.getHost());
                    case 3:
                        if (aa.isAvailabilityChecked() && bb.isAvailabilityChecked()) {
                            return (aa.isAvailable() && !bb.isAvailable()) ? 1 : -1;
                        } else
                            return -1;
                    case 4:
                        return aa.getPercent() > bb.getPercent() ? 1 : -1;
                    default:
                        return -1;
                    }
                }
            });
        }
        getJDBroadcaster().fireJDEvent(new FilePackageEvent(this, FilePackageEvent.FP_UPDATE));
    }

    /**
     * Alles undokumentiert, da selbsterklärend
     */
    @Override
    public String toString() {
        return this.getName() + " " + this.size();
    }

    public void updateCollectives() {
        synchronized (downloadLinks) {

            totalEstimatedPackageSize = 0;
            totalDownloadSpeed = 0;
            linksFinished = 0;
            linksInProgress = 0;
            linksFailed = 0;
            totalBytesLoaded = 0;
            long avg = 0;
            DownloadLink next;
            int i = 0;

            for (Iterator<DownloadLink> it = downloadLinks.iterator(); it.hasNext();) {
                next = it.next();

                if (next.getDownloadSize() > 0) {

                    if (next.isEnabled()) {
                        totalEstimatedPackageSize += next.getDownloadSize() / 1024;
                    }

                    avg = (i * avg + next.getDownloadSize() / 1024) / (i + 1);
                    // logger.info(i+"+ "+next.getDownloadMax()/1024+" kb
                    // avg:"+avg+" = +"+totalEstimatedPackageSize);
                    i++;
                } else {
                    if (it.hasNext()) {
                        if (next.isEnabled()) {
                            totalEstimatedPackageSize += avg;
                        }

                        // logger.info(i+"+avg "+avg+" kb
                        // =+"+totalEstimatedPackageSize);

                    } else {
                        if (next.isEnabled()) {
                            totalEstimatedPackageSize += avg / 2;
                            // logger.info(i+"+avg "+(avg/2)+" kb
                            // =+"+totalEstimatedPackageSize);
                        }
                    }
                }

                totalDownloadSpeed += Math.max(0, next.getDownloadSpeed());
                if (next.isEnabled()) {
                    totalBytesLoaded += next.getDownloadCurrent() / 1024;
                }
                linksInProgress += next.getLinkStatus().isPluginActive() ? 1 : 0;
                linksFinished += next.getLinkStatus().hasStatus(LinkStatus.FINISHED) ? 1 : 0;
                if (!next.getLinkStatus().hasStatus(LinkStatus.FINISHED | LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS | LinkStatus.TODO | LinkStatus.PLUGIN_IN_PROGRESS | LinkStatus.WAITING_USERIO)) {
                    linksFailed++;
                }
            }
        }
        updateTime = System.currentTimeMillis();
    }

}
