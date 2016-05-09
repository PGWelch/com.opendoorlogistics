/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.tabledefinitions;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.components.jsprit.VRPConfig;

public class InputTablesDfn {
	public final ODLDatastore<? extends ODLTableDefinition> ds;
	public final StopsTableDefn stops;
	public final VehiclesTableDfn vehicles;
	public final StopOrderTableDfn stopOrder;
	public final List<TableDfn> allTables = new ArrayList<>();
	
	public InputTablesDfn(ODLApi api,VRPConfig config){
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> alterable = api.tables().createAlterableDs();
		stops = new StopsTableDefn(api,alterable, config);
		allTables.add(stops);
		
		vehicles =new VehiclesTableDfn(api,alterable,config);
		allTables.add(vehicles);
		
		stopOrder = new StopOrderTableDfn(api,alterable);
		allTables.add(stopOrder);
		ds = alterable;
	}
	
}
