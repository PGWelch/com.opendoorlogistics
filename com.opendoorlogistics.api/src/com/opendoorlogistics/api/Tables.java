/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLFlatDatastore;
import com.opendoorlogistics.api.tables.ODLFlatDatastoreExt;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.BeanTableMapping;

/**
 * Provides utility functions to create and manipulate tables and datastores
 * @author Phil
 *
 */
public interface Tables {
	ODLTableDefinitionAlterable copyTableDefinition(ODLTableDefinition copyThis, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> copyTo);

	void copyTableDefinition(ODLTableDefinition copyThis, ODLTableDefinitionAlterable copyInto);

	ODLTableAlterable createTable(ODLTableDefinition tableDefinition);

	ODLTableAlterable copyTable(ODLTableReadOnly copyThis, ODLDatastoreAlterable<? extends ODLTableAlterable> copyTo);

	ODLDatastoreAlterable<? extends ODLTableAlterable> copyDs(ODLDatastore<? extends ODLTableReadOnly> ds);

	/**
	 * Adapt the from table in the input datastore to match the to table based on matching field names.
	 * Failure to adapt will be logged in the report object.
	 * @param ds
	 * @param fromTable
	 * @param toTable
	 * @param report
	 * @return
	 */
	<T extends ODLTableReadOnly> T adaptToTableUsingNames(ODLDatastore<? extends T> ds, String fromTable,ODLTableDefinition toTable, ExecutionReport report);
	
	/**
	 * Copy the datastore. All table and column level flags are preserved, row level flags are optionally preserved.
	 * @param copyFrom
	 * @param copyTo
	 * @param rowFlagsToCopy Row-level flags which should be preserved in the copy.
	 */
	void copyDs(ODLDatastore<? extends ODLTableReadOnly> copyFrom, ODLDatastoreAlterable<? extends ODLTableAlterable> copyTo, long rowFlagsToCopy);

	void copyColumnDefinition(ODLTableDefinition source, int sourceCol, ODLTableDefinitionAlterable destination);

	void copyColumnDefinition(ODLTableDefinition source, int sourceCol, ODLTableDefinitionAlterable destination, int destinationCol);

	/**
	 * Copy a row between identical tables
	 * @param from
	 * @param rowIndex
	 * @param to
	 */
	void copyRow(ODLTableReadOnly from, int rowIndex, ODLTable to);

	void copyRow(ODLTableReadOnly from, int fromRowIndex, ODLTable to, int toRowIndex);

	void copyRowById(ODLTableReadOnly from, long rowId, ODLTable to);

	/**
	 * Add a row to the table with the input values
	 * @param table
	 * @param values
	 * @return The index of the row
	 */
	int addRow(ODLTable table,Object...values);
	
	ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> createDefinitionDs();
	ODLDatastoreAlterable<? extends ODLTableAlterable> createAlterableDs();
	ODLDatastore<? extends ODLTable> createExampleDs();
	
	ODLTableAlterable createAlterableTable(String name);
	
	void setColumnIsOptional(ODLTableDefinitionAlterable table, int col, boolean optional);
	
	void clearTable(ODLTable table);
	
	/**
	 * Clear all tables from the datastore. Tables must be alterable in the input datastore
	 * as we need to remove read-only flags before deletion is allowed.
	 * @param ds
	 */
	void clearDatastore(ODLDatastoreAlterable<? extends ODLTableAlterable> ds);
	
	public enum KeyValidationMode{
		REMOVE_CORRUPT_FOREIGN_KEY,
		THROW_UNCHECKED_EXCEPTION
	}
	
	/**
	 * Validate the foreign key relation and either remove or throw exception if record
	 * found with no foreign key corresponding to record in master key column.
	 * @param primaryKeyTable
	 * @param primaryKeyColIndx
	 * @param foreignKeyTable
	 * @param foreignKeyIndex
	 * @param mode
	 */
	void validateForeignKey(ODLTableReadOnly primaryKeyTable, int primaryKeyColIndx, ODLTable foreignKeyTable, int foreignKeyIndex, KeyValidationMode mode);
	
	int findColumnIndex(ODLTableDefinition table,String name);
	
	<T extends ODLTableDefinition> T findTable(ODLDatastore<T> ds,String tableName);
	
	/**
	 * Get the column type corresponding to the java class, or null
	 * if the class is not supported.
	 * @param externalType
	 * @return
	 */
	ODLColumnType getColumnType(Class<?> externalType);
	
	/*
	 * Get the column type from its name
	 */
	ODLColumnType getColumnType(String columnTypeName);
	
	/**
	 * Find the table index using a standardised string compare
	 * @param ds
	 * @param tableName
	 * @return Table index or -1 if not found
	 */
	int findTableIndex(ODLDatastore<? extends ODLTableDefinition> ds, String tableName);

	/**
	 * Add tables and fields if not already existing from the input schema
	 * @param schema
	 * @param ds
	 * @param changeFieldTypes
	 */
	void addTableDefinitions(ODLDatastore<? extends ODLTableDefinition> schema, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, boolean changeFieldTypes);

	/**
	 * Add table and fields if not already existing from the input schema
	 * @param schema
	 * @param ds
	 * @param changeFieldTypes
	 */
	void addTableDefinition( ODLTableDefinition schema, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, boolean changeFieldTypes);

	/**
	 * Does the table exist in the datastore, are all its fields present and are they they correct type? (if checking type)
	 * @param schema
	 * @param ds
	 * @param checkFieldTypes
	 * @param ExecutionReport report Use this to record what's missing (can be null)
	 * @return
	 */
	boolean getTableDefinitionExists(ODLTableDefinition schema, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, boolean checkFieldTypes, ExecutionReport report);
	
	/**
	 * Merge the source datastore into the destination.
	 * Fields and tables are added as needed.
	 * @param source
	 * @param destination
	 */
	void addTablesWithData(ODLDatastore<? extends ODLTableReadOnly> source, ODLDatastoreAlterable<? extends ODLTableAlterable> destination);
	
//	ODLDatastore<? extends ODLTableDefinition> createParametersTableD
	
	ODLTableDefinition createParametersTableDefinition();
	
	/**
	 * Compare two tables
	 * @param a
	 * @param b
	 * @return Return true if (and only if) tables have identical structure and field values
	 */
	boolean isIdentical(ODLTableReadOnly a, ODLTableReadOnly b);
	
	/**
	 * Get the set of columns names. The set object supports standardised string
	 * lookup - i.e. case insensitive etc
	 * @param table
	 * @return
	 */
	Set<String> getColumnNamesSet(ODLTableDefinition table);
	
	
	Map<String, Integer> getColumnNamesMap(ODLTableDefinition table);
	
	/**
	 * Wrap a flat datastore to create a normal (hierarchical) datastore
	 * @param flatDatastore
	 * @return
	 */
	ODLDatastoreUndoable<ODLTableAlterable> unflattenDs(ODLFlatDatastoreExt flatDatastore);
	
	BeanTableMapping mapBeanToTable(Class<? extends BeanMappedRow> cls);
	
	<T extends ODLTableDefinition> List<T> getTables(ODLDatastore<T> ds);
	
	/**
	 * Modify a table column whilst transforming data as appropriate.
	 * Move the column, change its name or index
	 * @param index
	 * @param newIndx
	 * @param newName
	 * @param newType
	 * @param tableDfn
	 * @return
	 */
	boolean modifyColumn(int index, int newIndx, String newName, ODLColumnType newType,ODLTableAlterable table);
}
