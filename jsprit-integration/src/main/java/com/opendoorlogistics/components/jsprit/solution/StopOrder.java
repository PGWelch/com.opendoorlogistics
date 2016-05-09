/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.solution;

import com.opendoorlogistics.components.jsprit.RowWriter;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopOrderTableDfn;

public class StopOrder {
	public String stopId;
	public String vehicleId;
	
	public void writeRouteOrder(StopOrderTableDfn dfn, RowWriter writer){
		writer.write(vehicleId, dfn.vehicleid);
		writer.write(stopId, dfn.stopid);
	}

	@Override
	public String toString() {
		return "[stopId=" + stopId + ", vehicleId=" + vehicleId + "]";
	}
	
	
}
