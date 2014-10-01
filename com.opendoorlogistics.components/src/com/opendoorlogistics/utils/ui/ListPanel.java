/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.ui;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class ListPanel<T> extends ItemsPanel<T> {

	protected JList<ItemContainer> list;


	private class MyListModel implements ListModel<ItemContainer> {

		@Override
		public int getSize() {
			return items.size();
		}

		@Override
		public ItemContainer getElementAt(int index) {
			return new ItemContainer(items.get(index), index);
		}

		@Override
		public void addListDataListener(ListDataListener l) {
			// TODO Auto-generated method stub

		}

		@Override
		public void removeListDataListener(ListDataListener l) {
			// TODO Auto-generated method stub

		}

	}

	@Override
	protected JComponent createItemsComponent(){
		list = new JList<>();

		// add list selection changed listener
		list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateAppearance();
			}
		});
		
		return list;
	}

	public ListPanel(List<T> items, String itemName) {
		super(items, itemName);

	}

	@Override
	protected void updateList() {
		list.setModel(new MyListModel());
	}

	@Override
	protected void setSelectedIndex(int index){
		list.setSelectedIndex(index);		
	}

	@Override
	protected int getSelectedIndx(){
		return list.getSelectedIndex();
	}


}
