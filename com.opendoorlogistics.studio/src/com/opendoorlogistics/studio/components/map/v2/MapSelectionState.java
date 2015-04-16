package com.opendoorlogistics.studio.components.map.v2;

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
		set(null);
	}
	
	public void add(long id){
		selectedGlobalRowIds.add(id);
	}
	
	public void set(long [] ids){
		selectedGlobalRowIds.clear();
		if(ids!=null){
			selectedGlobalRowIds.addAll(ids);			
		}
	}
	
	public boolean contains(long id){
		return selectedGlobalRowIds.contains(id);
	}
	
	public boolean equals(TLongHashSet set){
		return selectedGlobalRowIds.equals(set);
	}
}
