/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.wizardgenerated;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ModalDialogResult;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptAdapter.ScriptAdapterType;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptOption.OutputType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.UIFactory.TextChangedListener;
import com.opendoorlogistics.core.api.impl.scripts.ScriptOptionImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.ComponentConfig;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.OutputConfig;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.elements.ScriptBaseElement;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.scripts.parameters.ParametersImpl;
import com.opendoorlogistics.core.scripts.utils.AdapterExpectedStructureProvider;
import com.opendoorlogistics.core.scripts.utils.ScriptFieldsParser;
import com.opendoorlogistics.core.scripts.utils.ScriptFieldsParser.SourcedDatastore;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils.OptionVisitor;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils.OutputWindowSyncLevel;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils.ScriptIds;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.LayoutUtils;
import com.opendoorlogistics.core.utils.ui.ModalDialog;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.core.utils.ui.TextEntryPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.controls.ODLScrollableToolbar;
import com.opendoorlogistics.studio.scripts.componentwizard.SetupComponentWizard;
import com.opendoorlogistics.studio.scripts.editor.OutputPanel;
import com.opendoorlogistics.studio.scripts.editor.ScriptEditor;
import com.opendoorlogistics.studio.scripts.editor.ScriptEditorToolbar;
import com.opendoorlogistics.studio.scripts.editor.adapters.AdapterTablesTabControl;
import com.opendoorlogistics.studio.scripts.editor.adapters.UserFormulaEditor;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;
import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.utils.ui.ODLAction;
import com.opendoorlogistics.utils.ui.SimpleAction;
import com.opendoorlogistics.utils.ui.SimpleActionConfig;

final public class ScriptEditorWizardGenerated extends ScriptEditor {
	private static final Map<DisplayNodeType, Icon> iconsByType;
	private static final Icon openOptionIcon;
	private static final Icon closedOptionIcon;
	// private static final HashMap<Pair<String, Integer>, Icon> iconByComponent
	// = new HashMap<>();
	private static final String htmlHorizontalWhitespace;
	private MyScrollPane currentPane;
	private ArrayList<TreeAction> treeActions = new ArrayList<>();
	private JTree tree;
	private JSplitPane splitPane;
	private static AdapterConfig dataAdapterClipboard;
	private static Option optionClipboard;

	private static abstract class TreeAction extends SimpleAction{

		public TreeAction(SimpleActionConfig config) {
			super(config);
		}

		public TreeAction(String name, String tooltip, String smallIconPng, String largeIconPng) {
			super(name, tooltip, smallIconPng, largeIconPng);
		}

		public TreeAction(String name, String tooltip, String smallIconPng) {
			super(name, tooltip, smallIconPng);
		}

		public boolean addToToolbar(){
			return true;
		}
	}
	
	static {
		iconsByType = new HashMap<>();
		iconsByType.put(DisplayNodeType.COMPONENT_CONFIGURATION, Icons.loadFromStandardPath("script-element-component-config.png"));
		iconsByType.put(DisplayNodeType.DATA_ADAPTER, Icons.loadFromStandardPath("script-element-data-adapter.png"));
		iconsByType.put(DisplayNodeType.PARAMETER, Icons.loadFromStandardPath("parameter.png"));
		iconsByType.put(DisplayNodeType.INSTRUCTION, Icons.loadFromStandardPath("script-element-instruction.png"));
		// iconsByType.put(DisplayNodeType.INSTRUCTION, new
		// DefaultTreeCellRenderer().getLeafIcon());
		iconsByType.put(DisplayNodeType.AVAILABLE_TABLES, Icons.loadFromStandardPath("available-tables.png"));
		iconsByType.put(DisplayNodeType.COPY_TABLES, Icons.loadFromStandardPath("script-element-output.png"));

		StringBuilder spacesBuilder = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			spacesBuilder.append("&#xA0");
		}
		htmlHorizontalWhitespace = spacesBuilder.toString();

