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
	
	Class<? extends BeanMappedRow> getBeanClass();

	boolean isReadFailsOnDisallowedNull();
	
	/**
	 * Set the conversion to fail if we encounter a null value where one is not allowed.
	 * This is true by default. If you are using the beans in a UI where the user
	 * has the chance to correct the bean, set it to false or you won't be able to read it. 
	 * @param failIfNulLValueNotAllowed
	 */
	void setReadFailsOnDisallowedNull(boolean failIfNulLValueNotAllowed) ;

}
