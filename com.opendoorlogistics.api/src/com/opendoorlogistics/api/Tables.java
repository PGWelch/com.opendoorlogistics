/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

/**
 * Provides utility functions to create and manipulate tables and datastores
 * @author Phil
 *
 */
public interface Tables {
	ODLTableDefinitionAlterable copyTableDefinition(ODLTableDefinition copyThis, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> copyTo);

	ODLTableAlterable createTable(ODLTableDefinition tableDefinition);

	ODLTableAlterable copyTable(ODLTableReadOnly copyThis, ODLDatastoreAlterable<? extends ODLTableAlterable> copyTo);

	ODLDatastoreAlterable<? extends ODLTableAlterable> copyDs(ODLDatastore<? extends ODLTableReadOnly> ds);
	
	void copyColumnDefinition(ODLTableDefinition source, int sourceCol, ODLTableDefinitionAlterable destination);
	
	/**
	 * Copy a row between identical tables
	 * @param from
	 * @param rowIndex
	 * @param to
	 */
	void copyRow(ODLTableReadOnly from, int rowIndex, ODLTable to);

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
}
