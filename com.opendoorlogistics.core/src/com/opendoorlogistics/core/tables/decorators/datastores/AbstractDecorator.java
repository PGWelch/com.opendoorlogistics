/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import gnu.trove.map.hash.TIntObjectHashMap;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableQuery;
import com.opendoorlogistics.core.tables.utils.TableUtils;

/**
 * A decorator which defines a table decorator where all calls to table methods are
 * redirected to the datastore decorator instance. This class is the base class for many others.
 * @author Phil
 *
 * @param <T>
 */
public abstract class AbstractDecorator<T extends ODLTableDefinition> implements ODLDatastoreAlterable<T> {
	private final TIntObjectHashMap<TableDecorator> tableDecorators = new TIntObjectHashMap<>();

	protected class TableDecorator implements ODLTableAlterable{
		private final int tableId;
		
		TableDecorator(int id) {
			super();
			this.tableId = id;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			AbstractDecorator.this.setValueAt(tableId, aValue, rowIndex, columnIndex);
		}

		@Override
		public void setValueById(Object aValue, long rowid, int columnIndex) {
			AbstractDecorator.this.setValueById(tableId, aValue, rowid, columnIndex);
		}
		
		@Override
		public int createEmptyRow(long rowLocalId) {
			return AbstractDecorator.this.createEmptyRow(tableId,rowLocalId);
		}

		@Override
		public void insertEmptyRow(int insertAtRowNb, long rowId) {
			AbstractDecorator.this.insertEmptyRow(tableId, insertAtRowNb,rowId);
		}

		@Override
		public void deleteRow(int rowNumber) {
			AbstractDecorator.this.deleteRow(tableId, rowNumber);			
		}

		@Override
		public int getRowCount() {
			return AbstractDecorator.this.getRowCount(tableId);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return AbstractDecorator.this.getValueAt(tableId, rowIndex, columnIndex);
		}

		@Override
		public ODLColumnType getColumnType(int i) {
			return AbstractDecorator.this.getColumnFieldType(tableId, i);
		}

		@Override
		public String getColumnName(int i) {
			return AbstractDecorator.this.getColumnName(tableId, i);
		}

		@Override
		public int getColumnCount() {
			return AbstractDecorator.this.getColumnCount(tableId);
		}

		@Override
		public String getName() {
			return AbstractDecorator.this.getName(tableId);
		}

		@Override
		public long getFlags() {
			return AbstractDecorator.this.getFlags(tableId);
		}

		@Override
		public long getColumnFlags(int i) {
			return AbstractDecorator.this.getColumnFlags(tableId, i);
		}

		@Override
		public int addColumn(int id,String name, ODLColumnType type, long flags) {
			return AbstractDecorator.this.addColumn(tableId,id, name, type, flags);
		}

		@Override
		public void setFlags(long flags) {
			AbstractDecorator.this.setFlags(tableId, flags);			
		}

		@Override
		public void setColumnFlags(int col, long flags) {
			AbstractDecorator.this.setColumnFlags(tableId, col, flags);			
		}

		@Override
		public int getImmutableId() {
			return tableId;
		}
		
		@Override
		public String toString(){
			return TableUtils.convertToString(this);
		}

		@Override
		public void deleteColumn(int col) {
			AbstractDecorator.this.deleteCol(tableId, col);
		}
		
		@Override
		public boolean insertColumn(int colId,int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
			return AbstractDecorator.this.insertCol(tableId,colId, col, name,type,flags,allowDuplicateNames);
		}


//		@Override
//		public int getRowIndexByGlobalId(long immutableId) {
//			return AbstractDecorator.this.getRowIndexByGlobalId(tableId, immutableId);
//		}

		@Override
		public long getRowId(int rowIndex) {
			return AbstractDecorator.this.getRowGlobalId(tableId, rowIndex);
		}

//		@Override
//		public int getRowIndexByLocalId(int localId) {
//			return AbstractDecorator.this.getRowIndexByLocalId(tableId, localId);
//		}

		@Override
		public String getColumnDescription(int col) {
			return AbstractDecorator.this.getColumnDescription(tableId, col);
		}

		@Override
		public void setColumnDescription(int col,String description) {
			AbstractDecorator.this.setColumnDescription(tableId, col, description);
		}

		@Override
		public java.util.Set<String> getColumnTags(int col) {
			return AbstractDecorator.this.getColumnTags(tableId, col);
		}

		@Override
		public java.util.Set<String> getTags() {
			return AbstractDecorator.this.getTags(tableId);
		}

		@Override
		public void setTags(java.util.Set<String> tags) {
			AbstractDecorator.this.setTags(tableId, tags);
		}

		@Override
		public void setColumnTags(int col, java.util.Set<String> tags) {
			AbstractDecorator.this.setColumnTags(tableId, col, tags);
		}

		@Override
		public Object getColumnDefaultValue(int col) {
			return AbstractDecorator.this.getColumnDefaultValue(tableId,col);
		}

