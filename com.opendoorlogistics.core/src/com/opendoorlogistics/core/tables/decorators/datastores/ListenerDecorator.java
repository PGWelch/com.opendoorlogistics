/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.TIntHashSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

/**
 * Decorator which implements table listeners
 * 
 * @author Phil
 * 
 * @param <T>
 */
final public class ListenerDecorator<T extends ODLTableDefinition> extends SimpleDecorator<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 663660648531373287L;
	private HashMap<ODLListener, TIntHashSet> tableChangedListeners = new HashMap<ODLListener, TIntHashSet>();
	private HashSet<ODLListener> tableSetChanged = new HashSet<>();
	private boolean enabled = true;
	private Pending pending;
	
	private class Pending{
		TIntHashSet tablesModified= new TIntHashSet();
		boolean tableSetChanged=false;
	}
	
	public ListenerDecorator(Class<T> tableClass, ODLDatastore<? extends T> decorated) {
		super(tableClass, decorated);
	}

	@Override
	public void addListener(ODLListener tml, int... tableIds) {
		switch (tml.getType()) {
		case TABLE_CHANGED:
			if (tableChangedListeners.containsKey(tml)) {
				throw new RuntimeException();
			}
			tableChangedListeners.put(tml, new TIntHashSet(tableIds));
			break;

		case DATASTORE_STRUCTURE_CHANGED:
			tableSetChanged.add(tml);
			break;
		}

	}

	@Override
	public void removeListener(ODLListener tml) {
		switch (tml.getType()) {
		case TABLE_CHANGED:
			tableChangedListeners.remove(tml);
			break;

		case DATASTORE_STRUCTURE_CHANGED:
			tableSetChanged.remove(tml);
			break;
		}		
	
	}
	
	private void fireStructureListener(){
		if(enabled){
			// take copy to stop concurrent modification problems
			HashSet<ODLListener> copy = new HashSet<>(tableSetChanged);
			for(ODLListener listener : copy){
				listener.datastoreStructureChanged();
			}
		}else{
			// save to pending instead
			if(pending==null){
				pending = new Pending();
			}
			pending.tableSetChanged = true;
		}
	}

	private void fireTableModelListener(int tableId, int firstRow, int lastRow) {
		if (!enabled) {
			// save to pending instead
			if(pending==null){
				pending = new Pending();
			}
			pending.tablesModified.add(tableId);
			return;
		}

		if (firstRow < 0) {
			firstRow = 0;
		}
		
		// take copy to stop concurrent modification problems
		HashMap<ODLListener,TIntHashSet > copy = new HashMap<>();
		copy.putAll(tableChangedListeners);
		
		for (Map.Entry<ODLListener, TIntHashSet> entry : copy.entrySet()) {
			if (tableId == -1 || entry.getValue().contains(tableId) || entry.getValue().contains(-1)) {
				entry.getKey().tableChanged(-1, firstRow, lastRow);
			}
		}
	}

	@Override
	public void setValueAt(int tableId, Object aValue, int rowIndex, int columnIndex) {
		long id = getRowGlobalId(tableId, rowIndex);
		if(id!=-1){
			setValueById(tableId, aValue, id, columnIndex);
		}
//		Object oldValue = getValueAt(tableId, rowIndex, columnIndex);
//		
//		super.setValueAt(tableId, aValue, rowIndex, columnIndex);
//		
//		// Test value has actually changed before firing listener.
//		// As type conversion can occur, we should check the value directly from the table
//		Object newValue = getValueAt(tableId, rowIndex, columnIndex);
//		boolean different = false;
//		if(oldValue!=null && newValue==null){
//			different = true;
//		}
//		else if(oldValue==null && newValue!=null){
//			different = true;
//		}else if(oldValue!=null && newValue!=null && oldValue.equals(newValue)==false){
//			different = true;
//		}
//
//		if(different){
//			fireTableModelListener(tableId, rowIndex, rowIndex);			
//		}
	}

