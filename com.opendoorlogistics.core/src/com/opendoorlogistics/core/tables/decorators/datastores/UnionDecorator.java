/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TLongHashSet;

import java.util.List;
import java.util.Set;

import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.tables.TableQuery;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.tables.decorators.tables.FlatDs2TableObject;
import com.opendoorlogistics.core.tables.utils.DatastoreComparer;
import com.opendoorlogistics.core.tables.utils.TableFlagUtils;
import com.opendoorlogistics.core.utils.Long2Ints;

final public class UnionDecorator<T extends ODLTableDefinition> extends AbstractDecorator<T> {
	private final List<ODLDatastore<? extends T>> stores;
	private final int length;
	
	private static class UnsupportedInUnion extends UnsupportedOperationException{
		public UnsupportedInUnion() {
			super("Operation is unsupported for a union table / datastore.");
		}
	}
	
	public UnionDecorator( List<ODLDatastore<? extends T>> datastores){
		if(datastores.size() < 2){
			throw new RuntimeException("A union must have at least two datastores.");
		}
		this.stores = datastores;
		this.length = stores.size();
		for(int i =1 ; i< length ; i++){
			if(!DatastoreComparer.isSameStructure(stores.get(0), stores.get(i), DatastoreComparer.CHECK_IMMUTABLE_TABLE_IDS)){
				throw new RuntimeException("Datastores input into union do not have same structure.");
			}
		}
	}
	
	/**
	 * Translate a row index in a union table into the datastore
	 * index and row index of the non-union table.
	 * @param tableId
	 * @param rowIndex
	 * @return
	 */
	private long dsRowIndx(int tableId, int rowIndex){
		for(int dsIndex = 0; dsIndex<length;dsIndex++){
			ODLTableReadOnly table = (ODLTableReadOnly)stores.get(dsIndex).getTableByImmutableId(tableId);
			if(table!=null){
				int nr =table.getRowCount();
				if(rowIndex < nr){
					return Long2Ints.get(dsIndex, rowIndex);
				}
				rowIndex -= nr;
			}
		}
		return -1;
	}
	
	private static int dsIndx(long dsIndxRowIndx){
		return Long2Ints.getFirst(dsIndxRowIndx);
	}

	private static int rowIndx(long dsIndxRowIndx){
		return Long2Ints.getSecond(dsIndxRowIndx);
	}
	
	
	private int dsIndexWithRowId(int tableId, long rowId) {
		for(int i =0 ; i< length ; i++){		
			ODLDatastore<? extends T> store=stores.get(i);
			ODLTableReadOnly table = (ODLTableReadOnly)store.getTableByImmutableId(tableId);
			if(table!=null){
				if(table.containsRowId(rowId)){
					return i;
				}
			}
		}
		return -1;
	}
	
	@Override
	public T createTable(String tablename, int id) {
		throw new UnsupportedInUnion();
	}

	@Override
	public void deleteTableById(int tableId) {
		throw new UnsupportedInUnion();
		
	}

	@Override
	public boolean setTableName(int tableId, String newName) {
		throw new UnsupportedInUnion();
	}

	@Override
	public long getFlags() {
		return stores.get(0).getFlags();
	}

	@Override
	public void setFlags(long flags) {
		throw new UnsupportedInUnion();
	}

	@Override
	public ODLDatastoreAlterable<T> deepCopyWithShallowValueCopy(boolean lazyCopy) {
		throw new UnsupportedInUnion();
	}

	@Override
	public int getTableCount() {
		return stores.get(0).getTableCount();
	}


	@SuppressWarnings("unchecked")
	@Override
	public T getTableAt(int tableIndex) {
		T ret =null;
		if(tableIndex < stores.get(0).getTableCount()){
			int id = stores.get(0).getTableAt(tableIndex).getImmutableId();
			ret= (T)new FlatDs2TableObject(this,id);
		}
		return ret;
	}

	@Override
	public void addListener(ODLListener tml, int... tableIds) {
		// listener support probably un-needed as script execution framework takes
		// care of notifying when underlying data has changed
	}

	@Override
	public void removeListener(ODLListener tml) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disableListeners() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enableListeners() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startTransaction() {
		new MultiDsTransactions<T>().startTransaction(stores);
	}

	@Override
	public void endTransaction() {
		new MultiDsTransactions<T>().endTransaction(stores);
	}

	@Override
	public void rollbackTransaction() {
		new MultiDsTransactions<T>().rollbackTransaction(stores);
	}

	@Override
	public boolean isInTransaction() {
		return new MultiDsTransactions<T>().isInTransaction(stores);
	}

