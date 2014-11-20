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
	protected int addColumn(int tableId,int id, String name, ODLColumnType type, long flags) {
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
	protected int createEmptyRow(int tableId, long rowLocalId) {
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
	protected void deleteCol(int tableId, int col) {
		getDependencies().addWrittenTableId(tableId);
		super.deleteCol(tableId, col);
	}

	@Override
	protected void deleteRow(int tableId, int rowNumber) {
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
	protected int getColumnCount(int tableId) {
		getDependencies().addReadTableId(tableId,false);
		return super.getColumnCount(tableId);
	}

	@Override
	protected ODLColumnType getColumnFieldType(int tableId, int colIndex) {
		getDependencies().addReadTableId(tableId,false);
		return super.getColumnFieldType(tableId, colIndex);
	}

	@Override
	protected long getColumnFlags(int tableId, int colIndx) {
		getDependencies().addReadTableId(tableId,false);
		return super.getColumnFlags(tableId, colIndx);
	}
	
	@Override
	protected int getColumnImmutableId(int tableId, int col) {
		getDependencies().addReadTableId(tableId,false);
		return super.getColumnImmutableId(tableId, col);
	}

	@Override
	protected String getColumnName(int tableId, int colIndex) {
		getDependencies().addReadTableId(tableId,false);
		return super.getColumnName(tableId, colIndex);
	}

	@Override
	public long getFlags() {
		getDependencies().setReadTableSet();
		return super.getFlags();
	}

	@Override
	protected long getFlags(int tableId) {
		getDependencies().addReadTableId(tableId,false);
		return super.getFlags(tableId);
	}

	@Override
	protected String getName(int tableId) {
		// Table names are considered a table set property, not an individual table's property.
		// This allows the dependencies for adapters to only inculde the tables they use.
		getDependencies().setReadTableSet();
		return super.getName(tableId);
	}

	@Override
	protected int getRowCount(int tableId) {
		getDependencies().addReadTableId(tableId,true);
		return super.getRowCount(tableId);
	}

	@Override
	protected long getRowGlobalId(int tableId, int rowIndex) {
		getDependencies().addReadTableId(tableId,true);
		return super.getRowGlobalId(tableId, rowIndex);
	}

	@Override
	protected long getRowFlags(int tableId, long rowId) {
		getDependencies().addReadTableId(tableId,true);
		getDependencies().setReadRowFlags(true);
		return super.getRowFlags(tableId, rowId);
	}

	@Override
	protected long getRowLastModifiedTimeMillisecs(int tableId, long rowId) {
		getDependencies().addReadTableId(tableId,true);
		return super.getRowLastModifiedTimeMillisecs(tableId, rowId);
		
	}
	
//	@Override
//	protected int getRowIndexByGlobalId(int tableId, long immutableId) {
//		getDependencies().addReadTableId(tableId);
//		return super.getRowIndexByGlobalId(tableId, immutableId);
//	}

//	@Override
//	protected int getRowIndexByLocalId(int tableId, int localId) {
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
	protected Object getValueAt(int tableId, int rowIndex, int columnIndex) {
		getDependencies().addReadTableId(tableId,true);
		return super.getValueAt(tableId, rowIndex, columnIndex);
	}

	@Override
	protected long[] find(int tableId, int col, Object value) {
		getDependencies().addReadTableId(tableId,true);
		return super.find(tableId, col, value);
	}

	@Override
	protected boolean containsRowId(int tableId, long rowId) {
		getDependencies().addReadTableId(tableId,true);
		return super.containsRowId(tableId, rowId);
	}
	
	@Override
	protected Object getValueById(int tableId, long rowId, int columnIndex) {
		getDependencies().addReadTableId(tableId,true);
		return super.getValueById(tableId, rowId, columnIndex);
	}
	
	@Override
	protected boolean insertCol(int tableId, int id,int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		getDependencies().addWrittenTableId(tableId);
		return super.insertCol(tableId,id, col, name, type, flags, allowDuplicateNames);
	}

	@Override
	protected void insertEmptyRow(int tableId, int insertAtRowNb, long rowId) {
		getDependencies().addWrittenTableId(tableId);
		super.insertEmptyRow(tableId, insertAtRowNb, rowId);
	}


	@Override
	public void removeListener(ODLListener tml) {
		super.removeListener(tml);
	}

	@Override
	protected void setColumnFlags(int tableId, int col, long flags) {
		getDependencies().addWrittenTableId(tableId);
		super.setColumnFlags(tableId, col, flags);
	}

	@Override
	protected void setFlags(int tableId, long flags) {
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
	protected void setValueAt(int tableId, Object aValue, int rowIndex, int columnIndex) {
		getDependencies().addWrittenTableId(tableId);
		super.setValueAt(tableId, aValue, rowIndex, columnIndex);
	}

	@Override
	protected void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {
		getDependencies().addWrittenTableId(tableId);
		super.setValueById(tableId, aValue, rowId, columnIndex);
	}


	

}
