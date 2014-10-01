/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.tabledefinitions;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.components.jsprit.VRPConfig;

public class OutputTablesDfn {
	public final StopDetailsTableDfn stopDetails;
	public final RouteDetailsTableDfn routeDetails; 
	public final SolutionDetailsTableDfn solutionDetails; 
	public final ODLDatastore<? extends ODLTableDefinition> ds;
	
	public OutputTablesDfn(ODLApi api,VRPConfig config){
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = api.tables().createAlterableDs();
		stopDetails = new StopDetailsTableDfn(api,ret, config);
		routeDetails = new RouteDetailsTableDfn(ret, config);
		solutionDetails = new SolutionDetailsTableDfn(ret, config);
		ds = ret;
	}
}
