/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores.deepcopying;

import java.util.ArrayList;
import java.util.Iterator;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.decorators.datastores.SimpleAbstractDecorator;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.memory.ODLTableDefinitionImpl;
import com.opendoorlogistics.core.tables.memory.ODLTableImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.IntIDGenerator;
import com.opendoorlogistics.core.utils.IntIDGenerator.IsExistingId;
import com.opendoorlogistics.core.utils.strings.Strings;

class OptimisedDeepCopy<T extends ODLTableDefinition> extends SimpleAbstractDecorator<T>{
	private class TableHolder{
		T table;
		boolean isImmutable;
	}
		
	private final ArrayList<TableHolder> tables = new ArrayList<>();
	private long flags;

	OptimisedDeepCopy(long flags) {
		this.flags = flags;
	}
	
	void addImmutableTable(T table){
		TableHolder h = new TableHolder();
		h.isImmutable = true;
		h.table = table;
		tables.add(h);
	}

	
	protected final IntIDGenerator tableIdGenerator = new IntIDGenerator(new IsExistingId() {
		
		@Override
		public boolean isExistingId(int id) {
			return getTableHolder(id,false)!=null;
		}
	});
	
	@SuppressWarnings("unchecked")
	private synchronized TableHolder getTableHolder(int id, boolean makeWritable){
		int n = tables.size();
		for(int i =0 ; i< n ; i++){
			TableHolder h = tables.get(i);
			
			// only take a deep copy when we need to
			// TO DO .. .this looks like we could optimise this by placing this within the return,
			// but we need to check for any adverse effects...
			if(makeWritable && h.isImmutable){
				h.table = (T)h.table.deepCopyWithShallowValueCopy();
				h.isImmutable = false;
			}
			
			if(h.table.getImmutableId()==id){
				return h;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T createTable(String tablename, int id) {
		if(TableUtils.findTableIndex(this, tablename, true)!=-1){
			throw new RuntimeException("Table already exists: " + tablename);
		}
		
		// get id
		if(id==-1){
			id = tableIdGenerator.generateId();
		}
		else if(getTableHolder(id, false)!=null){
			throw new RuntimeException("Duplicate table id");
		}
		
		TableHolder h = new TableHolder();
		h.isImmutable = false;
		h.table = (T) new ODLTableImpl(id, tablename);
		ODLDatastoreImpl.grantDefaultTablePermissions(this, h.table);
		tables.add(h);
		

		return getTableByImmutableId(id);
	}

	@Override
	public void deleteTableById(int tableId) {
		Iterator<TableHolder> it = tables.iterator();
		while(it.hasNext()){
			if(it.next().table.getImmutableId()==tableId){
				it.remove();
			}
		}
	}

	@Override
	public boolean setTableName(int tableId, String newName) {
	
		// check name not already used by another table
		for(TableHolder h : tables){
			if(h.table.getImmutableId()!=tableId && Strings.equalsStd(h.table.getName(), newName)){
				return false;
			}
		}

		TableHolder h = getTableHolder(tableId, true);
		if(h!=null && ODLTableDefinitionImpl.class.isInstance(h.table)){
			((ODLTableDefinitionImpl)h.table).setName(newName);
			return true;
		}

		return false;
	}

	@Override
	public void setFlags(long flags) {
		this.flags = flags;
	}

	@Override
	public int getTableCount() {
		return tables.size();
	}

	@Override
	public T getTableAt(int i) {
		return super.getTableByImmutableId(tables.get(i).table.getImmutableId());
	}

	@Override
	public void addListener(ODLListener tml, int... tableIds) {
		// TODO Auto-generated method stub
		
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
	public void rollbackTransaction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRollbackSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getFlags() {
		return flags;
	}

	@Override
	protected ODLTableReadOnly readOnlyTable(int tableId) {
		TableHolder h = getTableHolder(tableId,false);
		if(h!=null){
			return (ODLTableReadOnly)h.table;
		}
		return null;
	}

	@Override
	protected ODLTableDefinition definition(int tableId) {
		TableHolder h = getTableHolder(tableId,false);
		if(h!=null){
			return (ODLTableDefinition)h.table;
		}
		return null;
	}

	@Override
	protected ODLTable writable(int tableId) {
		TableHolder h = getTableHolder(tableId,true);
		if(h!=null){
			return (ODLTable)h.table;
		}
		return null;
	}

	@Override
	protected ODLTableAlterable alterable(int tableId) {
		TableHolder h = getTableHolder(tableId,true);
		if(h!=null){
			return (ODLTableAlterable)h.table;
		}
		return null;
	}

	@Override
	public boolean getTableExists(int tableId) {
		return getTableHolder(tableId, false)!=null;
	}

	@Override
	public ODLTableDefinition deepCopyWithShallowValueCopy(int tableId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ODLDatastoreAlterable<? extends T> deepCopyWithShallowValueCopy(boolean createLazyCopy) {
		throw new UnsupportedOperationException();
	}



}