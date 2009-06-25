//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.optional.langfileeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.gui.UserIO;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ChartAPIEntity;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.PieChartAPI;
import jd.gui.skins.simple.components.TwoTextFieldDialog;
import jd.nutils.io.JDFileFilter;
import jd.nutils.svn.Subversion;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.Filter;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.PatternFilter;
import org.jdesktop.swingx.search.SearchFactory;
import org.tmatesoft.svn.core.SVNException;

public class LFEGui extends JTabbedPanel implements ActionListener, MouseListener {

    private static final long serialVersionUID = -143452893912428555L;

    private static final String SOURCE_SVN = "svn://svn.jdownloader.org/jdownloader/trunk/src";

    private static final String LANGUAGE_SVN = "svn://svn.jdownloader.org/jdownloader/trunk/ressourcen/jd/languages";

    private static final String LOCALE_PREFIX = "plugins.optional.langfileeditor.";

    private final SubConfiguration subConfig;

    private final String PROPERTY_SHOW_DONE = "PROPERTY_SHOW_DONE";
    private final String PROPERTY_SHOW_MISSING = "PROPERTY_SHOW_MISSING";
    private final String PROPERTY_SHOW_OLD = "PROPERTY_SHOW_OLD";
    private final String PROPERTY_COLORIZE_DONE = "PROPERTY_COLORIZE_DONE";
    private final String PROPERTY_COLORIZE_MISSING = "PROPERTY_COLORIZE_MISSING";
    private final String PROPERTY_COLORIZE_OLD = "PROPERTY_COLORIZE_OLD";
    private final String PROPERTY_DONE_COLOR = "PROPERTY_DONE_COLOR";
    private final String PROPERTY_MISSING_COLOR = "PROPERTY_MISSING_COLOR";
    private final String PROPERTY_OLD_COLOR = "PROPERTY_OLD_COLOR";

    private final String PROPERTY_SVN_ACCESS_ANONYMOUS = "PROPERTY_SVN_CHECKOUT_ANONYMOUS";
    private final String PROPERTY_SVN_ACCESS_USER = "PROPERTY_SVN_CHECKOUT_USER";
    private final String PROPERTY_SVN_ACCESS_PASS = "PROPERTY_SVN_CHECKOUT_PASS";

    private JXTable table;
    private MyTableModel tableModel;
    private File languageFile;
//    private ComboBrowseFile cmboFile;
    private PieChartAPI keyChart;
    private ChartAPIEntity entDone, entMissing, entOld;
    private JMenu mnuFile, mnuSVN, mnuKey, mnuEntries;
    private JMenuItem mnuNew, mnuReload, mnuSave, mnuSaveAs;
    private JMenuItem mnuSVNSettings, mnuSVNCheckOutNow;
    private JMenuItem mnuAdd, mnuAdopt, mnuAdoptMissing, mnuClear, mnuDelete, mnuTranslate, mnuTranslateMissing;
    private JMenuItem mnuPickDoneColor, mnuPickMissingColor, mnuPickOldColor, mnuShowDupes, mnuOpenSearchDialog;
    private JCheckBoxMenuItem mnuColorizeDone, mnuColorizeMissing, mnuColorizeOld, mnuShowDone, mnuShowMissing, mnuShowOld;
    private JPopupMenu mnuContextPopup;
    private JMenuItem mnuContextAdopt, mnuContextClear, mnuContextDelete, mnuContextTranslate;

    // private HashMap<String, String> sourceEntries = new HashMap<String,
    // String>();
    // private ArrayList<String> sourcePatterns = new ArrayList<String>();
    private HashMap<String, String> fileEntries = new HashMap<String, String>();
    private ArrayList<KeyInfo> data = new ArrayList<KeyInfo>();
    private HashMap<String, ArrayList<String>> dupes = new HashMap<String, ArrayList<String>>();
    private String lngKey = null;
    private boolean changed = false;
    // private boolean initComplete = false;
    private boolean updatingInProgress = false;
    private final JDFileFilter fileFilter;
    private final File dirLanguages, dirWorkingCopy;

    private boolean colorizeDone, colorizeMissing, colorizeOld, showDone, showMissing, showOld;
    private Color colorDone, colorMissing, colorOld;
    private ColorHighlighter doneHighlighter, missingHighlighter, oldHighlighter;

    private SrcParser sourceParser;

    public LFEGui() {
        subConfig = SubConfiguration.getConfig("ADDONS_LANGFILEEDITOR");
        fileFilter = new JDFileFilter(JDLocale.L(LOCALE_PREFIX + "fileFilter2", "JD Language File (*.loc) or Folder with Sourcefiles"), ".loc", true);
        // String lfeHome =
        // JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() +
        // "/plugins/lfe/";
        dirLanguages = JDUtilities.getResourceFile("tmp/lfe/lng/");
        dirWorkingCopy = JDUtilities.getResourceFile("tmp/lfe/src/");
        dirLanguages.mkdirs();
        dirWorkingCopy.mkdirs();
        showGui();
    }

