/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
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
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.codefromweb.DropDownMenuButton;
import com.opendoorlogistics.core.api.impl.scripts.ScriptTemplatesImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.tables.decorators.tables.SimpleTableDefinitionDecorator;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.iterators.IteratorUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.strings.Strings.DoesStringExist;
import com.opendoorlogistics.core.utils.ui.PopupMenuMouseAdapter;
import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.utils.ui.ODLAction;
import com.opendoorlogistics.utils.ui.SimpleAction;

final public class DatastoreTablesPanel extends JPanel implements ODLListener {
	private ODLDatastoreAlterable<? extends ODLTableAlterable> ds;
	private final JList<ODLTableDefinition> list;
	private final AppFrame appFrame;
	private final List<MyAction> actions;
	private final List<ODLAction> wizardActions;
	private final DropDownMenuButton wizardsMenuButton;
	private final JLabel tablesLabel;

	public DatastoreTablesPanel() {
		this(null);
	}

	private abstract class MyAction extends SimpleAction {

		public MyAction(String name, String tooltip, String smallIconPng) {
			super(name, tooltip, smallIconPng);
		}

		@Override
		public void updateEnabled() {
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
		public void updateEnabled() {
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
			List<ODLTableDefinition> selected = list.getSelectedValuesList();
			int[] ids = new int[selected.size()];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = selected.get(i).getImmutableId();
			}

			appFrame.launchScriptWizard(ids, component);
		}

		public void updateEnabled() {
			// setEnabled(minNbTables == 0 || (ds!=null && hasSingleTableConfig
			// && list.getSelectedValue()!=null));
			setEnabled(true);
		}

	}

	/**
	 * Create the panel.
	 */
	public DatastoreTablesPanel(AppFrame launcher) {
		this.appFrame = launcher;
		setLayout(new BorderLayout(0, 0));

		list = new JList();
		list.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		list.setModel(new AbstractListModel() {
			String[] values = new String[] {};

			public int getSize() {
				return values.length;
			}

			public Object getElementAt(int index) {
				return values[index];
			}
		});
		list.setCellRenderer(new DefaultListCellRenderer() {
		});

		DefaultListSelectionModel selectionModel = new DefaultListSelectionModel() {
			@Override
			public void setSelectionInterval(int index0, int index1) {
				super.setSelectionInterval(index0, index1);
				
				// Comment out code do the deselection at the moment as it's a bit confusing in the UI; offer a unselect all action instead.
//				// if we only have one element selected and we've just clicked on it, deselect it
//				if (index0 == index1 && isSelectedIndex(index0) && getMinSelectionIndex() == index0 && getMaxSelectionIndex() == index1) {
//					removeSelectionInterval(index0, index1);
//				} else {
//					super.setSelectionInterval(index0, index1);
//				}
			}
		};
		selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setSelectionModel(selectionModel);

		// See
		// http://stackoverflow.com/questions/2528344/jlist-deselect-when-clicking-an-already-selected-item
		// Allows multiple selection and toggling of selected.
		// list.setSelectionModel(new DefaultListSelectionModel() {
		// private static final long serialVersionUID = 1L;
		//
		// boolean gestureStarted = false;
		//
		// @Override
		// public void setSelectionInterval(int index0, int index1) {
		// if(!gestureStarted){
		// if (isSelectedIndex(index0)) {
		// super.removeSelectionInterval(index0, index1);
		// } else {
		// super.addSelectionInterval(index0, index1);
		// }
		// }
		// gestureStarted = true;
		// }
		//
		// @Override
		// public void setValueIsAdjusting(boolean isAdjusting) {
		// if (isAdjusting == false) {
		// gestureStarted = false;
		// }
		// }
		//
		// });

		JScrollPane listScrollPane = new JScrollPane();
		listScrollPane.setViewportView(list);
		setLayout(new BorderLayout());
		add(listScrollPane, BorderLayout.CENTER);

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
			// public void mouseReleased(MouseEvent Me) {
			// if (Me.isPopupTrigger()) {
			// popup.show(Me.getComponent(), Me.getX(), Me.getY());
			// }
			// }
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
		wizardActions = createLaunchScriptWizardActions();
		final JPopupMenu wizardsPopupMenu = new JPopupMenu();
		JMenu wizardsMenu = new JMenu("Component wizard...");
		for (ODLAction action : wizardActions) {
			wizardsPopupMenu.add(action);
			wizardsMenu.add(action);
		}
		wizardsMenuButton = new DropDownMenuButton(Icons.loadFromStandardPath("tools-wizard-2.png")) {

			@Override
			protected JPopupMenu getPopupMenu() {
				return wizardsPopupMenu;
			}
		};
		wizardsMenuButton.setToolTipText("Run the component wizard");
		toolBar.add(wizardsMenuButton);
		popup.add(wizardsMenu);

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
					ODLTableDefinition table = null;
					try {
						table = ds.createTable(name, -1);
					} catch (Throwable e2) {

					}
					if (table == null) {
						showTableNameError();
					} else {
						appFrame.launchTableGrid(table.getImmutableId());
					}
				}
			}
		});

		ret.add(new MyNeedsSelectedAction("Delete table", "Delete table", "table-delete-2.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				ODLTableDefinition table = getSelectedShowErrorIfNone();
				if (table != null) {
					ds.deleteTableById(table.getImmutableId());
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
						if (!ds.setTableName(table.getImmutableId(), name)) {
							showTableNameError();
						}
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
						ODLTableReadOnly copy = DatastoreCopier.copyTableIntoSameDatastore(ds, table.getImmutableId(), name);
						if (copy != null) {
							appFrame.launchTableGrid(copy.getImmutableId());
						} else {
							showTableNameError();
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
		ODLDatastoreAlterable<? extends ODLTableAlterable> currentDs = ds;
		onDatastoreClosed();
		setDatastore(currentDs);
	}

	@Override
	public ODLListenerType getType() {
		return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
	}

	public void setDatastore(ODLDatastoreAlterable<? extends ODLTableAlterable> ds) {
		if (this.ds != null) {
			throw new RuntimeException();
		}

		this.ds = ds;
		this.ds.addListener(this);

		final List<ODLTableDefinition> tables = TableUtils.getAlphabeticallySortedTables(this.ds);

		// update list
		list.setModel(new ListModel<ODLTableDefinition>() {

			@Override
			public int getSize() {
				return tables.size();
			}

			@Override
			public ODLTableDefinition getElementAt(int index) {
				return new SimpleTableDefinitionDecorator(tables.get(index)) {

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

	public void onDatastoreClosed() {
		if (ds != null) {
			ds.removeListener(this);
			ds = null;
		}
		list.setModel(new DefaultListModel<ODLTableDefinition>());
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
			action.updateEnabled();
		}
		for (ODLAction action : wizardActions) {
			action.updateEnabled();
		}
		list.setEnabled(ds != null);
		// wizardsMenuButton.setEnabled(ds!=null);

		if (ds == null) {
			list.setBackground(new Color(220, 220, 220));
		} else {
			list.setBackground(Color.WHITE);
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

}
