/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.tables.TableQuery;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.tables.decorators.listeners.ListenerRedirector;
import com.opendoorlogistics.core.tables.decorators.tables.FlatDs2TableObject;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableFlagUtils;
import com.opendoorlogistics.core.tables.utils.TableUtils;

/**
 * A decorator which filters the original rows on one or more tables. Rows cannot be added or deleted to the original datastore whilst the filtering happens.
 * 
 * @author Phil
 * 
 */
final public class RowFilterDecorator <T extends ODLTableReadOnly> extends AbstractDecorator<T> {
	private final ODLDatastore<? extends T> src;
	private final ArrayList<FilteredTable> tablesByIndx = new ArrayList<>();
	private final TIntObjectHashMap<FilteredTable> tablesById = new TIntObjectHashMap<>();
	private final ListenerRedirector listenerRedirector;

	public int getRowCount(){
		int ret=0;
		for(FilteredTable table : tablesByIndx){
			ret += table.size();
		}
		return ret;
	}
	
	private class FilteredTable {
		final int tableId;
		final private TLongArrayList arrayList = new TLongArrayList();
		final private TLongHashSet hashSet = new TLongHashSet();
		
		FilteredTable(int tableId) {;
			this.tableId = tableId;
		}
		
		boolean contains(long rowId){
			return hashSet.contains(rowId);
		}

		boolean add(long rowId) {
			if (contains(rowId)) {
				return false;
			}
			arrayList.add(rowId);
			hashSet.add(rowId);
			return true;
		}

		int size() {
			return arrayList.size();
		}

		long getRowId(int indx) {
			if(indx>=arrayList.size()){
				return -1;
			}
			return arrayList.get(indx);
		}

		void removeAt(int indx) {
			long id = arrayList.removeAt(indx);
			hashSet.remove(id);
		}
		
		void clear(){
			arrayList.clear();
			hashSet.clear();
		}
	}


	public RowFilterDecorator(ODLDatastore<? extends T> src, int... filterTableIds) {
		this.src = src;
		this.listenerRedirector = new ListenerRedirector(src, false);
		
		class AddHelper{
			void add(int tableid){
				FilteredTable table = new FilteredTable(tableid);
				tablesByIndx.add(table);
				tablesById.put(table.tableId, table);			
			}
		}
		
		AddHelper helper = new AddHelper();
		
		if(filterTableIds.length > 0){
			// include subset
			for(int tableid : filterTableIds){
				helper.add(tableid);
			}
		}else{
			// include all 
			for (int i = 0; i < src.getTableCount(); i++) {
				helper.add(src.getTableAt(i).getImmutableId());
			}
		}

	}

	public boolean addRowToFilter(int tableId, long rowId) {
		// check table exists
		ODLTableReadOnly table = src.getTableByImmutableId(tableId);
		if (table == null) {
			return false;
		}
		
		// check row exists
		if (table.containsRowId(rowId)==false) {
			return false;
		}

		// add to filtered table
		FilteredTable filteredTable = tablesById.get(tableId);
		return filteredTable.add(rowId);
	}

	public static class UpdateCounter {
		private int nbRowsAdded;
		private int nbRowsDeleted;
		private int nbTablesAdded;
		private int nbTablesDeleted;

		private UpdateCounter() {
		}

		public int getNbRowsAdded() {
			return nbRowsAdded;
		}

		public int getNbRowsDeleted() {
			return nbRowsDeleted;
		}

		public int getNbTablesAdded() {
			return nbTablesAdded;
		}

		public int getNbTablesDeleted() {
			return nbTablesDeleted;
		}

		public boolean getChanged(){
			return (nbRowsAdded + nbRowsDeleted + nbTablesAdded + nbRowsDeleted)>0;
		}
	}

	private void removeTable(int tableId) {
		Iterator<FilteredTable> it = tablesByIndx.iterator();
		while (it.hasNext()) {
			if (it.next().tableId == tableId) {
				it.remove();
			}
		}
		tablesById.remove(tableId);
	}

	public void clearRows(){
		for(FilteredTable table: tablesById.valueCollection()){
			table.clear();
		}
	}
	
