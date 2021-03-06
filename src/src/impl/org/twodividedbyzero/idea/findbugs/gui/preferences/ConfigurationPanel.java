/*
 * Copyright 2008-2015 Andre Pfeiler
 *
 * This file is part of FindBugs-IDEA.
 *
 * FindBugs-IDEA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FindBugs-IDEA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FindBugs-IDEA.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.twodividedbyzero.idea.findbugs.gui.preferences;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.FilterComponent;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdesktop.swingx.color.ColorUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.twodividedbyzero.idea.findbugs.common.util.GuiUtil;
import org.twodividedbyzero.idea.findbugs.common.util.SonarImporterUtil;
import org.twodividedbyzero.idea.findbugs.core.FindBugsPlugin;
import org.twodividedbyzero.idea.findbugs.core.FindBugsPluginImpl;
import org.twodividedbyzero.idea.findbugs.gui.common.AaComboBox;
import org.twodividedbyzero.idea.findbugs.gui.common.AaSlider;
import org.twodividedbyzero.idea.findbugs.gui.common.VerticalFlowLayout;
import org.twodividedbyzero.idea.findbugs.gui.preferences.importer.SonarProfileImporter;
import org.twodividedbyzero.idea.findbugs.preferences.AnalysisEffort;
import org.twodividedbyzero.idea.findbugs.preferences.FindBugsPreferences;
import org.twodividedbyzero.idea.findbugs.preferences.PersistencePreferencesBean;
import org.twodividedbyzero.idea.findbugs.resources.GuiResources;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;


/**
 * $Date$
 *
 * @author Andre Pfeiler<andrepdo@dev.java.net>
 * @since 0.9.84-dev
 */
public final class ConfigurationPanel extends JPanel {

	private static final Logger LOGGER = Logger.getInstance(ConfigurationPanel.class.getName());

	private final FindBugsPlugin _plugin;
	private JCheckBox _compileBeforeAnalyseChkb;
	private JCheckBox _analyzeAfterCompileChkb;
	private JCheckBox _analyzeAfterAutoMakeChkb;
	private JCheckBox _runInBackgroundChkb;
	private JCheckBox _toolwindowToFront;

	private AaComboBox<AnalysisEffort> _effortLevelCombobox;
	private JPanel _topPanel;
	private JCheckBox _detectorThresholdChkb;
	private JPanel _effortPanel;
	private JBTabbedPane _tabbedPane;
	private DetectorConfiguration _detectorConfig;
	private ReportConfiguration _reporterConfig;
	private JButton _restoreDefaultsButton;
	private AaSlider _effortSlider;
	private FilterConfiguration _filterConfig;
	private PluginConfiguration _pluginConfig;
	private ImportExportConfiguration _importExportConfig;
	private AnnotationConfiguration _annotationConfig;
	private List<ConfigurationPage> _configPagesRegistry;
	private JToggleButton _showAdvancedConfigsButton;
	private JButton _exportButton;
	private JButton _importButton;
	private MyFilter _myFilter;


	public ConfigurationPanel(final FindBugsPlugin plugin) {
		super(new BorderLayout());

		if (plugin == null) {
			throw new IllegalArgumentException("Plugin may not be null.");
		}

		_plugin = plugin;

		initGui();

		final FindBugsPreferences preferences = getPreferences();
		if (!preferences.getBugCategories().containsValue("true") && !preferences.getDetectors().containsValue("true") || (preferences.getBugCategories().isEmpty() && preferences.getDetectors().isEmpty())) {
			restoreDefaultPreferences();
		}
	}


	Project getProject() {
		return _plugin.getProject();
	}


	public FindBugsPlugin getFindBugsPlugin() {
		return _plugin;
	}


	private void initGui() {
		updatePreferences();

		add(getTopPanel(), BorderLayout.NORTH);
		add(getTabbedPane());

		final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonsPanel.add(getMyFilter());
		buttonsPanel.add(getExportButton());
		buttonsPanel.add(getImportButton());
		buttonsPanel.add(getShowAdvancedConfigsButton());
		buttonsPanel.add(getRestoreDefaultsButton());
		add(buttonsPanel, BorderLayout.SOUTH);
	}


