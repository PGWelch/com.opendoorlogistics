/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import java.util.Set;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

/**
 * Abstract decorator class which separates its access of the decorated datatstore in writable
 * and non-writable which helps with some derived decorators.
 * @author Phil
 *
 * @param <T>
 */
public abstract class SimpleAbstractDecorator<T extends ODLTableDefinition> extends AbstractDecorator<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7411673940523284819L;
	

	/**
	 * Override this in subclasses to disable writing when needed
	 * @return
	 */
	protected boolean isWriteAllowed(){
		return true;
	}
	
	protected abstract ODLTableReadOnly readOnlyTable(int tableId);
	
	protected abstract ODLTableDefinition definition(int tableId);

	
	@Override
	protected int getRowCount(int tableId) {
		return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getRowCount():0;
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

	protected abstract ODLTable writable(int tableId) ;
	
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


	protected abstract ODLTableAlterable alterable(int tableId);

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
	protected long getRowGlobalId(int tableId, int rowIndex) {
		return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getRowId(rowIndex):-1;
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

}
