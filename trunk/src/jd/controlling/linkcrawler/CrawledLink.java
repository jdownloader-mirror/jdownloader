package jd.controlling.linkcrawler;

import java.util.List;

import jd.controlling.linkcollector.LinkCollectingInformation;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractNodeNotifier;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.LinkInfo;
import jd.plugins.PluginForHost;

import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;

public class CrawledLink implements AbstractPackageChildrenNode<CrawledPackage>, CheckableLink, AbstractNodeNotifier {

    private volatile boolean crawlDeep = false;

    public boolean isCrawlDeep() {
        return crawlDeep;
    }

    public void setCrawlDeep(boolean crawlDeep) {
        this.crawlDeep = crawlDeep;
    }

    private volatile CrawledPackage            parent               = null;
    private volatile UnknownCrawledLinkHandler unknownHandler       = null;
    private volatile CrawledLinkModifier       modifyHandler        = null;
    private volatile BrokenCrawlerHandler      brokenCrawlerHandler = null;
    private volatile boolean                   autoConfirmEnabled   = false;

    private volatile UniqueAlltimeID           uniqueID             = null;
    private LinkOriginDetails                  origin;

    public boolean isAutoConfirmEnabled() {
        return autoConfirmEnabled;
    }

    public void setOrigin(LinkOriginDetails source) {
        this.origin = source;
    }

    public LinkOriginDetails getOrigin() {
        return origin;
    }

    public void setAutoConfirmEnabled(boolean autoAddEnabled) {
        this.autoConfirmEnabled = autoAddEnabled;
    }

    public boolean isAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    private boolean forcedAutoStartEnabled = false;

    public boolean isForcedAutoStartEnabled() {
        return forcedAutoStartEnabled;
    }

    public void setForcedAutoStartEnabled(boolean forcedAutoStartEnabled) {
        this.forcedAutoStartEnabled = forcedAutoStartEnabled;
    }

    private boolean autoStartEnabled = false;

    public UnknownCrawledLinkHandler getUnknownHandler() {
        return unknownHandler;
    }

    public void setUnknownHandler(UnknownCrawledLinkHandler unknownHandler) {
        this.unknownHandler = unknownHandler;
    }

    private volatile LinkCollectingJob         sourceJob          = null;
    private volatile long                      created            = -1;

    boolean                                    enabledState       = true;
    private volatile PackageInfo               desiredPackageInfo = null;
    private volatile LinkCollectingInformation collectingInfo     = null;

    public PackageInfo getDesiredPackageInfo() {
        return desiredPackageInfo;
    }

    public void setDesiredPackageInfo(PackageInfo desiredPackageInfo) {
        this.desiredPackageInfo = desiredPackageInfo;
    }

    /**
     * Linkid should be unique for a certain link. in most cases, this is the url itself, but somtimes (youtube e.g.) the id contains info
     * about how to prozess the file afterwards.
     * 
     * example:<br>
     * 2 youtube links may have the same url, but the one will be converted into mp3, and the other stays flv. url is the same, but linkID
     * different.
     * 
     * @return
     */
    public String getLinkID() {
        String linkID = null;
        if (dlLink != null) {
            linkID = dlLink.getLinkID();
        }
        if (linkID != null) {
            return linkID;
        }
        return getURL();
    }

    /**
     * @return the sourceJob
     */
    public LinkCollectingJob getSourceJob() {
        return sourceJob;
    }

    /**
     * @param sourceJob
     *            the sourceJob to set
     */
    public void setSourceJob(LinkCollectingJob sourceJob) {
        this.sourceJob = sourceJob;
    }

    public long getSize() {
        if (dlLink != null) {
            return dlLink.getView().getBytesTotal();
        }
        return -1;
    }

    /**
     * @return the hPlugin
     */
    public PluginForHost gethPlugin() {
        if (dlLink != null) {
            return dlLink.getDefaultPlugin();
        }
        return null;
    }

    private DownloadLink dlLink = null;

    /**
     * @return the dlLink
     */
    public DownloadLink getDownloadLink() {
        return dlLink;
    }

