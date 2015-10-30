/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.lowagie.text.Table;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.*;
import com.opendoorlogistics.core.tables.decorators.rows.ODLRowReadOnlyImpl;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;

final public class DatastoreCopier {
	
//	public static boolean modifyColumnWithoutTransaction(int index, int newIndx, String newName, ODLColumnType newType, long newFlags,
//			ODLTableDefinitionAlterable tableDfn) {
//		
//		int found = TableUtils.findColumnIndx(tableDfn, newName, true);
//		if(found!=-1 && found!=index){
//			return false;
//		}
//			
//		// insert the new column first as this can be rejected
//		String description = tableDfn.getColumnDescription(index);
//		Set<String> tags = tableDfn.getColumnTags(index);
//		Object defaultValue = tableDfn.getColumnDefaultValue(index);
//		if(tableDfn.insertColumn(-1,newIndx, newName, newType, newFlags,true)){
//
//			// also copy description and tags
//			tableDfn.setColumnDescription(newIndx, description);
//			if(tags!=null){
//				tableDfn.setColumnTags(newIndx, tags);
//			}
//			if(defaultValue!=null){
//				tableDfn.setColumnDefaultValue(newIndx, defaultValue);
//			}
//			
//			// update the source index
//			if(index >= newIndx){
//				index++;
//			}
//			
//			// copy all values across to the new column
//			if(ODLTable.class.isInstance(tableDfn)){
//				ODLTable table = (ODLTable)tableDfn;
//				int nr = table.getRowCount();
//				for(int row =0 ; row < nr ; row++){
//					DatastoreCopier.copyCell(table, row, index, table, row, newIndx);
//				}				
//			}
//			
//			// remove the old column
//			tableDfn.deleteColumn(index);
//			
//			return true;
//		}
//		
//		return false;
//	}
	
	public static boolean modifyColumnWithoutTransaction(int index, int newIndx, String newName, ODLColumnType newType, long newFlags,
			ODLTableDefinitionAlterable tableDfn) {
		
		// ensure name is OK
		int found = TableUtils.findColumnIndx(tableDfn, newName, true);
		if(found!=-1 && found!=index){
			return false;
		}
			
		// copy all column details
		String description = tableDfn.getColumnDescription(index);
		Set<String> tags = tableDfn.getColumnTags(index);
		Object defaultValue = tableDfn.getColumnDefaultValue(index);
		int id = tableDfn.getColumnImmutableId(index);
	
		// save all values for this column
		LinkedList<Object> savedValues = new LinkedList<>();
		boolean isTable = ODLTable.class.isInstance(tableDfn);
		if(isTable){
			ODLTableReadOnly table = (ODLTableReadOnly)tableDfn;
			int nr = table.getRowCount();
			for(int row=0;row<nr;row++){
				savedValues.add(table.getValueAt(row, index));
			}
		}
		
		// remove the old column
		tableDfn.deleteColumn(index);
		
		// re-add the column
		if(tableDfn.insertColumn(id,newIndx, newName, newType, newFlags,true)){
		
			// copy description, tags and default value
			tableDfn.setColumnDescription(newIndx, description);
			if(tags!=null){
				tableDfn.setColumnTags(newIndx, tags);
			}
			if(defaultValue!=null){
				tableDfn.setColumnDefaultValue(newIndx, defaultValue);
			}
			
			// copy all values
			if(isTable){
				ODLTable table = (ODLTable)tableDfn;
				int row=0;
				for(Object val:savedValues){
					table.setValueAt(val, row, newIndx);
					row++;
				}
			}
			
		}else{
			throw new RuntimeException();
		}
		
//		// insert the new column first as this can be rejected
//		if(tableDfn.insertColumn(-1,newIndx, newName, newType, newFlags,true)){
//
//			// also copy description and tags
//			tableDfn.setColumnDescription(newIndx, description);
//			if(tags!=null){
//				tableDfn.setColumnTags(newIndx, tags);
//			}
//			if(defaultValue!=null){
//				tableDfn.setColumnDefaultValue(newIndx, defaultValue);
//			}
//			
//			// update the source index
//			if(index >= newIndx){
//				index++;
//			}
//			
//			// copy all values across to the new column
//			if(ODLTable.class.isInstance(tableDfn)){
//				ODLTable table = (ODLTable)tableDfn;
//				int nr = table.getRowCount();
//				for(int row =0 ; row < nr ; row++){
//					DatastoreCopier.copyCell(table, row, index, table, row, newIndx);
//				}				
//			}
//			
//			// remove the old column
//			tableDfn.deleteColumn(index);
//			
//			return true;
//		}
		
		return true;
	}
	public static void copyStructure(ODLDatastore<? extends ODLTableDefinition> copyThis, ODLDatastoreAlterable<? extends ODLTableAlterable> copyTo ){
		if(copyTo.getTableCount()>0){
			throw notEmptyException();
		}
		
		for(int i = 0 ; i < copyThis.getTableCount() ; i++){
			if(copyTableDefinition(copyThis.getTableAt(i), copyTo)==null){
				throw new RuntimeException("Failed to create table " + copyThis.getTableAt(i).getName());				
			}
		}
	}
	