		@Override
		public void setColumnDefaultValue(int col, Object value) {
			AbstractDecorator.this.setColumnDefaultValue(tableId, col, value);
		}

		@Override
		public Object getValueById(long rowId, int columnIndex) {
			return AbstractDecorator.this.getValueById(tableId, rowId, columnIndex);
		}

		@Override
		public boolean containsRowId(long rowId) {
			return AbstractDecorator.this.containsRowId(tableId, rowId);
		}


		@Override
		public int getColumnImmutableId(int col) {
			return AbstractDecorator.this.getColumnImmutableId(tableId, col);
		}

		@Override
		public long[] find(int col, Object value) {
			return AbstractDecorator.this.find(tableId, col, value);
		}

		@Override
		public long getRowFlags(long rowId) {
			return AbstractDecorator.this.getRowFlags(tableId, rowId);
		}

		@Override
		public void setRowFlags(long flags, long rowId) {
			AbstractDecorator.this.setRowFlags(tableId, flags, rowId);
		}

		@Override
		public ODLTableDefinition deepCopyWithShallowValueCopy() {
			return AbstractDecorator.this.deepCopyWithShallowValueCopy(tableId);
		}

		@Override
		public long getRowLastModifiedTimeMillsecs(long rowId) {
			return AbstractDecorator.this.getRowLastModifiedTimeMillisecs(tableId, rowId);
		}

		@Override
		public ODLTableReadOnly query(TableQuery query) {
			return AbstractDecorator.this.query(tableId, query);
		}



	}

	@SuppressWarnings("unchecked")
	@Override
	public T getTableByImmutableId(int tableId) {
		if(getTableExists(tableId)){
			// get table decorator from cache
			T ret = (T)tableDecorators.get(tableId);
			
			// create one if needed
			if(ret == null){
				TableDecorator td = new TableDecorator(tableId);
				tableDecorators.put(tableId, td);
				ret = (T)td;
			}
			
			return ret;				
		}
		return null;
	}

	
	protected abstract boolean getTableExists(int tableId);

	protected abstract int getRowCount(int tableId) ;

	protected abstract long[] find(int tableId,int col, Object value);

	protected abstract Object getValueAt(int tableId,int rowIndex, int columnIndex);
	
	protected abstract Object getValueById(int tableId,long rowId, int columnIndex);

	protected abstract ODLColumnType getColumnFieldType(int tableId,int col) ;

	protected abstract String getColumnName(int tableId,int col);

	protected abstract Object getColumnDefaultValue(int tableId,int col);

	protected abstract int getColumnCount(int tableId);
	
	protected abstract String getName(int tableId);
	
	protected abstract ODLTableReadOnly query(int tableId, TableQuery query);

	protected abstract long getFlags(int tableId);

	protected abstract void setRowFlags(int tableId,long flags, long rowId);

	protected abstract long getColumnFlags(int tableId,int col);

	protected abstract int getColumnImmutableId(int tableId,int col);

	protected abstract boolean containsRowId(int tableId,long rowId);
	
	protected abstract java.util.Set<String> getColumnTags(int tableId,int col);

	protected abstract java.util.Set<String> getTags(int tableId);

	protected abstract String getColumnDescription(int tableId,int col);
	
	protected abstract void setValueAt(int tableId,Object aValue, int rowIndex, int columnIndex);

	protected abstract void setValueById(int tableId,Object aValue, long rowId, int columnIndex);
		
	protected abstract int createEmptyRow(int tableId,long rowId);
	
	protected abstract void insertEmptyRow(int tableId,int insertAtRowNb, long rowId);
	
	protected abstract void deleteRow(int tableId,int rowNumber);

	protected abstract ODLTableDefinition deepCopyWithShallowValueCopy(int tableId);

	protected abstract void deleteCol(int tableId,int col);
	
	protected abstract boolean insertCol(int tableId,int id, int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames);
	
	protected abstract int addColumn(int tableId,int columnid, String name, ODLColumnType type, long flags);
	
	protected abstract void setFlags(int tableId,long flags);
	
	protected abstract void setColumnFlags(int tableId,int col, long flags);

	protected abstract void setColumnDefaultValue(int tableId,int col, Object value);

	protected abstract void setColumnTags(int tableId,int col, java.util.Set<String> tags);

	protected abstract void setTags(int tableId, java.util.Set<String> tags);
	
	protected abstract void setColumnDescription(int tableId,int col, String description);

	protected abstract long getRowGlobalId(int tableId,int rowIndex);

	protected abstract long getRowFlags(int tableId,long rowId);

	protected abstract long getRowLastModifiedTimeMillisecs(int tableId,long rowId);

	//protected abstract long getRowGlobalIdByLocal(int tableId,int rowId);
	
	//protected abstract int getRowIndexByGlobalId(int tableId,long immutableId);
	
	@Override
	public String toString(){
		return TableUtils.convertToString(this);
	}
}
