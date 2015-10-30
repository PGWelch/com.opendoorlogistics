package com.opendoorlogistics.studio.tables.custom;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.BeanTableMapping;
import com.opendoorlogistics.studio.LoadedState.HasLoadedDatastore;

class BeanMappingInfo<T extends BeanMappedRow> {
	private final ODLApi api;
	private final BeanTableMapping mapping;
	private final HasLoadedDatastore hasDs;
	
	BeanMappingInfo(ODLApi api,Class<T> cls,HasLoadedDatastore hasDs) {
		this.api = api;
		this.hasDs = hasDs;
		this.mapping = api.tables().mapBeanToTable(cls);
		
		// allow tolerant reading of beans
		this.mapping.setReadFailsOnDisallowedNull(false);
	}

	ODLApi getApi() {
		return api;
	}

	BeanTableMapping getMapping() {
		return mapping;
	}

//	ODLTable createTableAdapter(ExecutionReport report){
//		ODLDatastore<?extends ODLTable> adapted= adapt(report);
//		if(!report.isFailed() && adapted!=null){
//			return adapted.getTableAt(0);
//		}
//		return null;
//	}

	ODLTable createTableAdapter(ExecutionReport report){
	
		ODLDatastoreUndoable<? extends ODLTableAlterable> ds = getDs();
		if(ds==null){
			return null;
		}
		
		String tableName=mapping.getTableDefinition().getName();
		ODLTable ret=api.tables().adaptToTableUsingNames(ds, tableName,mapping.getTableDefinition(), report);
		if(report.isFailed()){
			return null;
		}
		return ret;
	};
	
	ODLTable getRawTable(){
		ODLDatastoreUndoable<? extends ODLTableAlterable> ds = getDs();
		if(ds==null){
			return null;
		}
		
		return api.tables().findTable(ds, mapping.getTableDefinition().getName());
	}
	
	ODLDatastoreUndoable<? extends ODLTableAlterable> getDs(){
		if(hasDs.getLoadedDatastore()!=null){
			return hasDs.getLoadedDatastore().getDs();
		}
		return null;
	}
}
