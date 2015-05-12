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
}
