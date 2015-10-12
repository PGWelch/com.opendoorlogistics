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
import com.opendoorlogistics.api.tables.TableQuery;

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
	public int getRowCount(int tableId) {
		ODLTableReadOnly table = readOnlyTable(tableId);
		if(table!=null){
			return table.getRowCount();
		}
		return 0;
	}

	
	@Override	
	public Object getValueAt(int tableId,int rowIndex, int columnIndex) {
		ODLTableReadOnly table = readOnlyTable(tableId);
		if(table!=null){
			return table.getValueAt(rowIndex, columnIndex);
		}
		return null;
		
	//	return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getValueAt(rowIndex, columnIndex):null;
	}

	@Override
	public Object getValueById(int tableId, long rowId, int columnIndex) {
		ODLTableReadOnly table = readOnlyTable(tableId);
		if(table!=null){
			return table.getValueById(rowId, columnIndex);
		}
		return null;
				
	//	return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getValueById(rowId, columnIndex):null;
	}
	
	@Override
	public ODLColumnType getColumnFieldType(int tableId,int colIndex) {
		ODLTableDefinition dfn = definition(tableId);
		if(dfn!=null){
			return dfn.getColumnType(colIndex); 
		}
		return null;
//		return definition(tableId)!=null? definition(tableId).getColumnType(colIndex):null;
	}

	@Override
	public String getColumnName(int tableId,int colIndex) {
		ODLTableDefinition dfn = definition(tableId);
		if(dfn!=null){
			return dfn.getColumnName(colIndex); 
		}
		return "";
//		return definition(tableId)!=null? definition(tableId).getColumnName(colIndex): "";
	}

	@Override
	public int getColumnCount(int tableId) {
		ODLTableDefinition dfn = definition(tableId);
		if(dfn!=null){
			return dfn.getColumnCount(); 
		}
		return 0;
		//return definition(tableId)!=null?definition(tableId).getColumnCount():0;
	}

	@Override
	public String getName(int tableId) {
		ODLTableDefinition dfn = definition(tableId);
		if(dfn!=null){
			return dfn.getName(); 
		}
		return "";
		//return definition(tableId)!=null? definition(tableId).getName():"";
	}

	@Override
	public long getFlags(int tableId) {
		ODLTableDefinition dfn = definition(tableId);
		if(dfn!=null){
			return dfn.getFlags(); 
		}
		return 0;
	//	return definition(tableId)!=null?definition(tableId).getFlags():0;
	}

	@Override
	public long getColumnFlags(int tableId,int colIndx) {
		ODLTableDefinition dfn = definition(tableId);
		if(dfn!=null){
			return dfn.getColumnFlags(colIndx); 
		}
		return 0;
		//return definition(tableId)!=null?definition(tableId).getColumnFlags(colIndx):0;
	}

	@Override
	public Object getColumnDefaultValue(int tableId, int col) {
		ODLTableDefinition dfn = definition(tableId);
		if(dfn!=null){
			return dfn.getColumnDefaultValue(col); 
		}
		return null;
	//	return definition(tableId)!=null?definition(tableId).getColumnDefaultValue(col):null;
	}

	@Override
	public int getColumnImmutableId(int tableId, int col) {
		ODLTableDefinition dfn = definition(tableId);
		if(dfn!=null){
			return dfn.getColumnImmutableId(col); 
		}
		return -1;
	//	return definition(tableId)!=null?definition(tableId).getColumnImmutableId(col):-1;
	}

	protected abstract ODLTable writable(int tableId) ;
	
	@Override
	public void setValueAt(int tableId,Object aValue, int rowIndex, int columnIndex) {
		if(writable(tableId)!=null){
			writable(tableId).setValueAt(aValue, rowIndex, columnIndex);			
		}
	}
	
	@Override
	public void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {
		if(writable(tableId)!=null){
			writable(tableId).setValueById(aValue, rowId, columnIndex);			
		}
	}

	@Override
	public int createEmptyRow(int tableId, long rowId) {
		if(writable(tableId)!=null){
			return writable(tableId).createEmptyRow(rowId);
		}
		return -1;
	}

	@Override
	public void insertEmptyRow(int tableId,int insertAtRowNb, long rowId) {
		if(writable(tableId)!=null){
			writable(tableId).insertEmptyRow(insertAtRowNb,rowId);			
		}
	}

	@Override
	public void deleteRow(int tableId,int rowNumber) {
		if(writable(tableId)!=null){
			writable(tableId).deleteRow(rowNumber);					
		}
	}

	@Override
	public int addColumn(int tableId,int id,String name, ODLColumnType type, long flags) {
		if(alterable(tableId)!=null){
			return alterable(tableId).addColumn(id,name, type, flags);			
		}
		return -1;
	}

	@Override
	public void setFlags(int tableId,long flags) {
		if(alterable(tableId)!=null){
			alterable(tableId).setFlags(flags);		
		}
	}

	@Override
	public void setColumnFlags(int tableId,int col, long flags) {
		if(alterable(tableId)!=null){		
			alterable(tableId).setColumnFlags(col, flags);
		}
	}


	protected abstract ODLTableAlterable alterable(int tableId);

	@Override
	public void deleteCol(int tableId, int col) {
		if(alterable(tableId)!=null){
			alterable(tableId).deleteColumn(col);			
		}
	}


	@Override
	public boolean insertCol(int tableId,int id, int col,String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		if(alterable(tableId)!=null){
			return alterable(tableId).insertColumn(id,col,name,type,flags,allowDuplicateNames);		
		}
		return false;
	}


	@Override
	public long getRowGlobalId(int tableId, int rowIndex) {
		ODLTableReadOnly t = readOnlyTable(tableId);
		if(t!=null){
			return t.getRowId(rowIndex);
		}
		return -1;
		//return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getRowId(rowIndex):-1;
	}


	@Override
	public String getColumnDescription(int tableId, int col) {
		return definition(tableId)!=null? definition(tableId).getColumnDescription(col):"";
	}

	@Override
	public java.util.Set<String> getColumnTags(int tableId, int col) {
		return definition(tableId)!=null?definition(tableId).getColumnTags(col):null;
	}

	@Override
	public java.util.Set<String> getTags(int tableId) {
		return definition(tableId)!=null?definition(tableId).getTags():null;
	}


	@Override
	public void setColumnDescription(int tableId, int col, String description) {
		if(alterable(tableId)!=null){
			alterable(tableId).setColumnDescription(col, description);			
		}
		
	}

	@Override
	public void setColumnTags(int tableId, int col, Set<String> tags) {
		if(alterable(tableId)!=null){
			alterable(tableId).setColumnTags(col, tags);			
		}
	}

	@Override
	public void setTags(int tableId, Set<String> tags) {
		if(alterable(tableId)!=null){
			alterable(tableId).setTags(tags);					
		}
	}

	@Override
	public void setColumnDefaultValue(int tableId, int col, Object value) {
		if(alterable(tableId)!=null){
			alterable(tableId).setColumnDefaultValue(col, value);			
		}
	}


	@Override
	public boolean containsRowId(int tableId, long rowId) {
		ODLTableReadOnly t = readOnlyTable(tableId);
		if(t!=null){
			return t.containsRowId(rowId);
		}
		return false;			
		//return readOnlyTable(tableId)!=null?readOnlyTable(tableId).containsRowId(rowId):false;
	}

	@Override
	public long[] find(int tableId, int col, Object value) {
		ODLTableReadOnly t = readOnlyTable(tableId);
		if(t!=null){
			return t.find(col,value);
		}
		return new long[0];
	//	return readOnlyTable(tableId)!=null?readOnlyTable(tableId).find(col, value):new long[0];
	}

	@Override
	public long getRowFlags(int tableId, long rowId) {
		ODLTableReadOnly t = readOnlyTable(tableId);
		if(t!=null){
			return t.getRowFlags(rowId);
		}
		return 0;		
	//	return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getRowFlags(rowId):0;
	}


	@Override
	public long getRowLastModifiedTimeMillisecs(int tableId, long rowId) {
		ODLTableReadOnly t = readOnlyTable(tableId);
		if(t!=null){
			return t.getRowLastModifiedTimeMillsecs(rowId);
		}
		return 0;		
	//	return readOnlyTable(tableId)!=null?readOnlyTable(tableId).getRowLastModifiedTimeMillsecs(rowId):0;
		
	}
	
	@Override
	public void setRowFlags(int tableId, long flags, long rowId) {
		if(writable(tableId)!=null){
			writable(tableId).setRowFlags(flags, rowId);
		}
	}

	@Override
	public ODLTableReadOnly query(int tableId, TableQuery query){
		ODLTableReadOnly table = readOnlyTable(tableId);
		if(table!=null){
			return table.query(query);
		}
		return null;
	}
}
