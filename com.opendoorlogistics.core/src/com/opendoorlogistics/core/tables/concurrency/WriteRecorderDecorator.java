/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.concurrency;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

import java.util.BitSet;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.decorators.datastores.SimpleDecorator;

/**
 * A decorator which records all writes (row creation, insertion, deletion, setting
 * values) and does not allow any structure modification with the exception of tables
 * created via this decorator.
 * UnsupportedOperationException are thrown if the client code tries to modify table structure
 * for any tables not created via this decorator.
 * @author Phil
 *
 * @param <T>
 */
final public class WriteRecorderDecorator<T extends ODLTableDefinition> extends SimpleDecorator<T>{
	private final TLongHashSet deletedOriginalRowIds = new TLongHashSet();
	private final TLongHashSet appendedRowIds = new TLongHashSet();
	private final TLongObjectHashMap<BitSet> setCols = new TLongObjectHashMap<>();
	private final TIntHashSet createdTableIds = new TIntHashSet();
	
	public WriteRecorderDecorator(Class<T> tableClass, ODLDatastore<? extends T> decorated) {
		super(tableClass, decorated);
	}

	public long [] getWrittenRowIds(){
		return setCols.keys();
	}

	public BitSet getWrittenCols(long rowId){
		return setCols.get(rowId);
	}
	
	public long [] getDeletedOriginalRowIds(){
		return deletedOriginalRowIds.toArray();
	}
	
	public boolean isCreatedTable(int tableId){
		return createdTableIds.contains(tableId);
	}
	
	public boolean isAppendedRow(long globalRowId){
		return appendedRowIds.contains(globalRowId);
	}
	
	
	@Override
	public void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {
	//	doCommand(new SetByRowId(tableId, rowId, columnIndex, aValue));	
		
		super.setValueById(tableId, aValue, rowId, columnIndex);
		
		ODLTableReadOnly table = readOnlyTable(tableId);
		if(table!=null){
			BitSet bs = setCols.get(rowId);
			if(bs == null){
				bs = new BitSet(table.getColumnCount());
				setCols.put(rowId, bs);
			}
			bs.set(columnIndex);
			
		}
	}

	@Override
	public void setValueAt(int tableId, Object aValue, int rowIndex, int columnIndex) {
		long id = getRowGlobalId(tableId, rowIndex);
		if(id!=-1){
			setValueById(tableId, aValue, id, columnIndex);
		}
	}

	@Override
	public int createEmptyRow(int tableId, long rowId) {
		int indx= super.createEmptyRow(tableId, rowId);
		appendedRowIds.add(getRowGlobalId(tableId, indx));
		return indx;
	}

	@Override
	public void insertEmptyRow(int tableId, int insertAtRowNb, long rowId) {		
		// only allow insertion at end
		ODLTable table = (ODLTable)getTableByImmutableId(tableId);
		if(insertAtRowNb == table.getRowCount()){
			int insertedAt = createEmptyRow(tableId, rowId);
			if(insertedAt!=insertAtRowNb){
				throw new RuntimeException();
			}
		}else{
			// running scripts which insert rows will screw up the merger with the original datastore...
			throw new UnsupportedOperationException();			
		}
	}

	@Override
	public void deleteRow(int tableId, int rowNumber) {
		long rowId = getRowGlobalId(tableId, rowNumber);
		if(appendedRowIds.contains(rowId)==false){
			deletedOriginalRowIds.add(rowId);			
		}
		
		super.deleteRow(tableId, rowNumber);
	}

	@Override
	public int addColumn(int tableId,int id, String name, ODLColumnType type, long flags) {
		if(createdTableIds.contains(tableId)){
			return super.addColumn(tableId, id,name, type, flags);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFlags(int tableId, long flags) {
		if(createdTableIds.contains(tableId)){
			super.setFlags(tableId, flags);
			return;
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public void setColumnFlags(int tableId, int col, long flags) {
		if(createdTableIds.contains(tableId)){
			super.setColumnFlags(tableId, col, flags);
			return;
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteCol(int tableId, int col) {
		if(createdTableIds.contains(tableId)){
			super.deleteCol(tableId, col);
			return;
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean insertCol(int tableId,int id, int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		if(createdTableIds.contains(tableId)){
			return super.insertCol(tableId, id,col, name, type, flags, allowDuplicateNames);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public T createTable(String tablename, int tableId) {
		T ret = super.createTable(tablename, tableId);
		createdTableIds.add(ret.getImmutableId());
		return ret;
	}

	@Override
	public void deleteTableById(int tableId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setTableName(int tableId, String newName) {
		if(createdTableIds.contains(tableId)){
			return super.setTableName(tableId, newName);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFlags(long flags) {
		throw new UnsupportedOperationException();
	}

	
}
