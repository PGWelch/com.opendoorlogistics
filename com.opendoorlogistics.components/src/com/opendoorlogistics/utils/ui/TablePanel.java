/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.ui;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

public abstract class TablePanel<T> extends ItemsPanel<T> {

	protected JTable table;
	protected abstract TableModel createTableModel();
	
	@Override
	protected JComponent createItemsComponent(){
		table = new JTable();
		table.getTableHeader().setReorderingAllowed(false);
		
		// add list selection changed listener
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateAppearance();
			}
		});
		
		table.setFillsViewportHeight(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		return table;
	}

	public TablePanel(List<T> items, String itemName) {
		super(items, itemName);

	}

	@Override
	protected void updateList() {
		table.setModel(createTableModel());
	}

	@Override
	protected void setSelectedIndex(int index){
		table.getSelectionModel().setSelectionInterval(index, index);
	}

	@Override
	protected int getSelectedIndx(){
		return table.getSelectedRow();
	}

}