	public UpdateCounter update(TLongSet globalRowIds, boolean allowTableDeletion) {
		final UpdateCounter ret = new UpdateCounter();

		// add everything
		int nbTables = tablesByIndx.size();
		globalRowIds.forEach(new TLongProcedure() {

			@Override
			public boolean execute(long globalRowId) {
				if (addRowToFilter(TableUtils.getTableId(globalRowId), globalRowId)) {
					ret.nbRowsAdded++;
				}
				return true;
			}
		});

		ret.nbTablesAdded = tablesByIndx.size() - nbTables;

		// remove everything that no longer exists
		for (final FilteredTable table : new ArrayList<FilteredTable>(tablesByIndx)) {
			// check if table still exists
			if (src.getTableByImmutableId(table.tableId) != null) {

				// get set of row ids for this table
				final TLongHashSet rowIds = new TLongHashSet();
				globalRowIds.forEach(new TLongProcedure() {

					@Override
					public boolean execute(long globalRowId) {
						int tableId = TableUtils.getTableId(globalRowId);
						if (tableId == table.tableId) {
							rowIds.add(globalRowId);
						}
						return true;
					}
				});

				// get the indices to be deleted
				TIntArrayList toDeleteIndices = new TIntArrayList();
				int n = table.size();
				for(int row =0 ; row< n;row++){
					if(rowIds.contains(table.getRowId(row))==false){
						toDeleteIndices.add(row);
					}
				}
				
				// delete them
				for(int i = toDeleteIndices.size()-1;i>=0;i--){
					table.removeAt(toDeleteIndices.get(i));
					ret.nbRowsDeleted++;
				}

			}
			else{
				// remove all
				ret.nbRowsDeleted += table.size();
				table.clear();
			}
			
			if (allowTableDeletion && table.size() == 0) {
				removeTable(table.tableId);
				ret.nbTablesDeleted++;
			}
		}
		return ret;
	}

	@Override
	public T createTable(String tablename, int id) {
		throwUnsupported();
		return null;
	}

	@Override
	public void deleteTableById(int tableId) {
		throwUnsupported();

	}

	@Override
	public boolean setTableName(int tableId, String newName) {
		throwUnsupported();
		return false;
	}

	@Override
	public int getTableCount() {
		return tablesByIndx.size();
	}

	@Override
	public T getTableAt(int i) {
		return getTableByImmutableId(tablesByIndx.get(i).tableId);
	}

	@Override
	public T getTableByImmutableId(int tableId) {
		if(tablesById.containsKey(tableId)){
			return (T)new FlatDs2TableObject(this,tableId);			
		}
		return null;
	}

	@Override
	public void addListener(ODLListener tml, int... tableIds) {
		listenerRedirector.addListener(tml, tableIds);
	}

	@Override
	public void removeListener(ODLListener tml) {
		listenerRedirector.removeListener(tml);
	}

	@Override
	public void disableListeners() {
		listenerRedirector.disableListeners();
	}

	@Override
	public void enableListeners() {
		listenerRedirector.enableListeners();
	}

	@Override
	public void startTransaction() {
		src.startTransaction();
	}

	@Override
	public void endTransaction() {
		src.endTransaction();
	}

	@Override
	public boolean isInTransaction() {
		return src.isInTransaction();
	}

	@Override
	public long getFlags() {
		return src.getFlags();
	}

	@Override
	public void setFlags(long flags) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getRowCount(int tableId) {
		return tablesById.get(tableId).size();
	}

	@Override
	public Object getValueById(int tableId, long rowId, int columnIndex) {
		ODLTableReadOnly srcTable = getSourceTable(tableId);
		if(srcTable!= null){
			return srcTable.getValueById(rowId, columnIndex);
		}
		return null;
	}
	
	@Override
	public Object getValueAt(int tableId, int rowIndex, int columnIndex) {
		FilteredTable filteredTable = tablesById.get(tableId);
		long srcRowId = filteredTable.getRowId(rowIndex);
		return getValueById(tableId, srcRowId, columnIndex);
	}

	private T getSourceTable(int tableId) {
		FilteredTable filteredTable = tablesById.get(tableId);
		T srcTable = (T)src.getTableByImmutableId(filteredTable.tableId);
		return srcTable;
	}

	@Override
	public ODLColumnType getColumnFieldType(int tableId, int col) {
		return getSourceTable(tableId)!=null?getSourceTable(tableId).getColumnType(col):null;
	}