	@NotNull
	private JPanel getTopPanel() {
		if (_topPanel == null) {
			_topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			_topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4 * GuiUtil.SCALE_FACTOR, 0));
			//_detectorThresholdChkb = new JCheckBox("Set DetectorThreshold");

			final JPanel generalPanel = new JPanel(new VerticalFlowLayout());
			generalPanel.setBorder(BorderFactory.createTitledBorder("General"));
			generalPanel.add(getRunInBgCheckbox());
			generalPanel.add(getToolwindowToFrontCheckbox());
			_topPanel.add(generalPanel);

			final JPanel analyzePanel = new JPanel(new VerticalFlowLayout());
			analyzePanel.setBorder(BorderFactory.createTitledBorder("Auto Analyze"));
			analyzePanel.add(getAnalyzeAfterCompileCheckbox());
			analyzePanel.add(getAnalyzeAfterAutoMakeCheckbox());
			analyzePanel.add(getCompileBeforeAnalyseCheckbox());
			analyzePanel.add(getCompileBeforeAnalyseCheckbox());
			_topPanel.add(analyzePanel);
		}
		return _topPanel;
	}


	public void updatePreferences() {
		getEffortSlider().setValue(AnalysisEffort.valueOfLevel(getPreferences().getProperty(FindBugsPreferences.ANALYSIS_EFFORT_LEVEL, AnalysisEffort.DEFAULT.getEffortLevel())).getValue(), false);
		getRunInBgCheckbox().setSelected(getPreferences().getBooleanProperty(FindBugsPreferences.RUN_ANALYSIS_IN_BACKGROUND, false));
		getCompileBeforeAnalyseCheckbox().setSelected(getPreferences().getBooleanProperty(FindBugsPreferences.COMPILE_BEFORE_ANALYZE, true));
		getAnalyzeAfterCompileCheckbox().setSelected(getPreferences().getBooleanProperty(FindBugsPreferences.ANALYZE_AFTER_COMPILE, false));
		getAnalyzeAfterAutoMakeCheckbox().setSelected(getPreferences().getBooleanProperty(FindBugsPreferences.ANALYZE_AFTER_AUTOMAKE, false));
		getToolwindowToFrontCheckbox().setSelected(getPreferences().getBooleanProperty(FindBugsPreferences.TOOLWINDOW_TO_FRONT, true));
		getEffortLevelComboBox().setSelectedItem(AnalysisEffort.valueOfLevel(getPreferences().getProperty(FindBugsPreferences.ANALYSIS_EFFORT_LEVEL, AnalysisEffort.DEFAULT.getEffortLevel())), false);
		getReporterConfig().updatePreferences();
		getDetectorConfig().updatePreferences();
		getFilterConfig().updatePreferences();
		if (!_plugin.isModuleComponent()) {
			getPluginConfig().updatePreferences();
			getImportExportConfig().updatePreferences();
			getAnnotationConfig().updatePreferences();
		}
	}


	private FindBugsPreferences getPreferences() {
		return _plugin.getPreferences();
	}


	private AbstractButton getRunInBgCheckbox() {
		if (_runInBackgroundChkb == null) {
			_runInBackgroundChkb = new JCheckBox("Run analysis in background");
			_runInBackgroundChkb.setFocusable(false);
			_runInBackgroundChkb.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					getPreferences().setProperty(FindBugsPreferences.RUN_ANALYSIS_IN_BACKGROUND, _runInBackgroundChkb.isSelected());
				}
			});
		}
		return _runInBackgroundChkb;
	}


	@NotNull
	private AbstractButton getCompileBeforeAnalyseCheckbox() {
		if (_compileBeforeAnalyseChkb == null) {
			_compileBeforeAnalyseChkb = new JCheckBox("Compile affected files before analyze");
			_compileBeforeAnalyseChkb.setFocusable(false);
			_compileBeforeAnalyseChkb.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					getPreferences().setProperty(FindBugsPreferences.COMPILE_BEFORE_ANALYZE, _compileBeforeAnalyseChkb.isSelected());
				}
			});
		}
		return _compileBeforeAnalyseChkb;
	}


	@NotNull
	private AbstractButton getAnalyzeAfterCompileCheckbox() {
		if (_analyzeAfterCompileChkb == null) {
			_analyzeAfterCompileChkb = new JCheckBox("Analyze affected files after compile");
			_analyzeAfterCompileChkb.setFocusable(false);
			_analyzeAfterCompileChkb.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					getPreferences().setProperty(FindBugsPreferences.ANALYZE_AFTER_COMPILE, _analyzeAfterCompileChkb.isSelected());
				}
			});
		}
		return _analyzeAfterCompileChkb;
	}


	private AbstractButton getAnalyzeAfterAutoMakeCheckbox() {
		if (_analyzeAfterAutoMakeChkb == null) {
			_analyzeAfterAutoMakeChkb = new JCheckBox("Analyze affected files after auto make");
			_analyzeAfterAutoMakeChkb.setFocusable(false);
			_analyzeAfterAutoMakeChkb.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					getPreferences().setProperty(FindBugsPreferences.ANALYZE_AFTER_AUTOMAKE, _analyzeAfterAutoMakeChkb.isSelected());
				}
			});
		}
		return _analyzeAfterAutoMakeChkb;
	}


	private AbstractButton getToolwindowToFrontCheckbox() {
		if (_toolwindowToFront == null) {
			_toolwindowToFront = new JCheckBox("Activate toolwindow on run");
			_toolwindowToFront.setFocusable(false);
			_toolwindowToFront.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					getPreferences().setProperty(FindBugsPreferences.TOOLWINDOW_TO_FRONT, _toolwindowToFront.isSelected());
				}
			});
		}
		return _toolwindowToFront;
	}


	@NotNull
	private JPanel getEffortPanel() {
		if (_effortPanel == null) {
			_effortPanel = new JPanel(new BorderLayout());
			_effortPanel.setBorder(BorderFactory.createTitledBorder("Analysis effort"));
			_effortPanel.add(getEffortSlider());
		}
		return _effortPanel;
	}


	@SuppressWarnings({"UseOfObsoleteCollectionType"})
	private AaSlider getEffortSlider() {
		if (_effortSlider == null) {
			_effortSlider = new AaSlider(JSlider.HORIZONTAL, 10, 30, 20);
			_effortSlider.setBackground(GuiResources.HIGHLIGHT_COLOR_DARKER);
			_effortSlider.setMajorTickSpacing(10);
			_effortSlider.setPaintTicks(true);
			_effortSlider.setSnapToTicks(true);
			_effortSlider.setBorder(null);
			_effortSlider.setFocusable(false);

			final Dictionary<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
			labelTable.put(10, new JLabel(AnalysisEffort.MIN.getMessage()));
			labelTable.put(20, new JLabel(AnalysisEffort.DEFAULT.getMessage()));
			labelTable.put(30, new JLabel(AnalysisEffort.MAX.getMessage()));
			_effortSlider.setLabelTable(labelTable);
			_effortSlider.setPaintLabels(true);

			_effortSlider.addValueChangeListener(new ChangeListener() {
				public void stateChanged(final ChangeEvent e) {
					final JSlider source = (JSlider) e.getSource();
					if (!source.getValueIsAdjusting()) {
						final int value = source.getValue();
						getPreferences().setProperty(FindBugsPreferences.ANALYSIS_EFFORT_LEVEL, AnalysisEffort.getEffortLevelByValue(value));
					}
				}
			});

		}
		return _effortSlider;
	}


	private AaComboBox<AnalysisEffort> getEffortLevelComboBox() {
		if (_effortLevelCombobox == null) {
			_effortLevelCombobox = new AaComboBox<AnalysisEffort>(AnalysisEffort.values());
			_effortLevelCombobox.addSelectionChangeListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					getPreferences().setProperty(FindBugsPreferences.ANALYSIS_EFFORT_LEVEL, ((AnalysisEffort)_effortLevelCombobox.getSelectedItem()).getEffortLevel());
				}
			});
		}
		return _effortLevelCombobox;
	}


	@NotNull
	private JBTabbedPane getTabbedPane() {
		if (_tabbedPane == null) {
			_tabbedPane = new JBTabbedPane();

			final List<ConfigurationPage> configPages = getConfigPages();
			for (final ConfigurationPage configPage : configPages) {
				if (!configPage.isAdvancedConfig()) {
					if (!_plugin.isModuleComponent() || configPage.showInModulePreferences()) {
						_tabbedPane.addTab(configPage.getTitle(), configPage.getComponent());
					}
				}
			}
		}
		return _tabbedPane;
	}


	public void showConfigPage(@NotNull final ConfigurationPage page) {
		final int index = getTabbedPane().indexOfTab(page.getTitle());
		if (index != -1) {
			getTabbedPane().setSelectedIndex(index);
		}
	}


	@NotNull
	List<ConfigurationPage> getConfigPages() {
		if (_configPagesRegistry == null) {
			_configPagesRegistry = new ArrayList<ConfigurationPage>();
			_configPagesRegistry.add(getDetectorConfig());
			_configPagesRegistry.add(getReporterConfig());
			_configPagesRegistry.add(getFilterConfig());
			if (!_plugin.isModuleComponent()) {
				_configPagesRegistry.add(getPluginConfig());
				_configPagesRegistry.add(getImportExportConfig());
				_configPagesRegistry.add(getAnnotationConfig());
			}
		}
		return _configPagesRegistry;
	}


	DetectorConfiguration getDetectorConfig() {
		if (_detectorConfig == null) {
			_detectorConfig = new DetectorConfiguration(this, getPreferences());
		}
		return _detectorConfig;
	}


	ConfigurationPage getReporterConfig() {
		if (_reporterConfig == null) {
			_reporterConfig = new ReportConfiguration(this, getPreferences());
		}
		return _reporterConfig;
	}


	ConfigurationPage getFilterConfig() {
		if (_filterConfig == null) {
			_filterConfig = new FilterConfiguration(this, getPreferences());
		}
		return _filterConfig;
	}


	@NotNull
	public ConfigurationPage getPluginConfig() {
		if (_pluginConfig == null) {
			_pluginConfig = new PluginConfiguration(this, getPreferences());
		}
		return _pluginConfig;
	}


	ConfigurationPage getImportExportConfig() {
		if (_importExportConfig == null) {
			_importExportConfig = new ImportExportConfiguration(this, getPreferences());
		}
		return _importExportConfig;
	}


	ConfigurationPage getAnnotationConfig() {
		if (_annotationConfig == null) {
			_annotationConfig = new AnnotationConfiguration(this, getPreferences());
		}
		return _annotationConfig;
	}


	private Component getRestoreDefaultsButton() {
		if (_restoreDefaultsButton == null) {
			_restoreDefaultsButton = new JButton("Restore defaults");
			_restoreDefaultsButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					restoreDefaultPreferences();
				}
			});
		}
		return _restoreDefaultsButton;
	}


	private Component getShowAdvancedConfigsButton() {
		if (_showAdvancedConfigsButton == null) {
			_showAdvancedConfigsButton = new JCheckBox("Advanced");
			_showAdvancedConfigsButton.setBackground(GuiResources.HIGHLIGHT_COLOR_DARKER);
			_showAdvancedConfigsButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					if(_showAdvancedConfigsButton.isSelected()) {
						showAdvancedConfigs(true);
					} else {
						showAdvancedConfigs(false);
					}
				}
			});
		}
		return _showAdvancedConfigsButton;
	}


	private Component getExportButton() {
		if (_exportButton == null) {
			_exportButton = new JButton("Export");
			_exportButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					exportPreferences();
				}
			});
		}
		return _exportButton;
	}


	private Component getImportButton() {
		if (_importButton == null) {
			_importButton = new JButton("Import");
			_importButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					importPreferences();
				}
			});
		}
		return _importButton;
	}


	private MyFilter getMyFilter() {
		if (_myFilter == null) {
			_myFilter = new MyFilter();
		}
		return _myFilter;
	}


	private void showAdvancedConfigs(final boolean show) {
		if (show) {
			getTopPanel().add(getEffortPanel());
		} else {
			getTopPanel().remove(getEffortPanel());
		}

		final String fontColorHex = ColorUtil.toHexString(GuiResources.HIGHLIGHT_COLOR_DARKER);

		final List<ConfigurationPage> configPages = getConfigPages();
		Component firstAdvancedPage = null;
		final int configPagesSize = configPages.size();
		for (int i = 0; i < configPagesSize; i++) {
			final ConfigurationPage configPage = configPages.get(i);
			if (configPage.isAdvancedConfig()) {
				if (!_plugin.isModuleComponent()) {
					if (show) {
						if (firstAdvancedPage == null) {
							firstAdvancedPage = configPage.getComponent();
						}
						_tabbedPane.insertTab("<html><b><font color='" + fontColorHex + "'>" + configPage.getTitle(), null, configPage.getComponent(), configPage.getTitle(), i);
						_tabbedPane.setForegroundAt(i, GuiResources.HIGHLIGHT_COLOR_DARKER);
					} else {
						_tabbedPane.remove(configPage.getComponent());
					}
				} else if (configPage.showInModulePreferences()) {
					if (show) {
						if (firstAdvancedPage == null) {
							firstAdvancedPage = configPage.getComponent();
						}
						_tabbedPane.insertTab("<html><b><font color='" + fontColorHex + "'>" + configPage.getTitle(), null, configPage.getComponent(), configPage.getTitle(), i);
						_tabbedPane.setForegroundAt(i, GuiResources.HIGHLIGHT_COLOR_DARKER);
					} else {
						_tabbedPane.remove(configPage.getComponent());
					}
				}
			}
		}
		if (firstAdvancedPage != null) {
			_tabbedPane.setSelectedComponent(firstAdvancedPage);
		}
	}


	private void restoreDefaultPreferences() {
		final FindBugsPreferences bugsPreferences = getPreferences();
		bugsPreferences.setDefaults(FindBugsPreferences.createDefault(getProject(), true));
		updatePreferences();
		bugsPreferences.setModified(true);
	}


	private void exportPreferences() {
		final PersistencePreferencesBean prefs = _plugin.getState();
		final VirtualFileWrapper wrapper = FileChooserFactory.getInstance().createSaveFileDialog(
				new FileSaverDescriptor("Export FindBugs Preferences to File...", "", "xml"), this).save( null, null );
		if (wrapper == null) return;
		final Element el= XmlSerializer.serialize(prefs);
		el.setName(SonarImporterUtil.PERSISTENCE_ROOT_NAME); // rename "PersistencePreferencesBean"
		final Document document = new Document(el);
		try {
			JDOMUtil.writeDocument(document, wrapper.getFile(), "\n");
		} catch (final IOException ex) {
			LOGGER.error(ex);
			final String msg = ex.getLocalizedMessage();
			Messages.showErrorDialog(this, msg != null && !msg.isEmpty() ? msg : ex.toString(), "Export Failed");
		}
	}


	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"SIC_INNER_SHOULD_BE_STATIC_ANON", "REC_CATCH_EXCEPTION"})
	private void importPreferences() {
		@SuppressWarnings("AnonymousInnerClassMayBeStatic")
		final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, false, true, false) {
			@Override
			public boolean isFileVisible(final VirtualFile file, final boolean showHiddenFiles) {
				return super.isFileVisible(file, showHiddenFiles) &&
						(file.isDirectory() || "xml".equals(file.getExtension()) || file.getFileType() == StdFileTypes.ARCHIVE);
			}

			@Override
			public boolean isFileSelectable(final VirtualFile file) {
				return file.getFileType() == StdFileTypes.XML;
			}
		};
		descriptor.setDescription( "Please select the configuration file (usually named findbugs.xml) to import." );
		descriptor.setTitle( "Import Configuration" );

		final VirtualFile [] files = FileChooser.chooseFiles(descriptor, this, getProject(), null);
		if (files.length != 1) {
			return;
		}
		final PersistencePreferencesBean prefs;
		try {
			final Document document = JDOMUtil.loadDocument(files[0].getInputStream());
			if (SonarProfileImporter.isValid(document)) {
				prefs = SonarProfileImporter.doImport(getProject(), document);
				if (prefs == null) {
					return;
				}
			} else {
				if (!SonarImporterUtil.PERSISTENCE_ROOT_NAME.equals(document.getRootElement().getName())) {
					Messages.showErrorDialog(this, "The file format is invalid.", "Invalid File");
					return;
				}
				prefs = XmlSerializer.deserialize(document, PersistencePreferencesBean.class);
			}
			if (!validatePreferences(prefs)) {
				return;
			}
		} catch (final Exception ex) {
			LOGGER.warn(ex);
			final String msg = ex.getLocalizedMessage();
			FindBugsPluginImpl.showToolWindowNotifier(getProject(), "Import failed! " + (msg != null && !msg.isEmpty() ? msg : ex.toString()), MessageType.WARNING);
			return;
		}
		_pluginConfig.importPreferences( getProject(), prefs, new PluginConfiguration.ImportCallback() {
			public void validated( final PersistencePreferencesBean prefs ) {
				_plugin.loadState( prefs );
				_plugin.getPreferences().setModified(true);
				updatePreferences();
			}
		} );
	}


	private boolean validatePreferences(@Nullable final PersistencePreferencesBean prefs) {
		if (prefs == null) {
			Messages.showErrorDialog(this, "The configuration is invalid.", "Invalid Configuration");
			return false;
		} else if (prefs.isEmpty()) {
			final int answer = Messages.showYesNoDialog(this, "The configuration is empty. Do you want to proceed?", "Empty Configuration", Messages.getQuestionIcon());
			if (answer != DialogWrapper.OK_EXIT_CODE) {
				return false;
			}
		}
		return true;
	}


	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);
		// do not call setEnabled() here for:
		//     - getTabbedPane() and getShowAdvancedConfigsButton() -> allow browsing all read only settings
		//     - getExportButton() -> allow export
		//     - getMyFilter() -> allow filter/search
		getCompileBeforeAnalyseCheckbox().setEnabled(enabled);
		getAnalyzeAfterCompileCheckbox().setEnabled(enabled);
		getAnalyzeAfterAutoMakeCheckbox().setEnabled(enabled);
		getRunInBgCheckbox().setEnabled(enabled);
		getToolwindowToFrontCheckbox().setEnabled(enabled);
		final List<ConfigurationPage> configPages = getConfigPages();
		for (final ConfigurationPage configPage : configPages) {
			configPage.setEnabled(enabled);
		}
		getImportButton().setEnabled(enabled);
		getRestoreDefaultsButton().setEnabled(enabled);
		getEffortSlider().setEnabled(enabled);
	}


	public void setFilter(final String filter) {
		_myFilter.setSelectedItem(filter);
	}


	public class MyFilter extends FilterComponent {

		public MyFilter() {
			super("FILTER", 5);
		}

		public void filter() {
			if (!_showAdvancedConfigsButton.isSelected()) {
				showAdvancedConfigs(true);
			}
			final String filter = getFilter().toLowerCase(Locale.ENGLISH);
			for (final ConfigurationPage page : _configPagesRegistry) {
				page.filter(filter);
			}
		}
	}
}