	@Override
	public int getRowCount(int tableId) {
		int sum=0;
		for(ODLDatastore<? extends T> ds: stores){
			ODLTableReadOnly table = (ODLTableReadOnly)ds.getTableByImmutableId(tableId);
			if(table!=null){
				sum += table.getRowCount();
			}
		}
		return sum;
	}
	
	@Override
	public Object getValueAt(int tableId, int rowIndex, int columnIndex) {
		long dsRow = dsRowIndx(tableId, rowIndex);
		if(dsRow!=-1){
			return readOnly(dsRow, tableId).getValueAt(rowIndx(dsRow), columnIndex);			
		}
		return null;
	}

	@Override
	public Object getValueById(int tableId, long rowId, int columnIndex) {
		int dsIndx = dsIndexWithRowId(tableId, rowId);
		if(dsIndx!=-1){
			return ((ODLTableReadOnly)stores.get(dsIndx).getTableByImmutableId(tableId)).getValueById(rowId, columnIndex);
		}
		return null;
	}

	@Override
	public ODLColumnType getColumnFieldType(int tableId, int col) {
		return stores.get(0).getTableByImmutableId(tableId).getColumnType(col);
	}

	@Override
	public String getColumnName(int tableId, int col) {
		return stores.get(0).getTableByImmutableId(tableId).getColumnName(col);
	}

	@Override
	public Object getColumnDefaultValue(int tableId, int col) {
		return stores.get(0).getTableByImmutableId(tableId).getColumnDefaultValue(col);
	}

	@Override
	public int getColumnCount(int tableId) {
		return stores.get(0).getTableByImmutableId(tableId).getColumnCount();
	}

	@Override
	public String getName(int tableId) {
		return stores.get(0).getTableByImmutableId(tableId).getName();
	}

	@Override
	public long getFlags(int tableId) {
		// Remove all edit flags as they are ambiguous - particularly as the same physical row (in the external datastore)
		// can appear multiple times in a union. 
		long ret= stores.get(0).getTableByImmutableId(tableId).getFlags();
		ret = TableFlagUtils.removeFlags(ret, TableFlags.UI_INSERT_ALLOWED | TableFlags.UI_MOVE_ALLOWED| TableFlags.UI_DELETE_ALLOWED|TableFlags.UI_SET_ALLOWED);

		return ret;
	}

	@Override
	public long getColumnFlags(int tableId, int col) {
		return stores.get(0).getTableByImmutableId(tableId).getColumnFlags(col);
	}

	@Override
	public int getColumnImmutableId(int tableId, int col) {
		return stores.get(0).getTableByImmutableId(tableId).getColumnImmutableId(col);
	}

	@Override
	public boolean containsRowId(int tableId, long rowId) {
		return dsIndexWithRowId(tableId, rowId)!=-1;
	}

	@Override
	public Set<String> getColumnTags(int tableId, int col) {
		return stores.get(0).getTableByImmutableId(tableId).getColumnTags(col);
	}

	@Override
	public Set<String> getTags(int tableId) {
		return stores.get(0).getTableByImmutableId(tableId).getTags();
	}

	@Override
	public String getColumnDescription(int tableId, int col) {
		return stores.get(0).getTableByImmutableId(tableId).getColumnDescription(col);
	}

	@Override
	public void setValueAt(int tableId, Object aValue, int rowIndex, int columnIndex) {
		long dsRow = dsRowIndx(tableId, rowIndex);
		if(dsRow!=-1){
			writable(dsRow, tableId).setValueAt(aValue,rowIndx(dsRow), columnIndex);			
		}
	}

	private ODLTable writable(long dsRow, int tableId) {
		return (ODLTable)stores.get((dsIndx(dsRow))).getTableByImmutableId(tableId);
	}

	@Override
	public void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {
		int dsIndx = dsIndexWithRowId(tableId, rowId);
		if(dsIndx!=-1){
			((ODLTable)stores.get(dsIndx).getTableByImmutableId(tableId)).setValueById(aValue,rowId, columnIndex);
		}
	}

	@Override
	public int createEmptyRow(int tableId, long rowId) {
		throw new UnsupportedInUnion();
	}

	@Override
	public void insertEmptyRow(int tableId, int insertAtRowNb, long rowId) {
		throw new UnsupportedInUnion();
	}

	@Override
	public void deleteRow(int tableId, int rowIndex) {
		long dsRow = dsRowIndx(tableId, rowIndex);
		if(dsRow!=-1){
			writable(dsRow, tableId).deleteRow(rowIndx(dsRow));			
		}
	}