	@Override
	public String getColumnName(int tableId, int col) {
		return getSourceTable(tableId)!=null?getSourceTable(tableId).getColumnName(col):null;
	}

	@Override
	public Object getColumnDefaultValue(int tableId, int col) {
		return getSourceTable(tableId)!=null?getSourceTable(tableId).getColumnDefaultValue(col):null;
	}
	
	@Override
	public int getColumnCount(int tableId) {
		return getSourceTable(tableId)!=null?getSourceTable(tableId).getColumnCount():0;
	}

	@Override
	public String getName(int tableId) {
		return getSourceTable(tableId)!=null?getSourceTable(tableId).getName():null;
	}

	@Override
	public long getFlags(int tableId) {
		long ret=0;
		if(getSourceTable(tableId)!=null){
			ret = getSourceTable(tableId).getFlags();
		}
		
		// don't allow insert or move
		ret = TableFlagUtils.removeFlags(ret, TableFlags.UI_MOVE_ALLOWED | TableFlags.UI_INSERT_ALLOWED);
		return ret;
	}

	@Override
	public long getColumnFlags(int tableId, int col) {
		return getSourceTable(tableId)!=null?getSourceTable(tableId).getColumnFlags(col):0;
	}

	@Override
	public void setValueAt(int tableId, Object aValue, int rowIndex, int columnIndex) {
		FilteredTable filteredTable = tablesById.get(tableId);
		long srcRowId = filteredTable.getRowId(rowIndex);
		setValueById(tableId, aValue, srcRowId, columnIndex);
	}

	@Override
	public void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {
		FilteredTable filteredTable = tablesById.get(tableId);
		ODLTable srcTable =(ODLTable) src.getTableByImmutableId(filteredTable.tableId);
		if(srcTable!=null){
			srcTable.setValueById(aValue, rowId, columnIndex);			
		}
	}

	@Override
	public int createEmptyRow(int tableId, long rowId) {
		ODLTable srcTable =(ODLTable) getSourceTable(tableId);
		int indx = srcTable.createEmptyRow(rowId);
		rowId = srcTable.getRowId(indx);

		FilteredTable filteredTable = tablesById.get(tableId);
		filteredTable.add(rowId);
		return filteredTable.size() - 1;
	}

	@Override
	public void insertEmptyRow(int tableId, int insertAtRowNb, long rowId) {
		throwUnsupported();
	}

	@Override
	public void deleteRow(int tableId, int rowNumber) {
		FilteredTable filteredTable = tablesById.get(tableId);
		long rowId = filteredTable.getRowId(rowNumber);
		filteredTable.removeAt(rowNumber);

		ODLTable srcTable = (ODLTable)getSourceTable(tableId);
		if(srcTable!=null){
			TableUtils.deleteById(srcTable, rowId);			
		}
	}

	@Override
	public void deleteCol(int tableId, int col) {
		throwUnsupported();
	}

	@Override
	public boolean insertCol(int tableId, int colId, int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		throwUnsupported();
		return false;
	}

	@Override
	public int addColumn(int tableId, int colId,String name, ODLColumnType type, long flags) {
		throwUnsupported();
		return -1;
	}

	@Override
	public void setFlags(int tableId, long flags) {
		throwUnsupported();
	}

	@Override
	public void setColumnFlags(int tableId, int col, long flags) {
		throwUnsupported();
	}



//	@Override
//	public int getRowIndexByGlobalId(int tableId, long globalId) {
//		return getRowIndexByLocalId(tableId, Utils.getLocalRowId(globalId));
//	}

	@Override
	public long getRowGlobalId(int tableId, int rowIndex) {
		return tablesById.get(tableId).getRowId(rowIndex);
	//	return getSourceTable(tableId)!=null? getSourceTable(tableId).getRowGlobalIdByLocal(localRowId):-1; 
	}

	@Override
	public boolean containsRowId(int tableId, long rowId) {
		return tablesById.get(tableId).contains(rowId);
	}


	@Override
	public ODLDatastoreAlterable<T> deepCopyWithShallowValueCopy(boolean lazyCopy) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getColumnDescription(int tableId, int col) {
		return getSourceTable(tableId)!=null?getSourceTable(tableId).getColumnDescription(col):null;
	}

