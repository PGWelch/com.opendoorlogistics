/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores.deepcopying;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.lang.ref.SoftReference;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.decorators.datastores.SimpleAbstractDecorator;

public class OptimisedDeepCopierDecorator<T extends ODLTableDefinition> extends SimpleAbstractDecorator<T>{
	private final ODLDatastoreAlterable<? extends T> decorated;
	private final TIntObjectHashMap<SoftReference<T>> deepCopies = new TIntObjectHashMap<>();
	
	public OptimisedDeepCopierDecorator(ODLDatastoreAlterable<? extends T> decorated) {
		this.decorated = decorated;
	}

	@Override
	public T createTable(String tablename, int id) {
		T table = decorated.createTable(tablename, id);
		if(table!=null){
			return getTableByImmutableId(table.getImmutableId());
		}
		return null;
	}

	@Override
	public void deleteTableById(int tableId) {
		deepCopies.remove(tableId);
		decorated.deleteTableById(tableId);
	}

	@Override
	public boolean setTableName(int tableId, String newName) {	
		deepCopies.remove(tableId);		
		return decorated.setTableName(tableId, newName);
	}

	@Override
	public void setFlags(long flags) {
		decorated.setFlags(flags);
	}

	@Override
	public synchronized ODLDatastoreAlterable<? extends T> deepCopyWithShallowValueCopy(boolean lazyCopy) {
		if(!lazyCopy){
			return decorated.deepCopyWithShallowValueCopy(lazyCopy);
		}
		
		OptimisedDeepCopy<T> ret = new OptimisedDeepCopy<>(getFlags());
		for(int i =0 ; i< decorated.getTableCount(); i++){
			T table = decorated.getTableAt(i);
			int id = table.getImmutableId();
			SoftReference<T> refCopy = deepCopies.get(id);
			if(refCopy!=null){
				T copy = refCopy.get();
				if(copy!=null){
					ret.addImmutableTable(copy);
					continue;
				}
			}
			
			// take new deep copy and save it for future use
			@SuppressWarnings("unchecked")
			T copy = (T)table.deepCopyWithShallowValueCopy();
			deepCopies.put(id, new SoftReference<T>(copy));
			ret.addImmutableTable(copy);
		}
		return ret;
	}

	@Override
	public int getTableCount() {
		return decorated.getTableCount();
	}

	@Override
	public T getTableAt(int i) {
		if(i<decorated.getTableCount()){
			int id = decorated.getTableAt(i).getImmutableId();
			
			// return the table decoratoe
			return super.getTableByImmutableId(id);
		}
		return null;
	}

	@Override
	public void addListener(ODLListener tml, int... tableIds) {
		decorated.addListener(tml, tableIds);
	}

	@Override
	public void removeListener(ODLListener tml) {
		decorated.removeListener(tml);
	}

	@Override
	public void disableListeners() {
		decorated.disableListeners();
	}

	@Override
	public void enableListeners() {
		decorated.enableListeners();
	}

	@Override
	public void startTransaction() {
		decorated.startTransaction();
	}

	@Override
	public void endTransaction() {
		decorated.endTransaction();
	}

	@Override
	public boolean isInTransaction() {
		return decorated.isInTransaction();
	}

	@Override
	public void rollbackTransaction() {
		decorated.rollbackTransaction();
	}

	@Override
	public boolean isRollbackSupported() {
		return decorated.isRollbackSupported();
	}

	@Override
	public long getFlags() {
		return decorated.getFlags();
	}

	@Override
	protected ODLTableReadOnly readOnlyTable(int tableId) {
		return (ODLTableReadOnly)decorated.getTableByImmutableId(tableId);
	}

	@Override
	protected ODLTableDefinition definition(int tableId) {
		return decorated.getTableByImmutableId(tableId);
	}

	@Override
	protected synchronized ODLTable writable(int tableId) {
		deepCopies.remove(tableId);
		return (ODLTable) decorated.getTableByImmutableId(tableId);
	}

	@Override
	protected synchronized ODLTableAlterable alterable(int tableId) {
		deepCopies.remove(tableId);
		return (ODLTableAlterable) decorated.getTableByImmutableId(tableId);
	}

	@Override
	public boolean getTableExists(int tableId) {
		return decorated.getTableByImmutableId(tableId)!=null;
	}

	@Override
	public ODLTableDefinition deepCopyWithShallowValueCopy(int tableId) {
		ODLTableDefinition t = decorated.getTableByImmutableId(tableId);
		if(t!=null){
			return t.deepCopyWithShallowValueCopy();
		}
		return null;
	}
	
}