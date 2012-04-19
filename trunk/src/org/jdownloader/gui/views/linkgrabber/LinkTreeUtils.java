package org.jdownloader.gui.views.linkgrabber;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;

public class LinkTreeUtils {

    public static ArrayList<CrawledLink> getChildren(ArrayList<AbstractNode> selection) {
        ArrayList<CrawledLink> ret = new ArrayList<CrawledLink>();
        for (AbstractNode a : selection) {
            if (a instanceof CrawledLink) {
                ret.add((CrawledLink) a);
            } else if (a instanceof CrawledPackage) {
                if (!((CrawledPackage) a).isExpanded()) {
                    ret.addAll(((CrawledPackage) a).getChildren());
                }
            }
        }
        return ret;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T extends AbstractNode> ArrayList<T> getPackages(AbstractNode contextObject, ArrayList<AbstractNode> selection, ArrayList<T> container) {
        HashSet<T> ret = new HashSet<T>();
        if (contextObject != null) {
            if (contextObject instanceof AbstractPackageNode) {
                ret.add((T) contextObject);
            } else {
                ret.add((T) ((AbstractPackageChildrenNode) contextObject).getParentNode());
            }
        }
        if (selection != null) {
            for (AbstractNode a : selection) {
                if (a instanceof AbstractPackageNode) {
                    ret.add((T) a);
                } else {
                    ret.add((T) ((AbstractPackageChildrenNode) a).getParentNode());
                }
            }
        }
        container.addAll(ret);
        return container;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T extends AbstractNode> ArrayList<T> getSelectedChildren(ArrayList<AbstractNode> selection2, ArrayList<T> container) {
        HashSet<AbstractNode> has = new HashSet<AbstractNode>(selection2);
        HashSet<T> ret = new HashSet<T>();
        for (AbstractNode node : selection2) {
            if (node instanceof AbstractPackageChildrenNode) {
                ret.add((T) node);
            } else {

                // if we selected a package, and ALL it's links, we want all
                // links
                // if we selected a package, and nly afew links, we probably
                // want only these few links.
                // if we selected a package, and it is NOT expanded, we want all
                // links

                if (!((AbstractPackageNode) node).isExpanded()) {
                    // add allTODO
                    List<T> childs = ((AbstractPackageNode) node).getChildren();
                    ret.addAll(childs);
                    // LinkGrabberTableModel.getInstance().getAllChildrenNodes()
                } else {
                    List<T> childs = ((AbstractPackageNode) node).getChildren();
                    boolean containsNone = true;
                    boolean containsAll = true;
                    for (AbstractNode l : childs) {
                        if (has.contains(l)) {
                            containsNone = false;
                        } else {
                            containsAll = false;
                        }

                    }
                    if (containsAll || containsNone) {
                        ret.addAll(childs);
                    }
                }
            }
        }
        container.addAll(ret);
        return container;
    }

    public static File getDownloadDirectory(AbstractNode node) {
        if (node instanceof DownloadLink) {
            return new File(((DownloadLink) node).getFilePackage().getDownloadDirectory());
        } else if (node instanceof FilePackage) {
            return new File(((FilePackage) node).getDownloadDirectory());
        } else if (node instanceof CrawledLink) {
            return new File(((CrawledLink) node).getParentNode().getDownloadFolder());
        } else if (node instanceof CrawledPackage) { return new File(((CrawledPackage) node).getDownloadFolder()); }
        throw new WTFException("Unknown Type: " + node.getClass());
    }

    public static HashSet<String> getURLs(ArrayList<AbstractNode> links) {
        HashSet<String> urls = new HashSet<String>();
        if (links == null) return urls;
        for (AbstractNode node : links) {
            DownloadLink link = null;
            if (node instanceof DownloadLink) {
                link = (DownloadLink) node;
            } else if (node instanceof CrawledLink) {
                link = ((CrawledLink) node).getDownloadLink();
            }
            if (link != null) {
                if (DownloadLink.LINKTYPE_CONTAINER != link.getLinkType()) {
                    if (link.gotBrowserUrl()) {
                        urls.add(link.getBrowserUrl());
                    } else {
                        urls.add(link.getDownloadURL());
                    }
                }
            }

        }
        return urls;
    }

}
