//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.components.treetable;

import java.awt.Color;
import java.awt.Component;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.DropMode;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.ClipboardHandler;
import jd.event.ControlEvent;
import jd.gui.skins.simple.DownloadInfo;
import jd.gui.skins.simple.DownloadLinksTreeTablePanel;
import jd.gui.skins.simple.DownloadLinksView;
import jd.gui.skins.simple.JDAction;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JLinkButton;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.PainterHighlighter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.tree.TreeModelSupport;

public class DownloadTreeTable extends JXTreeTable implements TreeWillExpandListener, TreeExpansionListener, TreeSelectionListener, MouseListener, ActionListener, MouseMotionListener, KeyListener {

    public static final String PROPERTY_EXPANDED = "expanded";

    public static final String PROPERTY_SELECTED = "selected";

    private static final long serialVersionUID = 1L;

    private static final long UPDATE_INTERVAL = 200;

    private SubConfiguration guiConfig = null;

    private TableCellRenderer cellRenderer;

    private DownloadLink currentLink;

    private long ignoreSelectionsAndExpansionsUntil;

    private Logger logger = JDUtilities.getLogger();

    private DownloadTreeTableModel model;

    private int mouseOverColumn = -1;

    public int mouseOverRow = -1;

    private Point mousePoint = null;

    private Timer timer;

    private TreeTableTransferHandler transferHandler;

    private HashMap<FilePackage, ArrayList<DownloadLink>> updatePackages;

    private long updateTimer = 0;

    private HashMap<DownloadLink, DownloadInfo> dlInfoWindows = new HashMap<DownloadLink, DownloadInfo>();

    private long lastMouseClicked = 0;

    private boolean usedoubleclick = true;

    private boolean updatelock = false;

    private Color SELECTED_ROW_COLOR;

    private TableColumnExt[] cols;

    private DownloadLinksTreeTablePanel panel;

    public DownloadTreeTable(DownloadTreeTableModel treeModel, DownloadLinksTreeTablePanel panel) {
        super(treeModel);
        this.panel = panel;
        guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        cellRenderer = new TreeTableRenderer(this);

        // setTreeCellRenderer(treeCellRenderer);
        // this.setHighlighters(new Highlighter[] { hl });
        // this.setModel(treeModel)
        model = treeModel;

        createColumns();
        // this.setUI(new TreeTablePaneUI());
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        // this.setExpandsSelectedPaths(true);
        setToggleClickCount(1);
        if (JDUtilities.getJavaVersion() >= 1.6) {
            setDropMode(DropMode.ON_OR_INSERT_ROWS);
        }
        setDragEnabled(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setColumnControlVisible(true);
        this.setColumnControl(new JColumnControlButton(this));

        usedoubleclick = guiConfig.getBooleanProperty(SimpleGUI.PARAM_DCLICKPACKAGE, false);
        setEditable(false);
        setAutoscrolls(false);
        addTreeExpansionListener(this);
        addTreeSelectionListener(this);
        addTreeWillExpandListener(this);
        addMouseListener(this);
        addKeyListener(this);
        addMouseMotionListener(this);
        UIManager.put("Table.focusCellHighlightBorder", null);
        setTransferHandler(transferHandler = new TreeTableTransferHandler(this));
        if (JDUtilities.getJavaVersion() > 1.6) {
            setDropMode(DropMode.USE_SELECTION);
        }

        this.setHighlighters(new Highlighter[] {});
//        setHighlighters(HighlighterFactory.createAlternateStriping(UIManager.getColor("Panel.background").brighter(), UIManager.getColor("Panel.background")));

        // addHighlighter(new ColorHighlighter(HighlightPredicate.ALWAYS,
        // JDTheme.C("gui.color.downloadlist.row_package", "fffa7c"),
        // Color.BLACK));

        addFinishedHighlighter();
        addDisabledHighlighter();
        addPostErrorHighlighter();
        addWaitHighlighter();
        addErrorHighlighter();

        // addHighlighter(new FilepackageRowHighlighter(this, Color.RED,
        // Color.BLUE, Color.RED, Color.BLUE) {
        // @Override
        // public boolean doHighlight(FilePackage fp) {
        // return true;
        // }
        // });
        addHighlighter(new PainterHighlighter(HighlightPredicate.IS_FOLDER, getFolderPainter()));

        // Highlighter extendPrefWidth = new AbstractHighlighter() {
        // @Override
        // protected Component doHighlight(Component component, ComponentAdapter
        // adapter) {
        // Dimension dim = component.getPreferredSize();
        // int width = 600;
        // dim.width = Math.max(dim.width, width);
        // component.setPreferredSize(dim);
        // return component;
        //
        // }
        // };
        //
        // addHighlighter(extendPrefWidth);
        // ATTENTION >=1.6
        /**
         * correct paint errors in JXTreeTable due to annimation over the first
         * row. The first row is a tree and thus does not implement
         * PainterHighlighter. Without modding the TreeTable code, it seems
         * unpossible to fix this.
         */
        if (JDUtilities.getJavaVersion() >= 1.6) {

        } else {
            /**
             * Set here colors if java version is below 1.6 and substance cannot
             * be used
             */
            // addHighlighter(new
            // ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, Color.GRAY,
            // Color.BLACK));
        }

    }

    public Painter getFolderPainter() {

        int width = 100;
        int height = 20;
        Color col = JDTheme.C("gui.color.downloadlist.row_package", "fffa7c").darker();
        Color col1 = new Color(col.getRed(), col.getGreen(), col.getBlue(), 220);
        Color col2 = new Color(col.getRed(), col.getGreen(), col.getBlue(), 80);
        Color col3 = new Color(col.getRed(), col.getGreen(), col.getBlue(), 20);
        Color col4 = new Color(col.getRed(), col.getGreen(), col.getBlue(), 80);
        Color col5 = new Color(col.getRed(), col.getGreen(), col.getBlue(), 220);
        LinearGradientPaint gradientPaint = new LinearGradientPaint(new Point(1, 0), new Point(1, height), new float[] { 0.0f, 0.2f, 0.40f,0.7f, 1.0f }, new Color[] { col1, col2,col3, col4, col5 });

        return new MattePainter(gradientPaint);

    }

    /**
     * Link HIghlighters
     */
    private void addWaitHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff9936",100);


        addHighlighter(new DownloadLinkRowHighlighter(this, background,background) {
            @Override
            public boolean doHighlight(DownloadLink dLink) {
                return dLink.getLinkStatus().getRemainingWaittime() > 0 || dLink.getPlugin() == null || dLink.getPlugin().getRemainingHosterWaittime() > 0;
            }
        });

    }

