/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.tabledefinitions;

import static com.opendoorlogistics.api.components.PredefinedTags.STOP_ID;
import static com.opendoorlogistics.api.components.PredefinedTags.VEHICLE_ID;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;

public class StopOrderTableDfn extends TableDfn{
	private final ODLApi api;
	final public int vehicleid;
	final public int stopid;
	
	public StopOrderTableDfn(ODLApi api,ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, String tablename) {
		super(ds, tablename);
		this.api = api;
		vehicleid = addStrColumn(VEHICLE_ID);
		table.setColumnFlags(vehicleid, table.getColumnFlags(vehicleid) | TableFlags.FLAG_IS_REPORT_KEYFIELD);
		
		stopid = addStrColumn(STOP_ID);
		
	}
	
	public StopOrderTableDfn(ODLApi api,ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds) {
		this(api,ds, "Stop-order");
	}
	
	public void onRowException(String messagePrefix, int row){
		throw new RuntimeException(messagePrefix + " on route-order table row " + (row + 1) + ".");	
	}
	
	public String getStopId(ODLTableReadOnly table, int row){
		String ret =(String) table.getValueAt(row, stopid);
		if(api.stringConventions().isEmptyString(ret)){
			onRowException("Empty " + PredefinedTags.STOP_ID, row);
		}
		return ret;
	}
	
	public String getVehicleId(ODLTableReadOnly table, int row){
		String ret =(String) table.getValueAt(row, vehicleid);
		if(api.stringConventions().isEmptyString(ret)){
			onRowException("Empty " + PredefinedTags.VEHICLE_ID, row);
		}
		return ret;
	}
}