	public static void copyData(ODLDatastore<? extends ODLTableReadOnly> copyThis, ODLDatastoreAlterable<? extends ODLTable> copyTo ){
		if(copyThis.getTableCount()!=copyTo.getTableCount()){
			throw unequalStructureException();
		}
		
		for(int i = 0 ; i < copyThis.getTableCount() ; i++){
			ODLTableReadOnly tFrom = copyThis.getTableAt(i);
			ODLTable tTo = copyTo.getTableAt(i);
			copyData(tFrom, tTo);
		}
	}
	

	public static ODLTableAlterable copyTableIntoSameDatastore(ODLDatastoreAlterable<? extends ODLTableAlterable> ds, int tableId, String copyName){
		boolean transaction = ds.isInTransaction();
		if(!transaction){
			ds.startTransaction();
		}
		
		ODLTableAlterable copy = ds.createTable(copyName, -1);
		if(copy!=null){
			ODLTableReadOnly original = ds.getTableByImmutableId(tableId);
			copyTableDefinition(original, copy);
			copyData(original, copy);
		}
		
		if(!transaction){
			ds.endTransaction();
		}
		return copy;
	}
	
	public static ODLTableAlterable copyTable(ODLTableReadOnly copyThis){
		ODLDatastoreAlterable<ODLTableAlterable> ds = ODLDatastoreImpl.alterableFactory.create();
		ODLTableAlterable ret = ds.createTable("tmp", -1);
		DatastoreCopier.copyTableDefinition(copyThis, ret);
		DatastoreCopier.copyData(copyThis, ret);
		return ret;
	}

	public static ODLTableAlterable copyTable(ODLTableReadOnly copyThis, ODLDatastoreAlterable<? extends ODLTableAlterable> copyInto, String newName){
		// is id already used?
		int id = copyThis.getImmutableId();
		if(copyInto.getTableByImmutableId(id)!=null){
			id = -1;
		}
		
		// is table name already used?
		if(TableUtils.findTable(copyInto,newName, true)!=null){
			return null;
		}
		
		ODLTableAlterable ret = (ODLTableAlterable)copyTableDefinition(copyThis, copyInto, newName);
		DatastoreCopier.copyData(copyThis, ret);
		return ret;	
	}
	
	/**
	 * Copy the table into the datastore. The same id is used if available.
	 * If the name is already used then the copy fails.
	 * @param copyThis
	 * @param copyInto
	 * @return
	 */
	public static ODLTableAlterable copyTable(ODLTableReadOnly copyThis, ODLDatastoreAlterable<? extends ODLTableAlterable> copyInto){
		return copyTable(copyThis, copyInto, copyThis.getName());
	}

	public static void copyData(ODLTableReadOnly tFrom, ODLTable tTo){
		copyData(tFrom, tTo, true);
	}
	
	public static void copyData(ODLTableReadOnly tFrom, ODLTable tTo, boolean copyRowFlags){
			
		if(!DatastoreComparer.isSameStructure(tFrom, tTo, 0)){
			throw unequalStructureException();
		}

		int nr = tFrom.getRowCount();
		for(int srcRow =0 ; srcRow < nr ; srcRow++){
			insertRow(tFrom, srcRow, tTo, tTo.getRowCount(),copyRowFlags);
		}
			
	}

