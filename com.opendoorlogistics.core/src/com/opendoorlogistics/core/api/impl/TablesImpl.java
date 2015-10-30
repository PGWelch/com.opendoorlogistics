/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.tables.HasUndoStateListeners;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLFlatDatastoreExt;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.BeanTableMapping;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.execution.adapters.AdapterBuilderUtils;
import com.opendoorlogistics.core.scripts.wizard.ColumnNameMatch;
import com.opendoorlogistics.core.tables.ODLFactory;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanTypeConversion;
import com.opendoorlogistics.core.tables.decorators.tables.FlatDs2TableObject;
import com.opendoorlogistics.core.tables.utils.DatastoreComparer;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.tables.utils.ParametersTable;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.EnumStdLookup;

public class TablesImpl implements Tables {
	private static final EnumStdLookup<ODLColumnType> CT_LOOKUP = new EnumStdLookup<ODLColumnType>(ODLColumnType.class);
	private final ODLApi api;

	public TablesImpl(ODLApi api) {
		this.api = api;
	}

	@Override
	public ODLTableDefinitionAlterable copyTableDefinition(ODLTableDefinition copyThis, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> copyTo) {
		return DatastoreCopier.copyTableDefinition(copyThis, copyTo, copyThis.getName(), copyTo.getTableByImmutableId(copyThis.getImmutableId()) == null ? copyThis.getImmutableId() : -1);
	}

	@Override
	public int addRow(ODLTable table, Object... values) {
		return TableUtils.addRow(table, values);
	}

	@Override
	public ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> createDefinitionDs() {
		return ODLFactory.createDefinition();
	}

	@Override
	public ODLDatastoreAlterable<? extends ODLTableAlterable> createAlterableDs() {
		return ODLFactory.createAlterable();
	}

	@Override
	public ODLTableAlterable createAlterableTable(String name) {
		return ODLFactory.createAlterableTable(name);
	}

	@Override
	public void setColumnIsOptional(ODLTableDefinitionAlterable table, int col, boolean optional) {
		long flags = table.getColumnFlags(col);
		if (optional) {
			flags |= TableFlags.FLAG_IS_OPTIONAL;
		} else {
			flags &= ~TableFlags.FLAG_IS_OPTIONAL;
		}
		table.setColumnFlags(col, flags);
	}

	@Override
	public void clearTable(ODLTable table) {
		TableUtils.removeAllRows(table);
	}

	@Override
	public void validateForeignKey(ODLTableReadOnly primaryKeyTable, int primaryKeyColIndx, ODLTable foreignKeyTable, int foreignKeyColIndx, KeyValidationMode mode) {
		int row = 0;
		while (row < foreignKeyTable.getRowCount()) {
			Object value = foreignKeyTable.getValueAt(row, foreignKeyColIndx);
			boolean missing = false;
			if (value == null) {
				missing = true;
			}

			if (!missing) {
				long[] vals = primaryKeyTable.find(primaryKeyColIndx, value);
				missing = vals == null || vals.length == 0;
			}

			if (missing) {
				switch (mode) {
				case REMOVE_CORRUPT_FOREIGN_KEY:
					// delete and continue here so we don't increment row
					foreignKeyTable.deleteRow(row);
					continue;

				case THROW_UNCHECKED_EXCEPTION:
					throw new RuntimeException(
							"Table \"" + foreignKeyTable.getName() + "\" has corrupt foreign key value. \"" + value + "\" cannot be found in table \"" + primaryKeyTable.getName() + "\".");
				}
			}
			row++;
		}
	}

	@Override
	public int findColumnIndex(ODLTableDefinition table, String name) {
		return TableUtils.findColumnIndx(table, name, true);
	}

	@Override
	public <T extends ODLTableDefinition> T findTable(ODLDatastore<T> ds, String tableName) {
		return TableUtils.findTable(ds, tableName, true);
	}

	@Override
	public ODLColumnType getColumnType(Class<?> externalType) {
		return BeanTypeConversion.getInternalType(externalType);
	}

	@Override
	public int findTableIndex(ODLDatastore<? extends ODLTableDefinition> ds, String table) {
		return TableUtils.findTableIndex(ds, table, true);
	}

	@Override
	public ODLDatastore<? extends ODLTable> createExampleDs() {
		return ExampleData.createTerritoriesExample(3);
	}

