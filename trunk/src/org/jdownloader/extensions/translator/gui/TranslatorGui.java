package org.jdownloader.extensions.translator.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.AddonPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.translator.TLocale;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.extensions.translator.gui.actions.LoadTranslationAction;
import org.jdownloader.extensions.translator.gui.actions.NewTranslationAction;
import org.jdownloader.images.NewTheme;

/**
 * Extension gui
 * 
 * @author thomas
 * 
 */
public class TranslatorGui extends AddonPanel<TranslatorExtension> implements ListSelectionListener {

    private static final String ID = "TRANSLATORGUI";
    private TranslateTableModel tableModel;
    private TranslateTable      table;

    private SwitchPanel         panel;

    private JMenu               mnuFile;

    public TranslatorGui(TranslatorExtension plg) {
        super(plg);

        this.panel = new SwitchPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill]")) {

            @Override
            protected void onShow() {

            }

            @Override
            protected void onHide() {
            }
        };
        // layout all contents in panel
        this.setContent(panel);
        initComponents();

        layoutPanel();
    }

    private void layoutPanel() {
        panel.add(new JScrollPane(table), "spanx 2");

    }

    private void initComponents() {
        tableModel = new TranslateTableModel();
        table = new TranslateTable(tableModel);
        table.getSelectionModel().addListSelectionListener(this);

    }

    protected void initMenu(JMenuBar menubar) {
        // Load Menu
        mnuFile = new JMenu("File");

        // this.mnuFileLoad = new JMenu("Load");
        // for (TLocale t : getExtension().getTranslations()) {
        // mnuFileLoad.add(new LoadTranslationAction(this, t));
        // }
        // if (getExtension().getTranslations().size() > 0) mnuFileLoad.add(new JSeparator());
        // mnuFileLoad.add();

        this.mnuFile.add(new AppAction() {
            {
                setName("Load Translation");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                final ListCellRenderer org = new JComboBox().getRenderer();
                try {

                    final ComboBoxDialog d = new ComboBoxDialog(0, "Choose Translation", "Please choose the Translation you want to modify, or create a new one", getExtension().getTranslations().toArray(new TLocale[] {}), 0, NewTheme.I().getIcon("language", 32), null, null, null);
                    d.setLeftActions(new NewTranslationAction(TranslatorGui.this) {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            super.actionPerformed(e);
                            d.dispose();
                        }

                    });
                    int sel = Dialog.getInstance().showDialog(d);
                    if (sel >= 0) {
                        new LoadTranslationAction(TranslatorGui.this, getExtension().getTranslations().get(sel)).actionPerformed(null);
                    }
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        });

        this.mnuFile.add(new AppAction() {
            {
                setName("Save");
                setAccelerator("CTRL+S");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });

        // Menu-Bar zusammensetzen
        menubar.add(this.mnuFile);
        // menubar.add(this.mnuView);

        // tableModel.setMarkDefaults(mnuViewMarkDef.getState());
        // tableModel.setMarkOK(mnuViewMarkOK.getState());

    }

    protected void save() {
    }

    /**
     * is called if, and only if! the view has been closed
     */
    @Override
    protected void onDeactivated() {
        Log.L.finer("onDeactivated " + getClass().getSimpleName());
        if (!getExtension().getSettings().isRememberLoginsEnabled()) getExtension().doLogout();
    }

    /**
     * is called, if the gui has been opened.
     */
    @Override
    protected void onActivated() {
        Log.L.finer("onActivated " + getClass().getSimpleName());

    }

    @Override
    public Icon getIcon() {
        return this.getExtension().getIcon(16);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getTitle() {
        return "Translator";
    }

    @Override
    public String getTooltip() {
        return "Translator - Edit JDownloader Translation";
    }

    /**
     * Is called if gui is visible now, and has not been visible before. For example, user starte the extension, opened the view, or
     * switched form a different tab to this one
     */
    @Override
    protected void onShow() {
        Log.L.finer("Shown " + getClass().getSimpleName());
        if (getExtension().isLoggedIn()) return;
        ProgressGetter pg = new ProgressDialog.ProgressGetter() {

            @Override
            public void run() throws Exception {
                getExtension().doLogin();

            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public int getProgress() {
                return -1;
            }
        };

        try {
            Dialog.getInstance().showDialog(new ProgressDialog(pg, Dialog.BUTTONS_HIDE_CANCEL, "Login", "Please wait.", null, null, null) {

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(200, 40);
                }

            });
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }

    /**
     * gets called of the extensiongui is not visible any more. for example because it has been closed or user switched to a different
     * tab/view
     */
    @Override
    protected void onHide() {
        Log.L.finer("hidden " + getClass().getSimpleName());
    }

    public TLocale getLoaded() {
        return getExtension().getLoadedLocale();
    }

    public void load(final TLocale locale) {

        ProgressGetter pg = new ProgressDialog.ProgressGetter() {

            @Override
            public void run() throws Exception {
                getExtension().load(locale);
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        refresh();
                    }
                };
            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public int getProgress() {
                return -1;
            }
        };

        try {
            Dialog.getInstance().showDialog(new ProgressDialog(pg, Dialog.BUTTONS_HIDE_CANCEL, "Load Language", "Please wait. Loading " + locale, null, null, null) {

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(200, 40);
                }

            });
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }

    public void refresh() {
        tableModel.refresh(getExtension());
    }

    public void valueChanged(ListSelectionEvent e) {
        // ip.setEntries(tableModel.getSelectedObjects());
    }

}
