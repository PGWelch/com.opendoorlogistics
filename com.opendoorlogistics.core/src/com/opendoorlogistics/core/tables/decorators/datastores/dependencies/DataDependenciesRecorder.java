/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores.dependencies;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.tables.decorators.datastores.SimpleDecorator;

/**
 * This decorator records which tables have been read and written to,
 * thereby recording the dependencies of the client code on the datastore.
 * @author Phil
 *
 */
final public class DataDependenciesRecorder<T extends ODLTableDefinition> extends SimpleDecorator<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2607725279850643297L;
	
	//private final DataDependencies dependencies;
	private final DataDependencies dependencies;

	public DataDependenciesRecorder(Class<T> tableClass, ODLDatastore<? extends T> decorated, DataDependencies dependencies) {
		super(tableClass, decorated);
		this.dependencies = dependencies;	
	}

	public DataDependenciesRecorder(Class<T> tableClass, ODLDatastore<? extends T> decorated) {
		this(tableClass, decorated, new DataDependencies());
	}
	
	public DataDependencies getDependencies() {
		return dependencies;
	}

	@Override
	public int addColumn(int tableId,int id, String name, ODLColumnType type, long flags) {
		getDependencies().addWrittenTableId(tableId);
		return super.addColumn(tableId, id,name, type, flags);
	}

	@Override
	public void addListener(ODLListener tml, int... tableIds) {
		for(int tableId : tableIds){
			getDependencies().addReadTableId(tableId,false);
		}
		super.addListener(tml, tableIds);
	}

	
	@Override
	public int createEmptyRow(int tableId, long rowLocalId) {
		getDependencies().addWrittenTableId(tableId);
		return super.createEmptyRow(tableId, rowLocalId);
	}

	@Override
	public T createTable(String tablename, int tableId) {
		getDependencies().setWrittenTableSet();
		if(tableId!=-1){
			getDependencies().addWrittenTableId(tableId);			
		}
		return super.createTable(tablename, tableId);
	}

	@Override
	public void deleteCol(int tableId, int col) {
		getDependencies().addWrittenTableId(tableId);
		super.deleteCol(tableId, col);
	}

	@Override
	public void deleteRow(int tableId, int rowNumber) {
		getDependencies().addWrittenTableId(tableId);
		super.deleteRow(tableId, rowNumber);
	}

	@Override
	public void deleteTableById(int tableId) {
		getDependencies().setWrittenTableSet();
		getDependencies().addWrittenTableId(tableId);
		super.deleteTableById(tableId);
	}

	@Override
	public void disableListeners() {
		super.disableListeners();
	}

	@Override
	public void enableListeners() {
		super.enableListeners();
	}

	
	@Override
	public int getColumnCount(int tableId) {
		getDependencies().addReadTableId(tableId,false);
		return super.getColumnCount(tableId);
	}

	@Override
	public ODLColumnType getColumnFieldType(int tableId, int colIndex) {
		getDependencies().addReadTableId(tableId,false);
		return super.getColumnFieldType(tableId, colIndex);
	}

	@Override
	public long getColumnFlags(int tableId, int colIndx) {
		getDependencies().addReadTableId(tableId,false);
		return super.getColumnFlags(tableId, colIndx);
	}
	
	@Override
	public int getColumnImmutableId(int tableId, int col) {
		getDependencies().addReadTableId(tableId,false);
		return super.getColumnImmutableId(tableId, col);
	}

	@Override
	public String getColumnName(int tableId, int colIndex) {
		getDependencies().addReadTableId(tableId,false);
		return super.getColumnName(tableId, colIndex);
	}

	@Override
	public long getFlags() {
		getDependencies().setReadTableSet();
		return super.getFlags();
	}

	@Override
	public long getFlags(int tableId) {
		getDependencies().addReadTableId(tableId,false);
		return super.getFlags(tableId);
	}

	@Override
	public String getName(int tableId) {
		// Table names are considered a table set property, not an individual table's property.
		// This allows the dependencies for adapters to only inculde the tables they use.
		getDependencies().setReadTableSet();
		return super.getName(tableId);
	}

	@Override
	public int getRowCount(int tableId) {
		getDependencies().addReadTableId(tableId,true);
		return super.getRowCount(tableId);
	}

	@Override
	public long getRowGlobalId(int tableId, int rowIndex) {
		getDependencies().addReadTableId(tableId,true);
		return super.getRowGlobalId(tableId, rowIndex);
	}

	@Override
	public long getRowFlags(int tableId, long rowId) {
		getDependencies().addReadTableId(tableId,true);
		getDependencies().setReadRowFlags(true);
		return super.getRowFlags(tableId, rowId);
	}

	@Override
	public long getRowLastModifiedTimeMillisecs(int tableId, long rowId) {
		getDependencies().addReadTableId(tableId,true);
		return super.getRowLastModifiedTimeMillisecs(tableId, rowId);
		
	}
	