	@Override
	public void setColumnDescription(int tableId, int col, String description) {
		if(getSourceTable(tableId)!=null){
			getSourceTable(tableId).setColumnDescription(col, description);			
		}
	}

	@Override
	public java.util.Set<String> getColumnTags(int tableId, int col) {
		return getSourceTable(tableId)!=null?getSourceTable(tableId).getColumnTags(col):null;
	}

	@Override
	public int getColumnImmutableId(int tableId, int col) {
		return getSourceTable(tableId)!=null? getSourceTable(tableId).getColumnImmutableId(col):-1;
	}
	
	@Override
	public java.util.Set<String> getTags(int tableId) {
		return getSourceTable(tableId)!=null?getSourceTable(tableId).getTags():null;
	}

	@Override
	public void setColumnTags(int tableId, int col, Set<String> tags) {
		throwUnsupported();
	}

	@Override
	public void setTags(int tableId, Set<String> tags) {
		throwUnsupported();
	}

	@Override
	public void setColumnDefaultValue(int tableId, int col, Object value) {
		throwUnsupported();
	}
	
	private void throwUnsupported(){
		throw new UnsupportedOperationException("Operation is not allowed when filtering data.");	
	}

	@Override
	public long[] find(int tableId, int col, Object value) {
		FilteredTable table = tablesById.get(tableId);
		if(table==null){
			return null;
		}
		
		ODLTableReadOnly src = getSourceTable(tableId);
		if(table.size() < 10 || src==null){
			// may be more efficient not use the index as its unfiltered
			return TableUtils.find(getTableByImmutableId(tableId), col, value);
		}else{
			// get unfiltered ids matching the value
			long[] unfiltered = src.find(col, value);
			
			// filter them
			int n = unfiltered.length;
			TLongArrayList ret = new TLongArrayList();
			for(int i =0 ; i<n;i++){
				if(table.contains(unfiltered[i])){
					ret.add(unfiltered[i]);
				}
			}
			return ret.toArray();
		}

	}
	
	@Override
	public ODLTableReadOnly query(int tableId, TableQuery query) {
		FilteredTable filteredRowIds = tablesById.get(tableId);
		if(filteredRowIds==null){
			return null;
		}
		
		ODLTableReadOnly src = getSourceTable(tableId);
		if(src==null){
			return null;
		}
		
		// Do the query first as we assume it will be more efficient at paring down the data
		ODLTableReadOnly queryResult = src.query(query);
		if(queryResult==null){
			return null;
		}

		// Now filter
		ODLApiImpl api = new ODLApiImpl();
		Tables tables = api.tables();
		ODLDatastoreAlterable<? extends ODLTableAlterable > ds = tables.createAlterableDs();
		ODLTableAlterable ret=(ODLTableAlterable)tables.copyTableDefinition(queryResult, ds);
		int n = queryResult.getRowCount();
		for(int row =0 ; row < n ; row++){
			if(filteredRowIds.contains(queryResult.getRowId(row))){
				tables.copyRow(queryResult, row, ret);
			}
		}
		return ret;
	}

	@Override
	public long getRowFlags(int tableId, long rowId) {
		ODLTableReadOnly srcTable = getSourceTable(tableId);
		if(srcTable!= null){
			return srcTable.getRowFlags(rowId);
		}

		return 0;
	}

	@Override
	public long getRowLastModifiedTimeMillisecs(int tableId, long rowId) {
		ODLTableReadOnly srcTable = getSourceTable(tableId);
		if(srcTable!= null){
			return srcTable.getRowLastModifiedTimeMillsecs(rowId);
		}

		return 0;
	}
	
	@Override
	public void setRowFlags(int tableId, long flags, long rowId) {
		ODLTableReadOnly srcTable = getSourceTable(tableId);
		if(srcTable!= null){
			((ODLTable)srcTable).setRowFlags(flags,rowId);
		}
		
	}

	@Override
	public void rollbackTransaction() {
		throwUnsupported();
	}

	@Override
	public boolean isRollbackSupported() {
		// cannot rollback a row filter as filtered row records held in this class will not be rolled back
		return false;
	}

	@Override
	public boolean getTableExists(int tableId) {
		return src.getTableByImmutableId(tableId)!=null;
	}

	@Override
	public ODLTableDefinition deepCopyWithShallowValueCopy(int tableId) {
		throw new UnsupportedOperationException();

	}





}
