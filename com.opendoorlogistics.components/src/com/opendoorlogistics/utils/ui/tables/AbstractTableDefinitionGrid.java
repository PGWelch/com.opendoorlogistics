/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.ui.tables;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.opendoorlogistics.utils.ui.ODLAction;
import com.opendoorlogistics.utils.ui.SimpleAction;
import com.opendoorlogistics.utils.ui.SimpleActionConfig;

public abstract class AbstractTableDefinitionGrid extends JPanel {
	protected final JTable table;
	protected final static String COLUMN = "Column";
	private final List<ODLAction> actions;

	public abstract class MyAction extends SimpleAction {

		public MyAction(SimpleActionConfig config) {
			super(config);
		}

		public void updateEnabledState() {
			setEnabled(requiresSelection == false || table.getSelectedRow() != -1);
		}
	}

	protected JTable createJTable(){
		return new JTable();
	}
	
	public AbstractTableDefinitionGrid() {
		this.table =createJTable();
		this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.table.setFillsViewportHeight(true);
		table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane, BorderLayout.CENTER);
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		
		// Override the default border setting an empty one as components
		// on the toolbar can make holes in the default border 
		toolBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		add(toolBar, BorderLayout.SOUTH);

		this.actions = createActions();
		fillToolbar(toolBar);

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateAppearance();
			}
		});

		// show disabled checkboxes as disabled
		JTableUtils.setDisabledCheckboxesGreyedOut(table);
		
		table.setRowHeight(26);
		updateAppearance();
		
//		table.addFocusListener(new FocusListener() {
//			
//			@Override
//			public void focusLost(FocusEvent arg0) {
//				if(table.isEditing()){
//					table.
//				}
//				System.out.println("focused lost "+ System.currentTimeMillis());
//			}
//			
//			@Override
//			public void focusGained(FocusEvent arg0) {
//				System.out.println("focused gained " + System.currentTimeMillis());
//				
//			}
//		});
	}

	protected void fillToolbar(JToolBar toolBar) {
		ODLAction.addToToolbar(toolBar, actions);

	}

	protected List<ODLAction> createActions() {
		ArrayList<ODLAction> ret = new ArrayList<>();

		ret.add(new MyAction(SimpleActionConfig.addItem.setItemName(COLUMN)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				createNewColumn();
				updateAppearance();
				table.getSelectionModel().setSelectionInterval(table.getRowCount()-1, table.getRowCount()-1);
			}
		});

		ret.add(new MyAction(SimpleActionConfig.moveItemUp.setItemName(COLUMN)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				int row = table.getSelectedRow();
				if (row != -1 && row > 0) {
					moveItemUp(row);
					updateAppearance();
					table.getSelectionModel().setSelectionInterval(row-1,row-1);					
				}

			}

		});

		ret.add(new MyAction(SimpleActionConfig.moveItemDown.setItemName(COLUMN)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				int row = table.getSelectedRow();
				if (row != -1 && row < table.getRowCount() - 1) {
					moveItemDown(row);
					updateAppearance();
					table.getSelectionModel().setSelectionInterval(row+1,row+1);					
				}

			}
		});

		ret.add(new MyAction(SimpleActionConfig.deleteItem.setItemName(COLUMN)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				int row = table.getSelectedRow();
				if (row != -1) {
					deleteItem(row);
					updateAppearance();
					if(row < table.getRowCount()){
						table.getSelectionModel().setSelectionInterval(row,row);											
					}
					else if(row - 1 < table.getRowCount()){
						table.getSelectionModel().setSelectionInterval(row-1,row-1);																	
					}
				}
				
			}
			
			@Override
			public void updateEnabledState() {
				setEnabled( (table.getSelectedRow() != -1) && isDeleteColumnAllowed(table.getSelectedRow()));
			}
		});

		return ret;
	}

	protected boolean isDeleteColumnAllowed(int col){
		return true;
	}
	
	public void updateAppearance() {
		for (ODLAction action : actions) {
			if(action!=null){
				action.updateEnabledState();				
			}
		}
	}

	protected abstract void createNewColumn();

	protected abstract void moveItemUp(int row);

	protected abstract void moveItemDown(int row);

	protected abstract void deleteItem(int row);
	

}