    private void addErrorHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff9936",120);


        addHighlighter(new DownloadLinkRowHighlighter(this, background,background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                return link.getLinkStatus().isFailed();
            }
        });

    }

    private void addPostErrorHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.error_post", "ff9936",120);
 

        addHighlighter(new DownloadLinkRowHighlighter(this, background,background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                return link.getLinkStatus().hasStatus(LinkStatus.ERROR_POST_PROCESS);
            }
        });

    }

    private void addDisabledHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.row_link_disabled", "adadad",100);
  
        addHighlighter(new DownloadLinkRowHighlighter(this, background,background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                return !link.isEnabled();
            }
        });

    }

    private void addFinishedHighlighter() {
        Color background = JDTheme.C("gui.color.downloadlist.row_link_done", "c4ffd2",80);
        Color backGroundPackage = JDTheme.C("gui.color.downloadlist.row_package_done", "339933",80);

        addHighlighter(new FilepackageRowHighlighter(this, backGroundPackage ,backGroundPackage) {
            @Override
            public boolean doHighlight(FilePackage fp) {
                return fp.isFinished();
            }
        });

        addHighlighter(new DownloadLinkRowHighlighter(this, background,background) {
            @Override
            public boolean doHighlight(DownloadLink link) {
                return link.getLinkStatus().hasStatus(LinkStatus.FINISHED);
            }
        });



    }

    public TableCellRenderer getCellRenderer(int row, int col) {
        return cellRenderer;
        // if (col >= 1) { return cellRenderer; }
        // return super.getCellRenderer(row, col);

    }

    private void createColumns() {
        // TODO Auto-generated method stub
        setAutoCreateColumnsFromModel(false);
        List<TableColumn> columns = getColumns(true);
        for (Iterator<TableColumn> iter = columns.iterator(); iter.hasNext();) {
            getColumnModel().removeColumn(iter.next());

        }

        final SubConfiguration config = JDUtilities.getSubConfig("gui");
        cols = new TableColumnExt[getModel().getColumnCount()];
        for (int i = 0; i < getModel().getColumnCount(); i++) {

            TableColumnExt tableColumn = getColumnFactory().createAndConfigureTableColumn(getModel(), i);
            cols[i] = tableColumn;
            if (i > 0) {
                tableColumn.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        TableColumnExt column = (TableColumnExt) evt.getSource();
                        if (evt.getPropertyName().equals("width")) {
                            config.setProperty("WIDTH_COL_" + column.getModelIndex(), evt.getNewValue());
                            config.save();
                        } else if (evt.getPropertyName().equals("visible")) {
                            config.setProperty("VISABLE_COL_" + column.getModelIndex(), evt.getNewValue());
                            config.save();
                        }
                    }
                });

                tableColumn.setVisible(config.getBooleanProperty("VISABLE_COL_" + i, true));
                tableColumn.setPreferredWidth(config.getIntegerProperty("WIDTH_COL_" + i, tableColumn.getWidth()));
                if (tableColumn != null) {
                    getColumnModel().addColumn(tableColumn);
                }
            } else {
                tableColumn.setVisible(false);
            }
        }

    }

    public TableColumnExt[] getCols() {
        return cols;
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        DownloadLink link;
        FilePackage fp;
        Vector<DownloadLink> links;
        Vector<FilePackage> fps;
        FilePackage next;
        String pw;
        HashMap<String, Object> prop;
        Integer prio;
        // boolean[] res;

        switch (e.getID()) {
        case TreeTableAction.SET_PW:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("links");
            pw = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.setpw.message", "Set download password"), null);
            for (int i = 0; i < links.size(); i++) {
                links.elementAt(i).setProperty("pass", pw);
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            break;
        case TreeTableAction.DOWNLOAD_INFO:
            links = getSelectedDownloadLinks();
            for (DownloadLink tmpLink : links) {
                if (dlInfoWindows.get(tmpLink) == null) {
                    dlInfoWindows.put(tmpLink, new DownloadInfo(SimpleGUI.CURRENTGUI.getFrame(), tmpLink));
                } else {
                    if (dlInfoWindows.get(tmpLink).isVisible() == false) dlInfoWindows.put(tmpLink, new DownloadInfo(SimpleGUI.CURRENTGUI.getFrame(), tmpLink));
                }
            }
            break;
        case TreeTableAction.DOWNLOAD_BROWSE_LINK:
            link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
            if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) {
                try {
                    JLinkButton.openURL(link.getBrowserUrl());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

            break;
        case TreeTableAction.DOWNLOAD_DOWNLOAD_DIR:
            link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
            JDUtilities.openExplorer(new File(link.getFileOutput()).getParentFile());
            break;
        case TreeTableAction.DELETE:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("links");

            if (!guiConfig.getBooleanProperty(SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?") + " (" + links.size() + ")")) {
                    JDUtilities.getController().removeDownloadLinks(links);
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
                }
            } else {
                JDUtilities.getController().removeDownloadLinks(links);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
            }
            break;
        case TreeTableAction.DOWNLOAD_ENABLE:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
            for (int i = 0; i < links.size(); i++) {
                links.elementAt(i).setEnabled(true);
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            break;
        case TreeTableAction.DOWNLOAD_PRIO:
            prop = (HashMap<String, Object>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("infos");
            links = (Vector<DownloadLink>) prop.get("downloadlinks");
            prio = (Integer) prop.get("prio");
            for (int i = 0; i < links.size(); i++) {
                links.elementAt(i).setPriority(prio);
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            break;
        case TreeTableAction.DOWNLOAD_RESUME:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
            for (int i = 0; i < links.size(); i++) {
                links.elementAt(i).getLinkStatus().setStatus(LinkStatus.TODO);
                links.elementAt(i).getLinkStatus().setStatusText(JDLocale.L("gui.linklist.status.doresume", "Warte auf Fortsetzung"));
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            break;
        case TreeTableAction.DOWNLOAD_DISABLE:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
            for (int i = 0; i < links.size(); i++) {
                links.elementAt(i).setEnabled(false);
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            break;
        case TreeTableAction.DOWNLOAD_DLC:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
            JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
            fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
            fc.showSaveDialog(SimpleGUI.CURRENTGUI.getFrame());
            File ret = fc.getSelectedFile();
            if (ret == null) { return; }
            if (JDIO.getFileExtension(ret) == null || !JDIO.getFileExtension(ret).equalsIgnoreCase("dlc")) {
                ret = new File(ret.getAbsolutePath() + ".dlc");
            }
            JDUtilities.getController().saveDLC(ret, links);
            break;
        case TreeTableAction.DOWNLOAD_NEW_PACKAGE:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
            FilePackage parentFP = links.get(0).getFilePackage();
            String name = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.newpackage.message", "Name of the new package"), parentFP.getName());
            if (name != null) {
                JDUtilities.getController().removeDownloadLinks(links);
                FilePackage nfp = new FilePackage();
                nfp.setName(name);
                nfp.setDownloadDirectory(parentFP.getDownloadDirectory());
                nfp.setPassword(parentFP.getPassword());
                nfp.setComment(parentFP.getComment());

                for (int i = 0; i < links.size(); i++) {
                    links.elementAt(i).setFilePackage(nfp);
                }
                JDUtilities.getController().addAllLinks(links);
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
            break;
        case TreeTableAction.DOWNLOAD_RESET:
            links = (Vector<DownloadLink>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlinks");
            if (!guiConfig.getBooleanProperty(SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.reset", "Reset selected downloads?"))) {
                    for (int i = 0; i < links.size(); i++) {
                        // if (!links.elementAt(i).isPluginActive()) {
                        links.elementAt(i).reset();
                    }
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                }
            } else {
                for (int i = 0; i < links.size(); i++) {
                    // if (!links.elementAt(i).isPluginActive()) {
                    links.elementAt(i).reset();
                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            }

            break;
        case TreeTableAction.DOWNLOAD_COPY_PASSWORD:
            link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(link.getFilePackage().getPassword()), null);
            break;
        case TreeTableAction.DOWNLOAD_COPY_URL:
            link = (DownloadLink) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("downloadlink");
            ClipboardHandler.getClipboard().setOldData(link.getBrowserUrl());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(link.getBrowserUrl()), null);
            break;
        case TreeTableAction.PACKAGE_INFO:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            // new PackageInfo(SimpleGUI.CURRENTGUI.getFrame(), fp);

            break;
        case TreeTableAction.PACKAGE_EDIT_DIR:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");

            fc = new JDFileChooser();
            fc.setApproveButtonText(JDLocale.L("gui.btn_ok", "OK"));
            fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);

            fc.setCurrentDirectory(fp.getDownloadDirectory() != null ? new File(fp.getDownloadDirectory()) : JDUtilities.getResourceFile("downloads"));
            if (fc.showOpenDialog(this) == JDFileChooser.APPROVE_OPTION) {
                ret = fc.getSelectedFile();
                if (ret != null) {
                    fp.setDownloadDirectory(ret.getAbsolutePath());
                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            }
            break;
        case TreeTableAction.PACKAGE_EDIT_NAME:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            name = SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L("gui.linklist.editpackagename.message", "Neuer Paketname"), fp.getName());

            if (name != null) {
                fp.setName(name);
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            break;
        case TreeTableAction.PACKAGE_DOWNLOAD_DIR:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            JDUtilities.openExplorer(new File(fp.getDownloadDirectory()));
            break;
        case TreeTableAction.PACKAGE_ENABLE:
            fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

            for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                next = it.next();
                for (int i = 0; i < next.size(); i++) {
                    next.get(i).setEnabled(true);
                }
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            break;
        case TreeTableAction.PACKAGE_PRIO:
            prop = (HashMap<String, Object>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("infos");
            fps = (Vector<FilePackage>) prop.get("packages");
            prio = (Integer) prop.get("prio");
            for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                next = it.next();
                for (int i = 0; i < next.size(); i++) {
                    next.get(i).setPriority(prio);
                }
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            break;
        case TreeTableAction.PACKAGE_DISABLE:
            fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");
            for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                next = it.next();
                for (int i = 0; i < next.size(); i++) {
                    next.get(i).setEnabled(false);
                }
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            break;
        case TreeTableAction.PACKAGE_DLC:
            links = new Vector<DownloadLink>();
            fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

            for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                next = it.next();
                for (int i = 0; i < next.size(); i++) {
                    links.add(next.get(i));
                }
            }
            fc = new JDFileChooser("_LOADSAVEDLC");
            fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
            fc.showSaveDialog(SimpleGUI.CURRENTGUI.getFrame());
            ret = fc.getSelectedFile();
            if (ret == null) { return; }
            if (JDIO.getFileExtension(ret) == null || !JDIO.getFileExtension(ret).equalsIgnoreCase("dlc")) {

                ret = new File(ret.getAbsolutePath() + ".dlc");
            }

            JDUtilities.getController().saveDLC(ret, links);
            break;
        case TreeTableAction.PACKAGE_RESET:
            if (!guiConfig.getBooleanProperty(SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.reset", "Reset selected downloads?"))) {
                    fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

                    for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                        next = it.next();
                        for (int i = 0; i < next.size(); i++) {
                            if (!next.get(i).getLinkStatus().isPluginActive()) {
                                next.get(i).getLinkStatus().setStatus(LinkStatus.TODO);
                                next.get(i).getLinkStatus().setStatusText("");
                                next.get(i).getPlugin().resetHosterWaitTime();
                                next.get(i).reset();
                            }
                        }
                    }
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
                }
            } else {
                fps = (Vector<FilePackage>) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("packages");

                for (Iterator<FilePackage> it = fps.iterator(); it.hasNext();) {
                    next = it.next();
                    for (int i = 0; i < next.size(); i++) {
                        if (!next.get(i).getLinkStatus().isPluginActive()) {
                            next.get(i).getLinkStatus().setStatus(LinkStatus.TODO);
                            next.get(i).getLinkStatus().setStatusText("");
                            next.get(i).getPlugin().resetHosterWaitTime();
                            next.get(i).reset();
                        }
                    }
                }
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED, this));
            }
            break;
        case TreeTableAction.PACKAGE_SORT:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            fp.sort(null);
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
            break;
        case TreeTableAction.PACKAGE_COPY_PASSWORD:
            fp = (FilePackage) ((TreeTableAction) ((JMenuItem) e.getSource()).getAction()).getProperty().getProperty("package");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(fp.getPassword()), null);
            break;
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void fireTableChanged(int id, Object param) {
        TreeModelSupport supporter = getDownladTreeTableModel().getModelSupporter();
        if (updatePackages == null) {
            updatePackages = new HashMap<FilePackage, ArrayList<DownloadLink>>();
        }
        switch (id) {
        /*
         * Es werden nur die Ãœbergebenen LinkPfade aktualisiert.
         * REFRESH_SPECIFIED_LINKS kann als Parameter eine Arraylist oder einen
         * einzellnen DownloadLink haben. ArrayLists werden nicht ausgewertet.
         * in diesem Fall wird die komplette Tabelle neu gezeichnet.
         */
        case DownloadLinksView.REFRESH_SPECIFIED_LINKS:
            // logger.info("REFRESH SPECS COMPLETE");
            if (param instanceof DownloadLink) {
                currentLink = (DownloadLink) param;
                // logger.info("Updatesingle "+currentLink);
                if (updatePackages.containsKey(currentLink.getFilePackage())) {
                    if (!updatePackages.get(currentLink.getFilePackage()).contains(currentLink)) {
                        updatePackages.get(currentLink.getFilePackage()).add(currentLink);
                    }
                } else {
                    ArrayList<DownloadLink> ar = new ArrayList<DownloadLink>();
                    updatePackages.put(currentLink.getFilePackage(), ar);
                    ar.add(currentLink);
                }

            } else if (param instanceof ArrayList) {
                for (Iterator<DownloadLink> it = ((ArrayList<DownloadLink>) param).iterator(); it.hasNext();) {
                    currentLink = it.next();
                    if (updatePackages.containsKey(currentLink.getFilePackage())) {
                        if (!updatePackages.get(currentLink.getFilePackage()).contains(currentLink)) {
                            updatePackages.get(currentLink.getFilePackage()).add(currentLink);
                        }
                    } else {
                        ArrayList<DownloadLink> ar = new ArrayList<DownloadLink>();
                        updatePackages.put(currentLink.getFilePackage(), ar);
                        ar.add(currentLink);
                    }
                }
            }
            if (System.currentTimeMillis() - updateTimer > UPDATE_INTERVAL && updatePackages != null) {
                Entry<FilePackage, ArrayList<DownloadLink>> next;
                DownloadLink next3;

                for (Iterator<Entry<FilePackage, ArrayList<DownloadLink>>> it2 = updatePackages.entrySet().iterator(); it2.hasNext();) {
                    next = it2.next();
                    // logger.info("Refresh " + next.getKey() + " - " +
                    // next.getValue().size());

                    if (!model.containesPackage(next.getKey())) {
                        continue;
                    }
                    supporter.firePathChanged(new TreePath(new Object[] { model.getRoot(), next.getKey() }));

                    if (next.getKey().getBooleanProperty(PROPERTY_EXPANDED, false)) {

                        int[] ind = new int[next.getValue().size()];
                        Object[] objs = new Object[next.getValue().size()];
                        int i = 0;

                        for (Iterator<DownloadLink> it3 = next.getValue().iterator(); it3.hasNext();) {
                            next3 = it3.next();
                            if (!next.getKey().contains(next3)) {
                                logger.warning("Dauniel bug");
                                continue;
                            }
                            ind[i] = next.getKey().indexOf(next3);
                            objs[i] = next3;

                            i++;
                            // logger.info(" children: " + next3 + " - " +
                            // ind[i]);
                        }

                        if (i > 0) {
                            supporter.fireChildrenChanged(new TreePath(new Object[] { model.getRoot(), next.getKey() }), ind, objs);
                        }
                    }

                }
                updatePackages = null;
                updateTimer = System.currentTimeMillis();
            }

            break;
        case DownloadLinksView.REFRESH_ALL_DATA_CHANGED:
            logger.info("Updatecomplete");
            supporter.fireChildrenChanged(new TreePath(model.getRoot()), null, null);

            break;
        case DownloadLinksView.REFRESH_DATA_AND_STRUCTURE_CHANGED:
            logger.info("REFRESH GUI COMPLETE");

            supporter.fireTreeStructureChanged(new TreePath(model.getRoot()));

            ignoreSelectionsAndExpansions(500);
            updateSelectionAndExpandStatus();
            // logger.info("finished");

            break;
        }

    }

    public DownloadTreeTableModel getDownladTreeTableModel() {
        return (DownloadTreeTableModel) getTreeTableModel();
    }

    public Vector<DownloadLink> getSelectedDownloadLinks() {
        int[] rows = getSelectedRows();
        Vector<DownloadLink> ret = new Vector<DownloadLink>();
        TreePath path;
        for (int element : rows) {
            path = getPathForRow(element);
            if (path != null && path.getLastPathComponent() instanceof DownloadLink) {
                ret.add((DownloadLink) path.getLastPathComponent());

            }
        }
        return ret;
    }

    public Vector<DownloadLink> getAllSelectedDownloadLinks() {
        Vector<DownloadLink> links = getSelectedDownloadLinks();
        Vector<FilePackage> fps = getSelectedFilePackages();
        for (FilePackage filePackage : fps) {
            links.addAll(filePackage.getDownloadLinks());
        }
        return links;
    }

    public Vector<FilePackage> getSelectedFilePackages() {
        int[] rows = getSelectedRows();
        Vector<FilePackage> ret = new Vector<FilePackage>();
        TreePath path;
        for (int element : rows) {
            path = getPathForRow(element);
            if (path != null && path.getLastPathComponent() instanceof FilePackage) {
                ret.add((FilePackage) path.getLastPathComponent());

            }
        }
        return ret;
    }

    void ignoreSelectionsAndExpansions(int i) {
        ignoreSelectionsAndExpansionsUntil = System.currentTimeMillis() + i;
    }

    void lastMouseClicked() {
        lastMouseClicked = System.currentTimeMillis();
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_DELETE) {

            Vector<DownloadLink> links = getSelectedDownloadLinks();
            Vector<FilePackage> fps = getSelectedFilePackages();

            if (fps.size() == 0 && links.size() == 0) { return; }

            for (Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                DownloadLink link = it.next();
                for (FilePackage filePackage : fps) {
                    if (filePackage.contains(link)) {
                        it.remove();
                        break;
                    }
                }

            }

            if (!guiConfig.getBooleanProperty(SimpleGUI.PARAM_DISABLE_CONFIRM_DIALOGS, false)) {
                if (SimpleGUI.CURRENTGUI.showConfirmDialog(JDLocale.L("gui.downloadlist.delete", "Ausgewählte Links wirklich entfernen?") + " (" + JDLocale.LF("gui.downloadlist.delete.size_package", "%s links in %s packages", links.size(), fps.size()) + ")")) {
                    // zuerst Pakete entfernen
                    for (FilePackage filePackage : fps) {
                        JDUtilities.getController().removePackage(filePackage);
                    }

                    JDUtilities.getController().removeDownloadLinks(links);
                    JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
                }
            } else {
                for (FilePackage filePackage : fps) {
                    JDUtilities.getController().removePackage(filePackage);
                }

                JDUtilities.getController().removeDownloadLinks(links);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, this));
            }

        } else if (e.getKeyCode() == KeyEvent.VK_UP && e.isControlDown()) {
            int cur = getSelectedRow();
            if (e.isAltDown()) {
                moveSelectedItems(JDAction.ITEMS_MOVE_TOP);
                getSelectionModel().setSelectionInterval(0, 0);
            } else {
                moveSelectedItems(JDAction.ITEMS_MOVE_UP);
                cur = Math.max(0, cur - 1);
                getSelectionModel().setSelectionInterval(cur, cur);
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && e.isControlDown()) {
            int cur = getSelectedRow();
            int len = getVisibleRowCount();
            if (e.isAltDown()) {
                moveSelectedItems(JDAction.ITEMS_MOVE_BOTTOM);
                getSelectionModel().setSelectionInterval(len, len);
            } else {
                moveSelectedItems(JDAction.ITEMS_MOVE_DOWN);
                cur = Math.min(len, cur + 1);
                getSelectionModel().setSelectionInterval(cur, cur);
            }
        }

    }

    public void keyTyped(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        ignoreSelectionsAndExpansions(-100);
        lastMouseClicked();

        if (e.getButton() == MouseEvent.BUTTON1 && 2 == e.getClickCount()) {
            TreePath path = getPathForRow(rowAtPoint(e.getPoint()));
            if (path == null) return;
            Object obj = path.getLastPathComponent();
            if (obj instanceof DownloadLink) {
                new DownloadInfo(SimpleGUI.CURRENTGUI.getFrame(), (DownloadLink) obj);
                panel.hideFilePackageInfo();
            } else if (obj instanceof FilePackage) {
                panel.showFilePackageInfo((FilePackage) obj);
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {

    }

    public void mouseMoved(MouseEvent e) {
        final int moRow = rowAtPoint(e.getPoint());
        final int moColumn = columnAtPoint(e.getPoint());
        mousePoint = e.getPoint();
        Point screen = getLocationOnScreen();
        mousePoint.x += screen.x;
        mousePoint.y += screen.y;

        mouseOverRow = moRow;
        mouseOverColumn = moColumn;

    }

    public void mousePressed(MouseEvent e) {
        ignoreSelectionsAndExpansions(-100);
        // TODO: isPopupTrigger() funktioniert nicht
        // logger.info("Press"+e.isPopupTrigger() );
        Point point = e.getPoint();
        int row = rowAtPoint(point);

        if (!isRowSelected(row) && e.getButton() == MouseEvent.BUTTON3) {
            getTreeSelectionModel().clearSelection();
            getTreeSelectionModel().addSelectionPath(getPathForRow(row));
        }
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {

            if (getPathForRow(row) == null) { return; }
            Vector<DownloadLink> alllinks = getAllSelectedDownloadLinks();
            Object obj = getPathForRow(row).getLastPathComponent();
            JMenuItem tmp;
            JPopupMenu popup = new JPopupMenu();
            if (obj instanceof DownloadLink) {
                Vector<FilePackage> fps = new Vector<FilePackage>();
                int enabled = 0;
                int disabled = 0;
                int resumeable = 0;
                for (DownloadLink next : alllinks) {
                    if (!fps.contains(next.getFilePackage())) {
                        fps.add(next.getFilePackage());
                    }
                    if (next.isEnabled()) {
                        enabled++;
                    } else {
                        disabled++;
                    }
                    if (!next.getLinkStatus().isPluginActive() && next.getLinkStatus().isFailed()) {
                        resumeable++;
                    }
                }

                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.info", "Detailansicht"), TreeTableAction.DOWNLOAD_INFO, new Property("downloadlink", obj))));

                JMenu packagePopup = new JMenu(JDLocale.L("gui.table.contextmenu.packagesubmenu", "Paket"));
                JMenu pluginPopup = new JMenu(JDLocale.L("gui.table.contextmenu.extrassubmenu", "Extras"));
                ArrayList<MenuItem> entries = new ArrayList<MenuItem>();
                JDUtilities.getController().fireControlEventDirect(new ControlEvent((DownloadLink) obj, ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU, entries));

                if (entries != null && entries.size() > 0) {
                    for (MenuItem next : entries) {
                        JMenuItem mi = SimpleGUI.getJMenuItem(next);
                        if (mi == null) {
                            pluginPopup.addSeparator();
                        } else {
                            pluginPopup.add(mi);
                        }
                    }
                } else {
                    pluginPopup.setEnabled(false);
                }
                popup.add(packagePopup);
                popup.add(pluginPopup);

                popup.add(buildpriomenuDownloadLink((DownloadLink) obj));
                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.downloadDir", "Zielordner öffnen"), TreeTableAction.DOWNLOAD_DOWNLOAD_DIR, new Property("downloadlink", obj))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.browseLink", "im Browser öffnen"), TreeTableAction.DOWNLOAD_BROWSE_LINK, new Property("downloadlink", obj))));
                if (((DownloadLink) obj).getLinkType() != DownloadLink.LINKTYPE_NORMAL) tmp.setEnabled(false);

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.delete", "entfernen") + " (" + alllinks.size() + ")", TreeTableAction.DELETE, new Property("links", alllinks))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.enable", "aktivieren") + " (" + disabled + ")", TreeTableAction.DOWNLOAD_ENABLE, new Property("downloadlinks", alllinks))));
                if (disabled == 0) tmp.setEnabled(false);
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.disable", "deaktivieren") + " (" + enabled + ")", TreeTableAction.DOWNLOAD_DISABLE, new Property("downloadlinks", alllinks))));
                if (enabled == 0) tmp.setEnabled(false);
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.reset", "zurücksetzen") + " (" + alllinks.size() + ")", TreeTableAction.DOWNLOAD_RESET, new Property("downloadlinks", alllinks))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.resume", "fortsetzen") + " (" + resumeable + ")", TreeTableAction.DOWNLOAD_RESUME, new Property("downloadlinks", alllinks))));
                if (resumeable == 0) tmp.setEnabled(false);

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.newpackage", "In neues Paket verschieben") + " (" + alllinks.size() + ")", TreeTableAction.DOWNLOAD_NEW_PACKAGE, new Property("downloadlinks", alllinks))));

                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.copyPassword", "Copy Password"), TreeTableAction.DOWNLOAD_COPY_PASSWORD, new Property("downloadlink", obj))));
                popup.add(tmp = new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.copyLink", "Copy URL"), TreeTableAction.DOWNLOAD_COPY_URL, new Property("downloadlink", obj))));
                if (((DownloadLink) obj).getLinkType() != DownloadLink.LINKTYPE_NORMAL) tmp.setEnabled(false);
                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.dlc", "DLC erstellen") + " (" + alllinks.size() + ")", TreeTableAction.DOWNLOAD_DLC, new Property("downloadlinks", alllinks))));
                popup.add(new JSeparator());
                popup.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.setdlpw", "Set download password") + " (" + alllinks.size() + ")", TreeTableAction.SET_PW, new Property("links", alllinks))));

                for (Component comp : createPackageMenu(((DownloadLink) obj).getFilePackage(), fps)) {
                    packagePopup.add(comp);
                }
            } else {
                for (Component comp : createPackageMenu((FilePackage) obj, getSelectedFilePackages())) {
                    popup.add(comp);
                }
            }
            popup.show(this, point.x, point.y);
        }
    }

    private JMenu buildpriomenuDownloadLink(DownloadLink link) {
        JMenuItem tmp;
        JMenu prioPopup = new JMenu(JDLocale.L("gui.table.contextmenu.priority", "Priority"));
        int prio = link.getPriority();
        HashMap<String, Object> prop = null;
        Vector<DownloadLink> links = getAllSelectedDownloadLinks();
        for (int i = 4; i >= -4; i--) {
            prop = new HashMap<String, Object>();
            prop.put("downloadlinks", links);
            prop.put("prio", new Integer(i));
            prioPopup.add(tmp = new JMenuItem(new TreeTableAction(this, Integer.toString(i), TreeTableAction.DOWNLOAD_PRIO, new Property("infos", prop))));
            if (i == prio) {
                tmp.setEnabled(false);
            } else
                tmp.setEnabled(true);
        }
        return prioPopup;
    }

    private JMenu buildpriomenuFilePackage(Vector<FilePackage> fps) {
        JMenuItem tmp;
        JMenu prioPopup = new JMenu(JDLocale.L("gui.table.contextmenu.priority", "Priority"));
        HashMap<String, Object> prop = null;
        for (int i = 4; i >= -4; i--) {
            prop = new HashMap<String, Object>();
            prop.put("packages", fps);
            prop.put("prio", new Integer(i));
            prioPopup.add(tmp = new JMenuItem(new TreeTableAction(this, Integer.toString(i), TreeTableAction.PACKAGE_PRIO, new Property("infos", prop))));
            tmp.setEnabled(true);
        }
        return prioPopup;
    }

    private Vector<Component> createPackageMenu(FilePackage fp, Vector<FilePackage> fps) {
        Vector<Component> res = new Vector<Component>();
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.info", "Detailansicht"), TreeTableAction.PACKAGE_INFO, new Property("package", fp))));
        JMenu pluginPopup = new JMenu(JDLocale.L("gui.table.contextmenu.extrasSubmenu", "Extras"));
        ArrayList<MenuItem> entries = new ArrayList<MenuItem>();
        JDUtilities.getController().fireControlEventDirect(new ControlEvent(fp, ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU, entries));

        if (entries != null && entries.size() > 0) {
            for (MenuItem next : entries) {
                JMenuItem mi = SimpleGUI.getJMenuItem(next);
                if (mi == null) {
                    pluginPopup.addSeparator();
                } else {
                    pluginPopup.add(mi);
                }
            }
        } else {
            pluginPopup.setEnabled(false);
        }
        int counter = 0;
        int enabled = 0;
        int disabled = 0;
        Vector<DownloadLink> alllinks = new Vector<DownloadLink>();
        for (FilePackage tfp : fps) {
            for (DownloadLink tlink : tfp.getDownloadLinks()) {
                alllinks.add(tlink);
                counter++;
                if (tlink.isEnabled()) {
                    enabled++;
                } else {
                    disabled++;
                }
            }
        }

        res.add(pluginPopup);
        res.add(buildpriomenuFilePackage(fps));

        res.add(new JSeparator());
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.editdownloadDir", "Zielordner ändern"), TreeTableAction.PACKAGE_EDIT_DIR, new Property("package", fp))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.downloadDir", "Zielordner öffnen"), TreeTableAction.PACKAGE_DOWNLOAD_DIR, new Property("package", fp))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.editpackagename", "Paketname ändern"), TreeTableAction.PACKAGE_EDIT_NAME, new Property("package", fp))));

        res.add(new JSeparator());
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.delete", "entfernen") + " (" + counter + ")", TreeTableAction.DELETE, new Property("links", alllinks))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.enable", "aktivieren") + " (" + disabled + ")", TreeTableAction.PACKAGE_ENABLE, new Property("packages", fps))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.disable", "deaktivieren") + " (" + enabled + ")", TreeTableAction.PACKAGE_DISABLE, new Property("packages", fps))));
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.reset", "zurücksetzen") + " (" + counter + ")", TreeTableAction.PACKAGE_RESET, new Property("packages", fps))));
        res.add(new JSeparator());
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.copyPassword", "Copy Password"), TreeTableAction.PACKAGE_COPY_PASSWORD, new Property("package", fp))));

        res.add(new JSeparator());
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.dlc", "DLC erstellen") + " (" + counter + ")", TreeTableAction.PACKAGE_DLC, new Property("packages", fps))));
        res.add(new JSeparator());
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.setdlpw", "Set download password") + " (" + counter + ")", TreeTableAction.SET_PW, new Property("links", alllinks))));
        res.add(new JSeparator());
        res.add(new JMenuItem(new TreeTableAction(this, JDLocale.L("gui.table.contextmenu.packagesort", "Paket sortieren"), TreeTableAction.PACKAGE_SORT, new Property("package", fp))));
        return res;
    }

    public void mouseReleased(MouseEvent e) {

        TreePath path = getPathForLocation(e.getX(), e.getY());

        int column = this.columnAtPoint(e.getPoint());
        if (path != null && path.getLastPathComponent() instanceof FilePackage) {
            JDSounds.PT("sound.gui.selectPackage");
            if (column == 0) {
                FilePackage fp = (FilePackage) path.getLastPathComponent();
                if (fp.getBooleanProperty(DownloadTreeTable.PROPERTY_EXPANDED, false)) {
                    this.collapsePath(path);

                } else {
                    expandPath(path);
                }
            }

        } else if (path != null) {
            JDSounds.PT("sound.gui.selectLink");
        }

    }

    public void moveSelectedItems(int id) {
        Vector<DownloadLink> links = getSelectedDownloadLinks();
        Vector<FilePackage> fps = getSelectedFilePackages();

        ignoreSelectionsAndExpansions(2000);
        logger.info(links.size() + " - " + fps.size());
        if (links.size() >= fps.size()) {
            if (links.size() == 0) { return; }

            switch (id) {
            case JDAction.ITEMS_MOVE_BOTTOM:
                DownloadLink lastLink = JDUtilities.getController().getPackages().lastElement().getDownloadLinks().lastElement();
                JDUtilities.getController().moveLinks(links, lastLink, null);
                break;
            case JDAction.ITEMS_MOVE_TOP:
                DownloadLink firstLink = JDUtilities.getController().getPackages().firstElement().getDownloadLinks().firstElement();
                JDUtilities.getController().moveLinks(links, null, firstLink);
                break;
            case JDAction.ITEMS_MOVE_UP:
                DownloadLink before = JDUtilities.getController().getDownloadLinkBefore(links.get(0));
                JDUtilities.getController().moveLinks(links, null, before);
                break;
            case JDAction.ITEMS_MOVE_DOWN:
                DownloadLink after = JDUtilities.getController().getDownloadLinkAfter(links.lastElement());
                JDUtilities.getController().moveLinks(links, after, null);
                break;
            }

        } else {

            switch (id) {
            case JDAction.ITEMS_MOVE_BOTTOM:
                FilePackage lastFilepackage = JDUtilities.getController().getPackages().lastElement();
                JDUtilities.getController().movePackages(fps, lastFilepackage, null);
                break;
            case JDAction.ITEMS_MOVE_TOP:
                FilePackage firstPackage = JDUtilities.getController().getPackages().firstElement();
                JDUtilities.getController().movePackages(fps, null, firstPackage);
                break;
            case JDAction.ITEMS_MOVE_UP:
                int i = JDUtilities.getController().getPackages().indexOf(fps.get(0));
                if (i <= 0) return;

                FilePackage before = JDUtilities.getController().getPackages().get(i - 1);
                JDUtilities.getController().movePackages(fps, null, before);
                break;
            case JDAction.ITEMS_MOVE_DOWN:
                i = JDUtilities.getController().getPackages().indexOf(fps.lastElement());
                if (i >= JDUtilities.getController().getPackages().size() - 1) return;

                FilePackage after = JDUtilities.getController().getPackages().get(i + 1);
                JDUtilities.getController().movePackages(fps, after, null);
                break;
            }

        }
        timer = new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateSelectionAndExpandStatus();
                logger.info("REFRESH");
            }

        });
        timer.setRepeats(false);
        timer.start();

    }

    /**
     * Die Listener speichern bei einer Selection oder beim aus/Einklappen von
     * Ästen deren Status
     */
    public void treeCollapsed(TreeExpansionEvent event) {
        FilePackage fp = (FilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(DownloadTreeTable.PROPERTY_EXPANDED, false);
    }

    public void treeExpanded(TreeExpansionEvent event) {
        FilePackage fp = (FilePackage) event.getPath().getLastPathComponent();
        fp.setProperty(DownloadTreeTable.PROPERTY_EXPANDED, true);
    }

    /**
     * Diese Methode setzt die gespeicherten Werte für die Selection und
     * Expansion
     */
    public synchronized void updateSelectionAndExpandStatus() {
        // logger.info("UPD");
        updatelock = true;
        int i = 0;
        while (getPathForRow(i) != null) {
            if (getPathForRow(i).getLastPathComponent() instanceof DownloadLink) {
                DownloadLink dl = (DownloadLink) getPathForRow(i).getLastPathComponent();
                if (dl.getBooleanProperty(PROPERTY_SELECTED, false)) {
                    getTreeSelectionModel().addSelectionPath(getPathForRow(i));
                }
            } else {
                FilePackage fp = (FilePackage) getPathForRow(i).getLastPathComponent();
                if (fp.getBooleanProperty(PROPERTY_EXPANDED, false)) {
                    expandPath(getPathForRow(i));
                }
                if (fp.getBooleanProperty(PROPERTY_SELECTED, false)) {
                    getTreeSelectionModel().addSelectionPath(getPathForRow(i));
                }
            }
            i++;
        }
        updatelock = false;
    }

    public void valueChanged(TreeSelectionEvent e) {

        TreePath[] paths = e.getPaths();
        // logger.info("" + e);
        if (ignoreSelectionsAndExpansionsUntil > System.currentTimeMillis()) { return; }
        for (TreePath path : paths) {
            if (e.isAddedPath(path)) {
                if (path.getLastPathComponent() instanceof DownloadLink) {
                    ((DownloadLink) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, true);

                } else {
                    ((FilePackage) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, true);

                }
            } else {

                if (path.getLastPathComponent() instanceof DownloadLink) {
                    ((DownloadLink) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, false);

                } else {
                    ((FilePackage) path.getLastPathComponent()).setProperty(DownloadTreeTable.PROPERTY_SELECTED, false);

                }
            }
        }

    }

    public void treeWillCollapse(TreeExpansionEvent arg0) throws ExpandVetoException {
        if (updatelock) return;
        if (this.usedoubleclick && System.currentTimeMillis() > lastMouseClicked + 200) throw new ExpandVetoException(arg0);
    }

    public void treeWillExpand(TreeExpansionEvent arg0) throws ExpandVetoException {
        if (updatelock) return;
        if (this.usedoubleclick && System.currentTimeMillis() > lastMouseClicked + 200) throw new ExpandVetoException(arg0);
    }

}