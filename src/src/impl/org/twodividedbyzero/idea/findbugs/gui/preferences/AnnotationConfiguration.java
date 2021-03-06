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

import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.ui.ColorChooser;
import com.intellij.ui.JBColor;
import com.intellij.ui.UIBundle;
import com.intellij.ui.components.JBList;
import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;
import org.jetbrains.annotations.NotNull;
import org.twodividedbyzero.idea.findbugs.common.util.GuiUtil;
import org.twodividedbyzero.idea.findbugs.gui.common.AaComboBox;
import org.twodividedbyzero.idea.findbugs.gui.common.AaTextField;
import org.twodividedbyzero.idea.findbugs.preferences.FindBugsPreferences;
import org.twodividedbyzero.idea.findbugs.resources.GuiResources;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * $Date$
 *
 * @author Andre Pfeiler<andrepdo@dev.java.net>
 * @version $Revision$
 * @since 0.9.97
 */
@SuppressWarnings({"HardcodedFileSeparator"})
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"SE_TRANSIENT_FIELD_NOT_RESTORED", "SE_BAD_FIELD", "SIC_INNER_SHOULD_BE_STATIC_ANON"})
public class AnnotationConfiguration implements ConfigurationPage {

	private AnnotationTypePanel _annotationTypePanel;


	private final FindBugsPreferences _preferences;
	private final ConfigurationPanel _parent;
	private Component _component;
	private JPanel _mainPanel;
	private AaTextField _annotationPathField;
	private JCheckBox _enableGutterIcon;
	private JCheckBox _enableTextRangeMarkUp;
	private JPanel _annotationPathPanel;
	private JPanel _markUpPanel;
	private JPanel _typeSettingsPanel;
	private JBList _annotationTypeList;


	public AnnotationConfiguration(final ConfigurationPanel parent, final FindBugsPreferences preferences) {
		//noinspection AssignmentToCollectionOrArrayFieldFromParameter
		_preferences = preferences;
		_parent = parent;
	}


	public Component getComponent() {
		if (_component == null) {
			final double border = 5;
			final double rowsGap = 5;
			final double colsGap = 10;
			final double[][] size = {{border, TableLayoutConstants.FILL, border}, // Columns
									 {border, TableLayoutConstants.PREFERRED, rowsGap, TableLayoutConstants.PREFERRED, border}};// Rows
			final TableLayout tbl = new TableLayout(size);

			final Container mainPanel = new JPanel(tbl);
			mainPanel.add(getMarkUpPanel(), "1, 1, 1, 1");
			mainPanel.add(getAnnotationPathPanel(), "1, 3, 1, 3");

			_component = mainPanel;
			setEnabled(true);
		}
		//updatePreferences();
		return _component;
	}


	public void updatePreferences() {
		clearModels();
		syncModels();
	}


	private void syncModels() {
		final String annotationSuppressWarningName = _preferences.getProperty(FindBugsPreferences.ANNOTATION_SUPPRESS_WARNING_CLASS);
		getAnnotationPathField().setText(annotationSuppressWarningName, false);

		getGutterIconCheckbox().setSelected(_preferences.getBooleanProperty(FindBugsPreferences.ANNOTATION_GUTTER_ICON_ENABLED, true));
		if (_preferences.getBooleanProperty(FindBugsPreferences.ANNOTATION_TEXT_RAGE_MARKUP_ENABLED, true)) {
			getTextRangeMarkupCheckbox().setSelected(true);
		} else {
			getAnnotationTypePanel().setEnabled(false);
			getAnnotationTypeList().setEnabled(false);
		}


		// todo:

		/*
		final Map<String,Map<String,String>> annotationTypeSettings = _preferences.getAnnotationTypeSettings();
		for (final AnnotationType annotationType : AnnotationType.values()) {
			annotationType.setForegroundColor();
			annotationType.setBackgroundColor();
			annotationType.setEffectColor();
			annotationType.setEffectType();
			annotationType.setFont();
		}*/


		/*for (final String s : _preferences.getPlugins()) {
			getModel(getPluginList()).addElement(s);
		}*/
	}


	private void clearModels() {
		getAnnotationPathField().setText("", false);
		//getModel(getPluginList()).clear();
	}