	/**
	 * Sync the To table to the contents of the From table, disregarding
	 * any difference in row order. Tables must have the same structure.
	 * @param syncFrom
	 * @param toSync
	 */
	public static void unorderedSync(ODLTableReadOnly syncFrom, ODLTable toSync){
		if(!DatastoreComparer.isSameStructure(syncFrom, toSync, 0)){
			throw unequalStructureException();
		}
		
		// rows are compared in the map based on their values and two rows could have the same values,
		// so we have to use a multi-map
		TreeMap<ODLRowReadOnly, ArrayList<ODLRowReadOnly>> map = new TreeMap<>(TableUtils.createRowComparatorUsingColumnType());
		
		// read sync from
		int nsr = syncFrom.getRowCount();
		for(int i =0 ; i< nsr ; i++){
			ODLRowReadOnly row = new ODLRowReadOnlyImpl(syncFrom, i);
			ArrayList<ODLRowReadOnly> list = map.get(row);
			if(list==null){
				list = new ArrayList<>();
				map.put(row, list);
			}
			list.add(row);
		}
		
		// loop over sync to in reverse order
		for(int i = toSync.getRowCount()-1 ; i>=0 ; i--){
			ODLRowReadOnly current= new ODLRowReadOnlyImpl(toSync, i);
			List<ODLRowReadOnly> exists =map.get(current);
			if(exists!=null){
				// keep row and remove one from syncfrom collection
				exists.remove(exists.size()-1);
				if(exists.size()==0){
					map.remove(current);
				}
			}else{
				toSync.deleteRow(i);
			}
		}
		
		// everything still remaining in toSync can stay; anything remaining in multimap must be added
		
		// fill in everything that needs adding
		for(List<ODLRowReadOnly> list : map.values()){
			for(ODLRowReadOnly row : list){
				int rowIndx = toSync.createEmptyRow(-1);
				int nc = row.getColumnCount();
				for(int col = 0 ; col < nc ; col++){
					toSync.setValueAt(row.get(col), rowIndx, col);
				}		
			}
		}

	}
	

	public static void insertRow(ODLTableReadOnly tFrom,int fromRow, ODLTable tTo, int toRow){
		insertRow(tFrom, fromRow, tTo, toRow, true);
	}
	
	public static void insertRow(ODLTableReadOnly tFrom,int fromRow, ODLTable tTo, int toRow, boolean copyRowFlags){
		// use original id if available
		long id = tFrom.getRowId(fromRow);
		
		// copy row flags but strip selection state and linked excel flags
		long flags =0;
		if(copyRowFlags){
			flags =removeLinkedExcelFlags(tFrom.getRowFlags(id));
			flags &= ~TableFlags.FLAG_ROW_SELECTED_IN_MAP;			
		}
				
		if(tTo.containsRowId(id)){
			id = -1;
		}
		
		tTo.insertEmptyRow(toRow,id);
		int nc = tFrom.getColumnCount();
		for(int col =0 ; col < nc; col++){
			copyCell(tFrom, fromRow,col, tTo, toRow, col);
		}
		
		// Set flags
		if(copyRowFlags){
			id = tTo.getRowId(toRow);
			tTo.setRowFlags(id, flags);				
		}
	}

	public static void copyRowById(ODLTableReadOnly tFrom,long fromRowId, ODLTable tTo){

		int row = tTo.createEmptyRow(-1);
		int nc = tFrom.getColumnCount();
		for(int col =0 ; col < nc; col++){
			tTo.setValueAt(tFrom.getValueById(fromRowId, col), row, col);
		}
	}

	
	public static void copyCell(ODLTableReadOnly from, int fromRow,int fromCol, ODLTable to, int toRow, int toCol) {
		to.setValueAt(from.getValueAt(fromRow, fromCol), toRow, toCol);
	}

