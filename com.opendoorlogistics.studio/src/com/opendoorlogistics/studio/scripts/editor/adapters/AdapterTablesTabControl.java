/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.adapters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.TargetIODsInterpreter;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.ScriptElementType;
import com.opendoorlogistics.core.scripts.elements.UserFormula;
import com.opendoorlogistics.core.scripts.io.XMLConversionHandler;
import com.opendoorlogistics.core.scripts.io.XMLConversionHandlerImpl;
import com.opendoorlogistics.core.scripts.utils.AdapterExpectedStructureProvider;
import com.opendoorlogistics.core.scripts.utils.AdapterExpectedStructureProvider;
import com.opendoorlogistics.core.scripts.wizard.TableLinkerWizard;
import com.opendoorlogistics.core.tables.ODLFactory;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.ODLDatastoreDefinitionProvider;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.scripts.editor.ScriptXMLTransferHandler;
import com.opendoorlogistics.studio.scripts.editor.adapters.AdaptedTableControl.FormChangedListener;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;
import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.utils.ui.ListPanel;
import com.opendoorlogistics.utils.ui.ODLAction;
import com.opendoorlogistics.utils.ui.SimpleAction;

public class AdapterTablesTabControl extends JPanel {
	private final ODLApi api;
	private final JTabbedPane tabs;
	private final long visibleTableFlags;
	private final long visibleColumnFlags;
	private final QueryAvailableData availableOptionsQuery;
	private final AdapterConfig config;
	private final ODLDatastoreDefinitionProvider targetDatastore;
	private final XMLConversionHandler conversionHandler = new XMLConversionHandlerImpl(ScriptElementType.ADAPTED_TABLE);
	private final ScriptUIManager scriptUIManager;

	private class MyXMLTransferHandler extends ScriptXMLTransferHandler {

		@Override
		protected XMLConversionHandler conversionHandler() {
			return conversionHandler;
		}

		@Override
		protected Object getSelected() {
			return getCurrentTable();
		}

		@Override
		protected void pasteItem(Object o) {
			addNewAdaptedTable((AdaptedTableConfig) o, config.getTableCount());
		}

	}

	public AdapterTablesTabControl(ODLApi api,final AdapterConfig config, long visibleTableFlags, long visibleFlags, QueryAvailableData availableOptionsQuery, AdapterExpectedStructureProvider targetDatastore, ScriptUIManager uiManager) {
		this.tabs = new JTabbedPane();
		this.api = api;
		this.tabs.setTransferHandler(new MyXMLTransferHandler());
		setTransferHandler(new MyXMLTransferHandler());
		this.visibleTableFlags = visibleTableFlags;
		this.visibleColumnFlags = visibleFlags;
		this.availableOptionsQuery = availableOptionsQuery;
		this.config = config;
		this.targetDatastore = targetDatastore;
		this.scriptUIManager = uiManager;

		setLayout(new BorderLayout());
		add(tabs, BorderLayout.CENTER);
		for (final AdaptedTableConfig table : config.getTables()) {
			addTabCtrl(table, tabs.getTabCount());
		}

		updateAppearance(true);
	}

