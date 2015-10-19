package com.opendoorlogistics.api.tables.beans;

import java.util.List;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public interface BeanTableMapping {
	ODLTableDefinition getTableDefinition();
	
	<T extends BeanMappedRow> List<T> readObjectsFromTable(ODLTableReadOnly inputTable) ;
	
	<T extends BeanMappedRow> List<T> readObjectsFromTable(ODLTableReadOnly inputTable, ExecutionReport report);

	<T extends BeanMappedRow> T readObjectFromTableByRow(ODLTableReadOnly inputTable, int row, ExecutionReport report);

	<T extends BeanMappedRow> void writeObjectsToTable(List<T> objs, ODLTable outTable);

	<T extends BeanMappedRow> long writeObjectToTable(T obj, ODLTable outTable);

	<T extends BeanMappedRow> void writeObjectToTable(T obj, ODLTable outTable, int rowNb);
	
	<T extends BeanMappedRow> void updateTableRow(T object, ODLTable table, long rowId);

}