	public static ODLDatastoreAlterable<ODLTableAlterable> copyAll(ODLDatastore<? extends ODLTableReadOnly> copyThis){
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
		copyAll(copyThis, ret);
		return ret;
	}

	
	public static void copyAll(ODLDatastore<? extends ODLTableReadOnly> copyThis, ODLDatastoreAlterable<? extends ODLTableAlterable> copyTo){
		copyTo.setFlags(copyThis.getFlags());
		for(int i =0 ; i < copyThis.getTableCount() ; i++){
			ODLTableAlterable table = (ODLTableAlterable)copyTableDefinition(copyThis.getTableAt(i), copyTo);
			copyData(copyThis.getTableAt(i), table);
		}
		
	}

	public static boolean copyTableDefinitions(ODLDatastore<? extends ODLTableDefinition> copyThese, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> copyTo){

		return copyTableDefinitions(new Iterable<ODLTableDefinition>() {
			@Override
			public Iterator<ODLTableDefinition> iterator() {
				int n = copyThese.getTableCount();
				return new Iterator<ODLTableDefinition>() {
					int index=-1;
					
					@Override
					public ODLTableDefinition next() {
						return copyThese.getTableAt(++index);
					}
					
					@Override
					public boolean hasNext() {
						return (index+1)< n;
					}
				};
			}
		}, copyTo);
	}

	
	public static boolean copyTableDefinitions(Iterable<? extends ODLTableDefinition> copyThese, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> copyTo){
		for(ODLTableDefinition table : copyThese){
			ODLTableDefinitionAlterable copyTable = copyTo.createTable(table.getName(), -1);
			if(copyTable==null){
				return false;
			}
			
			copyTableDefinition(table, copyTable);
		}
		return true;
	}

	public static ODLTableDefinitionAlterable copyTableDefinition(ODLTableDefinition copyThis, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> copyTo, String newName){
		return copyTableDefinition(copyThis, copyTo, newName, -1);
	}

	public static ODLTableDefinitionAlterable copyTableDefinition(ODLTableDefinition copyThis, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> copyTo, String newName, int id){
		
		// reuse the original id if we can
		if(id==-1){
			id = copyThis.getImmutableId();
		}
		
		// only use id if unique
		if(id!=-1 && copyTo.getTableByImmutableId(id)!=null){
			id = -1;
		}
		
		if(copyTo.createTable(newName,id)==null){
			return null;
		}
		
		ODLTableDefinitionAlterable table = copyTo.getTableAt(copyTo.getTableCount()-1);
		copyTableDefinition(copyThis, table);
		
		return table;
	}
	
	public static ODLTableDefinitionAlterable copyTableDefinition(ODLTableDefinition copyThis, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> copyTo){
		return copyTableDefinition(copyThis, copyTo, copyThis.getName());
	}


	public static boolean copyTableDefinition(ODLTableDefinition copyThis, ODLTableDefinitionAlterable copyTo) {
		return copyTableDefinition(copyThis, copyTo, true);
	}
	
	public static boolean copyTableDefinition(ODLTableDefinition copyThis, ODLTableDefinitionAlterable copyTo, boolean copyColumnIds) {
		copyTo.setFlags(removeLinkedExcelFlags(copyThis.getFlags()));
		copyTo.setTags(copyThis.getTags());

		
		boolean allAdded= true;
		for(int fromCol =0 ;fromCol < copyThis.getColumnCount() ; fromCol++){
			if(!copyColumnDefinition(copyThis, copyTo, fromCol, copyColumnIds)){
				allAdded = false;
			}
		}
		return allAdded;
	}
	