	@Override
	public void copyRow(ODLTableReadOnly from, int rowIndex, ODLTable to) {
		DatastoreCopier.insertRow(from, rowIndex, to, to.getRowCount());
	}


	@Override
	public void copyRow(ODLTableReadOnly from, int fromRowIndex, ODLTable to, int toRowIndex) {
		// copy values first in case we're inserting into same table at an earlier position
		int n = from.getColumnCount();
		Object [] vals = new Object[n];
		for(int i =0 ; i<n;i++){
			vals[i] = from.getValueAt(fromRowIndex, i);
		}
		
		to.insertEmptyRow(toRowIndex, -1);
		for(int i =0 ; i<n;i++){
			to.setValueAt(vals[0], toRowIndex, i);
		}
	}

	@Override
	public ODLTableAlterable createTable(ODLTableDefinition tableDefinition) {
		ODLDatastoreAlterable<? extends ODLTableAlterable> ds = createAlterableDs();
		DatastoreCopier.copyTableDefinition(tableDefinition, ds);
		return ds.getTableAt(0);
	}

	@Override
	public void copyRowById(ODLTableReadOnly from, long rowId, ODLTable to) {
		DatastoreCopier.copyRowById(from, rowId, to);
	}

	@Override
	public void addTableDefinitions(ODLDatastore<? extends ODLTableDefinition> schema, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, boolean changeFieldTypes) {
		DatastoreCopier.enforceSchema(schema, ds, changeFieldTypes);
	}

	@Override
	public void addTablesWithData(ODLDatastore<? extends ODLTableReadOnly> source, ODLDatastoreAlterable<? extends ODLTableAlterable> destination) {
		DatastoreCopier.mergeAll(source, destination);
	}

