/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.solution;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.components.jsprit.RowWriter;
import com.opendoorlogistics.components.jsprit.VRPBuilder.TravelCostType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.SolutionDetailsTableDfn;

public class SolutionDetail {
	public long unassignedStops;
	public long routesCount;
	final public long [] capacityViolation;
	final public long [] deliveredQuantities;
	public long deliveriesCount;
	final public long [] pickedUpQuantities;
	public long pickupsCount;
	public long assignedStopsCount;
	public long hasViolation;
	public double time;
	public double timeWindowViolation=0;
	public double [] travelCosts = new double[TravelCostType.values().length];
	public double waitingTime=0;
	public final List<RouteDetail> routes = new ArrayList<RouteDetail>();
	
	public SolutionDetail(int nbQuantities) {
		capacityViolation = new long[nbQuantities];
		pickedUpQuantities = new long[nbQuantities];
		deliveredQuantities = new long[nbQuantities];
	}
	
	public void writeDetails(SolutionDetailsTableDfn dfn, RowWriter w) {
		w.write(unassignedStops, dfn.unassignedStops);
		w.write(routesCount, dfn.routesCount);
		w.write(capacityViolation, dfn.capacityViolation);
		w.write(deliveredQuantities, dfn.deliveredQuantities);
		w.write(deliveriesCount, dfn.deliveriesCount);
		w.write(pickupsCount, dfn.pickupsCount);
		w.write(pickedUpQuantities, dfn.pickedUpQuantities);
		w.write(assignedStopsCount, dfn.assignedStopsCount);
		w.write(time, dfn.time);
		w.write(timeWindowViolation, dfn.timeWindowViolation);
		for(TravelCostType type:TravelCostType.values()){
			double value =travelCosts[type.ordinal()]; 
			if(type == TravelCostType.DISTANCE_KM){
				value /=1000;
			}
			w.write(value, dfn.travelCosts[type.ordinal()]);
		}
		w.write(waitingTime, dfn.waitingTime);
		w.write(hasViolation, dfn.hasViolation);

	}
	
	
}
