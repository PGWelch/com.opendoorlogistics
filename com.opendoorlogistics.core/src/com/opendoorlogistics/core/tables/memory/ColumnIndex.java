/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.memory;

import gnu.trove.list.array.TLongArrayList;

import java.util.HashMap;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.utils.TableFlagUtils;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class ColumnIndex {
	private HashMap<Object, TLongArrayList> index;
	private ODLColumnType lastColumnType;
	private IndexState state = IndexState.DISABLED;
	
	enum IndexState{
		DISABLED,
		PENDING,
		ACTIVE
	}

	void insert(long rowId, Object value, ODLTableReadOnly table, int colIndx){
		updateState(table, colIndx);
		
		if(state == IndexState.ACTIVE){
			put(value, rowId, table.getColumnType(colIndx));
		}
	}
	
	void remove(long rowId,Object value,ODLTableReadOnly table, int colIndx){
		updateState(table, colIndx);
		
		if(state == IndexState.ACTIVE){
			internalRemove(rowId, value);
		}
	}

	/**
	 * Change the value in the index. It is assumed the input value
	 * is of the correct type.
	 * @param rowId
	 * @param previousValue
	 * @param newValue
	 * @param table
	 * @param colIndx
	 */
	void set(long rowId,Object previousValue, Object newValue, ODLTableReadOnly table, int colIndx){
		updateState(table, colIndx);
		
		if(state == IndexState.ACTIVE){
			internalRemove(rowId, previousValue);
			put(newValue, rowId, table.getColumnType(colIndx));
		}
	}
	
	private void internalRemove(long id ,Object value){
		if(value!=null && String.class.isInstance(value)){
			value = Strings.std(value.toString());
		}
		
		TLongArrayList previous = index.get(value);
		if(previous!=null){
			previous.remove(id);
			if(previous.size()==0){
				index.remove(value);
			}
		}
	}
	
	
	/**
	 * Find all row ids with the value or return an empty collection
	 * if none found, using the index if available
	 * @param table
	 * @param colIndx
	 * @param value
	 * @return
	 */
	long[] find(ODLTableReadOnly table,int colIndx, Object value){
		updateState(table, colIndx);
		
		if(state == IndexState.PENDING){
			// active the hashmap
			buildHashmap(table, colIndx);
			state = IndexState.ACTIVE;
		}
		
		long[] ret =null;
		if(state == IndexState.ACTIVE){
			// convert the search value to the type and standardise if its a string
			ODLColumnType colType = table.getColumnType(colIndx);
			Object converted = ColumnValueProcessor.convertToMe(colType,value);
			if(colType == ODLColumnType.STRING && converted!=null){
				converted = Strings.std(converted.toString());
			}
			
			// only search if the value converts to the column type
			if((value!=null && converted==null)==false){
				TLongArrayList list = index.get(converted);
				if(list!=null){
					ret = list.toArray();					
				}else{
					return new long[0];
				}
			}
		}
		else{
			ret = TableUtils.find(table, colIndx, value);
		}

		if(ret==null){
			// create default object
			ret = new long[0];
		}

		return ret;
	}
	
	private void updateState(ODLTableReadOnly table, int colIndx){
		// check for change of type
		ODLColumnType colType = table.getColumnType(colIndx);
		boolean changedType = lastColumnType != colType;
		lastColumnType = colType;

		// get the new state
		if(colType!=ODLColumnType.DOUBLE && colType!=ODLColumnType.STRING && colType!=ODLColumnType.COLOUR && colType!=ODLColumnType.LONG){
			// non indexable type
			state = IndexState.DISABLED;
		}
		else if(TableFlagUtils.hasFlag(table.getColumnFlags(colIndx), TableFlags.FLAG_COLUMN_NOT_INDEXED)){
			// column marked as shouldn't be indexed
			state = IndexState.DISABLED;			
		}else if(state!=IndexState.ACTIVE){
			state = IndexState.PENDING;
		}
		
		// clear the hashmap if not being used
		if(state!=IndexState.ACTIVE){
			index = null;
		}
		
		// update the hashmap if being used and type has changed 
		if(state==IndexState.ACTIVE && changedType){
			buildHashmap(table, colIndx);
		}
	}
	
	private void buildHashmap(ODLTableReadOnly table, int colIndx){
		int nr = table.getRowCount();
		index = new HashMap<>(nr);
		
		ODLColumnType coltype = table.getColumnType(colIndx);
		for(int row =0 ; row<nr;row++){
			Object val = table.getValueAt(row, colIndx);
			long id = table.getRowId(row);
			put(val, id,coltype);			
		}
	}

	private void put(Object val, long id, ODLColumnType columnType) {
		if(columnType == ODLColumnType.STRING && val!=null){
			val = Strings.std(val.toString());
		}
		TLongArrayList list = index.get(val);
		if(list==null){
			list = new TLongArrayList(1);
			index.put(val, list);
		}
		list.add(id);
	}
}