//	@Override
//	public void setRowFlags(int tableId, long flags, long rowId) {
//		super.setRowFlags(tableId, flags, rowId);
//		fireTableModelListener(tableId, 0, Integer.MAX_VALUE);			
//	}
	
	@Override
	public void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {

		Object oldValue = getValueById(tableId, rowId, columnIndex);
		
		super.setValueById(tableId, aValue, rowId, columnIndex);
		
		// Test value has actually changed before firing listener.
		// As type conversion can occur, we should check the value directly from the table
		Object newValue = getValueById(tableId, rowId, columnIndex);
		boolean different = false;
		if(oldValue!=null && newValue==null){
			different = true;
		}
		else if(oldValue==null && newValue!=null){
			different = true;
		}else if(oldValue!=null && newValue!=null && oldValue.equals(newValue)==false){
			different = true;
		}

		if(different){
			fireTableModelListener(tableId, 0, Integer.MAX_VALUE);			
		}
	}
	
	@Override
	public int createEmptyRow(int tableId, long rowId) {
		int ret = super.createEmptyRow(tableId, rowId);
		fireTableModelListener(tableId, ret - 1, Integer.MAX_VALUE);
		return ret;
	}
	
	@Override
	public void insertEmptyRow(int tableId, int insertAtRowNb,long rowId) {
		super.insertEmptyRow(tableId, insertAtRowNb, rowId);
		fireTableModelListener(tableId, insertAtRowNb - 1, Integer.MAX_VALUE);
	}

	@Override
	public void deleteRow(int tableId, int rowNumber) {
		super.deleteRow(tableId, rowNumber);
		fireTableModelListener(tableId, rowNumber - 1, Integer.MAX_VALUE);
	}

	@Override
	public int addColumn(int tableId, int id,String name, ODLColumnType type, long flags) {
		int index =super.addColumn(tableId, id,name, type, flags); 
		if (index!=-1) {
			fireAllListeners(tableId);
			return index;
		}
		return index;
	}

	@Override
	public boolean insertCol(int tableId,int id, int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		if (super.insertCol(tableId, id,col, name, type, flags, allowDuplicateNames)) {
			fireAllListeners(tableId);
			return true;
		}
		return false;
	}

	@Override
	public void deleteCol(int tableId, int col) {
		super.deleteCol(tableId, col);
		fireAllListeners(tableId);
	}

	@Override
	public T createTable(String tablename, int id) {
		T ret = super.createTable(tablename, id);
		fireStructureListener();
		if (ret != null) {
			fireTableModelListener(-1, 0, Integer.MAX_VALUE);
		}
		return ret;
	}

	@Override
	public void deleteTableById(int tableId) {
		super.deleteTableById(tableId);
		fireAllListeners(tableId);
	}

	@Override
	public boolean setTableName(int tableId, String newName) {
		if(super.setTableName(tableId, newName)){
			fireAllListeners(tableId);
			return true;
		}
		return false;
	}

	
	@Override
	public void setFlags(int tableId, long flags) {
		super.setFlags(tableId, flags);
		fireAllListeners(tableId);
	}

	@Override
	public void setColumnFlags(int tableId, int col, long flags) {
		super.setColumnFlags(tableId, col, flags);
		fireAllListeners(tableId);
	}

	private void fireAllListeners(int tableId) {
		fireStructureListener();
		fireTableModelListener(tableId, 0, Integer.MAX_VALUE);
	}
	

	@Override
	public void setColumnDescription(int tableId, int col, String description) {
		super.setColumnDescription(tableId, col, description);
		fireAllListeners(tableId);
	}

	@Override
	public void setColumnTags(int tableId, int col, Set<String> tags) {
		super.setColumnTags(tableId, col, tags);
		fireAllListeners(tableId);
	}

	@Override
	public void setTags(int tableId, Set<String> tags) {
		super.setTags(tableId, tags);
		fireAllListeners(tableId);
	}

	@Override
	public void setColumnDefaultValue(int tableId, int col, Object value) {
		super.setColumnDefaultValue(tableId, col, value);
		fireAllListeners(tableId);
	}


	@Override
	public void disableListeners() {
		enabled = false;
	}

	@Override
	public void enableListeners() {
		enabled = true;
		
		// fire anything pending
		if(pending!=null){
			// fire table set changed listeners first; if tables have been deleted we should close forms first
			if(pending.tableSetChanged){
				fireStructureListener();
			}
			
			pending.tablesModified.forEach(new TIntProcedure() {
				
				@Override
				public boolean execute(int tableId) {
					fireTableModelListener(tableId, 0, Integer.MAX_VALUE);
					return true;
				}
			});
		
		}
		
		pending = null;

	}

}
