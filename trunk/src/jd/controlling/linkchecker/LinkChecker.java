package jd.controlling.linkchecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.linkcrawler.CheckableLink;
import jd.http.Browser;
import jd.http.BrowserSettingsThread;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging.Log;

public class LinkChecker<E extends CheckableLink> {

    /* static variables */
    private static AtomicInteger                                                    LINKCHECKER_THREAD_NUM = new AtomicInteger(0);
    private final static int                                                        MAX_THREADS;
    private final static int                                                        KEEP_ALIVE;
    private static HashMap<String, Thread>                                          CHECK_THREADS          = new HashMap<String, Thread>();
    private static HashMap<String, ArrayList<LinkChecker<? extends CheckableLink>>> LINKCHECKER            = new HashMap<String, ArrayList<LinkChecker<? extends CheckableLink>>>();
    private static final Object                                                     LOCK                   = new Object();

    /* local variables for this LinkChecker */
    private AtomicLong                                                              linksRequested         = new AtomicLong(0);
    private AtomicLong                                                              linksDone              = new AtomicLong(0);
    private HashMap<String, LinkedList<E>>                                          links2Check            = new HashMap<String, LinkedList<E>>();
    private boolean                                                                 forceRecheck           = false;
    private LinkCheckerHandler<E>                                                   handler                = null;
    private static int                                                              SPLITSIZE              = 80;

    static {
        MAX_THREADS = Math.max(JsonConfig.create(LinkCheckerConfig.class).getMaxThreads(), 1);
        KEEP_ALIVE = Math.max(JsonConfig.create(LinkCheckerConfig.class).getThreadKeepAlive(), 100);
    }

    public LinkChecker() {
        this(false);
    }

    public LinkChecker(boolean forceRecheck) {
        this.forceRecheck = forceRecheck;
    }

    public boolean isForceRecheck() {
        return forceRecheck;
    }

    public void setLinkCheckHandler(LinkCheckerHandler<E> handler) {
        this.handler = handler;
    }

    public LinkCheckerHandler<E> getLinkCheckHandler() {
        return handler;
    }

    @SuppressWarnings("unchecked")
    protected void linkChecked(CheckableLink link) {
        if (link == null) return;
        boolean stopped = linksDone.incrementAndGet() == linksRequested.get();
        LinkCheckerHandler<E> h = handler;
        if (h != null) {
            h.linkCheckDone((E) link);
            if (stopped) h.linkCheckStopped();
        }
    }

    public void check(List<E> links) {
        if (links == null) throw new IllegalArgumentException("links is null?");
        for (E link : links) {
            check(link);
        }
    }

    public void check(E link) {
        if (link == null || link.getDownloadLink() == null) throw new IllegalArgumentException("links is null?");
        DownloadLink dlLink = link.getDownloadLink();
        /* get Host of the link */
        String host = dlLink.getHost();
        if (Plugin.FTP_HOST.equalsIgnoreCase(host) || Plugin.DIRECT_HTTP_HOST.equalsIgnoreCase(host) || Plugin.HTTP_LINKS_HOST.equalsIgnoreCase(host)) {
            /* direct and ftp links are divided by their hostname */
            String specialHost = Browser.getHost(dlLink.getDownloadURL());
            if (specialHost != null) host = specialHost;
        }
        boolean started = false;
        synchronized (this) {
            /* add link to list of link2Check */
            LinkedList<E> map = links2Check.get(host);
            if (map == null) {
                map = new LinkedList<E>();
                links2Check.put(host, map);
            }
            map.add(link);
            if (linksRequested.get() == linksDone.get()) started = true;
            linksRequested.incrementAndGet();
        }
        LinkCheckerHandler<E> h = handler;
        if (h != null && started) {
            h.linkCheckStarted();
        }
        synchronized (LOCK) {
            ArrayList<LinkChecker<? extends CheckableLink>> checker = LINKCHECKER.get(host);
            if (checker == null) {
                checker = new ArrayList<LinkChecker<? extends CheckableLink>>();
                checker.add(this);
                LINKCHECKER.put(host, checker);
            } else if (!checker.contains(this)) {
                checker.add(this);
            }
            /* notify linkcheckThread or try to start new one */
            Thread thread = CHECK_THREADS.get(host);
            if (thread == null || !thread.isAlive()) {
                startNewThreads();
            }
        }
    }

    /**
     * is the LinkChecker running
     * 
     * @return
     */
    public boolean isRunning() {
        return linksRequested.get() != linksDone.get();
    }

    public long checksRequested() {
        return linksRequested.get();
    }

    public long checksDone() {
        return linksDone.get();
    }