	public static boolean copyColumnDefinition(ODLTableDefinition copyThis, ODLTableDefinitionAlterable copyTo, int fromCol, boolean copyColumnIds) {
		return copyColumnDefinition(copyThis, copyTo, fromCol, -1, copyColumnIds);
	}

	
	public static boolean copyColumnDefinition(ODLTableDefinition copyThis, ODLTableDefinitionAlterable copyTo, int fromCol,int toCol, boolean copyColumnIds) {
		int id = copyColumnIds? copyThis.getColumnImmutableId(fromCol) : -1;
		
		// check id not already used
		if(id!=-1){
			int n = copyTo.getColumnCount();
			for(int i=0 ; i<n;i++){
				if(copyTo.getColumnImmutableId(i)==id){
					id=-1;
					break;
				}
			}
		}

		// remove all flags indicating this data was from a linked excel (as its now a copy)
		long flags = removeLinkedExcelFlags(copyThis.getColumnFlags(fromCol));
		
		boolean copied;
		if(toCol==-1){
			copied = copyTo.addColumn(id,copyThis.getColumnName(fromCol), copyThis.getColumnType(fromCol), flags)!=-1;
			toCol = copyTo.getColumnCount()-1;
		}else{
			copied = copyTo.insertColumn(id, toCol, copyThis.getColumnName(fromCol), copyThis.getColumnType(fromCol), flags,false);
		}
		
		if(copied){
			copyTo.setColumnDescription(toCol, copyThis.getColumnDescription(fromCol));
			copyTo.setColumnTags(toCol, copyThis.getColumnTags(fromCol));
			copyTo.setColumnDefaultValue(toCol, copyThis.getColumnDefaultValue(fromCol));
		}
		
		return copied;
	}
	private static long removeLinkedExcelFlags(long flags) {
		flags &= ~ TableFlags.ALL_LINKED_EXCEL_FLAGS;
		return flags;
	}
	
	private static RuntimeException notEmptyException() {
		return new RuntimeException("Copy to database not empty");
	}
	
	private static RuntimeException unequalStructureException() {
		return new RuntimeException("Copy from and copy to databases have different structure");
	}
	
	/**
	 * Modify the datastore if needed to create the input tables and fields
	 * @param schema
	 * @param ds
	 */
	public static void enforceSchema(ODLDatastore<? extends ODLTableDefinition> schema, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, boolean changeFieldTypes){
		// check for table
		int nt = schema.getTableCount();
		for(int i =0 ; i < nt ; i++){
			ODLTableDefinition src = schema.getTableAt(i);
			ODLTableDefinitionAlterable dest = TableUtils.findTable(ds, src.getName());
			if(dest==null){
				copyTableDefinition(src, ds);
			}else{
				enforceSchema(src, dest, changeFieldTypes);
			}
		}
	}
	
	public static void enforceSchema(ODLTableDefinition schema,ODLTableDefinitionAlterable target, boolean changeFieldTypes){
		int nf = schema.getColumnCount();
		for(int srcCol =0 ; srcCol < nf ; srcCol++){
			String name = schema.getColumnName(srcCol);
			int targetCol = TableUtils.findColumnIndx(target, name);
			if(targetCol==-1){
				copyColumnDefinition(schema, target, srcCol, false);
			}
			else if(changeFieldTypes && schema.getColumnType(srcCol)!=target.getColumnType(targetCol)){
				modifyColumnWithoutTransaction(targetCol, targetCol, target.getColumnName(targetCol), schema.getColumnType(srcCol), target.getColumnFlags(targetCol), target);
			}
		}
	}

	
	/**
	 * Merge the source datastore into the destination.
	 * Fields and tables are added as needed.
	 * @param source
	 * @param destination
	 */
	public static void mergeAll(ODLDatastore<? extends ODLTableReadOnly> source, ODLDatastoreAlterable<? extends ODLTableAlterable> destination){
		
		// ensure all tables and fields exist
		enforceSchema(source, destination, false);
		
		// loop over all source tables
		int nt = source.getTableCount();
		for(int srcTableIndex =0 ; srcTableIndex < nt ; srcTableIndex++){
			
			// get the destination table
			ODLTableReadOnly srcTable = source.getTableAt(srcTableIndex);
			ODLTable destTable = TableUtils.findTable(destination, srcTable.getName());

			// get the destination column indices
			int nc = srcTable.getColumnCount();
			int [] destCols = new int[nc];
			for(int srcCol=0; srcCol < nc ; srcCol++){
				destCols[srcCol] = TableUtils.findColumnIndx(destTable, srcTable.getColumnName(srcCol));				
			}
			
			// loop over all source rows
			int nr = srcTable.getRowCount();
			for(int srcRow = 0 ; srcRow <nr ; srcRow++){
				
				// create destination row
				int destRow = destTable.createEmptyRow(-1);
				
				// copy column values
				for(int srcCol=0; srcCol < nc ; srcCol++){
					copyCell(srcTable, srcRow, srcCol, destTable, destRow, destCols[srcCol]);
				}
				
			}
		}
	}
}
