/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl;

import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.ODLFactory;
import com.opendoorlogistics.core.tables.beans.BeanTypeConversion;
import com.opendoorlogistics.core.tables.utils.DatastoreComparer;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.tables.utils.ParametersTable;
import com.opendoorlogistics.core.tables.utils.TableUtils;

public class TablesImpl implements Tables {

	@Override
	public ODLTableDefinitionAlterable copyTableDefinition(ODLTableDefinition copyThis, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> copyTo) {
		return DatastoreCopier.copyTableDefinition(copyThis, copyTo);
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
		if(optional){
			flags |= TableFlags.FLAG_IS_OPTIONAL;
		}else{
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
		int row =0 ; 
		while(row<foreignKeyTable.getRowCount()){
			Object value = foreignKeyTable.getValueAt(row, foreignKeyColIndx);
			boolean missing=false;
			if(value==null){
				missing = true;
			}
			
			if(!missing){
				long[]vals = primaryKeyTable.find(primaryKeyColIndx, value);
				missing = vals==null || vals.length==0;
			}
			
			if(missing){
				switch(mode){
				case REMOVE_CORRUPT_FOREIGN_KEY:
					// delete and continue here so we don't increment row
					foreignKeyTable.deleteRow(row);
					continue;
					
				case THROW_UNCHECKED_EXCEPTION:
					throw new RuntimeException("Table \"" + foreignKeyTable.getName() + "\" has corrupt foreign key value. \"" + value + "\" cannot be found in table \"" + primaryKeyTable.getName() + "\".");
				}
			}
			row++;
		}
	}

	@Override
	public int findColumnIndex(ODLTableDefinition table,String name) {
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
	public void addTableDefinitions(ODLDatastore<? extends ODLTableDefinition> schema, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds,
			boolean changeFieldTypes) {
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


}
