/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import java.util.Set;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

/**
 * Base class for other decorators. Also allows replacing of the decorated datastore
 * and disabling of writes by subclassing and overriding the isWriteAllowed mehtod.
 * @author Phil
 *
 * @param <T>
 */
public class SimpleDecorator<T extends ODLTableDefinition> extends AbstractDecorator<T> {
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
			ret= (T)new TableDecorator(id);
		}
		return ret;
	}
	
	@Override
	public T getTableByImmutableId(int tableId) {
		// check table still exists...
		if(decorated.getTableByImmutableId(tableId)!=null){
			return super.getTableByImmutableId(tableId);
		}
		return null;
	}

	@Override
	protected int getRowCount(int tableId) {
		return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getRowCount():0;
	}

	protected ODLTableReadOnly readOnlyTable(int tableId) {
		return (ODLTableReadOnly)decorated.getTableByImmutableId(tableId);
	}

	protected ODLTableDefinition definition(int tableId) {
		return (ODLTableDefinition)decorated.getTableByImmutableId(tableId);
	}
	
	@Override	
	protected Object getValueAt(int tableId,int rowIndex, int columnIndex) {
		return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getValueAt(rowIndex, columnIndex):null;
	}

	@Override
	protected Object getValueById(int tableId, long rowId, int columnIndex) {
		return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getValueById(rowId, columnIndex):null;
	}
	
	@Override
	protected ODLColumnType getColumnFieldType(int tableId,int colIndex) {
		return definition(tableId)!=null? definition(tableId).getColumnType(colIndex):null;
	}

	@Override
	protected String getColumnName(int tableId,int colIndex) {
		return definition(tableId)!=null? definition(tableId).getColumnName(colIndex): "";
	}

	@Override
	protected int getColumnCount(int tableId) {
		return definition(tableId)!=null?definition(tableId).getColumnCount():0;
	}

	@Override
	protected String getName(int tableId) {
		return definition(tableId)!=null? definition(tableId).getName():"";
	}

	@Override
	protected long getFlags(int tableId) {
		return definition(tableId)!=null?definition(tableId).getFlags():0;
	}

	@Override
	protected long getColumnFlags(int tableId,int colIndx) {
		return definition(tableId)!=null?definition(tableId).getColumnFlags(colIndx):0;
	}

	@Override
	protected Object getColumnDefaultValue(int tableId, int col) {
		return definition(tableId)!=null?definition(tableId).getColumnDefaultValue(col):null;
	}

	@Override
	protected int getColumnImmutableId(int tableId, int col) {
		return definition(tableId)!=null?definition(tableId).getColumnImmutableId(col):-1;
	}

	protected ODLTable writable(int tableId) {
		if(isWriteAllowed()){
			return (ODLTable)decorated.getTableByImmutableId(tableId);			
		}
		return null;
	}
	
	@Override
	protected void setValueAt(int tableId,Object aValue, int rowIndex, int columnIndex) {
		if(writable(tableId)!=null){
			writable(tableId).setValueAt(aValue, rowIndex, columnIndex);			
		}
	}
	
	@Override
	protected void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {
		if(writable(tableId)!=null){
			writable(tableId).setValueById(aValue, rowId, columnIndex);			
		}
	}

	@Override
	protected int createEmptyRow(int tableId, long rowId) {
		if(writable(tableId)!=null){
			return writable(tableId).createEmptyRow(rowId);
		}
		return -1;
	}

	@Override
	protected void insertEmptyRow(int tableId,int insertAtRowNb, long rowId) {
		if(writable(tableId)!=null){
			writable(tableId).insertEmptyRow(insertAtRowNb,rowId);			
		}
	}

	@Override
	protected void deleteRow(int tableId,int rowNumber) {
		if(writable(tableId)!=null){
			writable(tableId).deleteRow(rowNumber);					
		}
	}

	@Override
	protected int addColumn(int tableId,int id,String name, ODLColumnType type, long flags) {
		if(alterable(tableId)!=null){
			return alterable(tableId).addColumn(id,name, type, flags);			
		}
		return -1;
	}

	@Override
	protected void setFlags(int tableId,long flags) {
		if(alterable(tableId)!=null){
			alterable(tableId).setFlags(flags);		
		}
	}

	@Override
	protected void setColumnFlags(int tableId,int col, long flags) {
		if(alterable(tableId)!=null){		
			alterable(tableId).setColumnFlags(col, flags);
		}
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
	protected void deleteCol(int tableId, int col) {
		if(alterable(tableId)!=null){
			alterable(tableId).deleteColumn(col);			
		}
	}


	@Override
	protected boolean insertCol(int tableId,int id, int col,String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		if(alterable(tableId)!=null){
			return alterable(tableId).insertColumn(id,col,name,type,flags,allowDuplicateNames);		
		}
		return false;
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
	protected long getRowGlobalId(int tableId, int rowIndex) {
		return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getRowId(rowIndex):-1;
	}



	@Override
	public ODLDatastore<T> deepCopyDataOnly() {
		return (ODLDatastore<T>)decorated.deepCopyDataOnly();
	}

	@Override
	protected String getColumnDescription(int tableId, int col) {
		return definition(tableId)!=null? definition(tableId).getColumnDescription(col):"";
	}

	@Override
	protected java.util.Set<String> getColumnTags(int tableId, int col) {
		return definition(tableId)!=null?definition(tableId).getColumnTags(col):null;
	}

	@Override
	protected java.util.Set<String> getTags(int tableId) {
		return definition(tableId)!=null?definition(tableId).getTags():null;
	}


	@Override
	protected void setColumnDescription(int tableId, int col, String description) {
		if(alterable(tableId)!=null){
			alterable(tableId).setColumnDescription(col, description);			
		}
		
	}

	@Override
	protected void setColumnTags(int tableId, int col, Set<String> tags) {
		if(alterable(tableId)!=null){
			alterable(tableId).setColumnTags(col, tags);			
		}
	}

	@Override
	protected void setTags(int tableId, Set<String> tags) {
		if(alterable(tableId)!=null){
			alterable(tableId).setTags(tags);					
		}
	}

	@Override
	protected void setColumnDefaultValue(int tableId, int col, Object value) {
		if(alterable(tableId)!=null){
			alterable(tableId).setColumnDefaultValue(col, value);			
		}
	}


	@Override
	protected boolean containsRowId(int tableId, long rowId) {
		return readOnlyTable(tableId)!=null?readOnlyTable(tableId).containsRowId(rowId):false;
	}

	@Override
	protected long[] find(int tableId, int col, Object value) {
		return readOnlyTable(tableId)!=null?readOnlyTable(tableId).find(col, value):new long[0];
	}

	@Override
	protected long getRowFlags(int tableId, long rowId) {
		return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getRowFlags(rowId):0;
	}

	@Override
	protected void setRowFlags(int tableId, long flags, long rowId) {
		if(writable(tableId)!=null){
			writable(tableId).setRowFlags(flags, rowId);
		}
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
}
