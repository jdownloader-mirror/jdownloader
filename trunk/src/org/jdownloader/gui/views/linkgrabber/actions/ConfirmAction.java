package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.PackageLinkNode;

import org.appwork.utils.logging.Log;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmAction extends AbstractAction {
    private PackageLinkNode value;

    public ConfirmAction() {
        putValue(SMALL_ICON, NewTheme.I().getIcon("media-playback-start", 16));
        putValue(SHORT_DESCRIPTION, _GUI._.ConfirmAction_ConfirmAction_tooltip());

    }

    public void actionPerformed(ActionEvent e) {
        Log.L.finer("Start " + value);
    }

    public void setValue(PackageLinkNode value) {
        this.value = value;
    }

}
