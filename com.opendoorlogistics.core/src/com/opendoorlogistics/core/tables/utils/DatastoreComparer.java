/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.utils;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.NullComparer;

final public class DatastoreComparer {
	public static long CHECK_COLUMN_FLAGS = 1<<0;
	public static long CHECK_IMMUTABLE_TABLE_IDS = 1<<1;
	public static long CHECK_IMMUTABLE_COLUMN_IDS = 1<<2;
	public static long CHECK_IMMUTABLE_COLUMN_ROW_IDS = 1<<3;
	public static long CHECK_TABLE_NAME = 1<<4;
	public static long ALLOW_EXTRA_COLUMNS_ON_SECOND_TABLE = 1<<5;
	public static long CHECK_ALL = CHECK_COLUMN_FLAGS|CHECK_IMMUTABLE_TABLE_IDS|CHECK_IMMUTABLE_COLUMN_IDS|CHECK_IMMUTABLE_COLUMN_ROW_IDS|CHECK_TABLE_NAME;
	public static long CHECK_ROW_SELECTION_STATE = 1<<6;
	
	public static boolean isSameStructure(ODLDatastore<? extends ODLTableDefinition> a, ODLDatastore<? extends ODLTableDefinition> b) {
		return isSameStructure(a, b, CHECK_ALL);
	}

	/**
	 * Check if datastores have the same structure. If both are null, the method returns true.
	 * @param a
	 * @param b
	 * @param checkColumnFlags
	 * @return
	 */
	public static boolean isSameStructure(ODLDatastore<? extends ODLTableDefinition> a, ODLDatastore<? extends ODLTableDefinition> b, long flags) {
		if(a==null && b==null){
			return true;
		}
		
		if( (a!=null && b==null) || (a==null && b!=null)){
			return false;
		}
		
		if (a.getTableCount() != b.getTableCount()) {
			return false;
		}

		if (a.getFlags() != b.getFlags()) {
			return false;
		}

		for (int i = 0; i < a.getTableCount(); i++) {
			ODLTableDefinition ta = a.getTableAt(i);
			
			// retrieve by table name as table id changing isn't a structural difference by name is...
			ODLTableDefinition tb = TableUtils.findTable(b, ta.getName());
			if(tb==null){
				return false;
			}
			
			if(hasFlag(flags, CHECK_IMMUTABLE_TABLE_IDS)){
				if(ta.getImmutableId()!=tb.getImmutableId()){
					return false;
				}
			}
			
			if (!isSameStructure(ta, ta, flags)) {
				return false;
			}
		}

		return true;
	}

	public static boolean isSame(ODLDatastore<? extends ODLTableReadOnly> a, ODLDatastore<? extends ODLTableReadOnly> b, long flags) {
		if(NullComparer.compare(a, b)!=0){
			return false;
		}
		
		if (!isSameStructure(a, b,flags)) {
			return false;
		}

		for (int i = 0; i < a.getTableCount(); i++) {
			ODLTableReadOnly ta = a.getTableAt(i);
			ODLTableReadOnly tb = TableUtils.findTable(b, ta.getName());

			if(!isSame(ta, tb,flags)){
				return false;
			}
		}
		return true;
	}

	public static boolean isSame(ODLTableReadOnly ta, ODLTableReadOnly tb, long flags) {
		
		if(!isSameStructure(ta, tb, flags)){
			return false;
		}
		
		if(hasFlag(flags, CHECK_IMMUTABLE_TABLE_IDS)){
			if (ta.getImmutableId() != tb.getImmutableId()) {
				return false;
			}			
		}

		if (ta.getRowCount() != tb.getRowCount()) {
			return false;
		}
		
		int rows = ta.getRowCount();
		int cols = ta.getColumnCount();
		
		for (int row = 0; row < rows; row++) {
			if(hasFlag(flags, CHECK_IMMUTABLE_COLUMN_ROW_IDS)){
				if(ta.getRowId(row)!=tb.getRowId(row)){
					return false;
				}				
			}
			
			// check selection state if needed
			if(hasFlag(flags, CHECK_ROW_SELECTION_STATE)){
				long aRowId = ta.getRowId(row);
				long bRowId = tb.getRowId(row);
				if(aRowId!=-1 && bRowId!=-1){
					boolean aSel = hasFlag(ta.getRowFlags(aRowId), TableFlags.FLAG_ROW_SELECTED_IN_MAP);					
					boolean bSel = hasFlag(tb.getRowFlags(bRowId), TableFlags.FLAG_ROW_SELECTED_IN_MAP);
					if(aSel!=bSel){
						return false;
					}
				}
			}
			
			for(int col = 0 ; col < cols; col++){
				if(!ColumnValueProcessor.isEqualSameType(ta.getColumnType(col), ta.getValueAt(row, col), tb.getValueAt(row, col))){
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * Check if tables have the same structure. If both are null, the method returns true.
	 * @param a
	 * @param b
	 * @param flags
	 * @return
	 */
	public static boolean isSameStructure(ODLTableDefinition a, ODLTableDefinition b,long flags) {
		if(a==null && b==null){
			return true;
		}
		
		if( (a!=null && b==null) || (a==null && b!=null)){
			return false;
		}
		
		// table name changes are considered a structurally change
		if( ((flags & CHECK_TABLE_NAME) == CHECK_TABLE_NAME) && a.getName().equals(b.getName())==false){
			return false;
		}

		
		int nc = a.getColumnCount();
		if(hasFlag(flags, ALLOW_EXTRA_COLUMNS_ON_SECOND_TABLE)){
			if(b.getColumnCount()< nc){
				return false;
			}
		}else{
			if (nc != b.getColumnCount()) {
				return false;
			}			
		}

		for (int col = 0; col < nc; col++) {
			if (a.getColumnName(col).equals(b.getColumnName(col)) == false || a.getColumnType(col) != b.getColumnType(col)) {
				return false;
			}

			if (hasFlag(flags, CHECK_COLUMN_FLAGS) && a.getColumnFlags(col) != b.getColumnFlags(col)) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean hasFlag(long flags, long flagToCheckFor){
		return (flags & flagToCheckFor)==flagToCheckFor;
	}
}
