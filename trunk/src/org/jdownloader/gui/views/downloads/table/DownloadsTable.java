package org.jdownloader.gui.views.downloads.table;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import jd.event.ControlEvent;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;
import jd.utils.JDUtilities;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.OffScreenException;
import org.appwork.utils.swing.dialog.SimpleTextBallon;
import org.appwork.utils.swing.table.ExtColumn;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.linktable.LinkTable;
import org.jdownloader.gui.views.downloads.DownloadTableAction;
import org.jdownloader.gui.views.downloads.context.CheckStatusAction;
import org.jdownloader.gui.views.downloads.context.CopyPasswordAction;
import org.jdownloader.gui.views.downloads.context.CopyURLAction;
import org.jdownloader.gui.views.downloads.context.CreateDLCAction;
import org.jdownloader.gui.views.downloads.context.DeleteAction;
import org.jdownloader.gui.views.downloads.context.DeleteFromDiskAction;
import org.jdownloader.gui.views.downloads.context.DisableAction;
import org.jdownloader.gui.views.downloads.context.EditLinkOrPackageAction;
import org.jdownloader.gui.views.downloads.context.EnableAction;
import org.jdownloader.gui.views.downloads.context.ForceDownloadAction;
import org.jdownloader.gui.views.downloads.context.NewPackageAction;
import org.jdownloader.gui.views.downloads.context.OpenDirectoryAction;
import org.jdownloader.gui.views.downloads.context.OpenFileAction;
import org.jdownloader.gui.views.downloads.context.OpenInBrowserAction;
import org.jdownloader.gui.views.downloads.context.PackageDirectoryAction;
import org.jdownloader.gui.views.downloads.context.PackageNameAction;
import org.jdownloader.gui.views.downloads.context.PriorityAction;
import org.jdownloader.gui.views.downloads.context.RatedMenuController;
import org.jdownloader.gui.views.downloads.context.RatedMenuItem;
import org.jdownloader.gui.views.downloads.context.ResetAction;
import org.jdownloader.gui.views.downloads.context.ResumeAction;
import org.jdownloader.gui.views.downloads.context.SetPasswordAction;
import org.jdownloader.gui.views.downloads.context.StopsignAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class DownloadsTable extends LinkTable {

    private static final long serialVersionUID = 8843600834248098174L;

    private Color             sortNotifyColor;

    public DownloadsTable(final DownloadsTableModel tableModel) {
        super(tableModel);

        if (Application.getJavaVersion() >= Application.JAVA16) {
            this.setTransferHandler(new DownloadsTableTransferHandler(this));
            this.setDragEnabled(true);
            this.setDropMode(DropMode.ON_OR_INSERT_ROWS);
        }

        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isSortColumnHighlightEnabled()) {
            sortNotifyColor = Color.ORANGE;
        }
        initActions();
        onSelectionChanged(null);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    @Override
    protected void onSelectionChanged(ArrayList<PackageLinkNode> selected) {
        if (selected == null || selected.size() == 0) {
            // disable move buttons
            moveDownAction.setEnabled(false);
            moveToBottomAction.setEnabled(false);
            moveTopAction.setEnabled(false);
            moveUpAction.setEnabled(false);
        } else {
            // TODO
            moveDownAction.setEnabled(true);
            moveToBottomAction.setEnabled(true);
            moveTopAction.setEnabled(true);
            moveUpAction.setEnabled(true);
        }
    }

    @Override
    protected boolean processKeyBinding(KeyStroke stroke, KeyEvent evt, int condition, boolean pressed) {
        if (!pressed) { return super.processKeyBinding(stroke, evt, condition, pressed); }

        switch (evt.getKeyCode()) {

        case KeyEvent.VK_UP:
            if (evt.isAltDown()) {
                this.moveUpAction.actionPerformed(null);
                return true;
            }
            break;
        case KeyEvent.VK_DOWN:
            if (evt.isAltDown()) {
                this.moveDownAction.actionPerformed(null);
                return true;
            }
            break;

        case KeyEvent.VK_HOME:
            if (evt.isAltDown()) {
                moveTopAction.actionPerformed(null);
                return true;

            }
            break;
        case KeyEvent.VK_END:
            if (evt.isAltDown()) {
                moveToBottomAction.actionPerformed(null);
                return true;
            }
            break;
        }

        return super.processKeyBinding(stroke, evt, condition, pressed);
    }

    private void initActions() {
        moveTopAction = new DownloadTableAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_totop());
                setToolTipText(_GUI._.BottomBar_BottomBar_totop_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-top", 18));

            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled();
            }

            public void actionPerformed(ActionEvent e) {
                System.out.println("go-top");
            }

        };
        moveUpAction = new DownloadTableAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_moveup());
                setToolTipText(_GUI._.BottomBar_BottomBar_moveup_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-up", 18));

            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled();
            }

            public void actionPerformed(ActionEvent e) {
                System.out.println("go-up");
            }

        };
        moveDownAction = new DownloadTableAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_movedown());
                setToolTipText(_GUI._.BottomBar_BottomBar_movedown_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-down", 18));

            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled();
            }

            public void actionPerformed(ActionEvent e) {
                System.out.println("go-down");
            }

        };
        moveToBottomAction = new DownloadTableAction() {
            {
                // setName(_GUI._.BottomBar_BottomBar_tobottom());
                setToolTipText(_GUI._.BottomBar_BottomBar_tobottom_tooltip());
                setSmallIcon(NewTheme.I().getIcon("go-bottom", 18));

            }

            @Override
            public boolean isEnabled() {
                return super.isEnabled();
            }

            public void actionPerformed(ActionEvent e) {
                System.out.println("go-bottom");
            }

        };
    }

    private DownloadTableAction moveTopAction;
    private DownloadTableAction moveUpAction;
    private DownloadTableAction moveDownAction;
    private DownloadTableAction moveToBottomAction;

    public AppAction getMoveDownAction() {
        return moveDownAction;
    }

    public AppAction getMoveTopAction() {
        return moveTopAction;
    }

    public AppAction getMoveToBottomAction() {
        return moveToBottomAction;
    }

    public AppAction getMoveUpAction() {
        return moveUpAction;
    }

    @Override
    protected void onDoubleClick(final MouseEvent e, final PackageLinkNode obj) {

        new EditLinkOrPackageAction(this, obj).actionPerformed(null);
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, final PackageLinkNode contextObject, final ArrayList<PackageLinkNode> selection, ExtColumn<PackageLinkNode> column) {
        /* split selection into downloadlinks and filepackages */
        popup = super.onContextMenu(popup, contextObject, selection, column);
        popup.add(new JSeparator());
        popup.add(new EditLinkOrPackageAction(this, contextObject));
        return popup;
    }

    @Override
    protected boolean onShortcutDelete(final ArrayList<PackageLinkNode> selectedObjects, final KeyEvent evt, final boolean direct) {
        new DeleteAction(getAllDownloadLinks(selectedObjects), direct).actionPerformed(null);
        return true;
    }

    /**
     * Creates all contextmenu items, and a initial rating for it.
     * 
     * @param obj
     * @param col
     * @param alllinks
     * @param sfp
     * @return
     */
    @Override
    protected RatedMenuController createMenuItems(PackageLinkNode obj, int col, ArrayList<DownloadLink> alllinks, ArrayList<FilePackage> sfp) {
        final RatedMenuController ret = new RatedMenuController();

        ret.add(new RatedMenuItem(new StopsignAction(obj), 0));
        ret.add(new RatedMenuItem(new EnableAction(alllinks), 10));
        ret.add(new RatedMenuItem(new DisableAction(alllinks), 10));
        ret.add(new RatedMenuItem(new ForceDownloadAction(alllinks), 0));
        ret.add(new RatedMenuItem(new ResumeAction(alllinks), 10));

        ret.add(new RatedMenuItem(new ResetAction(alllinks), 5));
        ret.add(RatedMenuItem.createSeparator());

        ret.add(new RatedMenuItem(new NewPackageAction(alllinks), 0));
        ret.add(new RatedMenuItem(new CheckStatusAction(alllinks), 0));
        ret.add(new RatedMenuItem(new CreateDLCAction(alllinks), 0));
        ret.add(new RatedMenuItem(new CopyURLAction(alllinks), 0));
        ret.add(RatedMenuItem.createSeparator());

        ret.add(new RatedMenuItem(new SetPasswordAction(alllinks), 0));
        ret.add(new RatedMenuItem(new CopyPasswordAction(alllinks), 0));
        ret.add(new RatedMenuItem(ActionController.getToolBarAction("action.passwordlist"), 0));
        ret.add(new RatedMenuItem(new DeleteAction(alllinks), 0));
        ret.add(new RatedMenuItem(new DeleteFromDiskAction(alllinks), 0));

        ret.add(RatedMenuItem.createSeparator());
        if (obj instanceof FilePackage) {
            ret.add(new RatedMenuItem(new OpenDirectoryAction(new File(((FilePackage) obj).getDownloadDirectory())), 0));

            /* TODO: sort action */
            // final JDTableColumn column =
            // this.getJDTableModel().getJDTableColumn(col);
            // if (column.isSortable(sfp)) {
            // this.getDefaultSortMenuItem().set(column, sfp,
            // _GUI._.gui_table_contextmenu_packagesort() + " (" + sfp.size() +
            // "), (" + this.getJDTableModel().getColumnName(col) + ")");
            // ret.add(new RatedMenuItem("SORTITEM",
            // this.getDefaultSortMenuItem(), 0));
            // }

            ret.add(new RatedMenuItem(new PackageNameAction(sfp), 0));
            ret.add(new RatedMenuItem(new PackageDirectoryAction(sfp), 0));
        } else if (obj instanceof DownloadLink) {
            ret.add(new RatedMenuItem(new OpenDirectoryAction(new File(((DownloadLink) obj).getFileOutput()).getParentFile()), 0));
            ret.add(new RatedMenuItem(new OpenInBrowserAction(alllinks), 0));
            /*
             * check if Java version 1.6 or higher is installed, because the
             * Desktop-Class (e.g. to open a file with correct application) is
             * only supported by v1.6 or higher
             */
            if (Application.getJavaVersion() >= 16000000 && CrossSystem.isOpenFileSupported()) {
                // add the Open File entry
                ret.add(new RatedMenuItem(new OpenFileAction(new File(((DownloadLink) obj).getFileOutput())), 0));
            }
        }
        ret.add(RatedMenuItem.createSeparator());
        final ArrayList<MenuAction> entries = new ArrayList<MenuAction>();
        JDUtilities.getController().fireControlEventDirect(new ControlEvent(obj, ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU, entries));
        if (entries != null && entries.size() > 0) {
            for (final MenuAction next : entries) {
                if (next.getType() == ToolBarAction.Types.SEPARATOR) {
                    ret.add(RatedMenuItem.createSeparator());
                } else {
                    ret.add(new RatedMenuItem(next, 0));
                }
            }
        }
        ret.add(RatedMenuItem.createSeparator());
        ret.add(new RatedMenuItem("PRIORITY", PriorityAction.createPrioMenu(alllinks), 0));
        return ret;
    }

    @Override
    public boolean editCellAt(int row, int column) {

        boolean ret = super.editCellAt(row, column);

        return ret;
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        boolean ret = super.editCellAt(row, column, e);
        if (ret) {

            PackageLinkNode object = getExtTableModel().getObjectbyRow(row);
            if (object instanceof FilePackage) {
                String title = _GUI._.DownloadsTable_editCellAt_filepackage_title();
                String msg = _GUI._.DownloadsTable_editCellAt_filepackage_msg();
                ImageIcon icon = NewTheme.I().getIcon("wizard", 32);
                JDGui.help(title, msg, icon);

            }

        }
        return ret;
    }

    @Override
    protected void onHeaderSortClick(final MouseEvent e1, final ExtColumn<PackageLinkNode> oldSortColumn, String oldSortId) {

        // own thread to
        new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Timer t = (Timer) e.getSource();
                t.stop();
                if (oldSortColumn == getExtTableModel().getSortColumn()) return;
                if (getExtTableModel().getSortColumn() != null) {

                    try {

                        SimpleTextBallon d = new SimpleTextBallon(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.DownloadsTable_actionPerformed_sortwarner_title(getExtTableModel().getSortColumn().getName()), _GUI._.DownloadsTable_actionPerformed_sortwarner_text(), NewTheme.I().getIcon("sort", 32)) {

                            @Override
                            protected String getDontShowAgainKey() {
                                return "downloadtabe_sortwarner";
                            }

                        };
                        d.setDesiredLocation(e1.getLocationOnScreen());
                        Dialog.getInstance().showDialog(d);
                    } catch (OffScreenException e1) {
                        e1.printStackTrace();
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }

                }

            }
        }).start();

    }

    public boolean isDownloadOrder() {
        return getExtTableModel().getSortColumn() == null;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        Composite comp = g2.getComposite();
        final Rectangle visibleRect = this.getVisibleRect();
        Rectangle first;
        ExtColumn<PackageLinkNode> sortColumn = getExtTableModel().getSortColumn();
        if (sortColumn == null) return;
        int index = sortColumn.getIndex();

        if (index < 0) return;

        if (sortNotifyColor != null) {

            first = this.getCellRect(0, index, true);

            g2.setColor(Color.ORANGE);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            g2.fillRect(visibleRect.x + first.x, visibleRect.y, visibleRect.x + getExtTableModel().getSortColumn().getWidth(), visibleRect.y + visibleRect.height);
        }
        if (isDownloadOrder()) return;
        g2.setComposite(comp);

    }

}
