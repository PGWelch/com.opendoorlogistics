/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.codefromweb.DropDownMenuButton;
import com.opendoorlogistics.core.api.impl.scripts.ScriptTemplatesImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.Layer;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.Style;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.View;
import com.opendoorlogistics.core.tables.decorators.tables.SimpleTableReadOnlyDecorator;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.iterators.IteratorUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.strings.Strings.DoesStringExist;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.core.utils.ui.PopupMenuMouseAdapter;
import com.opendoorlogistics.studio.appframe.AbstractAppFrame;
import com.opendoorlogistics.studio.appframe.AppBackground;
import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.utils.ui.ODLAction;
import com.opendoorlogistics.utils.ui.SimpleAction;

final public class DatastoreTablesPanel extends JPanel implements ODLListener {
	private ODLDatastoreUndoable<? extends ODLTableAlterable> ds;
	private final JList<ODLTableReadOnly> list;
	private final AbstractAppFrame appFrame;
	private final List<MyAction> actions;
	private final List<ODLAction> wizardActions;
	private final JLabel tablesLabel;
	private final boolean useBackgroundImage;

//	public DatastoreTablesPanel() {
//		this(null);
//	}

	private abstract class MyAction extends SimpleAction {

		public MyAction(String name, String tooltip, String smallIconPng) {
			super(name, tooltip, smallIconPng);
		}

		@Override
		public void updateEnabledState() {
			setEnabled(ds != null);
		}
		
		public boolean addToToolbar(){
			return true;
		}
	}

	private abstract class MyNeedsSelectedAction extends MyAction {

		public MyNeedsSelectedAction(String name, String tooltip, String smallIconPng) {
			super(name, tooltip, smallIconPng);
		}

		@Override
		public void updateEnabledState() {
			setEnabled(ds != null && list.getSelectedValue() != null && list.getSelectedValuesList().size() == 1);
		}
	}

	// private class LaunchScriptWizardAction extends MyNeedsSelectedAction{
	// private final ScriptType type;
	// public LaunchScriptWizardAction(String name, String tooltip,ScriptType
	// type) {
	// super(name, tooltip, IconsByScriptType.getFilename(type));
	// this.type = type;
	// }
	//
	// @Override
	// public void actionPerformed(ActionEvent e) {
	// if(list.getSelectedValue()!=null){
	// appFrame.launchSingleTableScriptWizard(list.getSelectedValue().getImmutableId(),
	// type);
	// }
	// }
	// }

	private class LaunchScriptWizardActionV2 extends ODLAction {
		private final ODLComponent component;

		public LaunchScriptWizardActionV2(ODLComponent component) {
			super(component.getName(), component.getName(), component.getIcon(appFrame.getApi(), ODLComponent.MODE_DEFAULT));
			this.component = component;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			List<ODLTableReadOnly> selected = list.getSelectedValuesList();
			int[] ids = new int[selected.size()];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = selected.get(i).getImmutableId();
			}

			appFrame.launchScriptWizard(ids, component);
		}