	private void addTabCtrl(final AdaptedTableConfig table, int index) {

		AdaptedTableControl ctrl = new AdaptedTableControl(api,table, visibleTableFlags, visibleColumnFlags, availableOptionsQuery) {
			@Override
			protected List<ODLAction> createActions() {
				List<ODLAction> ret = super.createActions();
				ret.add(null); // null is a separator
				ret.addAll(createTabPageActions(table));
				return ret;
			}				
		};

		ctrl.setTargetDatastoreDefinitionProvider(targetDatastore);
		
		ctrl.setFormChangedListener(new FormChangedListener() {
			
			@Override
			public void formChanged(AdaptedTableControl form) {
				// Update the tables control but we don't need to updated the individual
				// tab contents as this has only changed for the input form and already been updated.
				updateAppearance(false);
			}
		});

		// String panelName = "Table \"" + table.getName() + "\""; // adapterConfig.getId();
		tabs.add(ctrl, index);
		// tabs.setTitleAt(index, panelName);
		tabs.setSelectedIndex(index);
	}

//	public static void main(String[] arg) {
//		InitialiseStudio.initialise();
//		AdapterConfig conf = new AdapterConfig();
//		// conf.createTable("blah", "blah2");
//		// conf.createTable("blah blah", "blah3");
//
//		QueryAvailableData queryAvailableData = QueryAvailableDataImpl.createExternalDsQuery(ExampleData.createLocationsWithDemandExample(0));
//		final ODLDatastore<? extends ODLTableDefinition> target = ExampleData.createTerritoriesExample(2);
//		System.out.println(target);
//		ShowPanel.showPanel(new AdapterTablesTabControl(conf, 0, TableFlags.FLAG_IS_BATCH_KEY | TableFlags.FLAG_IS_GROUP_BY_FIELD | TableFlags.FLAG_IS_OPTIONAL, queryAvailableData, new AdapterDestinationDefinition() {
//
//			@Override
//			public ODLDatastore<? extends ODLTableDefinition> getDatastoreDefinition() {
//				return target;
//			}
//
////			@Override
////			public ODLTableDefinition getTarget(AdaptedTableConfig tableConfig) {
////				// TODO Auto-generated method stub
////				return null;
////			}
//		}, null));
//	}

	protected String getTabTitle(int configIndex, AdapterConfig config) {
		// name the tabs differently if this is input into the reporter
		if(targetDatastore!=null){
			ODLDatastore<? extends ODLTableDefinition> target = targetDatastore.getDatastoreDefinition();
			if(target!=null && TableUtils.hasFlag(target, TableFlags.FLAG_IS_REPORTER_INPUT)){
				// create output definition first which will merge same name tables (i.e. union)
				// then find the index of the table
				String name = config.getTable(configIndex).getName();
				ODLDatastore<? extends ODLTableDefinition> output = config.createOutputDefinition();
				int nbNonMap = 0;
				for (int i = 0; i < output.getTableCount(); i++) {
					ODLTableDefinition dfn = output.getTableAt(i);
					boolean headerMap = Strings.equalsStd(api.standardComponents().reporter().getHeaderMapTableName(), dfn.getName());
		
					// do we have a name match?
					if (Strings.equalsStd(name, dfn.getName())) {
						if (headerMap) {
							return name;
						} else if (nbNonMap == 0) {
							return "Primary report \"" + name + "\"";
						} else {
							return "Subreport " + nbNonMap + " \"" + name + "\"";
						}
					}
		
					// count the non-map tables
					if (!headerMap) {
						nbNonMap++;
					}
				}		
			}
		}
		
		// Get the from datastore part of the title
		AdaptedTableConfig table = config.getTable(configIndex);
//		String fromDs=null;
//		boolean isExternal = Strings.equalsStd(table.getFromDatastore(), ScriptConstants.EXTERNAL_DS_NAME);
//		if(!Strings.isEmpty(table.getFromDatastore()) && !isExternal){
//			fromDs = table.getFromDatastore();
//		}		
//		
//		// Get the from table part but don't show if from datastore is empty and from and to table are same name
//		String fromTable = table.getFromTable();
//		if(fromDs==null && Strings.equalsStd(table.getName(), fromTable)){
//			fromTable = null;
//		}
//		
//		// Try checking if the datastore only has one table in which case just write the datastore name
//		if(fromTable!=null && !isExternal && availableOptionsQuery!=null && !Strings.isEmpty(table.getFromDatastore())){
//			ODLDatastore<? extends ODLTableDefinition> ds = availableOptionsQuery.getDatastoreDefinition(table.getFromDatastore());
//			if(ds!=null && ds.getTableCount()==1 && Strings.equalsStd(ds.getTableAt(0).getName(), fromTable)){
//				fromTable = null;
//			}
//		}
//		
//		// Build source string
//		StringBuilder src = new StringBuilder();
//		if(fromDs!=null){
//			src.append(fromDs);
//			if(fromTable!=null){
//				src.append(",");
//			}
//		}
//		if(fromTable!=null){
//			src.append(fromTable);
//		}
//		
//		// Get total available chars
//		int maxChars = 75;
//		int available =  maxChars - (table.getName()!=null ? table.getName().length() : 0) - 3;
//		
//		// Get the source string and truncate if needed
//		String srcStr = src.toString();
//		boolean isTruncated=false;
//		if(srcStr.length() > available){
//			srcStr = srcStr.substring(0, Math.max(available,0));
//			isTruncated =true;		
//		}
		
		// Build the title
		StringBuilder title = new StringBuilder();
		title.append(table.getName());
//		if((isTruncated && srcStr.length()>3) || srcStr.length()>0){
//			title.append(" (");
//			title.append(srcStr);
//			if(isTruncated){
//				title.append("...");
//			}
//			title.append(")");
//		}
//		
		if(Strings.isEmpty(table.getShortEditorUINote())==false){
			title.append(" (" + table.getShortEditorUINote() + ")");
		}
		
		return title.toString();
		

	
	}

