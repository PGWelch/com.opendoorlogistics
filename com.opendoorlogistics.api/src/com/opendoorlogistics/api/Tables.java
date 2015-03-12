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

	/**
	 * Add a row to the table with the input values
	 * @param table
	 * @param values
	 * @return The index of the row
	 */
	int addRow(ODLTable table,Object...values);
	
	ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> createDefinitionDs();
	ODLDatastoreAlterable<? extends ODLTableAlterable> createAlterableDs();
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
}