	JPanel getAnnotationPathPanel() {
		if (_annotationPathPanel == null) {

			final double border = 5;
			final double colsGap = 10;
			final double[][] size = {{border, TableLayoutConstants.PREFERRED, colsGap, TableLayoutConstants.PREFERRED, border}, // Columns
									 {border, TableLayoutConstants.PREFERRED, border}};// Rows
			final TableLayout tbl = new TableLayout(size);
			_annotationPathPanel = new JPanel(tbl);
			_annotationPathPanel.setBorder(BorderFactory.createTitledBorder("FindBugs Annotation class (@SuppressFBWarnings). e.g. edu.umd.cs.findbugs.annotations.SuppressFBWarnings"));

			_annotationPathPanel.add(getAnnotationPathField(), "1, 1, 1, 1"); // col ,row, col, row


			final double rowsGap = 5;
			final double[][] bPanelSize = {{border, TableLayoutConstants.PREFERRED}, // Columns
										   {border, TableLayoutConstants.PREFERRED, rowsGap, TableLayoutConstants.PREFERRED, border}};// Rows
			final TableLayout tableLayout = new TableLayout(bPanelSize);

			final Container buttonPanel = new JPanel(tableLayout);
			_annotationPathPanel.add(buttonPanel, "3, 1, 3, 1");


			/*final AbstractButton addButton = new JButton();
			final Action action = new BrowseAction(_parent, "Browse...", new ExtensionFileFilter(Collections.singletonMap(".java")), new BrowseActionCallback() {
				public void addSelection(final File selectedFile) {

					_preferences.setModified(true);
				}
			});*/
			/*addButton.setAction(action);
			buttonPanel.add(addButton, "1, 1, 1, 1");*/


		}

		return _annotationPathPanel;
	}


