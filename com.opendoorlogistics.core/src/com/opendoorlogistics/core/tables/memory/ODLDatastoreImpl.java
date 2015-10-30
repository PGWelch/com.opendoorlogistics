/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.memory;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.*;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.DeepCopyable;
import com.opendoorlogistics.core.utils.IntIDGenerator;
import com.opendoorlogistics.core.utils.IntIDGenerator.IsExistingId;

final public class ODLDatastoreImpl <T extends ODLTableDefinition> implements ODLDatastoreAlterable<T>, DeepCopyable<ODLDatastoreImpl<T>>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4261272361071414989L;
	private final ArrayList<T> tablesByIndx = new ArrayList<>();
	private final TIntObjectHashMap<T> tablesById = new TIntObjectHashMap<>();
	private final ODLTableFactory<T> factory;
	private long flags;
	protected final IntIDGenerator tableIdGenerator = new IntIDGenerator(new IsExistingId() {
		
		@Override
		public boolean isExistingId(int id) {
			return tablesById.containsKey(id);
		}
	});
	
	public ODLDatastoreImpl(ODLTableFactory<T> factory) {
		this.factory = factory;
		this.flags |= TableFlags.UI_EDIT_PERMISSION_FLAGS;
	}

	@SuppressWarnings("unchecked")
	protected ODLDatastoreImpl(ODLDatastoreImpl<T> copyThis){
		this.factory = copyThis.factory;
		this.flags = copyThis.flags;
		for(T table : copyThis.tablesByIndx){
			if(ODLTableImpl.class.isInstance(table)==false){
				throw new UnsupportedOperationException();
			}
			addTable((T)table.deepCopyWithShallowValueCopy());
		}
		this.tableIdGenerator.setNextId(copyThis.tableIdGenerator.getNextId());
	}
	
	@Override
	public long getFlags() {
		return flags;
	}

	@Override
	public void setFlags(long flags) {
		this.flags = flags;
	}

	@Override
	public int getTableCount() {
		return tablesByIndx.size();
	}

	@Override
	public T getTableAt(int i) {
		return tablesByIndx.get(i);
	}

	/**
	 * Add the table returning its index or -1 if rejected
	 * @param table
	 * @param logger
	 * @return
	 */
	public int addTable(T table){
		if(TableUtils.findTableIndex(this, table.getName(), true)!=-1){
			throw new RuntimeException("Table already exists: " + table.getName());
		}
		
		if(table.getImmutableId()==-1){
			throw new RuntimeException("Invalid table immutable id in table: " + table.getName());
		}
		
		// we throw an exception if the id is already used because this will 
		// be caused by a code error rather than a user error
		if(tablesById.get(table.getImmutableId())!=null){
			throw new RuntimeException("Duplicate table id");
		}
		
		tablesByIndx.add(table);
		tablesById.put(table.getImmutableId(), table);
		return tablesByIndx.size()-1;
	}
	
	@SuppressWarnings("unchecked")
	public boolean addTables(ODLDatastore<? extends ODLTableDefinition> database){
		for(int i =0 ; i < database.getTableCount() ; i++){
			if(addTable((T)database.getTableAt(i))==-1){
				return false;
			}
		}
		return true;
	}
	
	@Override
	public T createTable(String tablename, int id) {
		// assign id
		if(id==-1){
			id = tableIdGenerator.generateId();
		}
		
		T table = factory.create(this,tablename, id);
		
		// give table default permissions
		grantDefaultTablePermissions(this,table);
		
		if( addTable(table)!=-1){
			return table;
		}
		return null;
	}


	public static void grantDefaultTablePermissions(ODLDatastore<? extends ODLTableDefinition> ds,ODLTableDefinition table) {
		if(ODLTableDefinitionAlterable.class.isInstance(table)){
			ODLTableDefinitionAlterable dfn= (ODLTableDefinitionAlterable)table;
			
			// remove default permission flags
			long flags = dfn.getFlags() & (~TableFlags.UI_EDIT_PERMISSION_FLAGS);
			
			// re-add the datastore ones
			flags |= (TableFlags.UI_EDIT_PERMISSION_FLAGS & ds.getFlags());
			dfn.setFlags(flags);
		}
	}
	
	@Override
	public String toString(){
		return TableUtils.convertToString(this);
	}

	@Override
	public void addListener(ODLListener tml, int ...tableIds) {
		//throwListenersUnsupportedException();
	}

	@Override
	public void removeListener( ODLListener tml) {
	//	throwListenersUnsupportedException();		
	}


	@Override
	public T getTableByImmutableId(int id) {
		return tablesById.get(id);
	}

	public static final ODLDatastoreAlterableFactory<ODLTableAlterable> alterableFactory = new ODLDatastoreAlterableFactory<ODLTableAlterable>() {
		
		@Override
		public ODLDatastoreAlterable<ODLTableAlterable> create() {
			return new ODLDatastoreImpl<>(ODLTableImpl.ODLTableAlterableFactory);
		}
	};

	@Override
	public void disableListeners() {
		//throwListenersUnsupportedException();		
	}

	@Override
	public void enableListeners() {
		//throwListenersUnsupportedException();		
	}

	@Override
	public void deleteTableById(int tableId) {
		T table = tablesById.get(tableId);
		if(table!=null){
			tablesByIndx.remove(table);
			tablesById.remove(tableId);
		}
	}

	@Override
	public void startTransaction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endTransaction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isInTransaction() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setTableName(int tableId, String newName) {
		ODLTableDefinition dfn = TableUtils.findTable(this, newName, true);
		if(dfn!=null && dfn.getImmutableId()!=tableId){
			return false;
		}
		ODLTableDefinitionImpl table =(ODLTableDefinitionImpl) tablesById.get(tableId);
		if(table!=null){
			table.setName(newName);
		}
		return true;
	}

	@Override
	public ODLDatastoreImpl<T> deepCopy() {
		return new ODLDatastoreImpl<>(this);
	}

	@Override
	public ODLDatastoreAlterable<T> deepCopyWithShallowValueCopy(boolean lazyCopy) {
		return deepCopy();
	}

	@Override
	public void rollbackTransaction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRollbackSupported() {
		// TODO Auto-generated method stub
		return false;
	}


}
