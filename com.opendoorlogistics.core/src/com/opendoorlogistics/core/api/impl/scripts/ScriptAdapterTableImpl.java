/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl.scripts;

import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.api.impl.scripts.ScriptOptionImpl.FindMode;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig.SortField;
import com.opendoorlogistics.core.utils.strings.Strings;

public class ScriptAdapterTableImpl implements ScriptAdapterTable{
	private final AdaptedTableConfig table;

	public ScriptAdapterTableImpl(AdaptedTableConfig table) {
		this.table = table;
	}
	
	protected AdapterColumnConfig getAdaptedColumn( String columnName, FindMode findMode) {
		for (AdapterColumnConfig col : table.getColumns()) {
			if (Strings.equalsStd(col.getName(), columnName)) {
				return col;
			}
		}

		if (findMode!= FindMode.CANNOT_EXIST_ANYWHERE) {
			throw new RuntimeException("Column with name not found: " + columnName);
		}
		return null;
	}	

	@Override
	public void setFormula(String columnName, String formula) {
		AdapterColumnConfig conf = getAdaptedColumn( columnName, FindMode.MUST_EXIST_IN_CURRENT_OPTION);
		setFormula(formula, conf);
	}

	private void setFormula(String formula, AdapterColumnConfig conf) {
		conf.setFormula(formula);
		conf.setUseFormula(true);
	}

	@Override
	public void setFormula(int columnIdx, String formula) {
		setFormula(formula,table.getColumn(columnIdx));
	}

	
	@Override
	public void setSourceColumn(String columnName, String sourceColumn) {
		AdapterColumnConfig conf = getAdaptedColumn(  columnName, FindMode.MUST_EXIST_IN_CURRENT_OPTION);
		conf.setSourceFields(sourceColumn, null, false);
	}

	@Override
	public void setSourceTable(String sourceTable) {
		setSourceTable(ScriptConstants.EXTERNAL_DS_NAME,sourceTable);
	}


	@Override
	public void setSourceTable(String datastoreId, String sourceTable) {
		table.setFrom(datastoreId, sourceTable);
	}

	@Override
	public void setTableFilterFormula(String formula) {
		table.setFilterFormula(formula);
	}

	@Override
	public void setSourceColumns(String[][] columnNameSourcePairs) {
		for (String[] pair : columnNameSourcePairs) {
			if (pair.length != 2) {
				throw new RuntimeException("Found column name - source pair string array not of length 2.");
			}
			setSourceColumn(pair[0], pair[1]);
		}
	}

	@Override
	public void setFormulae(String[][] columnNameFormulaPairs) {
		for (String[] pair : columnNameFormulaPairs) {
			if (pair.length != 2) {
				throw new RuntimeException("Found column name - formula pair string array not of length 2.");
			}
			setFormula(pair[0], pair[1]);
		}
	}

	@Override
	public void setTableName(String name) {
		table.setName(name);

	}
	
	@Override
	public ODLTableDefinition getTableDefinition() {
		return table;
	}

	@Override
	public int addColumn(String name, ODLColumnType type, boolean usesFormula, String source) {
		AdapterColumnConfig col = new AdapterColumnConfig(-1, null, name, type, 0);
		if(usesFormula){
			col.setUseFormula(true);
			col.setFormula(source);
		}else{
			col.setUseFormula(false);
			col.setFrom(source);
		}
		
		table.getColumns().add(col);
		return table.getColumns().size()-1;
	}

	@Override
	public int getColumnCount() {
		return table.getColumnCount();
	}

	@Override
	public String getColumnName(int i) {
		return table.getColumnName(i);
	}

	@Override
	public long getColumnFlags(String name) {
		return getAdaptedColumn(name, FindMode.MUST_EXIST_IN_CURRENT_OPTION).getFlags();

	}

	@Override
	public void setColumnFlags(String name, long flags) {
		getAdaptedColumn(name, FindMode.MUST_EXIST_IN_CURRENT_OPTION).setFlags(flags);
	}

	@Override
	public long getFlags() {
		return table.getFlags();
	}

	@Override
	public void setFlags(long flags) {
		table.setFlags(flags);
	}

	@Override
	public ODLColumnType getColumnType(int i) {
		return table.getColumnType(i);
	}

	@Override
	public void removeColumn(int i) {
		table.deleteColumn(i);
	}

	@Override
	public void setSortType(int col, ColumnSortType cst) {
		switch(cst){
		case ASCENDING:
			table.getColumn(col).setSortField(SortField.ASCENDING);
			break;
			
		case DESCENDING:
			table.getColumn(col).setSortField(SortField.DESCENDING);
			break;
			
		case NONE:
			table.getColumn(col).setSortField(SortField.NO);
			break;
		}
	}

	@Override
	public void setFetchSourceField(boolean b) {
		table.setFetchSourceFields(b);
	}

	@Override
	public ODLTableReadOnly getDataTable() {
		return this.table.getDataTable();
	}

	@Override
	public void setDataTable(ODLTableReadOnly table) {
		this.table.setDataTable(table);
	}


}
