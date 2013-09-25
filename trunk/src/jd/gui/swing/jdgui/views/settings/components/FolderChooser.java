package jd.gui.swing.jdgui.views.settings.components;

import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.views.settings.panels.packagizer.VariableAction;

import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.pathchooser.PathChooser;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.DownloadPath;
import org.jdownloader.translate._JDT;

public class FolderChooser extends PathChooser implements SettingsComponent {
    /**
     * 
     */
    private static final long                     serialVersionUID = 1L;

    private StateUpdateEventSender<FolderChooser> eventSender;
    private boolean                               setting;

    public FolderChooser() {
        super("FolderChooser", true);

        eventSender = new StateUpdateEventSender<FolderChooser>();

        this.txt.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<FolderChooser>(FolderChooser.this));
            }

            public void insertUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<FolderChooser>(FolderChooser.this));
            }

            public void changedUpdate(DocumentEvent e) {
                if (!setting) eventSender.fireEvent(new StateUpdateEvent<FolderChooser>(FolderChooser.this));
            }
        });
        this.destination.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                destination.setList(DownloadPath.loadList(getPath()));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {

            }
        });
        // CFG_LINKGRABBER.DOWNLOAD_DESTINATION_HISTORY.getEventSender().addListener(, true);
    }

    @Override
    public JPopupMenu getPopupMenu(ExtTextField txt, AbstractAction cutAction, AbstractAction copyAction, AbstractAction pasteAction, AbstractAction deleteAction, AbstractAction selectAction) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new VariableAction(txt, _GUI._.PackagizerFilterRuleDialog_createVariablesMenu_date(), "<jd:" + PackagizerController.SIMPLEDATE + ":dd.MM.yyyy>"));
        menu.add(new VariableAction(txt, _GUI._.PackagizerFilterRuleDialog_createVariablesMenu_packagename(), "<jd:" + PackagizerController.PACKAGENAME + ">"));

        return menu;
    }

    public String getConstraints() {
        return null;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        eventSender.addListener(listener);
    }

    public void setText(String t) {
        setting = true;
        try {
            super.setPath(t);
        } finally {
            setting = false;
        }
    }

    @Override
    public File doFileChooser() {
        File ret;
        try {
            ret = DownloadFolderChooserDialog.open(new File(txt.getText()), true, _JDT._.gui_setting_folderchooser_title());
            return ret;
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isMultiline() {
        return false;
    }

    public String getText() {
        File file = getFile();
        file = checkPath(file);
        if (file == null) return null;
        DownloadPath.saveList(file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    public static File checkPath(File file) {
        String path = file.getAbsolutePath();
        File checkPath = file;
        int index = path.indexOf("<jd:");
        if (index >= 0) {
            path = path.substring(0, index);
            checkPath = new File(path);
        }
        File forbidden = null;
        if ((forbidden = DownloadWatchDog.getInstance().validateDestination(checkPath)) != null) {
            UIOManager.I().showErrorMessage(_GUI._.DownloadFolderChooserDialog_handleNonExistingFolders_couldnotcreatefolder(forbidden.getAbsolutePath()));
            return null;
        }
        if (DownloadFolderChooserDialog.handleNonExistingFolders(checkPath) == false) return null;
        return file;
    }
}