    private void showGui() {
        colorizeDone = subConfig.getBooleanProperty(PROPERTY_COLORIZE_DONE, false);
        colorizeMissing = subConfig.getBooleanProperty(PROPERTY_COLORIZE_MISSING, true);
        colorizeOld = subConfig.getBooleanProperty(PROPERTY_COLORIZE_OLD, false);

        showDone = subConfig.getBooleanProperty(PROPERTY_SHOW_DONE, true);
        showMissing = subConfig.getBooleanProperty(PROPERTY_SHOW_MISSING, true);
        showOld = subConfig.getBooleanProperty(PROPERTY_SHOW_OLD, true);

        colorDone = subConfig.getGenericProperty(PROPERTY_DONE_COLOR, Color.GREEN);
        colorMissing = subConfig.getGenericProperty(PROPERTY_MISSING_COLOR, Color.RED);
        colorOld = subConfig.getGenericProperty(PROPERTY_OLD_COLOR, Color.ORANGE);

        doneHighlighter = new ColorHighlighter(new DonePredicate(), colorDone, null);
        missingHighlighter = new ColorHighlighter(new MissingPredicate(), colorMissing, null);
        oldHighlighter = new ColorHighlighter(new OldPredicate(), colorOld, null);

        tableModel = new MyTableModel();
        table = new JXTable(tableModel);
        FilterPipeline pipeline = new FilterPipeline(new Filter[] { new MyPatternFilter() });
        table.setFilters(pipeline);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumn(0).setMinWidth(200);
        table.getColumn(0).setPreferredWidth(200);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoStartEditOnKeyStroke(false);

        if (colorizeDone) table.addHighlighter(doneHighlighter);
        if (colorizeMissing) table.addHighlighter(missingHighlighter);
        if (colorizeOld) table.addHighlighter(oldHighlighter);

        // cmboSource = new ComboBrowseFile("LANGFILEEDITOR_SOURCE");
        // cmboSource.setFileSelectionMode(JDFileChooser.FILES_AND_DIRECTORIES);
        // cmboSource.setFileFilter(fileFilter);
        // cmboSource.addActionListener(this);

//        cmboFile = new ComboBrowseFile("LANGFILEEDITOR_FILE");
//        cmboFile.setFileSelectionMode(JDFileChooser.FILES_ONLY);
//        cmboFile.setFileFilter(fileFilter);
//        cmboFile.addActionListener(this);

        keyChart = new PieChartAPI("", 225, 50);
        keyChart.addEntity(entDone = new ChartAPIEntity(JDLocale.L(LOCALE_PREFIX + "keychart.done", "Done"), 0, colorDone));
        keyChart.addEntity(entMissing = new ChartAPIEntity(JDLocale.L(LOCALE_PREFIX + "keychart.missing", "Missing"), 0, colorMissing));
        keyChart.addEntity(entOld = new ChartAPIEntity(JDLocale.L(LOCALE_PREFIX + "keychart.old", "Old"), 0, colorOld));

        this.setLayout(new MigLayout("wrap 3", "[][grow, fill][]", "[][grow, fill][]"));
        this.add(buildMenu(), "span 3, growx, spanx");
        // this.add(new JLabel(JDLocale.L(LOCALE_PREFIX + "source",
        // "Source:")));
        // this.add(cmboSource, "growx");

//        this.add(new JLabel(JDLocale.L(LOCALE_PREFIX + "languageFile", "Language File:")));
//        this.add(cmboFile, "growx");

      
        this.add(new JScrollPane(table), "span 3, grow, span");
        this.add(keyChart, "spany 1, w 225!, h 50!");
        // sourceFile = cmboSource.getCurrentPath();
//        languageFile = cmboFile.getCurrentPath();

        new Thread(new Runnable() {

            public void run() {

                LFEGui.this.setEnabled(false);

                /*
                 * SVN Working Copy nur dann automatisch Updaten, wenn per Jar
                 * gestartet!
                 */
                updateSVN();
                // if (languageFile == null) {
                // if (dirLanguages.exists() && new File(dirLanguages,
                // JDLocale.getLocale() + ".loc").exists()) {
                // cmboFile.setCurrentPath(new File(dirLanguages,
                // JDLocale.getLocale() + ".loc"));
                // } else {
                // cmboFile.setCurrentPath(JDLocale.getLanguageFile());
                // }
                // }

                // initComplete = true;

                // if (sourceFile != null)
                getSourceEntries();
                initLocaleData();

                LFEGui.this.setEnabled(true);

            }

        }).start();
    }

    private void updateKeyChart() {
        int numMissing = 0, numOld = 0;

        for (KeyInfo entry : data) {
            if (entry.isOld()) {
                numOld++;
            } else if (entry.isMissing()) {
                numMissing++;
            }
        }

        entDone.setData(data.size() - numMissing - numOld);
        entDone.setCaption(JDLocale.L(LOCALE_PREFIX + "keychart.done", "Done") + " [" + entDone.getData() + "]");
        entMissing.setData(numMissing);
        entMissing.setCaption(JDLocale.L(LOCALE_PREFIX + "keychart.missing", "Missing") + " [" + entMissing.getData() + "]");
        entOld.setData(numOld);
        entOld.setCaption(JDLocale.L(LOCALE_PREFIX + "keychart.old", "Old") + " [" + entOld.getData() + "]");
        keyChart.fetchImage();
    }