	@Override
	public void deleteCol(int tableId, int col) {
		throw new UnsupportedInUnion();
	}

	@Override
	public boolean insertCol(int tableId, int id, int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		throw new UnsupportedInUnion();
	}

	@Override
	public int addColumn(int tableId, int columnid, String name, ODLColumnType type, long flags) {
		throw new UnsupportedInUnion();
	}

	@Override
	public void setFlags(int tableId, long flags) {
		throw new UnsupportedInUnion();
	}

	@Override
	public void setColumnFlags(int tableId, int col, long flags) {
		throw new UnsupportedInUnion();
	}

	@Override
	public void setColumnDefaultValue(int tableId, int col, Object value) {
		throw new UnsupportedInUnion();
	}

	@Override
	public void setColumnTags(int tableId, int col, Set<String> tags) {
		throw new UnsupportedInUnion();
	}

	@Override
	public void setTags(int tableId, Set<String> tags) {
		throw new UnsupportedInUnion();
	}

	@Override
	public void setColumnDescription(int tableId, int col, String description) {
		throw new UnsupportedInUnion();
	}


	private ODLTableReadOnly readOnly(long dsRow, int tableId) {
		return (ODLTableReadOnly)stores.get((dsIndx(dsRow))).getTableByImmutableId(tableId);
	}
	
	@Override
	public long getRowGlobalId(int tableId, int rowIndex) {
		long dsRow = dsRowIndx(tableId, rowIndex);
		if(dsRow!=-1){
			return readOnly(dsRow, tableId).getRowId(rowIndx(dsRow));			
		}
		return -1;
	}

	@Override
	public long getRowFlags(int tableId, long rowId) {
		int dsIndx = dsIndexWithRowId(tableId, rowId);
		if(dsIndx!=-1){
			return ((ODLTableReadOnly)stores.get(dsIndx).getTableByImmutableId(tableId)).getRowFlags(rowId);
		}
		return 0;
	}

	@Override
	public void setRowFlags(int tableId, long flags, long rowId) {
		int dsIndx = dsIndexWithRowId(tableId, rowId);
		if(dsIndx!=-1){
			((ODLTable)stores.get(dsIndx).getTableByImmutableId(tableId)).setRowFlags(flags,rowId);
		}
		
	}

	@Override
	public boolean isRollbackSupported() {
		return false;
	}

	@Override
	public boolean getTableExists(int tableId) {
		return true;
	}

	@Override
	public ODLTableDefinition deepCopyWithShallowValueCopy(int tableId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getRowLastModifiedTimeMillisecs(int tableId, long rowId) {
		int dsIndx = dsIndexWithRowId(tableId, rowId);
		if(dsIndx!=-1){
			return ((ODLTableReadOnly)stores.get(dsIndx).getTableByImmutableId(tableId)).getRowLastModifiedTimeMillsecs(rowId);
		}
		return 0;
	}

	@Override
	public long[] find(int tableId, int col, Object value) {
		TLongArrayList ret = new TLongArrayList();
		
		TLongHashSet hashset = new TLongHashSet();
		for(int dsIndex = 0; dsIndex<length;dsIndex++){
			ODLTableReadOnly table = (ODLTableReadOnly)stores.get(dsIndex).getTableByImmutableId(tableId);
			if(table!=null){
				long[] result = table.find(col, value);
				int n = result.length;
				for(int i=0;i<n;i++){
					long id = result[i];
					if(hashset.contains(id)==false){
						hashset.add(id);
						ret.add(id);
					}
				}
			}
		}
		
		return ret.toArray();
	}
	
	@Override
	public ODLTableReadOnly query(int tableId, TableQuery query) {
		
		// Create empty return table
		ODLApiImpl api = new ODLApiImpl();
		Tables tables = api.tables();
		ODLDatastoreAlterable<? extends ODLTableAlterable > ds = tables.createAlterableDs();
		ODLTableDefinition outDfn = getTableByImmutableId(tableId);		
		ODLTableAlterable ret=(ODLTableAlterable)tables.copyTableDefinition(outDfn, ds);
		
		// Fill with results
		for(int dsIndex = 0; dsIndex<length;dsIndex++){
			ODLTableReadOnly srcTable = (ODLTableReadOnly)stores.get(dsIndex).getTableByImmutableId(tableId);
			if(srcTable!=null){
				ODLTableReadOnly queryResult = srcTable.query(query);
				if(queryResult!=null){
					int n = queryResult.getRowCount();
					for(int i =0 ; i < n ; i++){
						tables.copyRow(queryResult, i, ret);
					}
				}
			}
		}
		return ret;
	}



}
