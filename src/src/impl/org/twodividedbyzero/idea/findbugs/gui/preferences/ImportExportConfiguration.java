/*
 * Copyright 2008-2013 Andre Pfeiler
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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import info.clearthought.layout.TableLayout;
import org.twodividedbyzero.idea.findbugs.common.EventDispatchThreadHelper;
import org.twodividedbyzero.idea.findbugs.core.FindBugsPluginImpl;
import org.twodividedbyzero.idea.findbugs.gui.common.AaTextField;
import org.twodividedbyzero.idea.findbugs.gui.common.FilterFileChooserDescriptor;
import org.twodividedbyzero.idea.findbugs.preferences.FindBugsPreferences;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;


/**
 * $Date$
 *
 * @author Andre Pfeiler<andrepdo@dev.java.net>
 * @version $Revision$
 * @since 0.9.96
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public class ImportExportConfiguration implements ConfigurationPage {

	private final FindBugsPreferences _preferences;
	private final ConfigurationPanel _parent;
	private Component _component;
	private JPanel _mainPanel;

	private JPanel _exportDirPanel;
    private JPanel _importFilePathPanel;
	private AaTextField _exportDirTextField;
    private AaTextField _importFilePathTextField;
	private JCheckBox _exportDirFormatCheckbox;

	private JPanel _fileFormatPanel;
	private JCheckBox _writeXmlCheckbox;
	private JCheckBox _writeHtmlCheckbox;

	private JPanel _browserPanel;
	private JCheckBox _openBrowserCheckbox;
	private String _currentExportDir;
    private String _currentimportFilePath;


	public ImportExportConfiguration(final ConfigurationPanel parent, final FindBugsPreferences preferences) {
		//noinspection AssignmentToCollectionOrArrayFieldFromParameter
		_preferences = preferences;
		_parent = parent;
	}


	public Component getComponent() {
		if (_component == null) {
			final double border = 5;
			final double rowsGap = 5;
			final double colsGap = 10;
			final double[][] size = {{border, TableLayout.PREFERRED, border}, // Columns
									 {border, TableLayout.PREFERRED, rowsGap, TableLayout.PREFERRED, rowsGap, TableLayout.PREFERRED, rowsGap, TableLayout.PREFERRED, border}};// Rows
			final TableLayout tbl = new TableLayout(size);

			_mainPanel = new JPanel(tbl);
			_mainPanel.add(getExportDirPanel(), "1, 1, 1, 1");
			_mainPanel.add(getFileFormatPanel(), "1, 3, 1, 3");
			_mainPanel.add(getBrowserPanel(), "1, 5, 1, 5");
            _mainPanel.add(getImportFilePathPanel(), "1, 7, 1, 7");

			_component = _mainPanel;
		}
		//updatePreferences();
		return _component;
	}


	public void updatePreferences() {
		getExportDirFormatCheckbox().setSelected(Boolean.valueOf(_preferences.getProperty(FindBugsPreferences.EXPORT_CREATE_ARCHIVE_DIR)));
		getWriteHtmlCheckbox().setSelected(Boolean.valueOf(_preferences.getProperty(FindBugsPreferences.EXPORT_AS_HTML)));
		getWriteXmlCheckbox().setSelected(Boolean.valueOf(_preferences.getProperty(FindBugsPreferences.EXPORT_AS_XML)));
		getOpenBrowserCheckbox().setSelected(Boolean.valueOf(_preferences.getProperty(FindBugsPreferences.EXPORT_OPEN_BROWSER)));
		_currentExportDir = _preferences.getProperty(FindBugsPreferences.EXPORT_BASE_DIR);
		getExportDirTextField().setText(_currentExportDir);
		_currentimportFilePath = _preferences.getProperty(FindBugsPreferences.IMPORT_FILE_PATH);
        getimportFilePathTextField().setText(_currentimportFilePath);
	}


	public void setEnabled(final boolean enabled) {
		getExportDirFormatCheckbox().setEnabled(enabled);
		getExportDirTextField().setEnabled(enabled);
		getOpenBrowserCheckbox().setEnabled(enabled);
		getWriteHtmlCheckbox().setEnabled(enabled);
		getWriteXmlCheckbox().setEnabled(enabled);
	}


	public boolean showInModulePreferences() {
		return false;
	}


	public boolean isAdvancedConfig() {
		return false;
	}


	public String getTitle() {
		return "Import/Export";
	}


	private Component getExportDirPanel() {
		if (_exportDirPanel == null) {
			final double border = 5;
			final double rowsGap = 5;
			final double colsGap = 10;
			final double[][] size = {{border, TableLayout.PREFERRED, colsGap, TableLayout.FILL, colsGap, TableLayout.PREFERRED, border}, // Columns
									 {border, TableLayout.PREFERRED, rowsGap, TableLayout.PREFERRED, border}};// Rows
			final TableLayout tbl = new TableLayout(size);
			_exportDirPanel = new JPanel(tbl);
			_exportDirPanel.setBorder(BorderFactory.createTitledBorder("Export directory settings"));
			final JComponent exportDirLabel = new JLabel("Export Dir");
			exportDirLabel.setToolTipText("Set the default export directory. if not set selection dialog will be shown.");
			_exportDirPanel.add(exportDirLabel, "1, 1, 1, 1");
			_exportDirPanel.add(getExportDirTextField(), "3, 1, 3, 1");

			final AbstractButton browseButton = new JButton("Browse");
			browseButton.setPreferredSize(new Dimension(80, 20));
			browseButton.addActionListener(new ExportDirChooserActionListener());
			_exportDirPanel.add(browseButton, "5, 1, 5, 1");

			_exportDirPanel.add(getExportDirFormatCheckbox(), "1, 3, 1, 3");
			_exportDirPanel.add(new JLabel("Create archive sub dir(s) in the following format (MM_TT_YYYY)"), "3, 3, 3, 3");
		}

		return _exportDirPanel;
	}

    private Component getImportFilePathPanel() {
        if (_importFilePathPanel == null) {
            final double border = 5;
            final double rowsGap = 5;
            final double colsGap = 10;
            final double[][] size = {{border, TableLayout.PREFERRED, colsGap, TableLayout.FILL, colsGap, TableLayout.PREFERRED, border}, // Columns
                    {border, TableLayout.PREFERRED, rowsGap, TableLayout.PREFERRED, border}};// Rows
            final TableLayout tbl = new TableLayout(size);
            _importFilePathPanel = new JPanel(tbl);
            _importFilePathPanel.setBorder(BorderFactory.createTitledBorder("Import file path settings"));
            final JComponent importFilePathLabel = new JLabel("Import File Path");
            importFilePathLabel.setToolTipText("Set the file path used to import settings before each analysis. If not set the import process is ignored.");
            _importFilePathPanel.add(importFilePathLabel, "1, 1, 1, 1");
            _importFilePathPanel.add(getimportFilePathTextField(), "3, 1, 3, 1");

            final AbstractButton browseButton = new JButton("Browse");
            browseButton.setPreferredSize(new Dimension(80, 20));
            browseButton.addActionListener(new ImportFileChooserActionListener());
            _importFilePathPanel.add(browseButton, "5, 1, 5, 1");
        }

        return _importFilePathPanel;
    }


	private void showToolWindowNotifier(final String message, final MessageType type) {
		EventDispatchThreadHelper.invokeLater(new Runnable() {
			public void run() {
				FindBugsPluginImpl.showToolWindowNotifier(_parent.getProject(), message, type);
			}
		});
	}


	private JTextComponent getExportDirTextField() {
		if (_exportDirTextField == null) {
			_exportDirTextField = new AaTextField(30);
			_exportDirTextField.setText(_currentExportDir);
			_exportDirTextField.addTextChangeListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					_preferences.setProperty(FindBugsPreferences.EXPORT_BASE_DIR, _exportDirTextField.getText());
				}
			});
		}
		return _exportDirTextField;
	}

    private JTextComponent getimportFilePathTextField() {
        if (_importFilePathTextField == null) {
            _importFilePathTextField = new AaTextField(30);
            _importFilePathTextField.setText(_currentimportFilePath);
            _importFilePathTextField.addTextChangeListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    _preferences.setProperty(FindBugsPreferences.IMPORT_FILE_PATH, _importFilePathTextField.getText());
                }
            });
        }
        return _importFilePathTextField;
    }


	private AbstractButton getExportDirFormatCheckbox() {
		if (_exportDirFormatCheckbox == null) {
			_exportDirFormatCheckbox = new JCheckBox();
			_exportDirFormatCheckbox.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					_preferences.setProperty(FindBugsPreferences.EXPORT_CREATE_ARCHIVE_DIR, _exportDirFormatCheckbox.isSelected());
				}
			});
		}
		return _exportDirFormatCheckbox;
	}


	private Component getFileFormatPanel() {
		if (_fileFormatPanel == null) {
			final double border = 5;
			final double rowsGap = 5;
			final double colsGap = 10;
			final double[][] size = {{border, TableLayout.PREFERRED, colsGap, TableLayout.FILL, border}, // Columns
									 {border, TableLayout.PREFERRED, rowsGap, TableLayout.PREFERRED, border}};// Rows
			final TableLayout tbl = new TableLayout(size);
			_fileFormatPanel = new JPanel(tbl);
			_fileFormatPanel.setBorder(BorderFactory.createTitledBorder("Enable export file format"));

			_fileFormatPanel.add(getWriteHtmlCheckbox(), "1, 1, 1, 1");
			_fileFormatPanel.add(new JLabel("Export bug collection as HTML"), "3, 1, 3, 1");

			_fileFormatPanel.add(getWriteXmlCheckbox(), "1, 3, 1, 3");
			_fileFormatPanel.add(new JLabel("Export bug collection as XML"), "3, 3, 3, 3");

		}
		return _fileFormatPanel;
	}


	private Component getBrowserPanel() {
		if (_browserPanel == null) {
			final double border = 5;
			final double rowsGap = 5;
			final double colsGap = 10;
			final double[][] size = {{border, TableLayout.PREFERRED, colsGap, TableLayout.FILL, border}, // Columns
									 {border, TableLayout.PREFERRED, border}};// Rows
			final TableLayout tbl = new TableLayout(size);
			_browserPanel = new JPanel(tbl);
			_browserPanel.setBorder(BorderFactory.createTitledBorder("Result view settings"));

			_browserPanel.add(getOpenBrowserCheckbox(), "1, 1, 1, 1");
			_browserPanel.add(new JLabel("Open exported bug collection (only Html) in the configured browser"), "3, 1, 3, 1");
		}
		return _browserPanel;
	}


	private AbstractButton getWriteXmlCheckbox() {
		if (_writeXmlCheckbox == null) {
			_writeXmlCheckbox = new JCheckBox();
			_writeXmlCheckbox.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					_preferences.setProperty(FindBugsPreferences.EXPORT_AS_XML, _writeXmlCheckbox.isSelected());
				}
			});
		}
		return _writeXmlCheckbox;
	}


	private AbstractButton getWriteHtmlCheckbox() {
		if (_writeHtmlCheckbox == null) {
			_writeHtmlCheckbox = new JCheckBox();
			_writeHtmlCheckbox.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					_preferences.setProperty(FindBugsPreferences.EXPORT_AS_HTML, _writeHtmlCheckbox.isSelected());
				}
			});
		}
		return _writeHtmlCheckbox;
	}


	private AbstractButton getOpenBrowserCheckbox() {
		if (_openBrowserCheckbox == null) {
			_openBrowserCheckbox = new JCheckBox();
			_openBrowserCheckbox.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					_preferences.setProperty(FindBugsPreferences.EXPORT_OPEN_BROWSER, _openBrowserCheckbox.isSelected());
				}
			});
		}
		return _openBrowserCheckbox;
	}


	public void filter(final String filter) {
		// TODO support search
	}

    private class ExportDirChooserActionListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            final FileChooserDescriptor descriptor = new FilterFileChooserDescriptor("Select", "Select an export directory");
            final VirtualFile toSelect = LocalFileSystem.getInstance().findFileByPath(getExportDirTextField().getText());
            final VirtualFile chosen = FileChooser.chooseFile(descriptor, _parent, _parent.getProject(), toSelect);
            if (chosen != null) {
                final File selectedFile = VfsUtilCore.virtualToIoFile(chosen);
                if (selectedFile.isDirectory() && selectedFile.canWrite()) {
                    getExportDirTextField().setText(selectedFile.getPath());
                } else {
                    showToolWindowNotifier("Invalid directory.", MessageType.ERROR);
                }
            }
        }
    }

    private class ImportFileChooserActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            FileChooserDescriptor descriptor = new FilterFileChooserDescriptor("Select", "Select a file to import",
                    new FileNameExtensionFilter("XML files", "xml"));
            VirtualFile toSelect = LocalFileSystem.getInstance().findFileByPath(getimportFilePathTextField().getText());
            VirtualFile chosen = FileChooser.chooseFile(descriptor, _parent, _parent.getProject(), toSelect);
            if (chosen != null) {
                File selectedFile = VfsUtilCore.virtualToIoFile(chosen);
                if (selectedFile.canRead() && selectedFile.isFile()) {
                    getimportFilePathTextField().setText(selectedFile.getPath());
                } else {
                    showToolWindowNotifier("Invalid file to import : " + selectedFile.getName(), MessageType.ERROR);
                }
            }
        }
    }

}
