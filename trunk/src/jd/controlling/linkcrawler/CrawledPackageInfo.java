package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

public class CrawledPackageInfo implements AbstractPackageNode<CrawledLinkInfo, CrawledPackageInfo> {

    private ArrayList<CrawledLinkInfo>                             children         = new ArrayList<CrawledLinkInfo>();
    private PackageController<CrawledPackageInfo, CrawledLinkInfo> controller       = null;
    private boolean                                                expanded         = true;
    private String                                                 autoPackageName  = null;
    private boolean                                                allowAutoPackage = true;

    /**
     * @return the allowAutoPackage
     */
    public boolean isAllowAutoPackage() {
        return allowAutoPackage;
    }

    /**
     * @param allowAutoPackage
     *            the allowAutoPackage to set
     */
    public void setAllowAutoPackage(boolean allowAutoPackage) {
        this.allowAutoPackage = allowAutoPackage;
    }

    public CrawledPackageInfo() {

    }

    /**
     * @return the autoPackageName
     */
    public String getAutoPackageName() {
        return autoPackageName;
    }

    /**
     * @param autoPackageName
     *            the autoPackageName to set
     */
    public void setAutoPackageName(String autoPackageName) {
        this.autoPackageName = autoPackageName;
    }

    private String customName = null;
    private long   created    = -1;

    /**
     * @param created
     *            the created to set
     */
    public void setCreated(long created) {
        this.created = created;
    }

    public PackageController<CrawledPackageInfo, CrawledLinkInfo> getControlledBy() {
        return controller;
    }

    public void setControlledBy(PackageController<CrawledPackageInfo, CrawledLinkInfo> controller) {
        this.controller = controller;
    }

    public List<CrawledLinkInfo> getChildren() {
        return children;
    }

    public void notifyChanges() {
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean b) {
        this.expanded = b;
    }

    public String getName() {
        if (customName != null) return customName;
        return autoPackageName;
    }

    public boolean isEnabled() {
        return false;
    }

    public long getCreated() {
        return created;
    }

    public long getFinishedDate() {
        return 0;
    }

}