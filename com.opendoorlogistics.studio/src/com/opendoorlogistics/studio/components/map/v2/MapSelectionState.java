package com.opendoorlogistics.studio.components.map.v2;

import com.opendoorlogistics.studio.GlobalMapSelectedRowsManager;

import gnu.trove.set.hash.TLongHashSet;

public class MapSelectionState {
	private final TLongHashSet selectedGlobalRowIds = new TLongHashSet();
	
	public long [] copyIds(){
		return selectedGlobalRowIds.toArray();
	}
	
	public TLongHashSet copySet(){
		return new TLongHashSet(selectedGlobalRowIds);
	}
	
	public void clear(){
		selectedGlobalRowIds.clear();
	}
	
	public void add(long id){
		selectedGlobalRowIds.add(id);
	}
	
	public void set(long [] ids){
		selectedGlobalRowIds.clear();
		selectedGlobalRowIds.addAll(ids);
	}
	
	public boolean contains(long id){
		return selectedGlobalRowIds.contains(id);
	}
	
	public boolean equals(TLongHashSet set){
		return selectedGlobalRowIds.equals(set);
	}
}