	private final Font tabTitleFont = new Font("SansSerif", Font.PLAIN, 11);
	
	public void updateAppearance(boolean updateTabContents) {
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if (i < config.getTableCount()) {
				AdaptedTableControl ctrl = (AdaptedTableControl) tabs.getComponentAt(i);
				final AdaptedTableConfig table = config.getTable(i);

				// set tab title with a right-click menu
				JLabel titleLabel = new JLabel(getTabTitle(i, config));
				titleLabel.setFont(tabTitleFont);
				tabs.setTabComponentAt(i, titleLabel);
				final int tabIndex = i;
				titleLabel.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseReleased(MouseEvent Me) {
						doPopup(Me);
					}

					private void doPopup(MouseEvent Me) {
						if (Me.isPopupTrigger()) {
							JPopupMenu popup = new JPopupMenu();
							ODLAction.addToPopupMenu(popup, createTabPageActions(table));
							popup.show(Me.getComponent(), Me.getX(), Me.getY());
						}
					}

					@Override
					public void mouseClicked(MouseEvent e) {
						tabs.setSelectedIndex(tabIndex);
					}

					@Override
					public void mousePressed(MouseEvent e) {
						tabs.setSelectedIndex(tabIndex);
						doPopup(e);
					}

				});

//				// Check for error with table...
//				ODLDatastore<? extends ODLTableDefinition> targetDs = targetDatastore!=null?targetDatastore.getDatastoreDefinition():null;
//				HashMap<Object, String> validateResult =new TargetIODsInterpreter(api).validateAdapter(config, targetDs); 
//				String error = validateResult.get(table);
//				
//				if (error!=null) {
//					ctrl.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED, 2), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
//					tabs.setToolTipTextAt(i, error);
//				} else {
//					ctrl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//					tabs.setToolTipTextAt(i, null);
//				}
				