    /* wait till all requested links are done */
    public boolean waitForChecked() {
        while (isRunning()) {
            synchronized (this) {
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }
        return isRunning() == false;
    }

    /* start a new linkCheckThread for the given host */
    private static void startNewThread(final String threadHost) {
        synchronized (LOCK) {
            if (CHECK_THREADS.size() >= MAX_THREADS) return;
            final BrowserSettingsThread newThread = new BrowserSettingsThread(new Runnable() {

                public void run() {
                    int stopDelay = 1;
                    PluginForHost plg = null;
                    Browser br = new Browser();
                    while (true) {
                        /*
                         * map to hold the current checkable links and their
                         * linkchecker in this round
                         */
                        HashMap<CheckableLink, ArrayList<LinkChecker<? extends CheckableLink>>> round = new HashMap<CheckableLink, ArrayList<LinkChecker<? extends CheckableLink>>>();
                        try {
                            synchronized (LOCK) {
                                ArrayList<LinkChecker<? extends CheckableLink>> map = LINKCHECKER.get(threadHost);
                                if (map != null) {
                                    for (LinkChecker<? extends CheckableLink> lc : map) {
                                        synchronized (lc) {
                                            LinkedList<? extends CheckableLink> map2 = lc.links2Check.get(threadHost);
                                            if (map2 != null) {
                                                for (CheckableLink link : map2) {
                                                    ArrayList<LinkChecker<? extends CheckableLink>> map3 = round.get(link);
                                                    if (map3 == null) {
                                                        map3 = new ArrayList<LinkChecker<? extends CheckableLink>>();
                                                        map3.add(lc);
                                                        round.put(link, map3);
                                                    } else {
                                                        map3.add(lc);
                                                    }
                                                    if (lc.isForceRecheck()) {
                                                        /*
                                                         * linkChecker instance
                                                         * is set to
                                                         * forceRecheck
                                                         */
                                                        link.getDownloadLink().setAvailableStatus(AvailableStatus.UNCHECKED);
                                                    }
                                                }
                                                /*
                                                 * just clear the map to allow
                                                 * fast adding of new links to
                                                 * the given linkChecker
                                                 * instance
                                                 */
                                                map2.clear();
                                            }
                                        }
                                    }
                                    /*
                                     * remove threadHost from static list to
                                     * remove unwanted references
                                     */
                                    LINKCHECKER.remove(threadHost);
                                }
                            }
                            /* build map to get CheckableLink from DownloadLink */
                            HashMap<DownloadLink, CheckableLink> dlLink2checkLink = new HashMap<DownloadLink, CheckableLink>();
                            ArrayList<DownloadLink> links2Check = new ArrayList<DownloadLink>();
                            for (CheckableLink check : round.keySet()) {
                                dlLink2checkLink.put(check.getDownloadLink(), check);
                                links2Check.add(check.getDownloadLink());
                            }
                            int n = links2Check.size();
                            for (int i = 0; i < n; i += SPLITSIZE) {
                                List<DownloadLink> checks = links2Check.subList(i, Math.min(n, i + SPLITSIZE));
                                if (checks.size() > 0) {
                                    stopDelay = 1;
                                    DownloadLink linksList[] = checks.toArray(new DownloadLink[checks.size()]);
                                    /* now we check the links */
                                    if (plg == null) {
                                        /* create plugin if not done yet */
                                        plg = linksList[0].getDefaultPlugin().getWrapper().getNewPluginInstance();
                                        plg.setBrowser(br);
                                        plg.init();
                                    }
                                    try {
                                        /* try mass link check */
                                        plg.checkLinks(linksList);
                                    } catch (final Throwable e) {
                                        Log.exception(e);
                                    }
                                    for (DownloadLink link : linksList) {
                                        /*
                                         * this will check the link, if not
                                         * already checked
                                         */
                                        link.getAvailableStatus(plg);
                                        /*
                                         * notify all listener of this checkable
                                         * link
                                         */
                                        CheckableLink checkable = dlLink2checkLink.get(link);
                                        ArrayList<LinkChecker<? extends CheckableLink>> listeners = round.get(checkable);
                                        for (LinkChecker<? extends CheckableLink> listener : listeners) {
                                            listener.linkChecked(checkable);
                                        }
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            Log.exception(e);
                        }
                        try {
                            Thread.sleep(KEEP_ALIVE);
                        } catch (InterruptedException e) {
                            Log.exception(e);
                            synchronized (LOCK) {
                                CHECK_THREADS.remove(threadHost);
                                return;
                            }
                        }
                        synchronized (LOCK) {
                            ArrayList<LinkChecker<? extends CheckableLink>> stopCheck = LINKCHECKER.get(threadHost);
                            if (stopCheck == null || stopCheck.size() == 0) {
                                stopDelay--;
                                if (stopDelay < 0) {
                                    CHECK_THREADS.remove(threadHost);
                                    startNewThreads();
                                    return;
                                }
                            }
                        }
                    }
                }
            });
            newThread.setName("LinkChecker: " + LINKCHECKER_THREAD_NUM.incrementAndGet() + ":" + threadHost);
            newThread.setDaemon(true);
            CHECK_THREADS.put(threadHost, newThread);
            newThread.start();
        }
    }

    /* start new linkCheckThreads until max is reached or no left to start */
    private static void startNewThreads() {
        synchronized (LOCK) {
            Set<Entry<String, ArrayList<LinkChecker<? extends CheckableLink>>>> sets = LINKCHECKER.entrySet();
            for (Entry<String, ArrayList<LinkChecker<? extends CheckableLink>>> set : sets) {
                String host = set.getKey();
                Thread thread = CHECK_THREADS.get(host);
                if (thread == null || !thread.isAlive()) {
                    CHECK_THREADS.remove(host);
                    if (CHECK_THREADS.size() < MAX_THREADS) {
                        startNewThread(host);
                    } else {
                        break;
                    }
                }
            }
        }
    }
}
