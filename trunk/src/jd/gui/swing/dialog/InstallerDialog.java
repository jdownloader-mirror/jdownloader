package jd.gui.swing.dialog;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.components.BrowseFile;
import jd.gui.swing.jdgui.views.settings.sidebar.AddonConfig;
import jd.utils.JDGeoCode;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import jd.utils.locale.JDLocale;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GeneralSettings;

public class InstallerDialog extends AbstractDialog<Object> {

    private static final long serialVersionUID = 1869417100230097511L;

    public static boolean showDialog(final File dlFolder) {
        final InstallerDialog dialog = new InstallerDialog(dlFolder);
        try {
            Dialog.getInstance().showDialog(dialog);
            return JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder() != null;

        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String     language = null;
    private final File dlFolder;

    private BrowseFile browseFile;

    private final String getScriptCode(String cntry) {
        // TODO Serbian Cyrillic / Latin
        // es-castillian: remove
        // español and castellano correspond to the same language!
        if (cntry.equals("CN") || cntry.equals("SG")) {
            return "hans";
        } else if (cntry.equals("HK") || cntry.equals("MO") || cntry.equals("TW")) { return "hant"; }
        return null;
    }

    private InstallerDialog(final File dlFolder) {
        super(UserIO.NO_ICON, _GUI._.installer_gui_title(), null, null, null);

        Locale l = Locale.getDefault();
        // ISO 3166 alpha-2 country code or UN M.49 numeric-3 area code
        final String countryCode = JDGeoCode.COUNTRIES.containsKey(l.getCountry()) ? l.getCountry() : JDL.getCountryCodeByIP();
        // ISO 639 alpha-2 or alpha-3 language code, or registered language
        // subtags up to 8 alpha letters (for future enhancements).
        final String languageCode = JDGeoCode.LANGUAGES.containsKey(l.getLanguage()) ? l.getLanguage() : null;
        // ISO 15924 alpha-4 script code
        // 1.7! Locale.getScript
        String scriptCode = null;
        if (Application.getJavaVersion() >= 17000000) {
            if (JDGeoCode.EXTENSIONS.containsKey(l.getScript().toLowerCase())) {
                scriptCode = l.getScript().toLowerCase();
            }
        } else {
            scriptCode = (countryCode != null ? getScriptCode(countryCode) : null);
        }

        if (languageCode != null) {
            boolean C_Set = false;
            boolean S_Set = false;
            for (final JDLocale id : JDL.getLocaleIDs()) {
                if (id.getLanguageCode().equalsIgnoreCase(languageCode)) {
                    if (countryCode != null && id.getCountryCode() != null && id.getCountryCode().equalsIgnoreCase(countryCode)) {
                        if (!C_Set) {
                            S_Set = false;
                        }
                        C_Set = true;
                    } else if (C_Set) {
                        continue;
                    }
                    if (scriptCode != null && id.getLngGeoCode().contains(scriptCode)) {
                        S_Set = true;
                        this.language = id.getLngGeoCode();
                    } else if (!S_Set) {
                        this.language = id.getLngGeoCode();
                    }
                }
            }
        }

        if (this.language == null) {
            this.language = "en";
        }

        if (dlFolder != null) {
            this.dlFolder = dlFolder;
        } else {
            if (CrossSystem.isLinux()) {
                String outdir = null;
                try {
                    outdir = JDUtilities.runCommand("xdg-user-dir", new String[] { "DOWNLOAD" }, null, 10).trim();
                    if (!new File(outdir).isDirectory()) {
                        outdir = null;
                    }
                } catch (Throwable e) {
                    outdir = null;
                }
                if (outdir == null) outdir = System.getProperty("user.home");
                this.dlFolder = new File(outdir);
            } else if (CrossSystem.isMac()) {
                this.dlFolder = new File(System.getProperty("user.home") + "/Downloads");
            } else if (CrossSystem.isWindows() && new File(System.getProperty("user.home") + "/Downloads").exists()) {
                this.dlFolder = new File(System.getProperty("user.home") + "/Downloads");
            } else if (CrossSystem.isWindows() && new File(System.getProperty("user.home") + "/Download").exists()) {
                this.dlFolder = new File(System.getProperty("user.home") + "/Download");
            } else {
                this.dlFolder = JDUtilities.getResourceFile("downloads");
            }
        }

    }

    @Override
    protected Object createReturnValue() {
        return this.getReturnmask();
    }

    @Override
    public JComponent layoutDialogContent() {
        final JDLocale sel = SubConfiguration.getConfig(JDL.CONFIG).getGenericProperty(JDL.LOCALE_PARAM_ID, JDL.getInstance(this.language));
        JDL.setLocale(sel);

        this.browseFile = new BrowseFile();
        this.browseFile.setFileSelectionMode(BrowseFile.DIRECTORIES_ONLY);
        this.browseFile.setCurrentPath(this.dlFolder);

        final JList list = new JList(JDL.getLocaleIDs().toArray());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedValue(sel, true);
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                JDL.setConfigLocale((JDLocale) list.getSelectedValue());
                JDL.setLocale(JDL.getConfigLocale());

                InstallerDialog.this.dispose();
                InstallerDialog.showDialog(InstallerDialog.this.browseFile.getCurrentPath());
            }

        });

        final ConfigContainer container = new ConfigContainer();
        container.setGroup(new ConfigGroup(_GUI._.gui_config_gui_language(), "language"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, new JScrollPane(list), "growx,pushx"));
        container.setGroup(new ConfigGroup(_GUI._.gui_config_general_downloaddirectory(), "home"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, this.browseFile, "growx,pushx"));

        final JLabel lbl = new JLabel(_GUI._.installer_gui_message());
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);

        if (CrossSystem.getID() == CrossSystem.OS_WINDOWS_VISTA || CrossSystem.getID() == CrossSystem.OS_WINDOWS_7) {
            final String dir = JDUtilities.getResourceFile("downloads").getParent().substring(3).toLowerCase();

            if (!JDUtilities.getResourceFile("uninstall.exe").exists() && (dir.startsWith("programme\\") || dir.startsWith("program files\\"))) {
                lbl.setText(_GUI._.installer_vistaDir_warning(JDUtilities.getResourceFile("downloads").getParent()));
                lbl.setForeground(Color.RED);
            }
            if (!JDUtilities.getResourceFile("JD.port").canWrite()) {
                lbl.setText(_GUI._.installer_nowriteDir_warning(JDUtilities.getResourceFile("downloads").getParent()));
                lbl.setForeground(Color.RED);
            }
        }

        final JPanel panel = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]25[grow,fill]push[]"));
        panel.add(lbl, "pushx");
        panel.add(AddonConfig.getInstance(container, "", true).getPanel());
        panel.add(new JSeparator(), "pushx");
        return panel;
    }

    @Override
    protected void packed() {
        this.setAlwaysOnTop(true);
    }

    @Override
    protected void setReturnmask(final boolean b) {
        super.setReturnmask(b);

        if (b) {

            JsonConfig.create(GeneralSettings.class).setDefaultDownloadFolder(browseFile.getText());
        }
    }
}