		openOptionIcon = Icons.loadFromStandardPath("script-option-open.png");
		closedOptionIcon = Icons.loadFromStandardPath("script-option-closed.png");
	}

	enum DisplayNodeType {
		OPTION, INSTRUCTION, DATA_ADAPTER, PARAMETER, COMPONENT_CONFIGURATION, COPY_TABLES, AVAILABLE_TABLES
	}

	class DisplayNode implements TreeNode {
		DisplayNodeType type;
		Option option;
		AdapterConfig adapter;
		ComponentConfig componentConfig;
		InstructionConfig instruction;
		String displayName;
		boolean isRoot;
		final List<OutputConfig> outputs = new ArrayList<>();
		final List<DisplayNode> children = new ArrayList<>();
		DisplayNode parent;

		ScriptBaseElement getElement() {
			switch (type) {
			case OPTION:
				return option;

			case DATA_ADAPTER:
			case PARAMETER:
				return adapter;

			case COMPONENT_CONFIGURATION:
				return componentConfig;

			case INSTRUCTION:
				return instruction;

			case COPY_TABLES:
				return outputs.get(0);
			}
			throw new UnsupportedOperationException();
		}

		DisplayNode getRoot() {
			DisplayNode ret = this;
			while (ret.parent != null) {
				ret = ret.parent;
			}
			return ret;
		}

		private void createInfoPanel(VerticalLayoutPanel panel) {
			if (type == DisplayNodeType.OPTION) {
				throw new RuntimeException();
			}
			String elementName = Strings.convertEnumToDisplayFriendly(type);

			StringBuilder htmlBuilder = new StringBuilder();
			htmlBuilder.append("<html><h2>" + elementName + ": " + displayName + "</h2>");

			// add id
			switch (type) {
			case PARAMETER:
			case DATA_ADAPTER:
				htmlBuilder.append("<Strong>ID</Strong> : " + adapter.getId() + htmlHorizontalWhitespace);
				break;

			case COMPONENT_CONFIGURATION:
				htmlBuilder.append("<Strong>ID</Strong> : " + componentConfig.getConfigId() + htmlHorizontalWhitespace);
				break;
			default:
				break;
			}

			// add name
			ScriptBaseElement element = getElement();
			String name = element.getName();
			htmlBuilder.append("<Strong>Name</Strong> : " + (Strings.isEmpty(name) ? "<none>" : name));

			// add editable
			htmlBuilder.append(htmlHorizontalWhitespace);
			htmlBuilder.append("<Strong>Editable</Strong> : " + (element.isUserCanEdit() ? "yes" : "no"));

			// work out what vertical spread we should use
			boolean extraSpread = false;
			switch (type) {
			case COPY_TABLES:
				extraSpread = true;
				break;

			case COMPONENT_CONFIGURATION:
				extraSpread = true;
				break;

			case INSTRUCTION:
				extraSpread = true;
				break;

			default:
				break;
			}
			String vspace = "<br/>";
			if (extraSpread) {
				vspace += "<br/>";
			}

			// type specific
			switch (type) {
			case INSTRUCTION: {
				String componentName = ScriptUtils.getComponentName(instruction);
				// htmlBuilder.append(vspace + "<Strong>Input datastore</Strong>
				// : " + instruction.getDatastore());
				htmlBuilder.append(vspace + "<Strong>Calls component</Strong> : " + (componentName != null ? componentName : ""));
			}
				break;

			case COMPONENT_CONFIGURATION: {
				String componentName = ScriptUtils.getComponentName(componentConfig);
				htmlBuilder.append(vspace + "<Strong>Configuration for component</Strong> : " + (componentName != null ? componentName : ""));
			}
				break;
			default:
				break;
			}

			// htmlBuilder.append("<br/><br/><hr/></html>");
			htmlBuilder.append("</html>");
			addLabel(panel, htmlBuilder.toString(), true);
			// panel.addWhitespace(10);
		}

		/**
		 * Add the label with an editor. Editor only available in multipane
		 * view. Label will not be added in single pane view if its empty
		 * 
		 * @param scriptElement
		 * @param panel
		 * @param isMultipane
		 * @return True if the label was added
		 */
		private boolean addLabelWithEditor(final ScriptBaseElement scriptElement, VerticalLayoutPanel panel, boolean isMultipane) {
			// only allow editing if we're in multipane mode
			if (!isMultipane) {
				if (Strings.isEmpty(scriptElement.getEditorLabel()) == false) {
					addLabel(panel, scriptElement.getEditorLabel(), true);
					return true;
				}
				return false;
			}

			final String defaultText = "<html><i>...You can add your own notes here, just right click to edit them...</i></html>";
			final String text;
			if (Strings.isEmpty(scriptElement.getEditorLabel())) {
				text = defaultText;
			} else {
				text = scriptElement.getEditorLabel();
			}

			JLabel label = new JLabel(text);
			// label.setMaximumSize(new Dimension(400, 100));
			// label.setPreferredSize(new Dimension(400, 60));
			setDefaultBorder(label);
			label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(4, 4, 4, 4)));

			label.addMouseListener(new MouseListener() {

				@Override
				public void mouseReleased(MouseEvent e) {
					launchPopup(e);
				}

				@Override
				public void mousePressed(MouseEvent e) {
					launchPopup(e);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void mouseEntered(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void mouseClicked(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				private void launchPopup(MouseEvent e) {
					if (e.isPopupTrigger()) {
						JPopupMenu popup = new JPopupMenu();
						popup.add(new AbstractAction("Clear note", Icons.loadFromStandardPath("script-note-clear.png")) {

							@Override
							public void actionPerformed(ActionEvent e) {
								scriptElement.setEditorLabel("<html></html>");
								rebuildActivePanel();
							}
						});
						popup.add(new AbstractAction("Edit note", Icons.loadFromStandardPath("script-note-edit.png")) {

							@Override
							public void actionPerformed(ActionEvent e) {
								// launch a text editor
								String s = scriptElement.getEditorLabel();
								if (Strings.isEmpty(s)) {
									s = defaultText;
								}

								final JTextArea textArea = new JTextArea(s);
								textArea.setLineWrap(true);
								textArea.setWrapStyleWord(true);
								textArea.setEditable(true);
								textArea.setPreferredSize(new Dimension(600, 300));

								OkCancelDialog dlg = new OkCancelDialog(SwingUtilities.getWindowAncestor(ScriptEditorWizardGenerated.this)) {
									@Override
									protected Component createMainComponent(boolean inWindowsBuilder) {
										return new JScrollPane(textArea);
									}
								};
								dlg.setTitle("Enter note text");

								if (dlg.showModal() == OkCancelDialog.OK_OPTION) {
									scriptElement.setEditorLabel(textArea.getText());
									rebuildActivePanel();
								}

							}
						});
						popup.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			});

			panel.add(label);
			panel.addHalfWhitespace();
			return true;
		}

		/**
		 * Create the pane to display this component
		 * 
		 * @return
		 */
		MyScrollPane createPane(boolean isMultiPane) {
			MyScrollPane ret = null;
			if (isMultiPane) {
				if (type == DisplayNodeType.OPTION) {
					return createOptionPane();
				}
				if (type == DisplayNodeType.AVAILABLE_TABLES) {
					return createAvailableTablesPane();
				} else {
					ret = new MyScrollPane(this);
					createInfoPanel(ret.panel);
				}
			} else {
				ret = new MyScrollPane(this);
			}

			// add adapter
			if (adapter != null) {

				// add adapter's label
				addLabelWithEditor(adapter, ret.panel, isMultiPane);

				if (adapter.isUserCanEdit()) {

					// build the object which provides the adapter's destination
					AdapterExpectedStructureProvider dfnProvider = ScriptUtils.createAdapterExpectedStructure(api, script, option, adapter.getId());

					// show all flags for every adapter - even report key - even
					// though they're not used on each one
					long visibleColumnFlags = TableFlags.FLAG_IS_OPTIONAL | TableFlags.FLAG_IS_GROUP_BY_FIELD | TableFlags.FLAG_IS_BATCH_KEY | TableFlags.FLAG_IS_REPORT_KEYFIELD;

					AdapterTablesTabControl tabControl = new AdapterTablesTabControl(api, adapter, visibleColumnFlags, createAvailableOptionsQuery(), dfnProvider,
							ScriptEditorWizardGenerated.this.runner) {
						protected List<ODLAction> createTabPageActions(final AdaptedTableConfig table) {
							List<ODLAction> ret = super.createTabPageActions(table);

							ret.add(new SimpleAction(new SimpleActionConfig("Show generated table", "Show the table generated by the selected adapted table.", "table-go.png", null, true)) {

								@Override
								public void actionPerformed(ActionEvent e) {
									// view table
									executeAdapterResultViewer(table, false);
								}
							});

							ret.add(new SimpleAction(new SimpleActionConfig("Show generated map", "Show the map generated by the selected adapted table.", "world.png", null, true)) {

								@Override
								public void actionPerformed(ActionEvent e) {
									// view table
									executeAdapterResultViewer(table, true);
								}

								@Override
								public void updateEnabledState() {
									long flags = adapter.getFlags();
									if (table != null) {
										flags |= table.getFlags();
									}
									setEnabled((flags & TableFlags.FLAG_IS_DRAWABLES) == TableFlags.FLAG_IS_DRAWABLES);
								}
							});
							return ret;
						}
					};

					ret.adapterTabControls.add(tabControl);
					ret.panel.addNoWrap(tabControl);
					ret.panel.addHalfWhitespace();
				}
			}

			// add stand-alone component config
			if (componentConfig != null) {
				// get component
				ODLComponent component = ODLGlobalComponents.getProvider().getComponent(componentConfig.getComponent());
				if (component == null) {
					throw new RuntimeException("Unknown component: " + componentConfig.getComponent());
				}

				// create user panel
				JPanel userPanel = null;
				if (componentConfig.isUserCanEdit()) {
					if (component.getConfigClass() != null) {
						ScriptUtils.validateComponentConfigClass(component, componentConfig);
						userPanel = component.createConfigEditorPanel(createComponentEditorAPI(component.getId(), option, instruction), -1, componentConfig.getComponentConfig(), true);
					}
				}

				addLabelWithEditor(componentConfig, ret.panel, isMultiPane);

				// add the user panel
				if (userPanel != null) {
					// userPanel.setBorder(BorderFactory.createEmptyBorder(5, 5,
					// 5, 5));
					// userPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
					if (isMultiPane) {
						setMultiPaneBorder(userPanel);
					}
					ret.panel.add(userPanel);
					ret.panel.addHalfWhitespace();
				}
			}

			// add instruction
			if (instruction != null) {
				// add instruction's label
				boolean hasInstructionLabel = addLabelWithEditor(instruction, ret.panel, isMultiPane);

				if (instruction.isUserCanEdit()) {

					if (isMultiPane) {
						// add text entry box for the component
						TextEntryPanel dsid = new TextEntryPanel("Input datastore id: ", instruction.getDatastore(), new TextChangedListener() {

							@Override
							public void textChange(String newText) {
								instruction.setDatastore(newText);
							}
						});
						dsid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						ret.panel.add(dsid);
						ret.panel.addHalfWhitespace();
						
						// and for reports top label
						TextEntryPanel reportsTopLaber = new TextEntryPanel("Formula for label at the top of controls(s): ", instruction.getReportTopLabelFormula(), new TextChangedListener() {

							@Override
							public void textChange(String newText) {
								instruction.setReportTopLabelFormula(newText);
							}
						});
						reportsTopLaber.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						ret.panel.add(reportsTopLaber);
						ret.panel.addHalfWhitespace();
						
					}

					ODLComponent component = ODLGlobalComponents.getProvider().getComponent(instruction.getComponent());
					if (component == null) {
						throw new RuntimeException("Unknown component: " + instruction.getComponent());
					}
					if (component.getConfigClass() != null) {
						ScriptUtils.validateComponentConfigClass(component, instruction);
						Serializable inplaceConfig = instruction.getComponentConfig();
						if (inplaceConfig != null) {
							JPanel userPanel = component.createConfigEditorPanel(createComponentEditorAPI(component.getId(), option, instruction), instruction.getExecutionMode(), inplaceConfig, true);
							if (userPanel != null) {

								if (isMultiPane) {
									setMultiPaneBorder(userPanel);
								} else {
									if (adapter != null || outputs.size() > 0) {
										// If we have other components on the
										// frame, add a border with the text
										// "Setting for ..."
										if (hasInstructionLabel) {
											userPanel.setBorder(LayoutUtils.createInsetTitledBorder(""));
										} else {
											String text = "Settings for " + displayName;
											userPanel.setBorder(LayoutUtils.createInsetTitledBorder(text));
										}
									}
								}

								// else {
								// // add a default label
								// if (hasInstructionLabel == false) {
								// String text = "<html><h3>Settings for <i>" +
								// displayName + "</i>...</h3></html>";
								// addLabel(ret.panel, text,true);
								// }
								// userPanel.setBorder(BorderFactory.createEmptyBorder(5,
								// 5, 5, 5));
								// }
								ret.panel.add(userPanel);
								ret.panel.addHalfWhitespace();
							}
						}
					}
				}
			}

			// add outputs
			for (OutputConfig output : outputs) {
				addLabelWithEditor(output, ret.panel, isMultiPane);

				if (output.isUserCanEdit()) {
					OutputPanel outputPanel = new OutputPanel(output, true, OutputType.values());
					if (isMultiPane) {
						setMultiPaneBorder(outputPanel);
					} else {
						String borderTitle;
						if (Strings.isEmpty(output.getInputTable())) {
							borderTitle = "Output new table(s)";
						} else {
							borderTitle = "Output table \"" + output.getInputTable() + "\"";
						}
						outputPanel.setBorder(LayoutUtils.createInsetTitledBorder(borderTitle));
					}
					ret.panel.add(outputPanel);
				}
			}

			// if we're not multipane and we have children then add them as tabs
			if (!isMultiPane && children.size() > 0) {
				JTabbedPane tabbedPane = new JTabbedPane();
				for (DisplayNode node : children) {
					tabbedPane.addTab(node.displayName, node.createPane(false));
				}
				ret.panel.add(tabbedPane);
			}
			return ret;
		}

		private MyScrollPane createAvailableTablesPane() {
			// create table listing control
			TableListingsModel model = new TableListingsModel(api, this, runner != null ? runner.getDatastoreDefinition() : null);
			final TableListings table = new TableListings();
			table.setModel(model);

			// create return scroll pane
			MyScrollPane ret = new MyScrollPane(this) {
				@Override
				public void updateAppearance() {
					super.updateAppearance();

					// available tables may have changed if spreadsheet opened /
					// closed
					table.setModel(new TableListingsModel(api, DisplayNode.this, runner != null ? runner.getDatastoreDefinition() : null));
				}
			};

			addLabel(ret.panel,
					"<html><h2>Available tables</h2>The following tables are available to instructions within this option." + "<br/>Tables coloured in green are created within this option.</html>",
					true);

			// add table in its own scroll pane
			JScrollPane scrollPane = new JScrollPane(table);
			scrollPane.setPreferredSize(new Dimension(500, 160));
			setDefaultBorder(scrollPane);
			ret.panel.add(scrollPane);

			addLabel(ret.panel, "<html>The selected table has the following columns:</html>", true);

			// add table for fields in selected table
			final FieldListings fieldListings = new FieldListings();
			scrollPane = new JScrollPane(fieldListings);
			scrollPane.setPreferredSize(new Dimension(500, 160));
			setDefaultBorder(scrollPane);
			ret.panel.add(scrollPane);

			// add selection listener to update the fields table
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(ListSelectionEvent e) {
					int row = table.getSelectedRow();
					ODLTableDefinition dfn = null;
					if (row != -1) {
						dfn = ((TableListingsModel) table.getModel()).getTableDefinition(row);
					}

					fieldListings.set(dfn);
				}
			});

			if (table.getRowCount() > 0) {
				table.getSelectionModel().setSelectionInterval(0, 0);
			}

			return ret;
		}

		/**
		 * @return
		 */
		private MyScrollPane createOptionPane() {
			MyScrollPane ret = new MyScrollPane(this);

			// add labels
			String title;
			if (isRoot) {
				title = "Script";
			} else if (ScriptUtils.isRunnableOption(this.option)) {
				title = "Executable script option";
			} else {
				title = "Script option";
			}
			addLabel(ret.panel, "<html><h1>" + title + "</h1><h2><em>" + displayName + "</em></h2></html>", false);

			// add label for the option
			addLabelWithEditor(option, ret.panel, true);

			// ret.panel.setAlignmentX(Component.LEFT_ALIGNMENT);

			// add overrides for parameters
			if (api.scripts().parameters().getControlFactory() != null) {
				ret.panel.addWhitespace();

				JCheckBox overrideParamsCB = new JCheckBox("Override visible parameters?", option.isOverrideVisibleParameters());
				overrideParamsCB.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				JLabel label = new JLabel("<html>Define as a comma-separated line in the format:<br><i>[PROMPT_TYPE] parametername, e.g. ATTACH Potential, Sales, POPUP Workload, ...</i><html>");
				label.setBorder(BorderFactory.createEmptyBorder(5, 5, 2, 5));
				ret.panel.add(overrideParamsCB);
				ret.panel.add(label);
				JTextField editCtrl = new JTextField(option.getVisibleParametersOverride() != null ? option.getVisibleParametersOverride() : "");
				overrideParamsCB.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						option.setOverrideVisibleParameters(overrideParamsCB.isSelected());
						editCtrl.setEnabled(option.isOverrideVisibleParameters());
					}
				});
				
				editCtrl.setMaximumSize(new Dimension(200, 26));
				editCtrl.setEnabled(option.isOverrideVisibleParameters());

				// hack - wrap edit control in additional to get formatting
				// right
				JPanel editPanel = new JPanel();
				editPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				editPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
				editPanel.setLayout(new BorderLayout());
				editPanel.add(editCtrl, BorderLayout.CENTER);
				ret.panel.add(editPanel);
				editCtrl.getDocument().addDocumentListener(new DocumentListener() {

					@Override
					public void removeUpdate(DocumentEvent e) {
						readUI();
					}

					@Override
					public void insertUpdate(DocumentEvent e) {
						readUI();
					}

					@Override
					public void changedUpdate(DocumentEvent e) {
						readUI();
					}

					void readUI() {
						option.setVisibleParametersOverride(editCtrl.getText());
					}
				});

				// add checkbox for refresh always visible
				ret.panel.add(Box.createVerticalGlue());
				JCheckBox refreshAlwaysVisibleCB = new JCheckBox("Refresh button always enabled?", option.isRefreshButtonAlwaysEnabled());
				refreshAlwaysVisibleCB.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				refreshAlwaysVisibleCB.addActionListener((e)->option.setRefreshButtonAlwaysEnabled(refreshAlwaysVisibleCB.isSelected()));
				ret.panel.add(refreshAlwaysVisibleCB);

				// and last refreshed label
				ret.panel.add(Box.createVerticalGlue());
				JCheckBox lastRefreshedLabel = new JCheckBox("Show last refreshed time?", option.isShowLastRefreshedTime());
				lastRefreshedLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				lastRefreshedLabel.addActionListener((e)->option.setShowLastRefreshedTime(lastRefreshedLabel.isSelected()));
				ret.panel.add(lastRefreshedLabel);
				
				
				// create glue to swallow the spare space
				ret.panel.add(Box.createVerticalGlue());
			}
			
			// add user formula editor panel
			if(isRoot){
				ret.panel.addWhitespace();
				if(script.getUserFormulae()==null){
					script.setUserFormulae(new ArrayList<>());
				}
				JPanel userformula =UserFormulaEditor.createUserFormulaListPanel(script.getUserFormulae());
				userformula.setBorder(BorderFactory.createTitledBorder("Add global formulae here..."));
				ret.panel.add(userformula);
			}
			
			return ret;
		}

		/**
		 * @param panel
		 */
		private void setMultiPaneBorder(JPanel panel) {
			panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		}

		private void addLabel(VerticalLayoutPanel panel, String text, boolean halfspaceAfter) {

			JLabel label = new JLabel(text);
			setDefaultBorder(label);
			panel.add(label);
			if (halfspaceAfter) {
				panel.addHalfWhitespace();
			}

			// editorPane.addHyperlinkListener(new HyperlinkListener() {
			// /**
			// * See
			// http://stackoverflow.com/questions/3693543/hyperlink-in-jeditorpane
			// */
			// public void hyperlinkUpdate(HyperlinkEvent e) {
			// if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			// if (Desktop.isDesktopSupported()) {
			//
			// try {
			// URL url = e.getURL();
			// URI uri = url.toURI();
			// Desktop.getDesktop().browse(uri);
			// } catch (Throwable e1) {
			// // TODO Auto-generated catch block
			// // e1.printStackTrace();
			// }
			//
			// }
			// }
			// }
			// });
		}

		/**
		 * @param component
		 */
		private void setDefaultBorder(JComponent component) {
			component.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		}

		int getNodeCount() {
			int ret = 1;
			for (DisplayNode child : children) {
				ret += child.getNodeCount();
			}
			return ret;
		}

		@Override
		public TreeNode getChildAt(int childIndex) {
			return children.get(childIndex);
		}

		@Override
		public int getChildCount() {
			return children.size();
		}

		@Override
		public TreeNode getParent() {
			return parent;
		}

		@Override
		public int getIndex(TreeNode node) {
			for (int i = 0; i < children.size(); i++) {
				if (children.get(i) == node) {
					return i;
				}
			}
			return -1;
		}

		@Override
		public boolean getAllowsChildren() {
			return true;
		}

		@Override
		public boolean isLeaf() {
			return children.size() == 0;
		}

		@Override
		public Enumeration children() {
			final Iterator<DisplayNode> it = children.iterator();
			return new Enumeration<TreeNode>() {

				@Override
				public boolean hasMoreElements() {
					return it.hasNext();
				}

				@Override
				public TreeNode nextElement() {
					return it.next();
				}
			};
		}

		@Override
		public String toString() {
			return displayName;
		}
	}

	private static ComponentConfig findInstructionWithInputDS(String dsid, Iterable<InstructionConfig> instructions) {
		for (InstructionConfig inst : instructions) {
			if (Strings.equalsStd(dsid, inst.getDatastore())) {
				return inst;
			}
		}
		return null;
	}

	// private static ComponentConfig findInstructionWithOutputDS(String dsid,
	// Iterable<InstructionConfig> instructions) {
	// for (InstructionConfig inst : instructions) {
	// if (Strings.equalsStd(dsid, inst.getOutputDatastore())) {
	// return inst;
	// }
	// }
	// return null;
	// }

	private class Splitter {

		DisplayNode splitIntoDisplayNodes(Script script, boolean isSingleFrameView) {
			// boolean isSingleFrameView = ScriptUtils.getOptionsCount(script)
			// <= 1 && script.getAdapters().size()<=1;
			DisplayNode ret = recurseSplitIntoDisplayNodes(script, isSingleFrameView);
			fixParentReferences(null, ret);
			ret.isRoot = true;
			return ret;
		}

		private void fixParentReferences(DisplayNode parent, DisplayNode node) {
			node.parent = parent;
			for (DisplayNode child : node.children) {
				fixParentReferences(node, child);
			}
		}

		/**
		 * Split the script up into different display nodes to show in a tree
		 * 
		 * @param script
		 * @return
		 */
		private DisplayNode recurseSplitIntoDisplayNodes(Option option, boolean isSingleFrameView) {

			DisplayNode ret = new DisplayNode();
			ret.displayName = Strings.isEmpty(option.getName()) ? option.getOptionId() : option.getName();
			ret.type = DisplayNodeType.OPTION;
			ret.option = option;

			// unconnected adapters go on their own, at the start
			ArrayList<DisplayNode> unconnectedAdapters = new ArrayList<>();
			for (AdapterConfig adapter : option.getAdapters()) {
				if (adapter.isUserCanEdit() && (isSingleFrameView == false || findInstructionWithInputDS(adapter.getId(), option.getInstructions()) == null)) {
					unconnectedAdapters.add(createUnconnectedAdapterNode(option, adapter));
				}
			}

			// then come standalone configs
			ArrayList<DisplayNode> standaloneConfigs = new ArrayList<>();
			for (ComponentConfig config : option.getComponentConfigs()) {
				if (config.isUserCanEdit()) {
					DisplayNode node = new DisplayNode();
					node.option = option;
					node.type = DisplayNodeType.COMPONENT_CONFIGURATION;
					node.componentConfig = config;
					node.displayName = Strings.isEmpty(config.getName()) ? config.getConfigId() : config.getName();
					standaloneConfigs.add(node);
				}
			}

			// each instruction has its own node, together with any connected
			// adapters and outputs
			ArrayList<DisplayNode> instructions = new ArrayList<>();
			HashSet<OutputConfig> connectedOutputs = new HashSet<>();
			for (InstructionConfig instruction : option.getInstructions()) {
				DisplayNode node = new DisplayNode();
				node.option = option;
				node.type = DisplayNodeType.INSTRUCTION;
				node.instruction = instruction;
				node.displayName = Strings.isEmpty(instruction.getName()) ? ScriptUtils.getComponentName(instruction) : instruction.getName();
				if (node.displayName == null) {
					node.displayName = "Instruction";
				}

				for (AdapterConfig adapter : option.getAdapters()) {
					if (adapter.isUserCanEdit() && isSingleFrameView && Strings.equalsStd(instruction.getDatastore(), adapter.getId())) {
						node.adapter = adapter;
						break;
					}
				}

				for (OutputConfig output : option.getOutputs()) {
					// link output if they output from the instruction or if the
					// instruction owns the adapter and
					// they output the adapter contents
					if (output.isUserCanEdit() && isSingleFrameView && (Strings.equalsStd(instruction.getOutputDatastore(), output.getDatastore())
							|| (node.adapter != null && Strings.equalsStd(node.adapter.getId(), output.getDatastore())))) {
						node.outputs.add(output);
						connectedOutputs.add(output);
					}
				}

				// try measuring the instruction config height and unconnect the
				// adapter if its too high
				if (node.adapter != null) {
					ODLComponent component = ScriptUtils.getComponent(instruction);
					if (component != null) {
						ScriptUtils.validateComponentConfigClass(component, instruction);
						JPanel panel = component.createConfigEditorPanel(createComponentEditorAPI(component.getId(), option, instruction), instruction.getExecutionMode(),
								instruction.getComponentConfig(), false);
						if (panel != null) {
							double prefHeight = panel.getPreferredSize().getHeight();
							if (prefHeight > 300) {
								unconnectedAdapters.add(createUnconnectedAdapterNode(option, node.adapter));
								node.adapter = null;
							}
						}
					}
				}
				instructions.add(node);
			}

			// unconnected outputs go on their own at the end
			ArrayList<DisplayNode> unconnectedOutputs = new ArrayList<>();
			for (OutputConfig output : option.getOutputs()) {
				if (output.isUserCanEdit() && connectedOutputs.contains(output) == false) {
					DisplayNode node = new DisplayNode();
					node.option = option;
					node.type = DisplayNodeType.COPY_TABLES;
					node.outputs.add(output);
					node.displayName = Strings.isEmpty(output.getName()) ? "Copy table(s)" : output.getName();
					unconnectedOutputs.add(node);
				}
			}

			// merge into one node if not too many items and merge is allowed
			ArrayList<DisplayNode> allNodes = new ArrayList<>();
			allNodes.addAll(unconnectedAdapters);
			allNodes.addAll(standaloneConfigs);
			allNodes.addAll(instructions);
			allNodes.addAll(unconnectedOutputs);
			if (allNodes.size() == 1 && isSingleFrameView) {
				mergeChild(ret, allNodes.get(0));
			} else {
				// also create node for available tables
				if (isSingleFrameView == false) {
					DisplayNode availableTables = new DisplayNode();
					availableTables.type = DisplayNodeType.AVAILABLE_TABLES;
					availableTables.option = option;
					availableTables.displayName = "Available tables";
					ret.children.add(availableTables);
				}

				ret.children.addAll(allNodes);
			}

			// parse and add child options
			for (Option child : option.getOptions()) {
				ret.children.add(recurseSplitIntoDisplayNodes(child, isSingleFrameView));
			}

			return ret;
		}

		/**
		 * @param option
		 * @param adapter
		 * @return
		 */
		private DisplayNode createUnconnectedAdapterNode(Option option, AdapterConfig adapter) {
			DisplayNode node = new DisplayNode();
			node.option = option;
			node.type = (adapter != null && adapter.getAdapterType() == ScriptAdapterType.PARAMETER) ? DisplayNodeType.PARAMETER : DisplayNodeType.DATA_ADAPTER;
			node.adapter = adapter;

			// just use the adapter id as we use this in formulae and allow the
			// user to change it in the IU
			node.displayName = adapter.getId();
			return node;
		}

		private void mergeChild(DisplayNode parent, DisplayNode child) {
			// add child's children
			parent.children.clear();
			parent.children.addAll(child.children);

			// take child's name if we don't already have name
			if (Strings.isEmpty(parent.displayName)) {
				parent.displayName = child.displayName;
			}

			// take child's adapter, instruction and outputs BUT NOT THE TYPE
			parent.adapter = child.adapter;
			parent.instruction = child.instruction;
			parent.componentConfig = child.componentConfig;
			parent.outputs.addAll(child.outputs);
		}

	}

	/**
	 * Wizard generated scripts need to support options... Options are
	 * hierarchical and only leaf options are runnable. We can assume that an
	 * adapter is owned by the instruction which references it if its part of
	 * the same option. Same for outputs.
	 * 
	 * It is possible an adapter might go to several instructions (not in the
	 * same option).
	 * 
	 * Options appear as hierarchical tabs where anything where the parent tab
	 * is the first and the later ones appear in the same tab sheet and are
	 * called 'option X', 'option Y' etc?
	 * 
	 */

	public ScriptEditorWizardGenerated(ODLApi api, Script script, File file, String optionId, ScriptUIManager runner) {
		super(api, script, file, runner);

		// ensure script has valid synchronisation settings
		if (ScriptUtils.validateSynchonisation(api, script) == false) {
			throw new RuntimeException("Invalid script.");
		}

		initPanels(script, optionId, UseTree.UNDECIDED);

		// ensure control isn't too high
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 800));

		reinitialiseToolbar();
		pack();

		updateAppearance();

	}

	/**
	 * @param script
	 * @param optionId
	 */
	private void initPanels(Script script, String optionId, UseTree useTree) {
		treeActions.clear();

		// always use tree if we have more than one option...
		if (script.getOptions() != null && script.getOptions().size() > 0) {
			useTree = UseTree.YES;
		} else {
			// only one option...
			if (useTree == UseTree.UNDECIDED) {
				useTree = UseTree.NO;
			}
		}

		DisplayNode rootNode = new Splitter().splitIntoDisplayNodes(script, useTree == UseTree.NO);

		contentPane.removeAll();

		if (useTree == UseTree.YES) {
			splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			splitPane.setResizeWeight(0.3);

			createTree(optionId, rootNode);

			initTreeActions();

			JPanel treePanel = new JPanel();
			treePanel.setLayout(new BorderLayout());
			treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
			treePanel.add(createTreeToolBar(), BorderLayout.SOUTH);

			splitPane.setLeftComponent(treePanel);
			contentPane.add(splitPane, BorderLayout.CENTER);
		} else {
			currentPane = rootNode.createPane(false);
			contentPane.add(currentPane, BorderLayout.CENTER);
			tree = null;
		}
	}

	/**
	 * @param optionId
	 * @param rootNode
	 * @param splitter
	 * @return
	 */
	private JTree createTree(String optionId, DisplayNode rootNode) {
		tree = new JTree(rootNode);
		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				Component ret = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				DisplayNode node = (DisplayNode) value;
				Icon icon = null;
				switch (node.type) {
				case OPTION:
					// never show as leaf node
					if (expanded) {
						icon = openOptionIcon;// getOpenIcon();
					} else {
						icon = closedOptionIcon;// getClosedIcon();
					}
					setText("<html><strong>" + node.displayName + "</strong></html>");
					break;

				default:
					// always show as a leaf node
					icon = iconsByType.get(node.type);
					if (node.type == DisplayNodeType.AVAILABLE_TABLES) {
						setText("<html><em>" + node.displayName + "</em></html>");
					} else {
						setText("<html>" + node.displayName + "</html>");
					}
					break;
				}

				setIcon(icon);

				return ret;
			}
		});

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				rebuildActivePanel();
			}
		});
		tree.setRootVisible(true);
		tree.setEditable(false);

		tree.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				ensureSelected(e);
				launchPopup(e);
			}

			private void ensureSelected(MouseEvent e) {
				// ensure correct one is selected
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if (path != null) {
					tree.getSelectionModel().setSelectionPath(path);
				}
			}

			private void launchPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {

					TreePath path = tree.getPathForLocation(e.getX(), e.getY());
					if (path != null && path.getLastPathComponent() != null) {
						JPopupMenu popup = new JPopupMenu();
						for (Action action : treeActions) {
							popup.add(action);
						}
						popup.show(e.getComponent(), e.getX(), e.getY());
					}

				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				ensureSelected(e);
				launchPopup(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// System.out.println("mouse clicked " + e.getSource());

			}
		});

		// expand the root only (looks too complicated otherwise)
		tree.expandRow(0);

		// set the starting path using the input id if we have one
		selectOption(optionId, rootNode, tree);
		return tree;
	}

	/**
	 * @param optionId
	 * @param rootNode
	 * @param tree
	 */
	private static void selectOption(String optionId, DisplayNode rootNode, JTree tree) {
		TreePath path = null;
		if (!Strings.isEmpty(optionId)) {
			path = getTreePath(optionId, rootNode);
		}
		if (path == null) {
			path = new TreePath(rootNode);
		}
		tree.setSelectionPath(path);
	}

	private JToolBar createTreeToolBar() {
		JToolBar ret = new ODLScrollableToolbar().getToolBar();
		for (TreeAction action : treeActions) {
			if(action.addToToolbar()){
				ret.add(action);				
			}
		}
		ret.setFloatable(false);
		return ret;
	}

	public void setSelectedOption(String optionId) {
		if (tree != null) {
			reinitTree(optionId);
		}
	}

	private static TreePath getTreePath(final String optionID, DisplayNode root) {

		class Parser {
			TreePath parse(DisplayNode current, ArrayList<TreeNode> path) {
				// copy path
				path = new ArrayList<>(path);

				// add current
				path.add(current);

				// check for match
				if (Strings.equals(current.option.getOptionId(), optionID)) {
					return new TreePath(path.toArray());
				}

				// parse children
				for (int i = 0; i < current.getChildCount(); i++) {
					TreePath ret = parse((DisplayNode) current.getChildAt(i), path);
					if (ret != null) {
						return ret;
					}
				}
				return null;
			}
		}

		return new Parser().parse(root, new ArrayList<TreeNode>());
	}

	protected void messageBoxClose(String s) {
		JOptionPane.showMessageDialog(this, "This script is corrupt and will be closed");
		dispose();
	}

	private enum UseTree {
		YES, NO, UNDECIDED
	}

	protected ScriptEditorToolbar createToolbar() {

		ScriptEditorToolbar ret = new ScriptEditorToolbar(isRunScriptAllowed(),
				currentPane != null ? currentPane.displayNode.option.isSynchronised() : false,
						isRunScriptAllowed(),
				currentPane != null ? currentPane.displayNode.option.isLaunchMultiple() : false) {

			@Override
			protected void syncBoxChanged(boolean isSelected) {
				if (currentPane != null) {
					currentPane.displayNode.option.setSynchronised(isSelected);
				}
			}

			@Override
			protected boolean isSyncBoxEnabled() {
				if (isRunScriptAllowed()) {
					return ScriptUtils.getOutputWindowSyncLevel(api, script, currentPane.displayNode.option.getOptionId()) == OutputWindowSyncLevel.MANUAL;
				}

				return false;
			}

			@Override
			protected void toggleView() {
				initPanels(script, currentPane != null && currentPane.displayNode != null && currentPane.displayNode.option != null ? currentPane.displayNode.option.getOptionId() : null,
						tree == null ? UseTree.YES : UseTree.NO);
				// Rectangle bounds = getBounds();
				reinitialiseToolbar();
				// pack();
				repaint();
				updateAppearance();
				// setBounds(bounds);
			}

			@Override
			protected boolean isToggleViewEnabled() {
				return script.getOptions() == null || script.getOptions().size() == 0;
			}

			@Override
			protected void launchMultipleChanged(boolean isLaunchMultiple) {
				if (currentPane != null) {
					currentPane.displayNode.option.setLaunchMultiple(isLaunchMultiple);
				}
			}
		};

		ret.addAction(createSaveScriptAction());
		ret.addAction(createSaveScriptAsAction());

		// disable copy for the moment
		// ret.addAction(createCopyAction());

		if (isRunScriptAllowed()) {
			ret.addAction(createTestCompileScriptAction());
			ret.addAction(createRunScriptAction());
		}

		return ret;
	}

	private class MyScrollPane extends JScrollPane {
		final VerticalLayoutPanel panel;
		final List<AdapterTablesTabControl> adapterTabControls = new ArrayList<>();
		final DisplayNode displayNode;

		public MyScrollPane(DisplayNode node) {
			super(new VerticalLayoutPanel());
			this.displayNode = node;
			panel = (VerticalLayoutPanel) getViewport().getView();
		}

		public void updateAppearance() {
			for (AdapterTablesTabControl tabs : adapterTabControls) {
				tabs.updateAppearance(true);
			}
		}
	}

	@Override
	public void updateAppearance() {
		super.updateAppearance();
		if (currentPane != null) {
			currentPane.updateAppearance();
		}

		for (ODLAction action : treeActions) {
			action.updateEnabledState();
		}
	}

	@Override
	protected boolean isRunScriptAllowed() {
		return currentPane != null && currentPane.displayNode != null && currentPane.displayNode.option != null && ScriptUtils.isRunnableOption(currentPane.displayNode.option);
	}

	@Override
	protected void executeScript() {
		if (currentPane != null && runner != null) {
			DisplayNode node = currentPane.displayNode;
			runner.executeScript(script, node.isRoot ? null : new String[] { node.option.getOptionId() }, file != null ? file.getName() : null);
		}

	}

	// public static void main(String[] args) throws Exception {
	// InitialiseStudio.initialise();
	// ODLDatastoreAlterable<? extends ODLTableAlterable> ds =
	// ExampleData.createTerritoriesExample(3);
	// ScriptBuilderImpl builder = new ScriptBuilderImpl(api,ds, ds);
	// ScriptOptionBuilder option1 = builder.addOption("Option 1", "Option 1");
	// ScriptOptionBuilder option2 = option1.addOption("Option 2", "Option 2");
	// option2.addDataAdapter("Adapter 1");
	// option2.addDataAdapter("Adapter 2");
	// ScriptOptionBuilder option3 = option2.addOption("Option 3", "Option 3");
	//
	// ScriptOptionBuilder option4 = option1.addOption("Option 4", "Option 4");
	// option4.addCopyTable("hkkhj", "hkhkjkj", OutputType.COPY_ALL_TABLES,
	// "hhjgjhgjh");
	// option4.addCopyTable("ffhgfhg", "fhgfhgfhg", OutputType.COPY_ALL_TABLES,
	// "hhjgjhgjh");
	// option4.addComponentConfig("bcconfig", new BarchartComponent().getId(),
	// new BarchartComponent().getConfigClass().newInstance());
	// option4.addInstruction("hkkjk", new BarchartComponent().getId(),
	// ODLComponent.MODE_DEFAULT);
	// Script script = builder.build();
	// ScriptEditorWizardGenerated editor = new
	// ScriptEditorWizardGenerated(script, null, null, null);
	// ODLInternalFrame.showInDummyDesktopPane(editor);
	// }

	protected List<SourcedDatastore> getDatastores() {
		ODLDatastore<? extends ODLTableDefinition> external = runner != null ? runner.getDatastoreDefinition() : null;
		if (currentPane == null) {
			return ScriptFieldsParser.getSingleLevelDatastores(api, null, null, external);
		} else {
			return ScriptFieldsParser.getMultiLevelDatastores(api, script, currentPane.displayNode.option.getOptionId(), external);
		}
	}

	// protected List<SourcedColumn> getScriptInternalFields() {
	// if (currentPane == null) {
	// return new ArrayList<>();
	// }
	//
	// // get the fields available to the current node
	// return ScriptFieldsParser.getMultiLevelColumns(api, script,
	// currentPane.displayNode.option.getOptionId(), null);
	// }

	private void reinitTree(String selectOptionId) {
		DisplayNode rootNode = new Splitter().splitIntoDisplayNodes(script, false);
		DefaultTreeModel model = new DefaultTreeModel(rootNode);
		tree.setModel(model);

		if (selectOptionId != null) {
			selectOption(selectOptionId, rootNode, tree);
		}

	}

	private void initTreeActions() {
		abstract class EditOption extends TreeAction {

			EditOption(String name, String tooltip, String smallIconPng) {
				super(name, tooltip, smallIconPng);
			}

			boolean hasOption() {
				return currentPane != null && currentPane.displayNode != null && currentPane.displayNode.type == DisplayNodeType.OPTION;
			}

			@Override
			public void updateEnabledState() {
				setEnabled(hasOption());
			}

			Option option() {
				if (hasOption()) {
					return currentPane.displayNode.option;
				}
				return null;
			}

			int optionIndex() {
				if (hasOption()) {
					DisplayNode node = currentPane.displayNode;
					if (node.parent != null && node.parent.option != null) {
						return node.parent.option.getOptions().indexOf(option());
					} else {
						return 0;
					}
				}
				return -1;
			}
		}

		for(boolean mergeWithCurrent : new boolean[]{false,true}){
			treeActions.add(new EditOption(!mergeWithCurrent?"Add option using component":"Merge component into option",
					!mergeWithCurrent?"Add a new option using a component to the script below the currently selected option.":
						"Merge the option created by a component with the currently selected option",
					!mergeWithCurrent?"add-script-option.png":"add-component-to-current-option.png") {

				@Override
				public void actionPerformed(ActionEvent e) {
					SetupComponentWizard wizard = new SetupComponentWizard(SwingUtilities.getWindowAncestor(ScriptEditorWizardGenerated.this), api, createAvailableOptionsQuery());
					wizard.setMergeWithInputOption(mergeWithCurrent);
					Option newOption = wizard.showModal(script, currentPane.displayNode.option);
					if (newOption != null) {
						reinitTree(mergeWithCurrent? currentPane.displayNode.option.getOptionId():newOption.getOptionId());
					}
				}
			});
			
		}

		treeActions.add(new EditOption("Add empty option", "Add an empty option", "add-empty-script-option.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (hasOption()) {
					Option parent = option();
					ScriptOption parentBuilder = ScriptOptionImpl.createWrapperHierarchy(api, script, parent.getOptionId(), null);
					ScriptOption newOption = parentBuilder.addOption("New option", "New option");
					reinitTree(newOption.getOptionId());
				}
			}

		});

		for (ScriptAdapterType type : new ScriptAdapterType[] { ScriptAdapterType.NORMAL, ScriptAdapterType.PARAMETER }) {

			String name;
			String icon;
			String shortName;
			switch (type) {
			case NORMAL:
				shortName = "adapter";
				name = "Add data adapter";
				icon = "script-element-data-adapter.png";
				break;

			case PARAMETER:
				shortName = "parameter";
				name = "Add a parameter";
				icon = "parameter.png";
				break;

			default:
				throw new IllegalArgumentException();
			}

			treeActions.add(new EditOption(name, name, icon) {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (hasOption()) {
						Option parent = option();
						String name = JOptionPane.showInputDialog(ScriptEditorWizardGenerated.this, "Enter new " + shortName + " name", shortName);
						if (name != null) {
							name = ScriptUtils.createUniqueDatastoreId(script, name);
							AdapterConfig newAdapter = null;
							switch (type) {
							case NORMAL:
								newAdapter = new AdapterConfig(name);
								break;

							case PARAMETER:
								newAdapter = new ParametersImpl(api).createParameterAdapter(name);
								break;

							default:
								break;
							}
							parent.getAdapters().add(newAdapter);
							reinitTree(parent.getOptionId());
						}
					}
				}

				@Override
				public void updateEnabledState() {
					boolean enabled = currentPane != null && currentPane.displayNode != null && (currentPane.displayNode.type == DisplayNodeType.OPTION);
					setEnabled(enabled);
				}

			});

		}

		treeActions.add(new EditOption("Copy data adapter / parameter", "Copy the selected data adapter or parameter to the clipboard.", "copy-data-adapter.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentPane != null && currentPane.displayNode != null && currentPane.displayNode.adapter != null) {
					// take a deep copy of the script
					Script scriptCopy = ScriptIO.instance().deepCopy(script);

					// save the deep copied adapter to the global data adapter
					dataAdapterClipboard = ScriptUtils.getAdapterById(scriptCopy, currentPane.displayNode.adapter.getId(), true);
				}
			}

			@Override
			public void updateEnabledState() {
				boolean enabled = currentPane != null && currentPane.displayNode != null
						&& (currentPane.displayNode.type == DisplayNodeType.DATA_ADAPTER || currentPane.displayNode.type == DisplayNodeType.PARAMETER);
				setEnabled(enabled);
			}

		});

		treeActions.add(new EditOption("Paste data adapter / parameter", "Paste the data adapter from the clipboard into the option.", "paste-data-adapter.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentPane != null && currentPane.displayNode != null && currentPane.displayNode.type == DisplayNodeType.OPTION && dataAdapterClipboard != null) {
					Option parent = currentPane.displayNode.option;

					// create a dummy script with just the data adapter
					Script tmp = new Script();
					tmp.getAdapters().add(dataAdapterClipboard);

					// deep copy it to get a fresh copy of the data adapter
					tmp = ScriptIO.instance().deepCopy(tmp);
					AdapterConfig conf = tmp.getAdapters().get(0);

					// ensure id is unique
					String id = ScriptUtils.createUniqueDatastoreId(script, conf.getId());
					conf.setId(id);

					// add it to the parent option and reinit the display tree
					parent.getAdapters().add(conf);
					reinitTree(parent.getOptionId());
				}
			}

			@Override
			public void updateEnabledState() {
				boolean enabled = currentPane != null && currentPane.displayNode != null && (currentPane.displayNode.type == DisplayNodeType.OPTION && dataAdapterClipboard != null);
				setEnabled(enabled);
			}

		});
		

		treeActions.add(new EditOption("Copy option", "Copy the selected option to the clipboard.", "script-option-copy-paste.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentPane != null && currentPane.displayNode != null && currentPane.displayNode.type == DisplayNodeType.OPTION) {
					// take a deep copy of the script
					Script scriptCopy = ScriptIO.instance().deepCopy(script);

					// save the deep copied adapter to the global clipboard
					if(currentPane.displayNode.isRoot){
						optionClipboard =scriptCopy;
					}else if(currentPane.displayNode.option!=null){
						optionClipboard = ScriptUtils.getOption(scriptCopy, currentPane.displayNode.option.getOptionId());						
					}
				}
			}

			@Override
			public void updateEnabledState() {
				boolean enabled = currentPane != null && currentPane.displayNode != null
						&& (currentPane.displayNode.type == DisplayNodeType.OPTION);
				setEnabled(enabled);
			}

			@Override
			public boolean addToToolbar(){
				return false;
			}

		});

		treeActions.add(new EditOption("Paste option", "Paste the option from the option clipboard into the selected option.", "script-option-copy-paste.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentPane != null && currentPane.displayNode != null && currentPane.displayNode.type == DisplayNodeType.OPTION && optionClipboard != null) {
					// deep copy the option
					Script tmp = new Script();
					tmp.getOptions().add(optionClipboard);
					tmp = ScriptIO.instance().deepCopy(tmp);
					Option option = tmp.getOptions().get(0);
					
					
					// check for used adapters etc
					ScriptIds current = ScriptUtils.getIds(api, script);
					ScriptIds pasting = ScriptUtils.getIds(api, option);
					
					StringBuilder errors = new StringBuilder();
					class Helper{
						void checkOverlap(Set<String> A, Set<String> B, String message){
							Set<String> overlap = ScriptIds.getCommonStrings(api, A, B);
							if(overlap.size()>0){
								StringBuilder builder = new StringBuilder();
								int lineLen=0;
								for(String s: overlap){
									if(builder.length()>0){
										builder.append(", ");
										lineLen+=2;
									}
									if(lineLen>100){
										builder.append("\n");
										lineLen=0;
									}
									builder.append(s);
									lineLen +=s.length();
								}
								errors.append(message + builder.toString());
							}
						}
					}
					Helper helper = new Helper();
					helper.checkOverlap(current.datastoresAndAdapters, pasting.datastoresAndAdapters, 
							"Cannot paste option as the following output datastore or adapter ids would no longer be unique:\n");
					
					if(errors.length()==0){
						helper.checkOverlap(current.options, pasting.options, 
								"Cannot paste option as the following option ids would no longer be unique:\n");						
					}
					
					if(errors.length()>0){
						JOptionPane.showMessageDialog(ScriptEditorWizardGenerated.this, errors.toString());
						return;
					}
					
					// add it to the parent option and reinit the display tree
					Option parent = currentPane.displayNode.option;
					parent.getOptions().add(option);
					reinitTree(parent.getOptionId());
				}
			}

			@Override
			public void updateEnabledState() {
				boolean enabled = currentPane != null && currentPane.displayNode != null && (currentPane.displayNode.type == DisplayNodeType.OPTION && optionClipboard != null);
				setEnabled(enabled);
			}

			@Override
			public boolean addToToolbar(){
				return false;
			}
		});
		
		treeActions.add(new EditOption("Rename", "Rename the selected item.", "script-rename.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				Option option = currentPane != null && currentPane.displayNode != null ? currentPane.displayNode.option : null;
				if (option != null) {
					boolean modified = false;

					DisplayNode node = currentPane.displayNode;
					String current = null;
					if (node.type == DisplayNodeType.OPTION) {
						current = node.option.getName();
					} else {
						current = node.adapter.getId();
					}

					String newValue = JOptionPane.showInputDialog(ScriptEditorWizardGenerated.this, "Enter new name", current);
					if (newValue != null) {
						if (node.type == DisplayNodeType.OPTION) {
							option.setName(newValue);
							modified = true;
						} else {
							// if the standardised version of the value is
							// changing, ensure its unique
							if (Strings.equals(current, newValue)) {
								newValue = ScriptUtils.createUniqueDatastoreId(script, newValue);
							}
							node.adapter.setId(newValue);
							modified = true;
						}
					}

					if (modified) {
						reinitTree(option.getOptionId());
					}

					// String newValue =
					// JOptionPane.showInputDialog(ScriptEditorWizardGenerated.this,
					// "Enter new name name", option.getName());
					// if(newValue!=null){
					// option.setName(newValue);
					// reinitTree(option.getOptionId());
					// }
				}
			}

			@Override
			public void updateEnabledState() {
				boolean enabled = currentPane != null && currentPane.displayNode != null && (currentPane.displayNode.type == DisplayNodeType.OPTION
						|| currentPane.displayNode.type == DisplayNodeType.DATA_ADAPTER || currentPane.displayNode.type == DisplayNodeType.PARAMETER);
				setEnabled(enabled);
			}

		});

		treeActions.add(new EditOption("Move up", "Move the selected item up.", "script-option-up.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (calcEnabled()) {
					Option option = option();
					if (currentPane.displayNode.type == DisplayNodeType.OPTION) {
						int index = optionIndex();
						if (option != null && index >= 1) {
							moveUpList(currentPane.displayNode.parent.option.getOptions(), index);
						}
					} 
					else if(currentPane.displayNode.type == DisplayNodeType.INSTRUCTION){
						option = currentPane.displayNode.option;
						int index = option.getInstructions().indexOf(currentPane.displayNode.instruction);
						moveUpList(option.getInstructions(), index);
					}
					else {
						// must be adapter
						option = currentPane.displayNode.option;
						int index = option.getAdapters().indexOf(currentPane.displayNode.adapter);
						moveUpList(option.getAdapters(), index);
					}
					reinitTree(option.getOptionId());
				}
				// Option option = option();
				// int index = optionIndex();
				// if(option!=null && index>=1){
				// Option parent = currentPane.displayNode.parent.option;
				// parent.getOptions().remove(index);
				// parent.getOptions().add(index-1, option);
				// }
			}

			private <T> void moveUpList(List<T> list, int index) {
				if (index >= 1) {
					T item = list.get(index);
					list.remove(index);
					list.add(index - 1, item);
				}
			}

			@Override
			public void updateEnabledState() {
				setEnabled(calcEnabled());
			}

			private boolean calcEnabled() {
				boolean enabled = currentPane != null && currentPane.displayNode != null;
				if (enabled && currentPane.displayNode.type == DisplayNodeType.OPTION) {
					enabled = optionIndex() >= 1;
				} else if (enabled && (currentPane.displayNode.type == DisplayNodeType.DATA_ADAPTER || currentPane.displayNode.type == DisplayNodeType.PARAMETER)
						&& currentPane.displayNode.adapter != null) {
					enabled = currentPane.displayNode.option.getAdapters().indexOf(currentPane.displayNode.adapter) >= 1;
				} else if (enabled && (currentPane.displayNode.type == DisplayNodeType.INSTRUCTION )
						&& currentPane.displayNode.instruction != null) {
					enabled = currentPane.displayNode.option.getInstructions().indexOf(currentPane.displayNode.instruction) >= 1;
				} 
				else {
					enabled = false;
				}
				return enabled;
			}

		});

		treeActions.add(new EditOption("Move down", "Move the selected item down.", "script-option-down.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!isAllowed()) {
					return;
				}

				Option option = option();
				if (currentPane.displayNode.type == DisplayNodeType.OPTION) {
					int index = optionIndex();
					Option parent = currentPane.displayNode.parent.option;
					parent.getOptions().remove(index);
					parent.getOptions().add(index + 1, option);
				}
				else if(currentPane.displayNode.type ==DisplayNodeType.INSTRUCTION){
					option = currentPane.displayNode.option;
					int index = option.getInstructions().indexOf(currentPane.displayNode.instruction);
					option.getInstructions().remove(index);
					option.getInstructions().add(index + 1, currentPane.displayNode.instruction);
				}
				else {
					// must be adapter
					option = currentPane.displayNode.option;
					List<AdapterConfig> adapters = option.getAdapters();
					int index = adapters.indexOf(currentPane.displayNode.adapter);
					adapters.remove(index);
					adapters.add(index + 1, currentPane.displayNode.adapter);
				}
				reinitTree(option.getOptionId());
			}

			@Override
			public void updateEnabledState() {
				setEnabled(isAllowed());
			}

			/**
			 * @return
			 */
			protected boolean isAllowed() {
				boolean enabled = currentPane != null && currentPane.displayNode != null;
				DisplayNodeType type = enabled ? currentPane.displayNode.type : null;
				if (enabled && type == DisplayNodeType.OPTION) {
					int index = optionIndex();
					if (index != -1 && currentPane.displayNode.parent != null) {
						Option parent = currentPane.displayNode.parent.option;
						enabled = parent != null && index < (parent.getOptions().size() - 1);
					}
				}
				 else if (enabled && (type == DisplayNodeType.INSTRUCTION ) && currentPane.displayNode.instruction != null) {
						enabled = currentPane.displayNode.option.getInstructions().indexOf(currentPane.displayNode.instruction) < currentPane.displayNode.option.getInstructions().size() - 1;
					}
				else if (enabled && (type == DisplayNodeType.DATA_ADAPTER || type == DisplayNodeType.PARAMETER) && currentPane.displayNode.adapter != null) {
					enabled = currentPane.displayNode.option.getAdapters().indexOf(currentPane.displayNode.adapter) < currentPane.displayNode.option.getAdapters().size() - 1;
				} else {
					enabled = false;
				}
				return enabled;

				// boolean enabled=false;
				// int index = optionIndex();
				// if(index!=-1 && currentPane.displayNode.parent!=null){
				// Option parent = currentPane.displayNode.parent.option;
				// enabled = parent!=null && index <
				// (parent.getOptions().size()-1);
				// }
				// return enabled;
			}

		});

		treeActions.add(new EditOption("Move option to different parent", "Move option to different parent option.", "change-script-option-parent.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				final Option movingOption = option();
				if (movingOption == null) {
					return;
				}

				class OptionContainer {
					Option option;
					int depth;

					@Override
					public String toString() {
						StringBuilder builder = new StringBuilder();
						for (int i = 0; i < depth; i++) {
							builder.append("   ");
						}
						if (depth > 0) {
							builder.append("- ");
						}
						builder.append(option.getName());
						return builder.toString();
					}
				}

				final ArrayList<OptionContainer> containers = new ArrayList<>();

				class MyVisitor implements OptionVisitor {
					Option movingOptionParent;

					@Override
					public boolean visitOption(Option parent, Option currentOption, int depth) {
						if (currentOption == movingOption) {
							movingOptionParent = parent;
							// do not add moving option or children of the
							// moving option
							return false;
						}
						OptionContainer container = new OptionContainer();
						container.option = currentOption;
						container.depth = depth;
						containers.add(container);
						return true;
					}
				}

				MyVisitor visitor = new MyVisitor();
				ScriptUtils.visitOptions(script, visitor);

				// ensure we have a parent for the option
				if (visitor.movingOptionParent == null) {
					return;
				}

				// create a list of all valid options and select the current
				// parent
				JList<OptionContainer> list = new JList<>(containers.toArray(new OptionContainer[containers.size()]));
				for (int i = 0; i < list.getModel().getSize(); i++) {
					if (list.getModel().getElementAt(i).option == visitor.movingOptionParent) {
						list.setSelectedIndex(i);
					}
				}

				// show the dialog for selecting the new parent
				JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				panel.add(new JScrollPane(list));
				ModalDialog dlg = new ModalDialog(SwingUtilities.getWindowAncestor(ScriptEditorWizardGenerated.this), panel, "Select new parent option", ModalDialogResult.OK,
						ModalDialogResult.CANCEL);
				dlg.setMinimumSize(new Dimension(300, 200));
				if (dlg.showModal() == ModalDialogResult.OK && list.getSelectedValue() != null) {

					// move it!
					OptionContainer newParent = list.getSelectedValue();
					visitor.movingOptionParent.getOptions().remove(movingOption);
					newParent.option.getOptions().add(movingOption);
					reinitTree(movingOption.getOptionId());
				}
			}

			@Override
			public void updateEnabledState() {
				setEnabled(hasOption() && option() != script);
			}

		});
		treeActions.add(new EditOption("Delete item", "Delete the selected item.", "delete-script-option.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (JOptionPane.showConfirmDialog(ScriptEditorWizardGenerated.this, "Are you sure you want to delete this item? (This cannot be undone)", "Confirm",
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					ScriptUtils.visitOptions(script, new OptionVisitor() {

						@Override
						public boolean visitOption(Option parent, Option option, int depth) {
							DisplayNode node = currentPane.displayNode;
							List<? extends Object> toDelete = null;
							List<? extends Object> deleteFrom = null;

							switch (node.type) {
							case OPTION:
								toDelete = Arrays.asList(node.option);
								deleteFrom = option.getOptions();
								break;

							case INSTRUCTION:
								toDelete = Arrays.asList(node.instruction);
								deleteFrom = option.getInstructions();
								break;

							case COPY_TABLES:
								toDelete = node.outputs;
								deleteFrom = option.getOutputs();
								break;

							case DATA_ADAPTER:
							case PARAMETER:
								toDelete = Arrays.asList(node.adapter);
								deleteFrom = option.getAdapters();
								break;

							case COMPONENT_CONFIGURATION:
								toDelete = Arrays.asList(node.componentConfig);
								deleteFrom = option.getComponentConfigs();
								break;

							default:
								break;
							}

							if (deleteFrom != null && toDelete != null) {
								for (Object o : toDelete) {
									deleteFrom.remove(o);
								}
							}
							return true;
						}
					});

					reinitTree(null);
				}
			}

			@Override
			public void updateEnabledState() {
				boolean enabled = currentPane != null && currentPane.displayNode != null && currentPane.displayNode.type != DisplayNodeType.AVAILABLE_TABLES && !currentPane.displayNode.isRoot;
				setEnabled(enabled);
			}

		});

	}

	/**
	 * Completely rebuild the active panel
	 */
	private void rebuildActivePanel() {
		TreePath path = tree.getSelectionPath();
		if (path != null && path.getLastPathComponent() != null) {
			// replace panel
			DisplayNode node = (DisplayNode) path.getLastPathComponent();
			currentPane = node.createPane(true);
			splitPane.setRightComponent(currentPane);
			Dimension dim = getSize();
			int dividerLoc = splitPane.getDividerLocation();
			pack();
			if (dim != null) {
				setSize(dim);
			}
			splitPane.setDividerLocation(dividerLoc);

		} else {
			currentPane = null;
			splitPane.setRightComponent(new JPanel());
		}

		reinitialiseToolbar();
		updateAppearance();
	}
}
