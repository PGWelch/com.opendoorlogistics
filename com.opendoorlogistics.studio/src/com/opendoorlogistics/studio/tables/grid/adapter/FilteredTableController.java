/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid.adapter;

import java.util.Iterator;
import java.util.Map;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.decorators.datastores.RowFilterDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.SimpleDecorator;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;

class FilteredTableController   {
	private final SimpleDecorator<ODLTableReadOnly> switchableDecorator;
	private final RowFilterDecorator<ODLTableReadOnly> filterDecorator;
	private final ODLDatastore<? extends ODLTableReadOnly> unfiltered;
	private final FilterState filterState ;
	private final int tableId;
	private boolean hasFilters;
	
	public static class FilterState{
		private final  StandardisedStringTreeMap<String> map = new StandardisedStringTreeMap<>(true);
		
		public String getColumnFilter(String column){
			return map.get(column);
		}
	}

	public FilteredTableController(ODLDatastore<? extends ODLTableReadOnly> ds, int tableId){
		this(ds,tableId, new FilterState());
	}

	public FilteredTableController(ODLDatastore<? extends ODLTableReadOnly> ds, int tableId, FilterState filterState){
		switchableDecorator = new SimpleDecorator<>(ODLTableReadOnly.class, ds);
		filterDecorator = new RowFilterDecorator<>(ds, tableId);
		unfiltered = ds;
		this.tableId =tableId;
		this.filterState = filterState;

		update();
	}
	
	public void update(){
		hasFilters = false;
		
		// get the current filters by column and remove any no longer used
		Iterator<Map.Entry<String,String> > it = filterState.map.entrySet().iterator();
		ODLTableReadOnly table = unfiltered.getTableByImmutableId(tableId);
		if(table==null){
			filterState.map.clear();
			return;
		}
		int nc = table.getColumnCount();
		String [] vals = new String[nc];
		while(it.hasNext()){
			Map.Entry<String,String> entry = it.next();
			int colIndx = TableUtils.findColumnIndx(table, entry.getKey());
			if(colIndx==-1){
				it.remove();
			}else{
				vals[colIndx] = entry.getValue();
				if(vals[colIndx]!=null){
					hasFilters = true;
				}
			}
		}
		
		if(hasFilters){
	
			// clear filter and then parse table readding passing rows
			filterDecorator.clearRows();
			int nr = table.getRowCount();
			for(int i =0 ; i<nr;i++){
				boolean pass=true;
				for(int j=0;j<nc && pass;j++){
					if(vals[j]!=null){
						Object value = table.getValueAt(i, j);
						pass = ColumnValueProcessor.isEqual(vals[j], value);
					}
				}
				if(pass){
					filterDecorator.addRowToFilter(table.getImmutableId(), table.getRowId(i));
				}
			}

			switchableDecorator.replaceDecorated(filterDecorator);
		}else{
			switchableDecorator.replaceDecorated(unfiltered);			
		}
	}
	
	public ODLTableReadOnly getTable(){
		return switchableDecorator.getTableByImmutableId(tableId);
	}
	
	public ODLDatastore<ODLTableReadOnly> getDs(){
		return switchableDecorator;
	}


	public void setColumnFilter(String column, String value){
		filterState.map.put(column, value);
		update();
	}
	
	public boolean isFiltered(){
		return hasFilters;
	}
	
	public FilterState getFilterState(){
		return filterState;
	}
	

}
