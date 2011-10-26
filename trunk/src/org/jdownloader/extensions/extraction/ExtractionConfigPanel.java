package org.jdownloader.extensions.extraction;

import java.io.File;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.FolderChooser;
import jd.gui.swing.jdgui.views.settings.components.Spinner;
import jd.gui.swing.jdgui.views.settings.components.TextArea;
import jd.gui.swing.jdgui.views.settings.components.TextInput;

import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.settings.Pair;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class ExtractionConfigPanel extends ExtensionConfigPanel<ExtractionExtension> {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;
	private Pair<Checkbox> toggleCustomizedPath;
	private Pair<FolderChooser> customPath;
	private Pair<Checkbox> toggleUseSubpath;
	private Pair<Spinner> subPathMinFiles;
	private Pair<Checkbox> toggleUseSubpathOnlyIfNotFoldered;
	private Pair<TextInput> subPath;
	private Pair<Checkbox> toggleDeleteArchives;
	private Pair<Checkbox> toggleOverwriteExisting;
	private Pair<TextArea> blacklist;
	private Pair<ComboBox<String>> cpupriority;

	public ExtractionConfigPanel(ExtractionExtension plg) {
		super(plg);
		// ConfigEntry ce, conditionEntry;
		// final JSonWrapper subConfig = getPluginConfig();
		//
		// config.setGroup(new ConfigGroup(getName(), getIconKey()));
		//
		// plg.getSettings().isAskForUnknownPasswordsEnabled()

		this.addHeader(T._.settings_extractto(), NewTheme.I().getIcon("folder", 32));
		toggleCustomizedPath = this.addPair(T._.settings_extract_to_archive_folder(), null, new Checkbox());
		customPath = this.addPair(T._.settings_extract_to_path(), null, new FolderChooser("custom_extraction_path"));
		customPath.setConditionPair(toggleCustomizedPath);
		toggleUseSubpath = this.addPair(T._.settings_use_subpath(), null, new Checkbox());
		Spinner spinner = new Spinner(0, Integer.MAX_VALUE);
		spinner.setFormat("# " + T._.files());
		subPathMinFiles = this.addPair(T._.settings_subpath_minnum(), null, spinner);
		subPathMinFiles.setConditionPair(toggleUseSubpath);
		toggleUseSubpathOnlyIfNotFoldered = this.addPair(T._.settings_subpath_no_folder(), null, new Checkbox());
		toggleUseSubpathOnlyIfNotFoldered.setToolTipText(T._.settings_subpath_no_folder_tt());
		toggleUseSubpathOnlyIfNotFoldered.setConditionPair(toggleUseSubpath);
		subPath = this.addPair(T._.settings_subpath(), null, new TextInput());
		subPath.setConditionPair(toggleUseSubpath);

		this.addHeader(T._.settings_various(), NewTheme.I().getIcon("settings", 32));
		toggleDeleteArchives = this.addPair(T._.settings_remove_after_extract(), null, new Checkbox());
		toggleOverwriteExisting = this.addPair(T._.settings_overwrite(), null, new Checkbox());

		this.addHeader(T._.settings_multi(), NewTheme.I().getIcon("settings", 32));
		cpupriority = this.addPair(T._.settings_cpupriority(), null, new ComboBox<String>(T._.settings_cpupriority_high(), T._.settings_cpupriority_middle(), T._.settings_cpupriority_low()));
		blacklist = this.addPair(T._.settings_blacklist(), null, new TextArea());
	}

	@Override
	public void updateContents() {
		final ExtractionConfig s = extension.getSettings();
		new EDTRunner() {
			@Override
			protected void runInEDT() {
				toggleCustomizedPath.getComponent().setSelected(s.isCustomExtractionPathEnabled());
				String path = s.getCustomExtractionPath();
				if (path == null) path = new File(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder(), "extracted").getAbsolutePath();
				customPath.getComponent().setText(path);
				toggleDeleteArchives.getComponent().setSelected(s.isDeleteArchiveFilesAfterExtraction());
				toggleOverwriteExisting.getComponent().setSelected(s.isOverwriteExistingFilesEnabled());
				toggleUseSubpath.getComponent().setSelected(s.isSubpathEnabled());
				subPath.getComponent().setText(s.getSubPath());
				subPathMinFiles.getComponent().setValue(s.getSubPathFilesTreshhold());
				toggleUseSubpathOnlyIfNotFoldered.getComponent().setSelected(s.isSubpathEnabledIfAllFilesAreInAFolder());

				StringBuilder sb = new StringBuilder();
				for (String line : s.getBlacklistPatterns()) {
					sb.append(line + System.getProperty("line.separator"));
				}
				blacklist.getComponent().setText(sb.toString());
				System.out.println(s.getCPUPriority());
				if (s.getCPUPriority() == CPUPriority.HIGH) {
					cpupriority.getComponent().setValue(T._.settings_cpupriority_high());
				} else if (s.getCPUPriority() == CPUPriority.MIDDLE) {
					cpupriority.getComponent().setValue(T._.settings_cpupriority_middle());
				} else {
					cpupriority.getComponent().setValue(T._.settings_cpupriority_low());
				}
			}
		};
	}

	@Override
	public void save() {
		final ExtractionConfig s = extension.getSettings();
		s.setCustomExtractionPathEnabled(toggleCustomizedPath.getComponent().isSelected());
		s.setCustomExtractionPath(customPath.getComponent().getText());
		s.setDeleteArchiveFilesAfterExtraction(toggleDeleteArchives.getComponent().isSelected());
		s.setOverwriteExistingFilesEnabled(toggleOverwriteExisting.getComponent().isSelected());
		s.setSubpathEnabled(toggleUseSubpath.getComponent().isSelected());
		s.setSubPath(subPath.getComponent().getText());
		try {
			s.setSubPathFilesTreshhold((Integer) subPathMinFiles.getComponent().getValue());
		} catch (final Throwable e) {
			Log.exception(e);
		}
		s.setSubpathEnabledIfAllFilesAreInAFolder(toggleUseSubpathOnlyIfNotFoldered.getComponent().isSelected());
		s.setBlacklistPatterns(blacklist.getComponent().getText().split(System.getProperty("line.separator")));

		if (cpupriority.getComponent().getValue().equals(T._.settings_cpupriority_high())) {
			s.setCPUPriority(CPUPriority.HIGH);
		} else if (cpupriority.getComponent().getValue().equals(T._.settings_cpupriority_middle())) {
			s.setCPUPriority(CPUPriority.MIDDLE);
		} else {
			s.setCPUPriority(CPUPriority.LOW);
		}
	}
}