	@Override
	public void addTableDefinition(ODLTableDefinition schema, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, boolean changeFieldTypes) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> tempDs = createAlterableDs();
		DatastoreCopier.copyTableDefinition(schema, tempDs);
		addTableDefinitions(tempDs, ds, changeFieldTypes);
	}

	@Override
	public ODLTableDefinition createParametersTableDefinition() {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> tempDs = createAlterableDs();
		DatastoreCopier.copyTableDefinition(ParametersTable.tableDefinition(), tempDs);
		return tempDs.getTableAt(0);
	}

	@Override
	public void copyColumnDefinition(ODLTableDefinition source, int sourceCol, ODLTableDefinitionAlterable destination) {
		DatastoreCopier.copyColumnDefinition(source, destination, sourceCol, false);
	}
	
	@Override
	public void copyColumnDefinition(ODLTableDefinition source, int sourceCol, ODLTableDefinitionAlterable destination, int destinationCol) {
		DatastoreCopier.copyColumnDefinition(source, destination, sourceCol,destinationCol, false);
	}

	@Override
	public ODLTableAlterable copyTable(ODLTableReadOnly copyThis, ODLDatastoreAlterable<? extends ODLTableAlterable> copyTo) {
		return DatastoreCopier.copyTable(copyThis, copyTo);
	}

	@Override
	public boolean isIdentical(ODLTableReadOnly a, ODLTableReadOnly b) {
		return DatastoreComparer.isSame(a, b, 0);
	}

	@Override
	public ODLDatastoreAlterable<? extends ODLTableAlterable> copyDs(ODLDatastore<? extends ODLTableReadOnly> ds) {
		return DatastoreCopier.copyAll(ds);
	}

	@Override
	public ODLColumnType getColumnType(String columnTypeName) {
		return CT_LOOKUP.get(columnTypeName);
	}

	@Override
	public Set<String> getColumnNamesSet(ODLTableDefinition table) {
		Set<String> ret = api.stringConventions().createStandardisedSet();
		int nc = table.getColumnCount();
		for (int i = 0; i < nc; i++) {
			ret.add(table.getColumnName(i));
		}
		return ret;
	}

	@Override
	public Map<String, Integer> getColumnNamesMap(ODLTableDefinition table) {
		Map<String, Integer> ret = api.stringConventions().createStandardisedMap();
		int nc = table.getColumnCount();
		for (int i = 0; i < nc; i++) {
			ret.put(table.getColumnName(i), i);
		}
		return ret;
	}

	@Override
	public void copyTableDefinition(ODLTableDefinition copyThis, ODLTableDefinitionAlterable copyInto) {
		DatastoreCopier.copyTableDefinition(copyThis, copyInto);
	}

	@Override
	public ODLDatastoreUndoable<ODLTableAlterable> unflattenDs(ODLFlatDatastoreExt flatDatastore) {
		FlatDs2TableObject.Flat2DsTableCache tableCache = new FlatDs2TableObject.Flat2DsTableCache(flatDatastore);

		return new ODLDatastoreUndoable<ODLTableAlterable>() {

			@Override
			public ODLTableAlterable getTableByImmutableId(int id) {
				return tableCache.getTable(id);
			}

			@Override
			public void setFlags(long flags) {
				flatDatastore.setFlags(flags);
			}

			@Override
			public ODLDatastoreAlterable<? extends ODLTableAlterable> deepCopyWithShallowValueCopy(boolean createLazyCopy) {
				return flatDatastore.deepCopyWithShallowValueCopy(createLazyCopy);
			}

			@Override
			public int getTableCount() {
				return flatDatastore.getTableCount();
			}

			@Override
			public ODLTableAlterable getTableAt(int i) {
				int id = flatDatastore.getTableId(i);
				return getTableByImmutableId(id);
			}

			@Override
			public void addListener(ODLListener tml, int... tableIds) {
				flatDatastore.addListener(tml, tableIds);
			}

			@Override
			public void removeListener(ODLListener tml) {
				flatDatastore.removeListener(tml);
			}

			@Override
			public void disableListeners() {
				flatDatastore.disableListeners();
			}

			@Override
			public void enableListeners() {
				flatDatastore.enableListeners();
			}

			@Override
			public void startTransaction() {
				flatDatastore.startTransaction();
			}

			@Override
			public void endTransaction() {
				flatDatastore.endTransaction();
			}

			@Override
			public boolean isInTransaction() {
				return flatDatastore.isInTransaction();
			}

			@Override
			public void rollbackTransaction() {
				flatDatastore.rollbackTransaction();
			}

			@Override
			public boolean isRollbackSupported() {
				return flatDatastore.isRollbackSupported();
			}

			@Override
			public long getFlags() {
				return flatDatastore.getFlags();
			}

			@Override
			public ODLTableAlterable createTable(String tablename, int id) {
				id = flatDatastore.createTable(tablename, id);
				if(id!=-1){
					return getTableByImmutableId(id);
				}
				return null;
			}

			@Override
			public void deleteTableById(int tableId) {
				flatDatastore.deleteTableById(tableId);
			}

			@Override
			public boolean setTableName(int tableId, String newName) {
				return flatDatastore.setTableName(tableId, newName);
			}

			@Override
			public void undo() {
				flatDatastore.undo();
			}

			@Override
			public void redo() {
				flatDatastore.redo();
			}

			@Override
			public boolean hasRedo() {
				return flatDatastore.hasRedo();
			}

			@Override
			public boolean hasUndo() {
				return flatDatastore.hasUndo();
			}

			@Override
			public void addUndoStateListener(HasUndoStateListeners.UndoStateChangedListener<ODLTableAlterable> listener) {
				flatDatastore.addUndoStateListener(listener);
			}

			@Override
			public void removeUndoStateListener(HasUndoStateListeners.UndoStateChangedListener<ODLTableAlterable> listener) {
				flatDatastore.removeUndoStateListener(listener);
			}

			@Override
			public void clearUndoBuffer() {
				flatDatastore.clearUndoBuffer();
			}
		};
	}

	@Override
	public BeanTableMapping mapBeanToTable(Class<? extends BeanMappedRow> cls) {
		return BeanMapping.buildTable(cls);
	}

	@Override
	public <T extends ODLTableDefinition> List<T> getTables(ODLDatastore<T> ds) {
		int n = ds.getTableCount();
		ArrayList<T> ret = new ArrayList<>(n);
		for(int i =0 ; i < n ; i++){
			ret.add(ds.getTableAt(i));
		}
		return ret;
	}

	@Override
	public boolean modifyColumn(int index, int newIndx, String newName, ODLColumnType newType, ODLTableAlterable table) {
		return DatastoreCopier.modifyColumnWithoutTransaction(index, newIndx, newName, newType, table.getColumnFlags(index), table);
	}

	@Override
	public void clearDatastore(ODLDatastoreAlterable<? extends ODLTableAlterable> ds) {
		while(ds.getTableCount()>0){
			// Turn off all linked excel flags to allow deletion.
			// Deletion uses the undo / redo decorator which deletes row and columns first so 
			// we must remove linked flags from everywhere or deletion will throw an exception.
			
			// Table flags first
			ODLTableAlterable table = ds.getTableAt(0);
			table.setFlags(table.getFlags() & ~TableFlags.ALL_LINKED_EXCEL_FLAGS);
			
			// Then table column flags
			for(int col =0 ; col < table.getColumnCount() ; col++){
				table.setColumnFlags(col, table.getColumnFlags(col) & ~TableFlags.ALL_LINKED_EXCEL_FLAGS);
			}
			
			// Then row flags
			for(int row =0 ; row < table.getRowCount() ; row++){
				long id = table.getRowId(row);
				table.setRowFlags(table.getRowFlags(id) & ~TableFlags.ALL_LINKED_EXCEL_FLAGS, id);
			}
			
			// Finally delete the table itself
			ds.deleteTableById(table.getImmutableId());
		}
	}

	@Override
	public void copyDs(ODLDatastore<? extends ODLTableReadOnly> copyFrom, ODLDatastoreAlterable<? extends ODLTableAlterable> copyTo, long rowFlagsToCopy) {
		for(ODLTableReadOnly tableToCopy: getTables(copyFrom)){
			
			// don't duplicate table names...
			if(findTable(copyTo, tableToCopy.getName())!=null){
				continue;
			}
			
			ODLTableAlterable copiedTable = copyTable(tableToCopy,copyTo);
			
			// preserve table level flags
			copiedTable.setFlags( tableToCopy.getFlags());
			
			// preserve column flags
			int nc = tableToCopy.getColumnCount();
			for(int col=0;col < nc ; col++){
				long colFlags = tableToCopy.getColumnFlags(col);
				copiedTable.setColumnFlags(col, colFlags);
			}
			
			// preserve the row flags we're told to...
			int nr=tableToCopy.getRowCount();
			for(int row=0;row< nr ; row++){
				long originalRowId = tableToCopy.getRowId(row);
				long copiedRowId = copiedTable.getRowId(row);
				long flags = tableToCopy.getRowFlags(originalRowId);
				flags &= rowFlagsToCopy;
				copiedTable.setRowFlags(flags, copiedRowId);
			}
		}
		
	}

	@Override
	public boolean getTableDefinitionExists(ODLTableDefinition dfn, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, boolean checkFieldTypes, ExecutionReport report) {
		ODLTableDefinition found = findTable(ds, dfn.getName());
		if(found==null){
			if(report!=null){
				report.log("Table " + dfn.getName() + " was not found.");
			}
			return false;
		}
		ColumnNameMatch matcher = new ColumnNameMatch(dfn, found); 
		if(matcher.getUnmatchedInA().size()>0){
			if(report!=null){
				StringBuilder builder = new StringBuilder();
				builder.append("The following field(s) were not found in table " + dfn.getName() + ": " );
				int count=0;
				for(int i : matcher.getUnmatchedInA().toArray()){
					if(count>0){
						builder.append(", ");
					}
					builder.append(dfn.getColumnName(i));
					count++;
				}
				report.log(builder.toString());
			}
			return false;
		}
		
		if(checkFieldTypes){
			int nbFailed=0;
			StringBuilder builder = null;
			for(int i =0 ; i < dfn.getColumnCount() ; i++){
				if(dfn.getColumnType(i)!=found.getColumnType(matcher.getMatchForA(i))){
					if(nbFailed==0){
						builder=new StringBuilder();
						builder.append("The following field(s) in table " + dfn.getName() + " had the wrong type: ");
					}else{
						builder.append(", ");
					}
					builder.append(dfn.getColumnName(i));
					nbFailed++;
					
				}
			}
			if(nbFailed>0){
				if(report!=null){
					report.log(builder.toString());
				}
				return false;
			}
		}
		
		return true;
	}

	@Override
	public <T extends ODLTableReadOnly> T adaptToTableUsingNames(ODLDatastore<? extends T> ds, String fromTable, ODLTableDefinition toTable, ExecutionReport report) {
		
		AdapterConfig adapterConfig = new AdapterConfig();
		AdapterConfig.addSameNameTable(toTable, adapterConfig);
		adapterConfig.getTable(0).setFromTable(fromTable);
		ODLDatastore<?extends T> ret=AdapterBuilderUtils.createSimpleAdapter(ds, adapterConfig, report);
		if(report.isFailed()){
			return null;
		}
		
		return ret.getTableAt(0);
	}



}