		@Override
		public void updateEnabledState() {
			// setEnabled(minNbTables == 0 || (ds!=null && hasSingleTableConfig
			// && list.getSelectedValue()!=null));
			setEnabled(true);
		}

	}

	/**
	 * Create the panel.
	 */
	public DatastoreTablesPanel(AbstractAppFrame launcher) {
		this.appFrame = launcher;
		setLayout(new BorderLayout(0, 0));

		list = new JList<ODLTableReadOnly>();
		list.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		list.setModel(new DefaultListModel<ODLTableReadOnly>());
		list.setCellRenderer(new DefaultListCellRenderer() {

			@Override
		    public Component getListCellRendererComponent(
		        JList<?> list,
		        Object value,
		        int index,
		        boolean isSelected,
		        boolean cellHasFocus)
		    {
		    	Component ret = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
//		    	ListModel<? extends ODLTableReadOnly> model = DatastoreTablesPanel.this.list.getModel();
//		    	if(model!=null && index < model.getSize()){
//		    		ODLTableReadOnly table = model.getElementAt(index);
//		    		if(table!=null && table.getRowCount()>0){
//		    			long flags = table.getRowFlags(table.getRowId(0));
//		    			if((flags & TableFlags.FLAG_LINKED_EXCEL_READ_ONLY_DATA) == TableFlags.FLAG_LINKED_EXCEL_READ_ONLY_DATA){
//		    				// as the linked data always appears at the top of the table, this table has read only data
//		    				ret.setFont(ret.getFont().deriveFont(Font.ITALIC));			    				
//		    			}
//		    		}
//		    	}
		    	return ret;
		    }
		});

		DefaultListSelectionModel selectionModel = new DefaultListSelectionModel() {
			@Override
			public void setSelectionInterval(int index0, int index1) {
				super.setSelectionInterval(index0, index1);

			}
		};
		selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setSelectionModel(selectionModel);

		// put list in a scrollpane
		JScrollPane listScrollPane = new JScrollPane();
		listScrollPane.setViewportView(list);
		setLayout(new BorderLayout());
		
		// get properties to see if we should use a background image
		Boolean tmpBool = appFrame.getApi().properties().getBool("app.tablespanel.usebackgroundimage");
		if(tmpBool!=null && tmpBool){
			useBackgroundImage=true;
		}else{
			useBackgroundImage = false;
		}
		
		if(useBackgroundImage){
			// put in a further panel so we can draw a custom background
			JPanel listPanel = new JPanel(new BorderLayout()){
			     @Override
			        protected void paintComponent(Graphics g) {
			            super.paintComponent(g);
			        	AppBackground.paintBackground(this, g, appFrame.getBackgroundImage());
			        }
			};
			listScrollPane.setOpaque(false);
			listScrollPane.getViewport().setOpaque(false);
			listPanel.add(listScrollPane, BorderLayout.CENTER);
			add(listPanel, BorderLayout.CENTER);	
		}else{
			add(listScrollPane, BorderLayout.CENTER);				
		}


		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		add(toolBar, BorderLayout.SOUTH);

		// create right-click popup menu on the list
		final JPopupMenu popup = new JPopupMenu();
		list.addMouseListener(new PopupMenuMouseAdapter() {

			@Override
			protected void launchMenu(MouseEvent me) {
				//select the item under the mouse
				list.setSelectedIndex(list.locationToIndex(me.getPoint()));
				
				// launch the popup menu
				popup.show(me.getComponent(), me.getX(), me.getY());
			}
		});
		list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateAppearance();
			}
		});

		// create double click event on list
		list.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() >= 2) {
					// ensure the item is selected (may have been deselected by the toggle)
					list.setSelectedIndex(list.locationToIndex(evt.getPoint()));
					
					launchSelectedTable();
				}
			}
		});

		// create all actions and add as buttons and menu items
		actions = createActions();
		for (MyAction action : actions) {
			if(action.addToToolbar()){
				toolBar.add(action);				
			}
			popup.add(action);
		}

		// add wizard for single table script types
		if(appFrame.getAppPermissions().isScriptEditingAllowed()){
			wizardActions = createLaunchScriptWizardActions();
			final JPopupMenu wizardsPopupMenu = new JPopupMenu();
			JMenu wizardsMenu = new JMenu("Component wizard...");
			for (ODLAction action : wizardActions) {
				wizardsPopupMenu.add(action);
				wizardsMenu.add(action);
			}
			DropDownMenuButton wizardsMenuButton = new DropDownMenuButton(Icons.loadFromStandardPath("tools-wizard-2.png")) {

				@Override
				protected JPopupMenu getPopupMenu() {
					return wizardsPopupMenu;
				}
			};
			wizardsMenuButton.setToolTipText("Run the component wizard");
			toolBar.add(wizardsMenuButton);
			popup.add(wizardsMenu);			
		}else{
			wizardActions = null;
		}


		tablesLabel = new JLabel("Tables");
		tablesLabel.setHorizontalAlignment(SwingConstants.CENTER);
		add(tablesLabel, BorderLayout.NORTH);

		updateAppearance();
	}

	private List<ODLAction> createLaunchScriptWizardActions() {
		ArrayList<ODLAction> ret = new ArrayList<>();

		for (ODLComponent component : ODLGlobalComponents.getProvider()) {
			if (component.getIcon(appFrame.getApi(), ODLComponent.MODE_DEFAULT) != null && IteratorUtils.size(ScriptTemplatesImpl.getTemplates(appFrame.getApi(), component)) > 0) {
				ret.add(new LaunchScriptWizardActionV2(component));
			}
		}
		return ret;
	}

	private List<MyAction> createActions() {
		ArrayList<MyAction> ret = new ArrayList<>();

		ret.add(new MyAction("Add table", "Add table", "table-add.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				String newName = "New table";
				if(ds!=null){
					newName = Strings.makeUnique(newName, new DoesStringExist() {
						
						@Override
						public boolean isExisting(String s) {
							return TableUtils.findTable(ds, s)!=null;
						}
					});
				};
				String name = getTableNameFromDialog(newName);
				if (name != null) {
					class Result{
						ODLTableDefinition table;
					}
					Result result = new Result();
					if(modifyDs(new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							result.table = ds.createTable(name, -1);
							return true;
						}
					})){
						appFrame.launchTableGrid(result.table.getImmutableId());						
					};
	
				}
			}
		});

		ret.add(new MyNeedsSelectedAction("Delete table", "Delete table", "table-delete-2.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				ODLTableDefinition table = getSelectedShowErrorIfNone();
				if (table != null) {
					modifyDs(new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							ds.deleteTableById(table.getImmutableId());
							return true;
						}
					});
				}
			}
		});

		ret.add(new MyNeedsSelectedAction("Rename table", "Rename table", "table-rename.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				ODLTableDefinition table = getSelectedShowErrorIfNone();
				if (table != null) {
					String name = getTableNameFromDialog(table.getName());
					if (name != null) {
						modifyDs(new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return ds.setTableName(table.getImmutableId(), name);
							}
						});						
					}
				}
			}
		});

		ret.add(new MyNeedsSelectedAction("Edit table schema", "Edit table schema", "table-edit.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				ODLTableDefinition table = getSelectedShowErrorIfNone();
				if (table != null) {
					appFrame.launchTableSchemaEditor(table.getImmutableId());
				}
			}
		});

		ret.add(new MyNeedsSelectedAction("Copy table", "Copy table", "table-copy.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				ODLTableDefinition table = getSelectedShowErrorIfNone();
				if (table != null) {
					String name = getTableNameFromDialog("Copy of " + table.getName());
					if (name != null) {
						class Result{
							ODLTableReadOnly copy ;	
						}
						Result result = new Result();
						if(modifyDs(new Callable<Boolean>(){

							@Override
							public Boolean call() throws Exception {
								result.copy = DatastoreCopier.copyTableIntoSameDatastore(ds, table.getImmutableId(), name);
								return true;
							}
							
						})){
							appFrame.launchTableGrid(result.copy.getImmutableId());	
						}
				
					}
				}
			}
		});

		ret.add(new MyNeedsSelectedAction("Go to table", "Go to table", "table-go.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				launchSelectedTable();
			}

		});

		ret.add(new MyAction("Select all tables", "Select all tables", "select-all-tables.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				list.getSelectionModel().setSelectionInterval(0, list.getModel().getSize()-1);
			}

			@Override
			public boolean addToToolbar(){
				return false;
			}
		});
		

		ret.add(new MyNeedsSelectedAction("Unselect all tables", "Unselect all tables", "unselect-all-tables.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				list.clearSelection();
			}

			@Override
			public boolean addToToolbar(){
				return false;
			}

		});
		

		// ret.add(new LaunchScriptWizardAction("Build table view",
		// "Build a view of the table", ScriptType.ADAPTED_TABLE));
		// ret.add(new LaunchScriptWizardAction("Show map",
		// "Launch the show map wizard",ScriptType.SHOW_MAP));

		return ret;
	}

	@Override
	public void tableChanged(int tableId, int firstRow, int lastRow) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void datastoreStructureChanged() {
		ODLDatastoreUndoable<? extends ODLTableAlterable> currentDs = ds;
		onDatastoreClosed();
		setDatastore(currentDs);
	}

	@Override
	public ODLListenerType getType() {
		return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
	}

	public void setDatastore(ODLDatastoreUndoable<? extends ODLTableAlterable> ds) {
		if (this.ds != null) {
			throw new RuntimeException();
		}

		this.ds = ds;
		this.ds.addListener(this);

		List<ODLTableReadOnly> tables = getSortedTables(ds);
		
		// update list
		list.setModel(new ListModel<ODLTableReadOnly>() {

			@Override
			public int getSize() {
				return tables.size();
			}

			@Override
			public ODLTableReadOnly getElementAt(int index) {
				return new SimpleTableReadOnlyDecorator(tables.get(index)) {

					@Override
					public String toString() {
						return getName();
					}
				};
			}

			@Override
			public void addListDataListener(ListDataListener l) {
				// TODO Auto-generated method stub

			}

			@Override
			public void removeListDataListener(ListDataListener l) {
				// TODO Auto-generated method stub

			}
		});

		updateAppearance();
	}

	/**
	 * Sort tables, view-layer-style first and then any other table
	 * @param ds
	 * @return
	 */
	private List<ODLTableReadOnly> getSortedTables(ODLDatastoreAlterable<? extends ODLTableAlterable> ds) {
		// Get sorted table names
		List<ODLTableReadOnly> tables = new ArrayList<>(ds.getTableCount());
		for(int i =0 ; i < ds.getTableCount() ; i++){
			tables.add(ds.getTableAt(i));
		}
		Collections.sort(tables,new Comparator<ODLTableReadOnly>(){
			final String [] specials = new String[]{View.TABLE_NAME,Layer.TABLE_NAME,Style.TABLE_NAME};
			@Override
			public int compare(ODLTableReadOnly o1, ODLTableReadOnly o2) {

				int i1 = getNameScore(o1);
				int i2 = getNameScore(o2);
				if(i1!=i2){
					return Integer.compare(i1, i2);
				}
				return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
			}
			
			private int getNameScore(ODLTableDefinition o1) {
				ODLTableDefinition t = o1;
				int i =0 ;
				while(i < specials.length){
					if(Strings.equals(specials[i], t.getName())){
						break;
					}
					i++;
				}
				return i;
			}
			
		});
		return tables;
	}

	public void onDatastoreClosed() {
		if (ds != null) {
			ds.removeListener(this);
			ds = null;
		}
		list.setModel(new DefaultListModel<ODLTableReadOnly>());
		updateAppearance();
	}

	private void launchSelectedTable() {
		if (list.getSelectedValue() == null) {
			return;
		}
		DatastoreTablesPanel.this.appFrame.launchTableGrid(list.getSelectedValue().getImmutableId());
	}

	// public interface UILauncher {
	// JComponent launchTableGrid(int tableId);
	// void launchSingleTableScriptWizard(int tableId, final ScriptType type);
	//
	// }

	private void updateAppearance() {
		for (MyAction action : actions) {
			action.updateEnabledState();
		}
		
		if(wizardActions!=null){
			for (ODLAction action : wizardActions) {
				action.updateEnabledState();
			}			
		}
		
		list.setEnabled(ds != null);
		// wizardsMenuButton.setEnabled(ds!=null);

		if (ds == null) {
			// set list to grey
			list.setBackground(new Color(220, 220, 220));
			if(useBackgroundImage){
				list.setOpaque(false);				
			}
		} else {
			list.setBackground(Color.WHITE);
			if(useBackgroundImage){
				list.setOpaque(true);				
			}
		}

		tablesLabel.setEnabled(ds != null);
	}

	private ODLTableDefinition getSelectedShowErrorIfNone() {
		ODLTableDefinition ret = list.getSelectedValue();
		if (ret == null) {
			JOptionPane.showMessageDialog(this.getParent(), "No table is selected");
		}
		return ret;
	}

	private String getTableNameFromDialog(String current) {
		String s = JOptionPane.showInputDialog(this.getParent(), "Enter new table name", current);
		return s;
	}

	private void showTableNameError() {
		JOptionPane.showMessageDialog(DatastoreTablesPanel.this, "Could not perform action. Is name already used?");
	}

	
	private boolean modifyDs(Callable<Boolean> callable){
		ExecutionReportImpl report = new ExecutionReportImpl();
		if(!TableUtils.runTransaction(ds, callable,report)){
			report.setFailed("An error occurred when attempting to modify the datastore.");
			new ExecutionReportDialog((JFrame)SwingUtilities.getWindowAncestor(this), "Error", report, false).setVisible(true);
			return false;
		}
		return true;
	}
}
