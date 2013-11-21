package org.jdownloader.gui.views.linkgrabber.bottombar;

import org.jdownloader.gui.IconKey;

public class ClearLinkgrabberAction extends GenericDeleteFromLinkgrabberAction {

    public ClearLinkgrabberAction() {

    }

    @Override
    protected void initContextDefaults() {
        super.initContextDefaults();
        setDeleteAll(true);
        includedSelection.setIncludeSelectedLinks(true);
        includedSelection.setIncludeUnselectedLinks(true);
        setCancelLinkcrawlerJobs(true);
        setClearSearchFilter(true);
        setClearFilteredLinks(true);
        setResetTableSorter(true);
        setIconKey(IconKey.ICON_RESET);
        setTooltipText(super.createName());

    }

    @Override
    protected String createName() {
        return null;
    }
}