	private AaTextField getAnnotationPathField() {
		if (_annotationPathField == null) {
			_annotationPathField = new AaTextField(30);
			_annotationPathField.setEditable(true);
			_annotationPathField.addTextChangeListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					_preferences.setAnnotationSuppressWarningsClass(_annotationPathField.getText());
				}
			});
		}
		return _annotationPathField;
	}


	JPanel getMarkUpPanel() {
		if (_markUpPanel == null) {
			final double border = 5;
			final double colsGap = 10;
			final double rowsGap = 15;
			final double[][] size = {{border, TableLayoutConstants.PREFERRED, colsGap, TableLayoutConstants.PREFERRED, border}, // Columns
									 {border, TableLayoutConstants.PREFERRED, rowsGap, TableLayoutConstants.PREFERRED, border}};// Rows
			final TableLayout tbl = new TableLayout(size);
			_markUpPanel = new JPanel(tbl) {
				@Override
				public void paint(final Graphics g) {
					super.paint(g);
					final Graphics2D graphics = (Graphics2D) g.create();
					graphics.setColor(GuiResources.HIGHLIGHT_COLOR_DARKER);
					GuiUtil.configureGraphics(graphics);
					graphics.setFont(getFont().deriveFont(Font.BOLD, 33f));
					graphics.rotate(.3);
					graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .7f));
					final String str = "Work in progress ...";
					final int strWidth = SwingUtilities.computeStringWidth(getFontMetrics(getFont()), str);
					graphics.drawString(str, getSize().width / 2 - strWidth / 2, 10);
					graphics.dispose();
				}
			};
			_markUpPanel.setBorder(BorderFactory.createTitledBorder("Annotation/MarkUp Settings"));

			_markUpPanel.add(getGutterIconCheckbox(), "1, 1, 1, 1"); // col ,row, col, row
			_markUpPanel.add(getAnnotationTypeSettingsPanel(), "1, 3, 1, 3"); // col ,row, col, row

		}

		return _markUpPanel;
	}


	private JPanel getAnnotationTypeSettingsPanel() {
		if (_typeSettingsPanel == null) {
			final double border = 5;
			final double colsGap = 10;
			final double rowsGap = 5;
			final double[][] size = {{border, TableLayoutConstants.PREFERRED, colsGap, TableLayoutConstants.PREFERRED, border}, // Columns
									 {border, TableLayoutConstants.PREFERRED, 10, TableLayoutConstants.PREFERRED, rowsGap, TableLayoutConstants.PREFERRED, border}};// Rows
			final TableLayout tbl = new TableLayout(size);
			_typeSettingsPanel = new JPanel(tbl);
			_typeSettingsPanel.setBorder(BorderFactory.createTitledBorder("Annotation type settings"));

			_typeSettingsPanel.add(getTextRangeMarkupCheckbox(), "1, 1, 3, 1"); // col ,row, col, row
			_typeSettingsPanel.add(getAnnotationTypeList(), "1, 3, 1, 3");
			_typeSettingsPanel.add(getAnnotationTypePanel() , "3, 3, 3, 3");
			getAnnotationTypeList().setSelectedIndex(0);

		}
		return _typeSettingsPanel;
	}

	@NotNull
	private JBList getAnnotationTypeList() {
		if (_annotationTypeList == null) {
			_annotationTypeList = new JBList((Object[])AnnotationType.values());
			_annotationTypeList.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(final ListSelectionEvent e) {
					if (!e.getValueIsAdjusting()) {
						getAnnotationTypePanel().setAnnotationType((AnnotationType) _annotationTypeList.getSelectedValue());
					}
				}
			});
		}
		return _annotationTypeList;
	}


	private JCheckBox getTextRangeMarkupCheckbox() {
		if (_enableTextRangeMarkUp == null) {
			_enableTextRangeMarkUp = new JCheckBox("Enable editor TextRange markup & Suppress bug pattern feature");
			_enableTextRangeMarkUp.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					final boolean selected = _enableTextRangeMarkUp.isSelected();
					getAnnotationTypePanel().setEnabled(selected);
					getAnnotationTypeList().setEnabled(selected);
					_preferences.setAnnotationTextRangeMarkupEnabled(selected);
				}
			});
		}
		return _enableTextRangeMarkUp;
	}


	private JCheckBox getGutterIconCheckbox() {
		if (_enableGutterIcon == null) {
			_enableGutterIcon = new JCheckBox("Enable editor GutterIcon markup");
			_enableGutterIcon.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					_preferences.setAnnotationGutterIconEnabled(_enableGutterIcon.isSelected());
				}
			});
		}
		return _enableGutterIcon;
	}


	public void setEnabled(final boolean enabled) {
		getGutterIconCheckbox().setEnabled(false);
		getTextRangeMarkupCheckbox().setEnabled(false);
		getAnnotationTypePanel().setEnabled(false);
		getAnnotationTypeList().setEnabled(false);
		getMarkUpPanel().setEnabled(false);
		getAnnotationPathPanel().setEnabled(enabled);
		getAnnotationPathField().setEditable(enabled);

	}


	public boolean showInModulePreferences() {
		return false;
	}


	public boolean isAdvancedConfig() {
		return true;
	}


	public String getTitle() {
		return "Annotations";
	}


	private AnnotationTypePanel getAnnotationTypePanel() {
		if (_annotationTypePanel == null) {
			_annotationTypePanel = new AnnotationTypePanel(this, AnnotationType.HighPriority);
		}
		return _annotationTypePanel;
	}


	public void filter(final String filter) {
		// TODO support search
	}


	private static class AnnotationTypePanel extends JPanel {

		private final AnnotationConfiguration _configuration;

		private JCheckBox _plainBox;
		private JCheckBox _italicBox;
		private JCheckBox _boldBox;
		private ColorBox _foreground;
		private ColorBox _background;
		private ColorBox _effectTypeColor;
		private AaComboBox _typeComboBox;
		private AnnotationType _annotationType;


		private AnnotationTypePanel(final AnnotationConfiguration configuration, final AnnotationType annotationType) {
			_configuration = configuration;
			_annotationType = annotationType;
			initGui();
		}


		private void initGui() {
			final double border = 5;
			final double colsGap = 10;
			final double rowsGap = 5;
			final double[][] size = {{border, TableLayoutConstants.PREFERRED, colsGap, TableLayoutConstants.FILL,  border}, // Columns
									 {border, TableLayoutConstants.PREFERRED, rowsGap, TableLayoutConstants.PREFERRED, rowsGap, TableLayoutConstants.PREFERRED, rowsGap, TableLayoutConstants.PREFERRED, border}};// Rows
			final LayoutManager tbl = new TableLayout(size);
			setLayout(tbl);

			final JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			_plainBox = new JCheckBox("plain");
			_plainBox.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					//TODO: implement
				}
			});
			fontPanel.add(_plainBox);

			_italicBox = new JCheckBox("italic");
			_italicBox.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					//TODO: implement
				}
			});
			fontPanel.add(_italicBox);

			_boldBox = new JCheckBox("bold");
			_boldBox.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					//TODO: implement
				}
			});
			fontPanel.add(_boldBox);

			final ButtonGroup group = new ButtonGroup();
			group.add(_plainBox);
			group.add(_italicBox);
			group.add(_boldBox);

			add(fontPanel, "1, 1, 3, 1");

			_foreground = new ColorBox(this, _annotationType.getForegroundColor(), 24, true);
			add(_foreground, "1, 3, 1, 3");
			add(new JLabel("Foreground"), "3, 3, 3, 3");

			_background = new ColorBox(this, _annotationType.getBackgroundColor(), 24, true);
			add(_background, "1, 5, 1, 5");
			add(new JLabel("Background"), "3, 5, 3, 5");


			final JPanel effectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			_typeComboBox = new AaComboBox<EffectType>(EffectType.values());
			_typeComboBox.addSelectionChangeListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					//TODO: implement
				}
			});
			_typeComboBox.setSelectedItem(_annotationType);

			_effectTypeColor = new ColorBox(this, _annotationType.getEffectColor(), 24, true);
			effectPanel.add(_effectTypeColor);
			effectPanel.add(new JLabel(" Effect"));
			effectPanel.add(_typeComboBox);

			add(effectPanel, "1, 7, 3, 7");
		}


		public void setAnnotationType(final AnnotationType annotationType) {
			_annotationType = annotationType;

			_foreground.setColor(annotationType.getForegroundColor());
			_background.setColor(annotationType.getBackgroundColor());
			_effectTypeColor.setColor(annotationType.getEffectColor());
			_typeComboBox.setSelectedItem(annotationType.getEffectType());

			final int fontType = annotationType.getFont();
			switch (fontType) {
				case Font.BOLD :
					_boldBox.setSelected(true);
					_italicBox.setSelected(false);
					_plainBox.setSelected(false);
					break;
				case Font.ITALIC :
					_boldBox.setSelected(false);
					_italicBox.setSelected(true);
					_plainBox.setSelected(false);
					break;
				case Font.PLAIN :
				default:
					_boldBox.setSelected(false);
					_italicBox.setSelected(false);
					_plainBox.setSelected(true);
			}

		}


		@Override
		public void setEnabled(final boolean enabled) {
			super.setEnabled(enabled);
			_foreground.setEnabled(enabled);
			_background.setEnabled(enabled);
			_effectTypeColor.setEnabled(enabled);
			_typeComboBox.setEnabled(enabled);
			_plainBox.setEnabled(enabled);
			_boldBox.setEnabled(enabled);
			_italicBox.setEnabled(enabled);
		}
	}


	private static class ColorBox extends JComponent {

		public static final String RGB = "RGB";
		public static final Color DISABLED_COLOR = UIManager.getColor("Panel.background");

		private final Dimension _size;
		private final boolean _isSelectable;
		private Runnable _selectColorAction;
		private Color _color;
		private final JComponent _parent;


		private ColorBox(final JComponent parent, final Color color, final int size, final boolean isSelectable) {
			_parent = parent;
			_size = new Dimension(size, size);
			_isSelectable = isSelectable;
			_color = color;
			setBorder(BorderFactory.createLineBorder(JBColor.GRAY));

			updateToolTip();
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(final MouseEvent mouseevent) {
					if (!isEnabled()) {
						return;
					}
					if (mouseevent.isPopupTrigger()) {
						selectColor();
					}
				}


				@Override
				public void mousePressed(final MouseEvent mouseevent) {
					if (!isEnabled()) {
						return;
					}
					if (mouseevent.getClickCount() == 2) {
						selectColor();
					} else {
						if (SwingUtilities.isLeftMouseButton(mouseevent)) {
							//setSelectedColor(myColor);
							//fireActionEvent();
						} else {
							if (mouseevent.isPopupTrigger()) {
								selectColor();
							}
						}
					}
				}
			});
		}


		public void setSelectColorAction(final Runnable selectColorAction) {
			_selectColorAction = selectColorAction;
		}


		private void selectColor() {
			if (_isSelectable) {
				final Color color = ColorChooser.chooseColor(_parent, UIBundle.message("color.panel.select.color.dialog.description"), _color);
				if (color != null) {
					setColor(color);
					if (_selectColorAction != null) {
						_selectColorAction.run();
					}
				}
			}
		}


		@Override
		public Dimension getMinimumSize() {
			return _size;
		}


		@Override
		public Dimension getMaximumSize() {
			return _size;
		}


		@Override
		public Dimension getPreferredSize() {
			return _size;
		}


		@Override
		public void paintComponent(final Graphics g) {
			if (isEnabled()) {
				g.setColor(_color);
			} else {
				g.setColor(DISABLED_COLOR);
			}
			g.fillRect(0, 0, getWidth(), getHeight());
		}


		private void updateToolTip() {
			if (_color == null) {
				return;
			}
			final StringBuilder buffer = new StringBuilder(64);
			buffer.append(RGB + ": ");
			buffer.append(_color.getRed());
			buffer.append(", ");
			buffer.append(_color.getGreen());
			buffer.append(", ");
			buffer.append(_color.getBlue());

			if (_isSelectable) {
				buffer.append(" (").append(UIBundle.message("color.panel.right.click.to.customize.tooltip.suffix")).append(')');
			}
			setToolTipText(buffer.toString());
		}


		public void setColor(final Color color) {
			_color = color;
			updateToolTip();
			repaint();
		}


		public Color getColor() {
			return _color;
		}
	}

}