				if(updateTabContents){
					ctrl.updateAppearance();	
				}
				
			} else {
				// dummy page...
				if (config.getTableCount() > 0) {
					tabs.remove(i);
				}
			}
		}

		if (tabs.getTabCount() == 0) {
			// add dummy page
			VerticalLayoutPanel dummy = new VerticalLayoutPanel();
			dummy.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			JButton button = new JButton("Add a table");
			dummy.addWhitespace();
			dummy.addLine(new JLabel("No adapter tables available! No input data will be given to the component."));
			dummy.addHalfWhitespace();
			dummy.addLine(new JLabel("Press this button to "), button);
			button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					addNewAdaptedTable(0);
				}
			});
			tabs.add("Add table", dummy);
		}
	}

	private class PromptNewTableResult {
		String sourceDatastore;
		String sourceTable;
		String destinationTable;
	}

	/**
	 * Prompt the user for a new table including both the destination and the source
	 * @param source
	 * @param destination
	 * @param currentDestination
	 * @return
	 */
	private PromptNewTableResult promptNewAdaptedTable(boolean source, boolean destination, String currentDestination) {
		final VerticalLayoutPanel panel = new VerticalLayoutPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		class DSTable {
			final String ds;
			final String table;

			public DSTable(String ds, String table) {
				super();
				this.ds = ds;
				this.table = table;
			}

			@Override
			public String toString() {
				return ds != null ? ds + " - " + table : table;
			}
		}

		DSTable noSrcTable = null;
		JComboBox<DSTable> srcTableCombo = null;
		if (source) {
			noSrcTable = new DSTable(null, "No table");
			ArrayList<DSTable> srcTables = new ArrayList<>();
			srcTables.add(noSrcTable);
			if (availableOptionsQuery != null) {
				for (String ds : availableOptionsQuery.queryAvailableDatastores()) {
					for (String table : availableOptionsQuery.queryAvailableTables(ds)) {
						if (availableOptionsQuery.getTableDefinition(ds, table) != null) {
							srcTables.add(new DSTable(ds, table));
						}
					}
				}
			}

			srcTableCombo = new JComboBox<>(srcTables.toArray(new DSTable[srcTables.size()]));
			panel.addLine(new JLabel("Select source table "), srcTableCombo);
			panel.addWhitespace();
		}

		JComboBox<String> destTableCombo = null;
		String newTable = "New table";
		if (destination) {
			// add "new table"
			ArrayList<String> destTables = new ArrayList<>();
			destTables.add(newTable);
			
			// add predefined table names
			if (targetDatastore != null && targetDatastore.getDatastoreDefinition() != null) {
				for (String table : TableUtils.getTableNames(targetDatastore.getDatastoreDefinition())) {
					destTables.add(table);
				}
			}

			destTableCombo = new JComboBox<>(destTables.toArray(new String[destTables.size()]));
			destTableCombo.setEditable(true);

			if (currentDestination != null) {
				destTableCombo.getEditor().setItem(currentDestination);
			}

			panel.addLine(new JLabel("Select destination table "), destTableCombo);
			if (destTableCombo.getItemCount() > 1 && currentDestination == null) {
				// selected the alphabetically first destination table by default
				destTableCombo.setSelectedIndex(1);
			}
		}

		OkCancelDialog okCancelDialog = new OkCancelDialog(SwingUtilities.getWindowAncestor(this)) {
			@Override
			protected Component createMainComponent(boolean inWindowsBuilder) {
				return panel;
			}

		};

		if (okCancelDialog.showModal() == OkCancelDialog.OK_OPTION) {
			PromptNewTableResult ret = new PromptNewTableResult();
			if (source && srcTableCombo.getSelectedItem() != noSrcTable) {
				DSTable src = (DSTable) srcTableCombo.getSelectedItem();
				ret.sourceDatastore = src.ds;
				ret.sourceTable = src.table;
			}

			if (destination) {

				if (currentDestination != null) {
					ret.destinationTable = destTableCombo.getEditor().getItem().toString();
				} else {
					ret.destinationTable = destTableCombo.getSelectedItem().toString();
				}
			}

			return ret;
		}

		return null;
	}

	private void addNewAdaptedTable(int index) {
		ODLDatastore<? extends ODLTableDefinition> target = targetDatastore != null ? targetDatastore.getDatastoreDefinition() :null;
		PromptNewTableResult prompt = promptNewAdaptedTable(true, target!=null, null);
		if (prompt == null) {
			return;
		}

		ODLTableDefinition src = null;
		if (prompt.sourceTable != null) {
			src = availableOptionsQuery.getTableDefinition(prompt.sourceDatastore, prompt.sourceTable);
		}
		
		AdaptedTableConfig newTable  = new TargetIODsInterpreter(api).buildAdaptedTableConfig(prompt.sourceDatastore, src, target, prompt.destinationTable);
		
//		ODLTableDefinition dest = null;
//		if (prompt.destinationTable != null) {
//			dest = TableUtils.findTable(targetDatastore.getDatastoreDefinition(), prompt.destinationTable);
//		}
//
//		AdaptedTableConfig newTable = null;
//		if (dest != null && dest.getColumnCount() > 0) {
//			newTable = TableLinkerWizard.createBestGuess(src, dest);
//		} else if (src != null) {
//			newTable = WizardUtils.createAdaptedTableConfig(src, src.getName());
//		} else {
//			newTable = new AdaptedTableConfig();
//			newTable.setName("New table");
//		}
//
//		if (prompt.sourceTable != null) {
//			newTable.setFrom(prompt.sourceDatastore, prompt.sourceTable);
//		}

		addNewAdaptedTable(newTable, index);
	}

	void addNewAdaptedTable(AdaptedTableConfig newTable, int index) {
		config.getTables().add(index, newTable);
		addTabCtrl(newTable, index);
		AdapterTablesTabControl.this.updateAppearance(true);
	}

	private void updateTable(int index) {
		AdaptedTableConfig table = config.getTable(index);

		ODLTableDefinition src = availableOptionsQuery != null ? availableOptionsQuery.getTableDefinition(table.getFromDatastore(), table.getFromTable()) : null;
		ODLTableDefinition dest = TableUtils.findTable(targetDatastore.getDatastoreDefinition(), table.getName());

		final AdaptedTableConfig relinked = TableLinkerWizard.createBestGuess(src, dest);

		// add any missing columns...
		for (AdapterColumnConfig col : relinked.getColumns()) {
			if (TableUtils.findColumnIndx(table, col.getName()) == -1) {
				table.getColumns().add(new AdapterColumnConfig(col, -1));
			}
		}

		// set any sources not set
		for (AdapterColumnConfig col : table.getColumns()) {
			String colSrc = col.isUseFormula() ? col.getFormula() : col.getFrom();
			if (Strings.isEmpty(colSrc) || colSrc.trim().length() == 0) {
				int rli = TableUtils.findColumnIndx(relinked, col.getName());
				if (rli != -1) {
					AdapterColumnConfig relinkedCol = relinked.getColumn(rli);
					col.setUseFormula(relinkedCol.isUseFormula());
					col.setFrom(relinkedCol.getFrom());
					col.setFormula(relinkedCol.getFormula());
				}
			}
		}

		// set correct types
		for (AdapterColumnConfig col : table.getColumns()) {
			int rli = TableUtils.findColumnIndx(relinked, col.getName());
			if (rli != -1) {
				col.setType(relinked.getColumn(rli).getType());
			}
		}
		
		// sort in order of appearance in relinked, put anything that doesn't appear last
		Collections.sort(table.getColumns(), new Comparator<AdapterColumnConfig>() {

			@Override
			public int compare(AdapterColumnConfig o1, AdapterColumnConfig o2) {
				int index1 = TableUtils.findColumnIndx(relinked, o1.getName());
				int index2 = TableUtils.findColumnIndx(relinked, o2.getName());
				if (index1 == -1) {
					index1 = Integer.MAX_VALUE;
				}
				if (index2 == -1) {
					index2 = Integer.MAX_VALUE;
				}
				return Integer.compare(index1, index2);
			}
		});

		// remove and re-add control (so all child controls up-to-date)
		tabs.remove(index);
		addTabCtrl(table, index);
		updateAppearance(true);
	}

	protected List<ODLAction> createTabPageActions(final AdaptedTableConfig table) {
		ArrayList<ODLAction> ret = new ArrayList<>();

		abstract class TabPageAction extends SimpleAction {

			public TabPageAction(String name, String tooltip, String smallIconPng) {
				super(name, tooltip, smallIconPng);
			}

			int getIndex() {
				int index = -1;
				for (int i = 0; i < config.getTableCount(); i++) {
					if (config.getTable(i) == table) {
						index = i;
					}
				}
				return index;
			}

			void move(int dir) {
				int index = getIndex();
				Component component = tabs.getComponentAt(index);
				// String title = tabs.getTitleAt(index);
				tabs.remove(index);
				config.getTables().remove(index);
				index += dir;
				tabs.add(component, index);
				// tabs.setTitleAt(index, title);
				config.getTables().add(index, table);
				tabs.setSelectedIndex(index);
				AdapterTablesTabControl.this.updateAppearance(true);
			}

		}

		ret.add(new TabPageAction("Add table", "Add new adapted table", "insert-table-2.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				addNewAdaptedTable(getIndex());
			}

			@Override
			public void updateEnabled() {

			}
		});

		ret.add(new TabPageAction("Rename table", "Rename selected adapter table", "table-rename.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				PromptNewTableResult result = promptNewAdaptedTable(false, true, table.getName());
				if (result != null && result.destinationTable != null) {
					table.setName(result.destinationTable);
					// tabs.setTitleAt(getIndex(), "Table \"" + result.destinationTable+ "\"");
					AdapterTablesTabControl.this.updateAppearance(true);
				}
			}

		});

		ret.add(new TabPageAction("Edit table label", "Change the label shown in brackets after the table name in the tab control.", "adapted-table-short-ui-note.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				String current = table.getShortEditorUINote();
				if(current==null){
					current = "";
				}
				
				current = JOptionPane.showInputDialog(AdapterTablesTabControl.this, "Enter the label for table " + table.getName(), current);
				if(current!=null){
					table.setShortEditorUINote(current);
					AdapterTablesTabControl.this.updateAppearance(true);
				}
//				PromptNewTableResult result = promptNewAdaptedTable(false, true, table.getName());
//				if (result != null && result.destinationTable != null) {
//					table.setName(result.destinationTable);
//					// tabs.setTitleAt(getIndex(), "Table \"" + result.destinationTable+ "\"");
//					AdapterTablesTabControl.this.updateAppearance(true);
//				}
			}

		});
		
		ret.add(new TabPageAction("Copy table", "Copy the current table to the clipboard", "table-copy.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(((ScriptXMLTransferHandler) getTransferHandler()).createTransferable(AdapterTablesTabControl.this), null);
			}

			@Override
			public void updateEnabled() {
				setEnabled(getCurrentTable() != null);
			}
		});

		ret.add(new TabPageAction("Paste table", "Paste an adapted table from the clipboard", "edit-paste-7.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				((ScriptXMLTransferHandler) getTransferHandler()).importData(AdapterTablesTabControl.this, Toolkit.getDefaultToolkit().getSystemClipboard().getContents(AdapterTablesTabControl.this));
			}
		});

		// wizard tools actions (appear in a popup)
		ArrayList<ODLAction> wizardTools = new ArrayList<>();
		wizardTools.add(new TabPageAction("Update table based on destination", "Update table based on its destination. Any missing fields are added and empty from/formulae are updated where possible.", null) {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateTable(getIndex());
			}

			@Override
			public void updateEnabled() {
				setEnabled(targetDatastore != null && targetDatastore.getDatastoreDefinition() != null && TableUtils.findTable(targetDatastore.getDatastoreDefinition(), table.getName()) != null);
			}
		});

		// create seleted empty tables in datastore action
		wizardTools.add(new TabPageAction("Export this table to datastore", "Create empty table in the datastore with same design as this table", null) {

			@Override
			public void actionPerformed(ActionEvent e) {
				ODLDatastoreAlterable<ODLTableDefinitionAlterable> ds = ODLFactory.createDefinition();
				DatastoreCopier.copyTableDefinition(config.getTable(getIndex()), ds);
				scriptUIManager.launchCreateTablesWizard(ds);
			}

			@Override
			public void updateEnabled() {
				setEnabled(getIndex() != -1);
			}
		});

		// create all empty tables in datastore action
		wizardTools.add(new TabPageAction("Export all tables to datastore", "Create empty tables in the datastore with same design as these tables", null) {

			@Override
			public void actionPerformed(ActionEvent e) {
				ODLDatastoreAlterable<ODLTableDefinitionAlterable> ds = ODLFactory.createDefinition();
				for (int i = 0; i < config.getTableCount(); i++) {
					DatastoreCopier.copyTableDefinition(config.getTable(i), ds);
				}
				scriptUIManager.launchCreateTablesWizard(ds);
			}

			@Override
			public void updateEnabled() {
				setEnabled(config.getTableCount() > 0);
			}
		});

		ret.add(ODLAction.createParentAction("Data adapter wizard actions", Icons.loadFromStandardPath("tools-wizard-2.png"), wizardTools));

		ret.add(new TabPageAction("Delete table", "Delete selected adapted table", "table-delete-2.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				// find the index of this table
				int index = getIndex();

				if (index != -1) {
					config.getTables().remove(index);
					tabs.remove(index);

					// need to disable deletion of tables if we only have one
					// (otherwise controls will be unavailable!)
					AdapterTablesTabControl.this.updateAppearance(true);
				}
			}

			// @Override
			// public void updateEnabled() {
			// boolean enabled = tabs.getTabCount() > 1;
			// setEnabled(enabled);
			// }
		});

		ret.add(new TabPageAction("Move table earlier", "Move adapted table earlier", "go-left.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				int index = getIndex();
				if (index > 0) {
					move(-1);
				}
			}

			@Override
			public void updateEnabled() {
				setEnabled(getIndex() > 0);
			}
		});

		ret.add(new TabPageAction("Move table later", "Move adapted table later", "go-right.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				int index = getIndex();
				if (index < config.getTableCount() - 1) {
					move(+1);
				}
			}

			@Override
			public void updateEnabled() {
				setEnabled(getIndex() < config.getTableCount() - 1);
			}
		});
		
		ret.add(new TabPageAction("Edit user formulae", "Edit the adapted tables user formulae", "user-formula.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				int index = getIndex();
				final AdaptedTableConfig tableConfig = config.getTables().get(index);
				
				// get a copy of the current formulae
				final ArrayList<UserFormula> formulaeCopy = new ArrayList<UserFormula>();
				if(tableConfig.getUserFormulae()!=null){
					for(UserFormula uf: tableConfig.getUserFormulae()){
						formulaeCopy.add(new UserFormula(uf));
					}
				}
				

				// create the editor dialog
				OkCancelDialog dlg = new OkCancelDialog(){
					@Override
					protected Component createMainComponent(boolean inWindowsBuilder) {
						return new ListPanel<UserFormula>(formulaeCopy, "user formula") {

							@Override
							protected UserFormula createNewItem() {
								return editItem(new UserFormula("funcname() = X"));
							}

							@Override
							protected UserFormula editItem(final UserFormula item) {
								final JTextArea textArea = new JTextArea(item.getValue());	
								textArea.setEditable(true);
								textArea.setLineWrap(true);
								OkCancelDialog dlg = new OkCancelDialog(){
									@Override
									protected Component createMainComponent(boolean inWindowsBuilder) {
										return new JScrollPane(textArea); 
									}
								};
								dlg.setMinimumSize(new Dimension(400, 200));
								dlg.setLocationRelativeTo(this);
								dlg.setTitle("Enter formula text");
								if(dlg.showModal() == OkCancelDialog.OK_OPTION){
									item.setValue(textArea.getText());
								}
								return item;
							}
						};
//						return new TablePanel<String>(formulaeCopy, "user formula") {
//
//							@Override
//							protected TableModel createTableModel() {
//								// TODO Auto-generated method stub
//								return null;
//							}
//
//							@Override
//							protected String createNewItem() {
//								// TODO Auto-generated method stub
//								return null;
//							}
//
//							@Override
//							protected String editItem(String item) {
//								// TODO Auto-generated method stub
//								return null;
//							}
//						};
					}
					
				};
				dlg.setTitle( "" + tableConfig.getName() + " user formulae");
				dlg.setLocationRelativeTo(AdapterTablesTabControl.this);
				dlg.setMinimumSize(new Dimension(400, 200));
//				dlg.setMaximumSize(new Dimension(400, 800));
				
				// replace the formulae
				if(dlg.showModal() == OkCancelDialog.OK_OPTION){
					tableConfig.setUserFormulae(formulaeCopy);
				}
			}
		});
				
		
		return ret;
	}

	public AdaptedTableConfig getCurrentTable() {
		if (config.getTableCount() > 0 && tabs.getSelectedIndex() != -1) {
			return config.getTable(tabs.getSelectedIndex());
		}
		return null;
	}
}
