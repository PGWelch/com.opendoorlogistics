package com.opendoorlogistics.studio.tables.custom;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.BeanTableMapping;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.execution.adapters.AdapterBuilderUtils;

public class BeanMappingInfo<T extends BeanMappedRow> {
	private final ODLApi api;
	private final BeanTableMapping mapping;
	private final ODLDatastoreUndoable<? extends ODLTableAlterable>  ds;
//	private final int tableId;
	
	public BeanMappingInfo(ODLApi api,Class<T> cls,ODLDatastoreUndoable<? extends ODLTableAlterable> ds) {
		this.api = api;
		this.ds = ds;
	//	this.tableId = tableId;
		this.mapping = api.tables().mapBeanToTable(cls);
	}

	public ODLApi getApi() {
		return api;
	}

	public BeanTableMapping getMapping() {
		return mapping;
	}

	public ODLDatastoreUndoable<? extends ODLTableAlterable>  getDs() {
		return ds;
	}

//	public int getTableId() {
//		return tableId;
//	}

	public ODLTable createTableAdapter(ExecutionReport report){
		ODLDatastore<?extends ODLTable> adapted= adapt(report);
		if(!report.isFailed() && adapted!=null){
			return adapted.getTableAt(0);
		}
		return null;
	}
	
//	public T getObject(int row, ExecutionReport report){
//		ODLTableReadOnly table = getAdaptedTable(report);
//		if(!report.isFailed() && table!=null && row < table.getRowCount()){
//			return mapping.readObjectFromTableByRow(table, row, report);
//		}
//		
//		return null;
//	}
//	
//	public void setObject(T obj, int row, ExecutionReport report){
//		ODLTable table = getAdaptedTable(report);
//		if(!report.isFailed() && table!=null && row < table.getRowCount()){
//			mapping.writeObjectToTable(obj, table, row);
//		}
//		
//	}
	
	private ODLDatastore<?extends ODLTable> adapt(ExecutionReport report){
		ODLTableDefinition destination=mapping.getTableDefinition();
		AdapterConfig adapterConfig = new AdapterConfig();
		AdapterConfig.addSameNameTable(destination, adapterConfig);
		ODLDatastore<?extends ODLTable> ret=AdapterBuilderUtils.createSimpleAdapter(ds, adapterConfig, report);
		if(report.isFailed()){
			return null;
		}
		return ret;
	};
	
	public ODLTable getRawTable(){
		return api.tables().findTable(ds, mapping.getTableDefinition().getName());
	}
}
