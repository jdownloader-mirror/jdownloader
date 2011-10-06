package org.jdownloader.gui.views.components.packagetable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;

public abstract class PackageControllerTableModel<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> extends ExtTableModel<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static enum TOGGLEMODE {
        CURRENT,
        TOP,
        BOTTOM
    }

    private DelayedRunnable                                    asyncRefresh;
    protected PackageController<E, V>                          pc;
    private DelayedRunnable                                    asyncRecreate = null;
    private ArrayList<PackageControllerTableModelFilter<E, V>> tableFilters       = new ArrayList<PackageControllerTableModelFilter<E, V>>();
    private Object                                             LOCK          = new Object();

    private ScheduledThreadPoolExecutor                        queue         = new ScheduledThreadPoolExecutor(1);

    public PackageControllerTableModel(PackageController<E, V> pc, String id) {
        super(id);
        queue.setKeepAliveTime(10000, TimeUnit.MILLISECONDS);
        queue.allowCoreThreadTimeOut(true);
        this.pc = pc;
        asyncRefresh = new DelayedRunnable(queue, 150l, 250l) {
            @Override
            public void delayedrun() {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        /* we just want to repaint */
                        getTable().repaint();
                    }
                };
            }
        };
        asyncRecreate = new DelayedRunnable(queue, 300l, 1000l) {
            @Override
            public void delayedrun() {
                final ArrayList<AbstractNode> newtableData = refreshSort(tableData);
                _fireTableStructureChanged(newtableData, false);
            }
        };

    }

    public ScheduledThreadPoolExecutor getThreadPool() {
        return queue;
    }

    public void recreateModel() {
        asyncRecreate.run();
    }

    public void refreshModel() {
        asyncRefresh.run();
    }

    public void toggleFilePackageExpand(final AbstractPackageNode fp2, final TOGGLEMODE mode) {
        queue.execute(new Runnable() {
            public void run() {
                final boolean cur = !fp2.isExpanded();
                boolean doToggle = true;
                switch (mode) {
                case CURRENT:
                    fp2.setExpanded(cur);
                    break;
                case TOP:
                    doToggle = true;
                    break;
                case BOTTOM:
                    doToggle = false;
                    break;
                }
                if (mode != TOGGLEMODE.CURRENT) {
                    final boolean readL = pc.readLock();
                    try {
                        for (E fp : pc.getPackages()) {

                            if (doToggle) {
                                fp.setExpanded(cur);
                                if (fp == fp2) doToggle = false;
                            } else {
                                if (fp == fp2) {
                                    doToggle = true;
                                    fp.setExpanded(cur);
                                }
                            }

                        }
                    } finally {
                        pc.readUnlock(readL);
                    }
                }
                asyncRecreate.delayedrun();
            };
        });

    }

    public void addFilter(PackageControllerTableModelFilter<E, V> filter) {
        synchronized (LOCK) {
            if (tableFilters.contains(filter)) return;
            ArrayList<PackageControllerTableModelFilter<E, V>> newfilters = new ArrayList<PackageControllerTableModelFilter<E, V>>(tableFilters);
            newfilters.add(filter);
            tableFilters = newfilters;
        }
    }

    public void removeFilter(PackageControllerTableModelFilter<E, V> filter) {
        synchronized (LOCK) {
            if (!tableFilters.contains(filter)) return;
            ArrayList<PackageControllerTableModelFilter<E, V>> newfilters = new ArrayList<PackageControllerTableModelFilter<E, V>>(tableFilters);
            newfilters.remove(filter);
            tableFilters = newfilters;
        }
    }

    /*
     * we override sort to have a better sorting of packages/files, to keep
     * their structure alive,data is only used to specify the size of the new
     * ArrayList
     */
    @SuppressWarnings("unchecked")
    @Override
    public ArrayList<AbstractNode> sort(final ArrayList<AbstractNode> data, ExtColumn<AbstractNode> column) {
        this.sortColumn = column;
        String id = null;
        if (column != null) id = column.getSortOrderIdentifier();
        try {
            if (column != null) {
                getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, id);
                getStorage().put(ExtTableModel.SORTCOLUMN_KEY, column.getID());
            } else {
                getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
                getStorage().put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
            }
        } catch (final Exception e) {
            Log.exception(e);
        }
        ArrayList<AbstractNode> packages = null;
        final boolean readL = pc.readLock();
        try {
            /* get all packages from controller */
            packages = new ArrayList<AbstractNode>(pc.size());
            packages.addAll(pc.getPackages());
        } finally {
            pc.readUnlock(readL);
        }
        ArrayList<PackageControllerTableModelFilter<E, V>> filters = this.tableFilters;
        /* filter packages */
        for (int index = packages.size() - 1; index >= 0; index--) {
            AbstractNode pkg = packages.get(index);
            for (PackageControllerTableModelFilter<E, V> filter : filters) {
                if (filter.isFiltered((E) pkg)) {
                    /* remove package because it is filtered */
                    packages.remove(index);
                    break;
                }
            }
        }
        /* sort packages */
        if (column != null) Collections.sort(packages, column.getRowSorter());
        ArrayList<AbstractNode> newData = new ArrayList<AbstractNode>(Math.max(data.size(), packages.size()));
        for (AbstractNode node : packages) {
            ArrayList<AbstractNode> files = null;
            synchronized (node) {
                files = new ArrayList<AbstractNode>(((E) node).getChildren());
            }
            /* filter children of this package */
            for (int index = files.size() - 1; index >= 0; index--) {
                AbstractNode child = files.get(index);
                for (PackageControllerTableModelFilter<E, V> filter : filters) {
                    if (filter.isFiltered((V) child)) {
                        /* remove child because it is filtered */
                        files.remove(index);
                        break;
                    }
                }
            }
            boolean expanded = ((E) node).isExpanded();
            if (column != null && expanded) {
                /* we only have to sort children if the package is expanded */
                Collections.sort(files, column.getRowSorter());
            }
            if (files.size() > 0) {
                /* only add package node if it contains children */
                newData.add(node);
            }
            if (!expanded) {
                /* not expanded */
                continue;
            } else {
                /* expanded, add its children */
                newData.addAll(files);
            }
        }
        return newData;
    }
}
