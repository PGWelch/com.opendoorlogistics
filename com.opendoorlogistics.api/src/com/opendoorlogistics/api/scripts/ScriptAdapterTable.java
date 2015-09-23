/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.scripts;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public interface ScriptAdapterTable {

	ODLTableDefinition getTableDefinition();

	/**
	 * Set the formula for an adapted table's column
	 * @param columnName
	 * @param formula
	 * @param tableName
	 */
	void setFormula(String columnName, String formula);
	
	void setFormula(int columnIdx, String formula);

	/**
	 * Set source formulae on the adapter table
	 * @param columnNameFormulaPairs An array of pairs of strings
	 */
	void setFormulae(String[][]columnNameFormulaPairs);

	/**
	 * Set the source column for an adapted table's column
	 * @param columnName
	 * @param tableName
	 * @param formula
	 */
	void setSourceColumn(String columnName, String sourceColumn);

	/**
	 * Set source columns on the table.
	 * @param columnNameSourcePairs An array of pairs of strings
	 */
	void setSourceColumns(String[][]columnNameSourcePairs);

	void setSourceTable(String sourceTable);

	void setSourceTable(String datastoreId, String sourceTable);

	void setTableName(String name);
	
	void setTableFilterFormula(String formula);
	
	int addColumn(String name, ODLColumnType type, boolean usesFormula, String source);

	int getColumnCount();
	
	String getColumnName(int i);

	long getColumnFlags(String name);

	void setColumnFlags(String name, long flags);
	
	long getFlags();
	
	void setFlags(long flags);
	
	ODLColumnType getColumnType(int i);
	
	void removeColumn(int i);
	
	public enum ColumnSortType{
		NONE,
		ASCENDING,
		DESCENDING
	}
	
	void setSortType(int col, ColumnSortType cst);
	
	void setFetchSourceField(boolean b);
	
	ODLTableReadOnly getDataTable();
	
	void setDataTable(ODLTableReadOnly table);
	
}
