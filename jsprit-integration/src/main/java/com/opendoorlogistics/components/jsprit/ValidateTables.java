/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.util.Map;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.components.jsprit.tabledefinitions.InputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn.StopType;

/**
 * Performs all validation on the input tables that can't be done for a single row at a time
 * @author Phil
 *
 */
public class ValidateTables {
	private final InputTablesDfn dfn;
	private final ODLApi api;
	
	public ValidateTables(ODLApi api,InputTablesDfn dfn) {
		this.api = api;
		this.dfn = dfn;
	}

	void validate(ODLDatastore<? extends ODLTable> ioDs){
		// check vehicle and stop ids are unique by building maps..
		ODLTableReadOnly stops = ioDs.getTableByImmutableId(dfn.stops.tableId);
		Map<String,Integer> stopIdByRow = dfn.stops.getStopIdMap(stops);
		dfn.vehicles.getVehicleIdToRowIndex(ioDs.getTableByImmutableId(dfn.vehicles.tableId));
		
		
		// check multi-stop jobs are correct by building the grouped map (validation happens in the get)
		dfn.stops.getGroupedByMultiStopJob(stops,true);
		
		// check no job id is also used as a stop id
		int n = stops.getRowCount();
		for(int row =0 ; row<n;row++){
			String jobId = dfn.stops.getJobId(stops, row);
			if(api.stringConventions().isEmptyString(jobId)==false && stopIdByRow.get(jobId)!=null){
				throw new RuntimeException(PredefinedTags.JOB_ID + " " + jobId + " is also used as a " + PredefinedTags.STOP_ID + ".");
			}
		}
	}


}
