/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio;

import java.util.HashSet;

import com.opendoorlogistics.api.standardcomponents.map.MapSelectionList;
import com.opendoorlogistics.api.standardcomponents.map.MapSelectionList.MapSelectionListRegister;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.utils.TableFlagUtils;

public abstract class GlobalMapSelectedRowsManager implements MapSelectionListRegister{
	private HashSet<MapSelectionList> registeredSelectionLists = new HashSet<>();
	private HashSet<GlobalSelectionChangedCB> listeners = new HashSet<>();

	public interface GlobalSelectionChangedCB{
		void selectionChanged(GlobalMapSelectedRowsManager manager);
	}
	
	protected void fireListeners(){
		for(GlobalSelectionChangedCB listener:listeners){
			listener.selectionChanged(this);
		}
	}
	
	@Override
	public void registerMapSelectionList(MapSelectionList list){
		registeredSelectionLists.add(list);
	}
	
	@Override
	public void unregisterMapSelectionList(MapSelectionList list){
		registeredSelectionLists.remove(list);
	}

	public void registerListener(GlobalSelectionChangedCB listener){
		listeners.add(listener);
	}
	
	public void unregisterListener(GlobalSelectionChangedCB listener){
		listeners.remove(listener);
	}

	public boolean isRowSelectedInMap(long rowId){
		for(MapSelectionList list:registeredSelectionLists){
			if(list.isSelectedId(rowId)){
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Count the number of rows selected in the datastore
	 * @param ds
	 * @return
	 */
	public static long countSelectedInDs(ODLDatastore<? extends ODLTableReadOnly> ds){
		long countSelected=0;
		for(int i = 0 ;i<ds.getTableCount() ; i++){
			ODLTableReadOnly table = ds.getTableAt(i);
			int n = table.getRowCount();
			for(int row=0;row<n;row++){
				long id = table.getRowId(row);
				long flags = table.getRowFlags(id);
				boolean selectedInDs = (flags & TableFlags.FLAG_ROW_SELECTED_IN_MAP)==TableFlags.FLAG_ROW_SELECTED_IN_MAP;
				if(selectedInDs){
					countSelected++;
				}
			}
		}
		
		return countSelected;
	}
}
