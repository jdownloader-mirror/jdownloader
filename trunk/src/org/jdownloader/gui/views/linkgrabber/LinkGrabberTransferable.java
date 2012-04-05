package org.jdownloader.gui.views.linkgrabber;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;

import org.appwork.utils.ClipboardUtils;
import org.jdownloader.gui.views.components.packagetable.dragdrop.PackageControllerTableTransferable;
import org.jdownloader.gui.views.components.packagetable.dragdrop.PackageControllerTableTransferableContent;

public class LinkGrabberTransferable extends PackageControllerTableTransferable<CrawledPackage, CrawledLink> {

    public LinkGrabberTransferable(PackageControllerTableTransferable<CrawledPackage, CrawledLink> transferable) {
        super(transferable.getContent().getPackages(), transferable.getContent().getLinks(), transferable.getContent().getTable());
        ArrayList<DataFlavor> availableFlavors = new ArrayList<DataFlavor>();
        availableFlavors.add(FLAVOR);
        availableFlavors.add(ClipboardUtils.stringFlavor);
        flavors = availableFlavors.toArray(new DataFlavor[] {});
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
        if (flavor.equals(FLAVOR)) return content;
        if (flavor.equals(ClipboardUtils.stringFlavor)) {
            StringBuilder sb = new StringBuilder();
            HashSet<String> urls = getURLs();
            Iterator<String> it = urls.iterator();
            while (it.hasNext()) {
                if (sb.length() > 0) sb.append("\r\n");
                sb.append(it.next());
            }
            return sb.toString();
        }
        throw new UnsupportedFlavorException(flavor);
    }

    private HashSet<String> getURLs() {
        PackageControllerTableTransferableContent<CrawledPackage, CrawledLink> lcontent = content;
        HashSet<String> urls = new HashSet<String>();
        if (lcontent == null) return urls;
        if (lcontent.getLinks() != null) {
            for (CrawledLink link : lcontent.getLinks()) {
                DownloadLink llink = link.getDownloadLink();
                if (llink != null && DownloadLink.LINKTYPE_CONTAINER != llink.getLinkType()) {
                    if (llink.gotBrowserUrl()) {
                        urls.add(llink.getBrowserUrl());
                    } else {
                        urls.add(llink.getDownloadURL());
                    }
                }
            }
        }
        if (lcontent.getPackages() != null) {
            for (CrawledPackage fp : lcontent.getPackages()) {
                synchronized (fp) {
                    for (CrawledLink link : fp.getChildren()) {
                        DownloadLink llink = link.getDownloadLink();
                        if (llink != null && DownloadLink.LINKTYPE_CONTAINER != llink.getLinkType()) {
                            if (llink.gotBrowserUrl()) {
                                urls.add(llink.getBrowserUrl());
                            } else {
                                urls.add(llink.getDownloadURL());
                            }
                        }
                    }
                }
            }
        }
        return urls;
    }

}
