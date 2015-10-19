/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.decorators.tables.FlatDs2TableObject;

/**
 * Base class for other decorators. Also allows replacing of the decorated datastore
 * and disabling of writes by subclassing and overriding the isWriteAllowed mehtod.
 * @author Phil
 *
 * @param <T>
 */
public class SimpleDecorator<T extends ODLTableDefinition> extends SimpleAbstractDecorator<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7411673940523284819L;
	protected ODLDatastore<? extends T> decorated;
	protected final Class<T> tableClass;
	
	/**
	 * The constructor specifies you can only decorate databases with tables that
	 * are superclasses of ODLTableAlterable
	 * @param decorated
	 */
	public SimpleDecorator(Class<T> tableClass, ODLDatastore<? extends T> decorated) {
		this.decorated = decorated;
		this.tableClass = tableClass;

		if(tableClass.isAssignableFrom(ODLTableAlterable.class)==false){
			throw new RuntimeException("Can only use this generic with " + ODLTableAlterable.class.getSimpleName() 
				+ " or one of its superclasses");
		}
	}

	/**
	 * Override this in subclasses to disable writing when needed
	 * @return
	 */
	protected boolean isWriteAllowed(){
		return true;
	}
	
	@Override
	public String toString(){
		return decorated.toString();
	}
	
	@Override
	public int getTableCount() {
		return decorated.getTableCount();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getTableAt(int tableIndex) {
		T ret =null;
		if(tableIndex < getTableCount()){
			int id = decorated.getTableAt(tableIndex).getImmutableId();
			ret= (T)new FlatDs2TableObject(this,id);
		}
		return ret;
	}
	

	protected ODLTableReadOnly readOnlyTable(int tableId) {
		return (ODLTableReadOnly)decorated.getTableByImmutableId(tableId);
	}

	protected ODLTableDefinition definition(int tableId) {
		return (ODLTableDefinition)decorated.getTableByImmutableId(tableId);
	}
	
	protected ODLTable writable(int tableId) {
		if(isWriteAllowed()){
			return (ODLTable)decorated.getTableByImmutableId(tableId);			
		}
		return null;
	}
	

	protected ODLTableAlterable alterable(int tableId) {
		if(isWriteAllowed()){
			return (ODLTableAlterable)decorated.getTableByImmutableId(tableId);			
		}
		return null;
	}

	@Override
	public void addListener(ODLListener tml, int ...tableIds) {
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
	public T createTable(String tablename, int tableId) {
		if(isWriteAllowed()){
			ODLTableDefinition dfn =  ((ODLDatastoreAlterable<? extends T>)decorated).createTable(tablename,tableId);
			if(dfn!=null){
				return getTableByImmutableId(dfn.getImmutableId());
			}
		}
		return null;
	}


	@Override
	public void deleteTableById(int tableId) {
		if(isWriteAllowed()){
			((ODLDatastoreAlterable<? extends T>)decorated).deleteTableById(tableId);
		}		
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
	public boolean setTableName(int tableId, String newName) {
		if(isWriteAllowed()){
			return ((ODLDatastoreAlterable<? extends T>)decorated).setTableName(tableId, newName);
		}
		return false;
	}


	@Override
	public long getFlags() {
		return decorated.getFlags();
	}


	@Override
	public void setFlags(long flags) {
		if(isWriteAllowed()){
			decorated.setFlags(flags);
		}			
	}

	@Override
	public ODLDatastoreAlterable<T> deepCopyWithShallowValueCopy(boolean lazyCopy) {
		return (ODLDatastoreAlterable<T>)decorated.deepCopyWithShallowValueCopy(lazyCopy);
	}

	@Override
	public void rollbackTransaction() {
		decorated.rollbackTransaction();
	}

	@Override
	public boolean isRollbackSupported() {
		return decorated.isRollbackSupported();
	}


	public void replaceDecorated(ODLDatastore<? extends T> decorated){
		this.decorated =decorated;
	}

	@Override
	public boolean getTableExists(int tableId) {
		return decorated.getTableByImmutableId(tableId)!=null;
	}

	@Override
	public ODLTableDefinition deepCopyWithShallowValueCopy(int tableId) {
		ODLTableDefinition t = readOnlyTable(tableId);
		if(t!=null){
			return t.deepCopyWithShallowValueCopy();
		}
		return null;
	}
}