//	@Override
//	public int getRowIndexByGlobalId(int tableId, long immutableId) {
//		getDependencies().addReadTableId(tableId);
//		return super.getRowIndexByGlobalId(tableId, immutableId);
//	}

//	@Override
//	public int getRowIndexByLocalId(int tableId, int localId) {
//		getDependencies().addReadTableId(tableId);
//		return super.getRowIndexByLocalId(tableId, localId);
//	}

	@Override
	public T getTableAt(int tableIndex) {
		getDependencies().setReadTableSet();
		return super.getTableAt(tableIndex);
	}

	@Override
	public T getTableByImmutableId(int tableId) {
		getDependencies().setReadTableSet();
		return super.getTableByImmutableId(tableId);
	}

	@Override
	public int getTableCount() {
		getDependencies().setReadTableSet();
		return super.getTableCount();
	}

	@Override
	public Object getValueAt(int tableId, int rowIndex, int columnIndex) {
		getDependencies().addReadTableId(tableId,true);
		return super.getValueAt(tableId, rowIndex, columnIndex);
	}

	@Override
	public long[] find(int tableId, int col, Object value) {
		getDependencies().addReadTableId(tableId,true);
		return super.find(tableId, col, value);
	}

	@Override
	public boolean containsRowId(int tableId, long rowId) {
		getDependencies().addReadTableId(tableId,true);
		return super.containsRowId(tableId, rowId);
	}
	
	@Override
	public Object getValueById(int tableId, long rowId, int columnIndex) {
		getDependencies().addReadTableId(tableId,true);
		return super.getValueById(tableId, rowId, columnIndex);
	}
	
	@Override
	public boolean insertCol(int tableId, int id,int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		getDependencies().addWrittenTableId(tableId);
		return super.insertCol(tableId,id, col, name, type, flags, allowDuplicateNames);
	}

	@Override
	public void insertEmptyRow(int tableId, int insertAtRowNb, long rowId) {
		getDependencies().addWrittenTableId(tableId);
		super.insertEmptyRow(tableId, insertAtRowNb, rowId);
	}


	@Override
	public void removeListener(ODLListener tml) {
		super.removeListener(tml);
	}

	@Override
	public void setColumnFlags(int tableId, int col, long flags) {
		getDependencies().addWrittenTableId(tableId);
		super.setColumnFlags(tableId, col, flags);
	}

	@Override
	public void setFlags(int tableId, long flags) {
		getDependencies().addWrittenTableId(tableId);
		super.setFlags(tableId, flags);
	}

	@Override
	public void setFlags(long flags) {
		getDependencies().setWrittenTableSet();
		super.setFlags(flags);
	}

	@Override
	public boolean setTableName(int tableId, String newName) {
		// Table names are considered a table set property, not an individual table's property.
		// This allows the dependencies for adapters to only inculde the tables they use.
		getDependencies().setWrittenTableSet();
		return super.setTableName(tableId, newName);
	}

	@Override
	public void setValueAt(int tableId, Object aValue, int rowIndex, int columnIndex) {
		getDependencies().addWrittenTableId(tableId);
		super.setValueAt(tableId, aValue, rowIndex, columnIndex);
	}

	@Override
	public void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {
		getDependencies().addWrittenTableId(tableId);
		super.setValueById(tableId, aValue, rowId, columnIndex);
	}


	

}