    private JMenuBar buildMenu() {
        // File Menü
        mnuFile = new JMenu(JDLocale.L(LOCALE_PREFIX + "file", "File"));

        mnuFile.add(mnuNew = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "new", "New")));
        mnuFile.addSeparator();
        mnuFile.add(mnuSave = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "save", "Save")));
        mnuFile.add(mnuSaveAs = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "saveAs", "Save As")));
        mnuFile.addSeparator();
        mnuFile.add(mnuReload = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "reload", "Reload")));

        mnuNew.addActionListener(this);
        mnuSave.addActionListener(this);
        mnuSaveAs.addActionListener(this);
        mnuReload.addActionListener(this);

        mnuSave.setEnabled(false);
        mnuSaveAs.setEnabled(false);
        mnuReload.setEnabled(false);

        mnuNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        mnuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        mnuReload.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));

        // SVN Menü
        mnuSVN = new JMenu(JDLocale.L(LOCALE_PREFIX + "SVN", "SVN"));

        mnuSVN.add(mnuSVNSettings = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "svn.settings", "SVN Settings")));
        mnuSVN.addSeparator();
        mnuSVN.add(mnuSVNCheckOutNow = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "svn.checkOut", "CheckOut SVN now (This may take several seconds ...)")));

        mnuSVNSettings.addActionListener(this);
        mnuSVNCheckOutNow.addActionListener(this);

        // Key Menü
        mnuKey = new JMenu(JDLocale.L(LOCALE_PREFIX + "key", "Key"));
        mnuKey.setEnabled(false);

        mnuKey.add(mnuAdd = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "addKey", "Add Key")));
        mnuKey.add(mnuDelete = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "deleteKeys", "Delete Key(s)")));
        mnuKey.add(mnuClear = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "clearValues", "Clear Value(s)")));
        mnuKey.addSeparator();
        mnuKey.add(mnuAdopt = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "adoptDefaults", "Adopt Default(s)")));
        mnuKey.add(mnuAdoptMissing = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "adoptDefaults.missing", "Adopt Defaults of Missing Entries")));
        mnuKey.addSeparator();
        mnuKey.add(mnuTranslate = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "translate", "Translate with Google")));
        mnuKey.add(mnuTranslateMissing = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "translate.missing", "Translate Missing Entries with Google")));

        mnuAdd.addActionListener(this);
        mnuDelete.addActionListener(this);
        mnuClear.addActionListener(this);
        mnuAdopt.addActionListener(this);
        mnuAdoptMissing.addActionListener(this);
        mnuTranslate.addActionListener(this);
        mnuTranslateMissing.addActionListener(this);

        mnuDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));

        // Entries Menü
        mnuEntries = new JMenu(JDLocale.L(LOCALE_PREFIX + "entries", "Entries"));
        mnuEntries.setEnabled(false);

        mnuEntries.add(mnuShowMissing = new JCheckBoxMenuItem(JDLocale.L(LOCALE_PREFIX + "showMissing", "Show Missing Entries")));
        mnuEntries.add(mnuColorizeMissing = new JCheckBoxMenuItem(JDLocale.L(LOCALE_PREFIX + "colorizeMissing", "Colorize Missing Entries")));
        mnuEntries.add(mnuPickMissingColor = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "pickMissingColor", "Pick Color for Missing Entries")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuShowOld = new JCheckBoxMenuItem(JDLocale.L(LOCALE_PREFIX + "showOld", "Show Old Entries")));
        mnuEntries.add(mnuColorizeOld = new JCheckBoxMenuItem(JDLocale.L(LOCALE_PREFIX + "colorizeOld", "Colorize Old Entries")));
        mnuEntries.add(mnuPickOldColor = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "pickOldColor", "Pick Color for Old Entries")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuShowDone = new JCheckBoxMenuItem(JDLocale.L(LOCALE_PREFIX + "showDone", "Show Done Entries")));
        mnuEntries.add(mnuColorizeDone = new JCheckBoxMenuItem(JDLocale.L(LOCALE_PREFIX + "colorizeDone", "Colorize Done Entries")));
        mnuEntries.add(mnuPickDoneColor = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "pickDoneColor", "Pick Color for Done Entries")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuShowDupes = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "showDupes", "Show Dupes")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuOpenSearchDialog = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "openSearchDialog", "Open Search Dialog")));

        mnuShowMissing.setSelected(showMissing);
        mnuColorizeMissing.setSelected(colorizeMissing);
        mnuShowOld.setSelected(showOld);
        mnuColorizeOld.setSelected(colorizeOld);
        mnuShowDone.setSelected(showDone);
        mnuColorizeDone.setSelected(colorizeDone);

        mnuShowMissing.addActionListener(this);
        mnuColorizeMissing.addActionListener(this);
        mnuPickMissingColor.addActionListener(this);
        mnuShowOld.addActionListener(this);
        mnuColorizeOld.addActionListener(this);
        mnuPickOldColor.addActionListener(this);
        mnuShowDone.addActionListener(this);
        mnuColorizeDone.addActionListener(this);
        mnuPickDoneColor.addActionListener(this);
        mnuShowDupes.addActionListener(this);
        mnuOpenSearchDialog.addActionListener(this);

        mnuColorizeMissing.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        mnuColorizeOld.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        mnuColorizeDone.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
        mnuShowDupes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
        mnuOpenSearchDialog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));

        // Menü-Bar zusammensetzen
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(mnuFile);
        menuBar.add(mnuSVN);
        menuBar.add(mnuKey);
        menuBar.add(mnuEntries);

        return menuBar;
    }

    private void buildContextMenu() {
        // Context Menü
        mnuContextPopup = new JPopupMenu();

        mnuContextPopup.add(mnuContextDelete = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "deleteKeys", "Delete Key(s)")));
        mnuContextPopup.add(mnuContextClear = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "clearValues", "Clear Value(s)")));
        mnuContextPopup.addSeparator();
        mnuContextPopup.add(mnuContextAdopt = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "adoptDefaults", "Adopt Default(s)")));
        mnuContextPopup.add(mnuContextTranslate = new JMenuItem(JDLocale.L(LOCALE_PREFIX + "translate", "Translate with Google")));

        mnuContextDelete.addActionListener(this);
        mnuContextClear.addActionListener(this);
        mnuContextAdopt.addActionListener(this);
        mnuContextTranslate.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        // if (e.getSource() == cmboSource) {
        //
        // if (!initComplete) return;
        // File sourceFile = cmboSource.getCurrentPath();
        // if (sourceFile == this.sourceFile) return;
        //
        // if (!saveChanges(this.sourceFile, true, null)) return;
        //
        // if (sourceFile != this.sourceFile && sourceFile != null) {
        // this.sourceFile = sourceFile;
        // initLocaleDataComplete();
        // }
        //
        // } else
