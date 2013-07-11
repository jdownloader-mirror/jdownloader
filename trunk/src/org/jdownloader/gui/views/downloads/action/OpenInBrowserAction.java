package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class OpenInBrowserAction extends SelectionAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 7911375550836173693L;
    private static final int  MAX_LINKS        = 4;

    public OpenInBrowserAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);

        setIconKey("browse");
        setName(_GUI._.gui_table_contextmenu_browselink());
    }

    @Override
    public boolean isEnabled() {
        List<DownloadLink> links = getSelection().getChildren();
        if (links.size() > MAX_LINKS) return false;
        if (!CrossSystem.isOpenBrowserSupported()) return false;
        for (DownloadLink link : links) {
            if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) return true;
            if (link.getLinkType() == DownloadLink.LINKTYPE_CONTAINER && link.gotBrowserUrl()) return true;
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        if (this.isEnabled()) { // additional security measure. Someone may call
                                // actionPerformed in the code although the
                                // action should be disabled
            for (DownloadLink link : getSelection().getChildren()) {
                CrossSystem.openURLOrShowMessage(link.getBrowserUrl());
            }
        }
    }

}