    /**
     * @return the cLink
     */
    public CryptedLink getCryptedLink() {
        return cLink;
    }

    private volatile CryptedLink                    cLink          = null;
    private volatile String                         url;
    private volatile CrawledLink                    sourceLink     = null;
    private volatile String                         name           = null;
    private volatile FilterRule                     matchingFilter;

    private volatile ArchiveInfo                    archiveInfo;
    private volatile UniqueAlltimeID                previousParent = null;
    private volatile String[]                       sourceUrls;
    private final NullsafeAtomicReference<LinkInfo> linkInfo       = new NullsafeAtomicReference<LinkInfo>();

    public CrawledLink(DownloadLink dlLink) {
        this.dlLink = dlLink;
        passwordForward(dlLink);
    }

    private void passwordForward(DownloadLink dlLink) {
        if (dlLink == null) {
            return;
        }
        List<String> lst = dlLink.getSourcePluginPasswordList();
        if (lst != null && lst.size() > 0) {
            getArchiveInfo().getExtractionPasswords().addAll(lst);
        }
    }

    public void setDownloadLink(DownloadLink dlLink) {
        this.dlLink = dlLink;
        passwordForward(dlLink);

    }

    public CrawledLink(CryptedLink cLink) {
        this.cLink = cLink;
    }

    public CrawledLink(String url) {
        if (url == null) {
            return;
        }
        this.url = new String(url);
    }

    public String getName() {
        String lname = name;
        if (lname != null) {
            CrawledPackage lparent = this.getParentNode();
            String packageName = null;
            if (lparent != null) {
                packageName = lparent.getName();
            }
            return PackagizerController.replaceDynamicTags(lname, packageName);
        }
        if (dlLink != null) {
            return dlLink.getView().getDisplayName();
        }
        return "RAWURL:" + getURL();
    }

    public int getChunks() {
        if (dlLink != null) {
            return dlLink.getChunks();
        }
        return -1;
    }

    public void setChunks(int chunks) {
        if (dlLink != null) {
            dlLink.setChunks(chunks);
        }
    }

