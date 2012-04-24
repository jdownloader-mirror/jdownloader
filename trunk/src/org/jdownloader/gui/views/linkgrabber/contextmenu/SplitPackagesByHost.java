package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;

public class SplitPackagesByHost extends AppAction {

    /**
     * 
     */
    private static final long       serialVersionUID = 2636706677433058054L;
    private ArrayList<AbstractNode> selection;

    public SplitPackagesByHost(AbstractNode contextObject, ArrayList<AbstractNode> selection) {
        this.selection = new ArrayList<AbstractNode>(selection);
        this.selection.add(0, contextObject);
        setName(_GUI._.SplitPackagesByHost_SplitPackagesByHost_object_());
        setIconKey("split_packages");
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {

            @Override
            public void run() {
                HashMap<String, ArrayList<CrawledLink>> splitMap = new HashMap<String, ArrayList<CrawledLink>>();
                ArrayList<AbstractNode> children = LinkTreeUtils.getSelectedChildren(selection, new ArrayList<AbstractNode>());
                CrawledPackage samePkg = null;
                boolean samePackage = true;
                for (AbstractNode child : children) {
                    if (child instanceof CrawledLink) {
                        CrawledLink cL = (CrawledLink) child;
                        if (samePkg == null) {
                            samePkg = cL.getParentNode();
                        } else if (cL.getParentNode() != samePkg) {
                            samePackage = false;
                        }
                        ArrayList<CrawledLink> map = splitMap.get(cL.getHost());
                        if (map == null) {
                            map = new ArrayList<CrawledLink>();
                            splitMap.put(cL.getHost(), map);
                        }
                        map.add(cL);
                    }
                }
                if (!samePackage) {
                    samePkg = null;
                }
                Iterator<Entry<String, ArrayList<CrawledLink>>> it = splitMap.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, ArrayList<CrawledLink>> next = it.next();
                    String host = next.getKey();
                    final ArrayList<CrawledLink> links = next.getValue();
                    final CrawledPackage newPkg = new CrawledPackage();
                    newPkg.setExpanded(true);
                    if (samePkg != null) {
                        samePkg.copyPropertiesTo(newPkg);
                    } else {
                        newPkg.setName(host);
                    }
                    IOEQ.getQueue().add(new QueueAction<Object, RuntimeException>() {

                        @Override
                        protected Object run() {
                            LinkCollector.getInstance().addmoveChildren(newPkg, links, -1);
                            return null;
                        }

                    });
                }
            }
        }, true);
    }
}