//        if (e.getSource() == cmboFile) {

//            File languageFile = cmboFile.getCurrentPath();
//            if (languageFile == this.languageFile) return;
//
//            if (!languageFile.getAbsolutePath().startsWith(this.dirLanguages.getAbsolutePath())) {
//                UserIO.getInstance().requestMessageDialog(JDLocale.LF(LOCALE_PREFIX + "wrongLanguageFile", "With the selected LanguageFile you are unable to let the LanguageFileEditor commit your changes to the SVN! Please change to a LanguageFile from the folder %s", dirLanguages.getAbsolutePath()));
//            }
//
//            if (!saveChanges(this.languageFile, false, languageFile)) return;
//
//            if (languageFile != this.languageFile && languageFile != null) {
//                this.languageFile = languageFile;
//                initLocaleData();
//            }

//        } else 
            if (e.getSource() == mnuNew) {

            if (!saveChanges()) return;

            JDFileChooser chooser = new JDFileChooser("LANGFILEEDITOR_FILE");
            chooser.setFileFilter(fileFilter);
            if (languageFile != null) chooser.setCurrentDirectory(languageFile.getParentFile());

            if (chooser.showSaveDialog(this) == JDFileChooser.APPROVE_OPTION) {
                languageFile = chooser.getSelectedFile();
                if (!languageFile.getAbsolutePath().endsWith(".loc")) languageFile = new File(languageFile.getAbsolutePath() + ".loc");
                if (!languageFile.exists()) {
                    try {
                        languageFile.createNewFile();
                    } catch (IOException e1) {
                        JDLogger.exception(e1);
                    }
                }
//                cmboFile.setCurrentPath(languageFile);

                initLocaleDataComplete();
            }

        } else if (e.getSource() == mnuSave) {

            saveLanguageFile(languageFile);

        } else if (e.getSource() == mnuSaveAs) {

            JDFileChooser chooser = new JDFileChooser("LANGFILEEDITOR_FILE");
            chooser.setFileFilter(fileFilter);
            chooser.setCurrentDirectory(languageFile.getParentFile());

            if (chooser.showSaveDialog(this) == JDFileChooser.APPROVE_OPTION) {
                languageFile = chooser.getSelectedFile();
                if (!languageFile.getAbsolutePath().endsWith(".loc")) languageFile = new File(languageFile.getAbsolutePath() + ".loc");
                saveLanguageFile(languageFile);
            }

        } else if (e.getSource() == mnuAdd) {

            String[] result = TwoTextFieldDialog.showDialog(SimpleGUI.CURRENTGUI, JDLocale.L(LOCALE_PREFIX + "addKey.title", "Add new key"), JDLocale.L(LOCALE_PREFIX + "addKey.message1", "Type in the name of the key:"), JDLocale.L(LOCALE_PREFIX + "addKey.message2", "Type in the translated message of the key:"), "", "");
            if (result[0].equals("")) return;
            result[0] = result[0].toLowerCase();
            for (KeyInfo ki : data) {
                if (ki.getKey().equals(result[0])) {
                    UserIO.getInstance().requestMessageDialog(JDLocale.LF(LOCALE_PREFIX + "addKey.error.message", "The key '%s' is already in use!", result[0]));
                    return;
                }
            }
            data.add(new KeyInfo(result[0].toLowerCase(), null, result[1]));
            tableModel.fireTableDataChanged();
            updateKeyChart();

        } else if (e.getSource() == mnuDelete || e.getSource() == mnuContextDelete) {

            deleteSelectedKeys();

        } else if (e.getSource() == mnuReload) {

            if (!saveChanges()) return;

            initLocaleDataComplete();

        } else if (e.getSource() == mnuAdopt || e.getSource() == mnuContextAdopt) {

            for (int row : getSelectedRows()) {
                tableModel.setValueAt(tableModel.getValueAt(row, 1), row, 2);
            }

        } else if (e.getSource() == mnuAdoptMissing) {

            for (int i = 0; i < tableModel.getRowCount(); ++i) {

                if (tableModel.getValueAt(i, 2).equals("")) {
                    tableModel.setValueAt(tableModel.getValueAt(i, 1), i, 2);
                }
            }

        } else if (e.getSource() == mnuClear || e.getSource() == mnuContextClear) {

            for (int row : getSelectedRows()) {
                tableModel.setValueAt("", row, 2);
            }

        } else if (e.getSource() == mnuShowMissing) {

            showMissing = mnuShowMissing.isSelected();
            subConfig.setProperty(PROPERTY_SHOW_MISSING, showMissing);
            subConfig.save();
            tableModel.fireTableDataChanged();

        } else if (e.getSource() == mnuShowOld) {

            showOld = mnuShowOld.isSelected();
            subConfig.setProperty(PROPERTY_SHOW_OLD, showOld);
            subConfig.save();
            tableModel.fireTableDataChanged();

        } else if (e.getSource() == mnuShowDone) {

            showDone = mnuShowDone.isSelected();
            subConfig.setProperty(PROPERTY_SHOW_DONE, showDone);
            subConfig.save();
            tableModel.fireTableDataChanged();

        } else if (e.getSource() == mnuColorizeMissing) {

            colorizeMissing = mnuColorizeMissing.isSelected();
            subConfig.setProperty(PROPERTY_COLORIZE_MISSING, colorizeMissing);
            subConfig.save();
            if (colorizeMissing) {
                table.addHighlighter(missingHighlighter);
            } else {
                table.removeHighlighter(missingHighlighter);
            }
            tableModel.fireTableDataChanged();

        } else if (e.getSource() == mnuColorizeOld) {

            colorizeOld = mnuColorizeOld.isSelected();
            subConfig.setProperty(PROPERTY_COLORIZE_OLD, colorizeOld);
            subConfig.save();
            if (colorizeOld) {
                table.addHighlighter(oldHighlighter);
            } else {
                table.removeHighlighter(oldHighlighter);
            }
            tableModel.fireTableDataChanged();

        } else if (e.getSource() == mnuColorizeDone) {

            colorizeDone = mnuColorizeDone.isSelected();
            subConfig.setProperty(PROPERTY_COLORIZE_DONE, colorizeDone);
            subConfig.save();
            if (colorizeDone) {
                table.addHighlighter(doneHighlighter);
            } else {
                table.removeHighlighter(doneHighlighter);
            }
            tableModel.fireTableDataChanged();

        } else if (e.getSource() == mnuPickMissingColor) {

            Color newColor = JColorChooser.showDialog(this, JDLocale.L(LOCALE_PREFIX + "pickMissingColor", "Pick Color for Missing Entries"), colorMissing);
            if (newColor != null) {
                colorMissing = newColor;
                subConfig.setProperty(PROPERTY_MISSING_COLOR, colorMissing);
                subConfig.save();
                tableModel.fireTableDataChanged();
                entMissing.setColor(colorMissing);
                missingHighlighter.setBackground(colorMissing);
                keyChart.fetchImage();
            }

        } else if (e.getSource() == mnuPickOldColor) {

            Color newColor = JColorChooser.showDialog(this, JDLocale.L(LOCALE_PREFIX + "pickOldColor", "Pick Color for Old Entries"), colorOld);
            if (newColor != null) {
                colorOld = newColor;
                subConfig.setProperty(PROPERTY_OLD_COLOR, colorOld);
                subConfig.save();
                tableModel.fireTableDataChanged();
                entOld.setColor(colorOld);
                oldHighlighter.setBackground(colorOld);
                keyChart.fetchImage();
            }

        } else if (e.getSource() == mnuPickDoneColor) {

            Color newColor = JColorChooser.showDialog(this, JDLocale.L(LOCALE_PREFIX + "pickDoneColor", "Pick Color for Done Entries"), colorDone);
            if (newColor != null) {
                colorDone = newColor;
                subConfig.setProperty(PROPERTY_DONE_COLOR, colorDone);
                subConfig.save();
                tableModel.fireTableDataChanged();
                entDone.setColor(colorDone);
                doneHighlighter.setBackground(colorDone);
                keyChart.fetchImage();
            }

        } else if (e.getSource() == mnuShowDupes) {

            LFEDupeDialog.showDialog(SimpleGUI.CURRENTGUI, dupes);

        } else if (e.getSource() == mnuOpenSearchDialog) {

            SearchFactory.getInstance().showFindInput(table, table.getSearchable());

        } else if (e.getSource() == mnuTranslate || e.getSource() == mnuContextTranslate) {

            if (getLanguageKey() == null) return;

            int[] rows = getSelectedRows();
            for (int i = rows.length - 1; i >= 0; --i) {
                translateRow(rows[i]);
            }

        } else if (e.getSource() == mnuTranslateMissing) {

            if (getLanguageKey() == null) return;

            for (int i = tableModel.getRowCount() - 1; i >= 0; --i) {
                if (tableModel.getValueAt(i, 2).equals("")) {
                    translateRow(i);
                }
            }

        } else if (e.getSource() == mnuSVNSettings) {

            ConfigEntry ce, conditionEntry;
            ConfigContainer container = new ConfigContainer();

            container.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_SVN_ACCESS_ANONYMOUS, JDLocale.L(LOCALE_PREFIX + "svn.access.anonymous2", "Anonymous SVN Access (You can't commit your changes without a SVN account!)")).setDefaultValue(true));
            container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_SVN_ACCESS_USER, JDLocale.L(LOCALE_PREFIX + "svn.access.user", "SVN Username")));
            ce.setEnabledCondidtion(conditionEntry, "==", false);
            container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, subConfig, PROPERTY_SVN_ACCESS_PASS, JDLocale.L(LOCALE_PREFIX + "svn.access.pass", "SVN Password")));
            ce.setEnabledCondidtion(conditionEntry, "==", false);
            container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
            container.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

                public void actionPerformed(ActionEvent e) {

                    updateSVNinThread();

                }

            }, JDLocale.L(LOCALE_PREFIX + "svn.checkOut", "CheckOut SVN now (This may take several seconds ...)")));
            SimpleGUI.displayConfig(container, 0);

        } else if (e.getSource() == mnuSVNCheckOutNow) {

            updateSVNinThread();

        }

    }

  

    private boolean saveChanges() {
        if (changed) {
            int res = JOptionPane.showConfirmDialog(this, JDLocale.L(LOCALE_PREFIX + "changed.message", "Language File changed! Save changes?"), JDLocale.L(LOCALE_PREFIX + "changed.title", "Save changes?"), JOptionPane.YES_NO_CANCEL_OPTION);
            if (res == JOptionPane.CANCEL_OPTION) {
              
                return false;
            } else if (res == JOptionPane.YES_OPTION) {
                saveLanguageFile(languageFile);
               
            } else {
                changed = false;
            }
        }

        return true;
    }

    private void updateSVNinThread() {
        if (updatingInProgress) return;
        new Thread(new Runnable() {

            public void run() {
                updateSVN();
            }

        }).start();
    }

    private void updateSVN() {
        SimpleGUI.CURRENTGUI.setWaiting(true);
        updatingInProgress = true;

        if (!dirLanguages.exists()) dirLanguages.mkdirs();
        if (!dirWorkingCopy.exists()) dirWorkingCopy.mkdirs();

        final ProgressController progress = new ProgressController(JDLocale.L(LOCALE_PREFIX + "svn.updating", "Updating SVN: Please wait"));
        progress.setIndeterminate(true);
        try {
            Subversion svn;
            Subversion svnLanguageDir;
            if (subConfig.getBooleanProperty(PROPERTY_SVN_ACCESS_ANONYMOUS, true)) {
                svn = new Subversion(SOURCE_SVN);
                svnLanguageDir = new Subversion(LANGUAGE_SVN);
            } else {
                svn = new Subversion(SOURCE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
                svnLanguageDir = new Subversion(LANGUAGE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
            }
            svn.getBroadcaster().addListener(new MessageListener() {

                public void onMessage(MessageEvent event) {
                    progress.setStatusText(JDLocale.L(LOCALE_PREFIX + "svn.updating", "Updating SVN: Please wait") + ": " + event.getMessage().replace(dirWorkingCopy.getParentFile().getAbsolutePath(), ""));

                }

            });
            try {
                svn.update(this.dirWorkingCopy, null);
            } catch (Exception e) {

                JDLogger.exception(e);
                UserIO.getInstance().requestMessageDialog(JDLocale.L(LOCALE_PREFIX + "error.title", "Error occured"), JDLocale.LF(LOCALE_PREFIX + "error.updatesource.message", "Error while updating source:\r\n %s", JDLogger.getStackTrace(e)));
            }
            try {
                svnLanguageDir.update(dirLanguages, null);
            } catch (Exception e) {
                JDLogger.exception(e);
                UserIO.getInstance().requestMessageDialog(JDLocale.L(LOCALE_PREFIX + "error.title", "Error occured"), JDLocale.LF(LOCALE_PREFIX + "error.updatelanguages.message", "Error while updating languages:\r\n %s", JDLogger.getStackTrace(e)));

            }
            progress.setStatusText(JDLocale.L(LOCALE_PREFIX + "svn.updating.ready", "Updating SVN: Complete"));
            progress.finalize(2 * 1000l);
        } catch (SVNException e) {
            JDLogger.exception(e);
            progress.setColor(Color.RED);
            progress.setStatusText(JDLocale.L(LOCALE_PREFIX + "svn.updating.error", "Updating SVN: Error!"));
            progress.finalize(5 * 1000l);
        }
        updatingInProgress = false;
        SimpleGUI.CURRENTGUI.setWaiting(false);

        // if (sourceFile == null || !sourceFile.equals(dirWorkingCopy)) {
        // if (!initComplete) sourceFile = dirWorkingCopy;
        // cmboSource.setCurrentPath(dirWorkingCopy);
        // } else if (sourceFile.equals(dirWorkingCopy) && !changed) {
        // if (initComplete) initLocaleDataComplete();
        // }
    }

    private String getLanguageKey() {
        if (lngKey == null) {
            String[] localeKeys = new String[] { "da", "de", "fi", "fr", "el", "hi", "it", "ja", "ko", "hr", "nl", "no", "pl", "pt", "ro", "ru", "sv", "es", "cs", "en", "ar" };
            Object newKey = JOptionPane.showInputDialog(this, JDLocale.L("plugins.optional.langfileeditor.translatedialog.message", "Choose Languagekey:"), JDLocale.L("plugins.optional.langfileeditor.translatedialog.title", "Languagekey"), JOptionPane.QUESTION_MESSAGE, null, localeKeys, null);
            lngKey = (newKey == null) ? null : newKey.toString();
        }
        return lngKey;
    }

    private void translateRow(int row) {
        String def = tableModel.getValueAt(row, 1);
        if (!def.equals("")) {
            String res = JDLocale.translate(lngKey, def);
            if (res != null) tableModel.setValueAt(res, row, 2);
        }
    }

    private void deleteSelectedKeys() {
        int[] rows = getSelectedRows();

        int len = rows.length - 1;
        ArrayList<String> keys = new ArrayList<String>(dupes.keySet());
        ArrayList<ArrayList<String>> obj = new ArrayList<ArrayList<String>>(dupes.values());
        ArrayList<String> values;
        for (int i = len; i >= 0; --i) {
            String temp = data.remove(rows[i]).getKey();
            data.remove(temp);
            for (int j = obj.size() - 1; j >= 0; --j) {
                values = obj.get(j);
                values.remove(temp);
                if (values.size() == 1) dupes.remove(keys.get(j));
            }
            tableModel.fireTableRowsDeleted(rows[i], rows[i]);
        }
        int newRow = Math.min(rows[len] - len, tableModel.getRowCount() - 1);
        table.getSelectionModel().setSelectionInterval(newRow, newRow);

        updateKeyChart();
        changed = true;
    }

    private int[] getSelectedRows() {
        int[] rows = table.getSelectedRows();
        int[] ret = new int[rows.length];

        for (int i = 0; i < rows.length; ++i) {
            ret[i] = table.convertRowIndexToModel(rows[i]);
        }

        return ret;
    }

    private void saveLanguageFile(File file) {
        StringBuilder sb = new StringBuilder();

        Collections.sort(data);

        for (KeyInfo entry : data) {
            if (!entry.isMissing()) sb.append(entry.toString() + "\n");
        }

        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            out.write(sb.toString());
            out.close();

            File noUpdateFile = new File(file.getAbsolutePath() + ".noupdate");
            if (!noUpdateFile.exists()) noUpdateFile.createNewFile();
        } catch (Exception e) {
            UserIO.getInstance().requestMessageDialog(JDLocale.LF(LOCALE_PREFIX + "save.error.message", "An error occured while writing the LanguageFile:\n%s", e.getMessage()));
            return;
        }

//        if (languageFile.getAbsolutePath() != cmboFile.getText()) cmboFile.setCurrentPath(languageFile);
        changed = false;
        UserIO.getInstance().requestMessageDialog(JDLocale.L(LOCALE_PREFIX + "save.success.message", "LanguageFile saved successfully!"));
    }

    private void initLocaleDataComplete() {
        getSourceEntries();
        initLocaleData();
    }

    private void initLocaleData() {
        SimpleGUI.CURRENTGUI.setWaiting(true);
        parseLanguageFile(languageFile, fileEntries);

        HashMap<String, String> dupeHelp = new HashMap<String, String>();
        data.clear();
        dupes.clear();
        lngKey = null;

        ArrayList<String> values;
        String value, key, language;
        KeyInfo keyInfo;
        for (LngEntry entry : sourceParser.getEntries()) {
            key = entry.getKey();
            keyInfo = new KeyInfo(key, entry.getValue(), fileEntries.remove(key));
            if (key.equalsIgnoreCase("$Version$")) keyInfo.setLanguage("$Revision$");
            data.add(keyInfo);
            if (!keyInfo.isMissing()) {

                language = keyInfo.getLanguage();
                if (dupeHelp.containsKey(language)) {
                    values = dupes.get(language);
                    if (values == null) {
                        values = new ArrayList<String>();
                        values.add(dupeHelp.get(language));
                        dupes.put(language, values);
                    }
                    values.add(key);
                }
                dupeHelp.put(language, key);
            }
        }

        // for (Entry<String, String> entry : fileEntries.entrySet()) {
        // key = entry.getKey();
        // value = null;
        //
        // for (String pattern : sourcePatterns) {
        // if (key.matches(pattern)) {
        // value = JDLocale.L(LOCALE_PREFIX + "patternEntry",
        // "<Entry matches Pattern>");
        // break;
        // }
        // }
        // data.add(new KeyInfo(key, value, entry.getValue()));
        // }

        Collections.sort(data);

        tableModel.fireTableRowsInserted(0, data.size() - 1);
        table.packAll();
        changed = false;

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                updateKeyChart();
                mnuEntries.setEnabled(true);
                mnuKey.setEnabled(true);
                mnuReload.setEnabled(true);
                mnuSave.setEnabled(true);
                mnuSaveAs.setEnabled(true);
            }

        });

        SimpleGUI.CURRENTGUI.setWaiting(false);
    }

    private void getSourceEntries() {
        SimpleGUI.CURRENTGUI.setWaiting(true);
        // if (sourceFile.isDirectory()) {
        getSourceEntriesFromFolder();
        // } else {
        // getSourceEntriesFromFile();
        // }
        SimpleGUI.CURRENTGUI.setWaiting(false);
    }

    // private void getSourceEntriesFromFile() {
    // sourcePatterns.clear();
    // // parseLanguageFile(sourceFile, sourceEntries);
    // }

    private void getSourceEntriesFromFolder() {

        ProgressController progress = new ProgressController(JDLocale.L(LOCALE_PREFIX + "analyzingSource1", "Analyzing Source Folder"));
        progress.setIndeterminate(true);
        sourceParser = new SrcParser(this.dirWorkingCopy);
        sourceParser.getBroadcaster().addListener(progress);
        sourceParser.parse();
        progress.setStatusText(JDLocale.L(LOCALE_PREFIX + "analyzingSource.ready", "Analyzing Source Folder: Complete"));
        progress.finalize(2 * 1000l);
    }

    private void parseLanguageFile(File file, HashMap<String, String> data) {
        data.clear();

        if (file == null || !file.exists()) {
            System.out.println("JDLocale: " + file + " not found");
            return;
        }

        try {
            BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            String line;
            String key;
            String value;
            while ((line = f.readLine()) != null) {
                if (line.startsWith("#")) continue;
                int split = line.indexOf("=");
                if (split <= 0) continue;

                key = line.substring(0, split).trim().toLowerCase();
                value = line.substring(split + 1).trim() + (line.endsWith(" ") ? " " : "");

                data.put(key, value);
            }
            f.close();
        } catch (IOException e) {
            JDLogger.exception(e);
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            int row = table.rowAtPoint(e.getPoint());
            if (!table.isRowSelected(row)) {
                table.getSelectionModel().setSelectionInterval(row, row);
            }
            if (mnuContextPopup == null) buildContextMenu();
            mnuContextPopup.show(table, e.getX(), e.getY());
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    private class KeyInfo implements Comparable<KeyInfo> {

        private final String key;

        private String source = "";

        private String language = "";

        public KeyInfo(String key, String source, String language) {
            this.key = key;
            this.setSource(source);
            this.setLanguage(language);
        }

        public String getKey() {
            return this.key;
        }

        public String getLanguage() {
            return this.language;
        }

        public String getSource() {
            return this.source;
        }

        public void setLanguage(String language) {
            if (language != null) this.language = language;
        }

        public void setSource(String source) {
            if (source != null) this.source = source;
        }

        public boolean isMissing() {
            return this.getLanguage().equals("");
        }

        public boolean isOld() {
            return this.getSource().equals("");
        }

        public int compareTo(KeyInfo o) {
            return this.getKey().compareToIgnoreCase(o.getKey());
        }

        @Override
        public String toString() {
            return this.getKey() + " = " + this.getLanguage();
        }

    }

    private class DonePredicate implements HighlightPredicate {
        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return (!table.getValueAt(arg1.row, 1).equals("") && !table.getValueAt(arg1.row, 2).equals(""));
        }
    }

    private class MissingPredicate implements HighlightPredicate {
        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return (table.getValueAt(arg1.row, 2).equals(""));
        }
    }

    private class OldPredicate implements HighlightPredicate {
        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return (table.getValueAt(arg1.row, 1).equals(""));
        }
    }

    private class MyPatternFilter extends PatternFilter {

        @Override
        public boolean test(int row) {
            boolean result = true;
            if (!subConfig.getBooleanProperty(PROPERTY_SHOW_DONE, true)) result = result && !(!getInputString(row, 1).equals("") && !getInputString(row, 2).equals(""));
            if (!subConfig.getBooleanProperty(PROPERTY_SHOW_MISSING, true)) result = result && !getInputString(row, 2).equals("");
            if (!subConfig.getBooleanProperty(PROPERTY_SHOW_OLD, true)) result = result && !getInputString(row, 1).equals("");
            return result;
        }

    }

    private class MyTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;

        private String[] columnNames = { JDLocale.L(LOCALE_PREFIX + "key", "Key"), JDLocale.L(LOCALE_PREFIX + "sourceValue", "Default Value"), JDLocale.L(LOCALE_PREFIX + "languageFileValue", "Language File Value") };

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public String getValueAt(int row, int col) {
            switch (col) {
            case 0:
                return data.get(row).getKey();
            case 1:
                return data.get(row).getSource();
            case 2:
                return data.get(row).getLanguage();
            }
            return "";
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            if (table.getValueAt(row, 0).toString().equalsIgnoreCase("$Version$")) return false;
            return true;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 2) {
                data.get(row).setLanguage((String) value);
                this.fireTableRowsUpdated(row, row);
                updateKeyChart();
                changed = true;
            }
        }

    }

    @Override
    public void onDisplay() {
    }

    @Override
    public void onHide() {
        if (changed && JOptionPane.showConfirmDialog(this, JDLocale.L(LOCALE_PREFIX + "changed.message", "Language File changed! Save changes?"), JDLocale.L(LOCALE_PREFIX + "changed.title", "Save changes?"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            saveLanguageFile(languageFile);
        }
    }

    @Override
    public boolean needsViewport() {
        return false;
    }

}