    public void setName(String name) {
        if (StringUtils.equals(name, this.name)) {
            return;
        }
        if (name != null) {
            name = CrossSystem.alleviatePathParts(name);
            if (StringUtils.equals(name, this.name)) {
                return;
            }
        }
        if (StringUtils.isEmpty(name)) {
            this.name = null;
        } else {
            this.name = name;
        }
        setLinkInfo(null);
        if (hasNotificationListener()) {
            nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.NAME, getName()));
        }
    }

    /* returns unmodified name variable */
    public String _getName() {
        return name;
    }

    public boolean isNameSet() {
        return name != null;
    }

    public String getHost() {
        if (dlLink != null) {
            return dlLink.getHost();
        }
        return null;
    }

    public String getURL() {
        if (dlLink != null) {
            return dlLink.getDownloadURL();
        }
        if (cLink != null) {
            return cLink.getCryptedUrl();
        }
        if (url != null) {
            return url;
        }
        return null;
    }

    @Override
    public String toString() {
        CrawledLink parentL = sourceLink;
        StringBuilder sb = new StringBuilder();
        if (isNameSet()) {
            sb.append("NAME:");
            sb.append(getName());
        }
        if (url != null) {
            sb.append("URL:" + getURL());
        }
        if (dlLink != null) {
            sb.append("DLLink:" + getURL());
        }
        if (cLink != null) {
            sb.append("CLink:" + getURL());
        }
        if (parentL != null) {
            sb.append("<--");
            sb.append(parentL.toString());
        }
        return sb.toString();
    }

    public CrawledPackage getParentNode() {
        return parent;
    }

    public synchronized void setParentNode(CrawledPackage parent) {
        if (this.parent == parent) {
            this.previousParent = null;
            return;
        }
        if (this.parent != null) {
            this.previousParent = this.parent.getUniqueID();
        }
        this.parent = parent;
    }

    public boolean isEnabled() {
        return enabledState;
    }

    public void setArchiveID(String id) {
        if (dlLink != null) {
            dlLink.setArchiveID(id);
        }
    }

    public void setEnabled(boolean b) {
        if (b == enabledState) {
            return;
        }
        enabledState = b;
        if (hasNotificationListener()) {
            nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.ENABLED, b));
        }
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getFinishedDate() {
        return 0;
    }

    public CrawledLink getSourceLink() {
        return sourceLink;
    }

    public CrawledLink getOriginLink() {
        CrawledLink lsourceLink = getSourceLink();
        if (lsourceLink == null) {
            return this;
        }
        return lsourceLink.getOriginLink();
    }

    public void setSourceLink(CrawledLink parent) {
        sourceLink = parent;
    }

    public void setMatchingFilter(FilterRule matchedFilter) {
        this.matchingFilter = matchedFilter;
    }

    /**
     * If this Link got filtered by {@link CaptchaHandler}, you can get the matching deny rule here.<br>
     * <br>
     * 
     * @return
     */
    public FilterRule getMatchingFilter() {
        return matchingFilter;
    }

    public AvailableLinkState getLinkState() {
        if (dlLink != null) {
            switch (dlLink.getAvailableStatus()) {
            case FALSE:
                return AvailableLinkState.OFFLINE;
            case TRUE:
                return AvailableLinkState.ONLINE;
            case UNCHECKABLE:
                return AvailableLinkState.TEMP_UNKNOWN;
            case UNCHECKED:
                return AvailableLinkState.UNKNOWN;
            default:
                return AvailableLinkState.UNKNOWN;
            }
        }
        return AvailableLinkState.UNKNOWN;
    }

    public Priority getPriority() {
        try {
            if (dlLink == null) {
                return Priority.DEFAULT;
            }
            return dlLink.getPriorityEnum();
        } catch (Throwable e) {
            return Priority.DEFAULT;
        }
    }

    public void setPriority(Priority priority) {
        if (dlLink != null) {
            dlLink.setPriorityEnum(priority);
        }
    }

    /**
     * Returns if this linkc an be handled without manual user captcha input
     * 
     * @return
     */
    public boolean hasAutoCaptcha() {
        if (gethPlugin() != null) {
            return gethPlugin().hasAutoCaptcha();
        }
        return true;
    }

    public boolean hasCaptcha(Account acc) {
        if (gethPlugin() != null) {
            return gethPlugin().hasCaptcha(dlLink, acc);
        }
        return false;
    }

    public boolean isDirectHTTP() {
        if (gethPlugin() != null) {
            return gethPlugin().getClass().getName().endsWith("r.DirectHTTP");
        }
        return false;
    }

    public boolean isFTP() {
        if (gethPlugin() != null) {
            return gethPlugin().getClass().getName().endsWith("r.Ftp");
        }
        return false;
    }

    public DomainInfo getDomainInfo() {
        if (dlLink != null) {
            return dlLink.getDomainInfo();
        }
        return null;
    }

    public CrawledLinkModifier getCustomCrawledLinkModifier() {
        return modifyHandler;
    }

    public void setCustomCrawledLinkModifier(CrawledLinkModifier modifier) {
        this.modifyHandler = modifier;
    }

    /**
     * @param brokenCrawlerHandler
     *            the brokenCrawlerHandler to set
     */
    public void setBrokenCrawlerHandler(BrokenCrawlerHandler brokenCrawlerHandler) {
        this.brokenCrawlerHandler = brokenCrawlerHandler;
    }

    /**
     * @return the brokenCrawlerHandler
     */
    public BrokenCrawlerHandler getBrokenCrawlerHandler() {
        return brokenCrawlerHandler;
    }

    public boolean hasVariantSupport() {
        return dlLink.hasVariantSupport();
    }

    public UniqueAlltimeID getUniqueID() {
        if (dlLink != null) {
            return dlLink.getUniqueID();
        }
        if (uniqueID != null) {
            return uniqueID;
        }
        synchronized (this) {
            if (uniqueID != null) {
                return uniqueID;
            }
            uniqueID = new UniqueAlltimeID();
        }
        return uniqueID;
    }

    /**
     * @param collectingInfo
     *            the collectingInfo to set
     */
    public void setCollectingInfo(LinkCollectingInformation collectingInfo) {
        this.collectingInfo = collectingInfo;
    }

    /**
     * @return the collectingInfo
     */
    public LinkCollectingInformation getCollectingInfo() {
        LinkCollectingInformation lcollectingInfo = collectingInfo;
        CrawledLink lsourceLink = getSourceLink();
        if (lcollectingInfo != null || lsourceLink == null) {
            return lcollectingInfo;
        }
        return lsourceLink.getCollectingInfo();
    }

    public ArchiveInfo getArchiveInfo() {
        if (archiveInfo != null) {
            return archiveInfo;
        }
        synchronized (this) {
            if (archiveInfo != null) {
                return archiveInfo;
            }
            archiveInfo = new ArchiveInfo();
        }
        return archiveInfo;
    }

    public boolean hasArchiveInfo() {
        ArchiveInfo larchiveInfo = archiveInfo;
        if (larchiveInfo != null) {
            if (!BooleanStatus.UNSET.equals(larchiveInfo.getAutoExtract())) {
                return true;
            }
            if (larchiveInfo.getExtractionPasswords() != null && larchiveInfo.getExtractionPasswords().size() > 0) {
                return true;
            }
        }
        return false;
    }

    public void setArchiveInfo(ArchiveInfo archiveInfo) {
        this.archiveInfo = archiveInfo;
    }

    public LinkInfo getLinkInfo() {
        LinkInfo ret = linkInfo.get();
        if (ret == null) {
            ret = LinkInfo.getLinkInfo(this);
            linkInfo.set(ret);
        }
        return ret;
    }

    private void setLinkInfo(LinkInfo linkInfo) {
        if (linkInfo != null) {
            this.linkInfo.set(linkInfo);
        } else {
            this.linkInfo.getAndClear();
        }
    }

    @Override
    public void nodeUpdated(AbstractNode source, NOTIFY notify, Object param) {
        CrawledPackage lparent = parent;
        if (lparent == null) {
            return;
        }
        AbstractNode lsource = source;
        if (lsource != null && lsource instanceof DownloadLink) {
            if (param instanceof DownloadLinkProperty) {
                DownloadLinkProperty propertyEvent = (DownloadLinkProperty) param;
                switch (propertyEvent.getProperty()) {
                case AVAILABILITY:
                    nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.AVAILABILITY, propertyEvent.getValue()));
                    return;
                case ENABLED:
                    /* not needed to forward at the moment */
                    // nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this,
                    // CrawledLinkProperty.Property.ENABLED,
                    // propertyEvent.getValue()));
                    return;
                case NAME:
                    if (!isNameSet()) {
                        /* we use the name from downloadLink */
                        setLinkInfo(null);
                        nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.NAME, propertyEvent.getValue()));
                        return;
                    }
                case PRIORITY:
                    nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, CrawledLinkProperty.Property.PRIORITY, propertyEvent.getValue()));
                    return;
                }
            }
        }
        if (lsource == null) {
            lsource = this;
        }
        lparent.nodeUpdated(lsource, notify, param);
    }

    @Override
    public boolean hasNotificationListener() {
        CrawledPackage lparent = parent;
        if (lparent != null && lparent.hasNotificationListener()) {
            return true;
        }
        return false;
    }

    @Override
    public UniqueAlltimeID getPreviousParentNodeID() {
        return previousParent;
    }

    public String getArchiveID() {
        if (dlLink != null) {
            return dlLink.getArchiveID();
        }
        return null;
    }

    public void firePropertyChanged(CrawledLinkProperty.Property property, Object value) {
        if (hasNotificationListener()) {
            nodeUpdated(this, AbstractNodeNotifier.NOTIFY.PROPERTY_CHANCE, new CrawledLinkProperty(this, property, value));
        }
    }

    public void setSourceUrls(String[] sourceUrls) {
        this.sourceUrls = sourceUrls;
    }

    public String[] getSourceUrls() {
        return sourceUrls;
    }

}
