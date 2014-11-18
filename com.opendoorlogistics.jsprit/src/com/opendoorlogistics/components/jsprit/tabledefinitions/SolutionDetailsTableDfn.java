/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.tabledefinitions;

import static com.opendoorlogistics.api.components.PredefinedTags.*;

import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.components.jsprit.VRPConfig;
import com.opendoorlogistics.components.jsprit.VRPBuilder.TravelCostType;

public class SolutionDetailsTableDfn extends TableDfn{
	public final int unassignedStops;
	public final int routesCount;
	public final int []capacityViolation;
	public final int deliveriesCount;
	public final int []deliveredQuantities;
	public final int [] pickedUpQuantities;
	public final int pickupsCount;
	public final int assignedStopsCount;
	public final int time;
	public final int timeWindowViolation;
	public final int [] travelCosts = new int[TravelCostType.values().length];
	public final int waitingTime;
	public final int hasViolation;

	public SolutionDetailsTableDfn(ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, VRPConfig config) {
		super(ds, "Solution-Details");
		routesCount = addStrColumn("used-routes");
		unassignedStops = addStrColumn("unassigned-stops");
		
		assignedStopsCount = addLngColumn("assigned-stops");
		
		capacityViolation = addQuantities("capacity-violation", config);
		hasViolation = addLngColumn("has-violation");
		
		deliveriesCount = addLngColumn("deliveries");
		deliveredQuantities	= addQuantities("delivered-quantity", config);
		pickedUpQuantities = addQuantities("picked-up-quantity", config);
		pickupsCount = addLngColumn("pickups");
		time = addTimeColumn(TIME);
		timeWindowViolation = addTimeColumn(TIME_WINDOW_VIOLATION);
		travelCosts[TravelCostType.COST.ordinal()] = addDblColumn(TRAVEL_COST);
		travelCosts[TravelCostType.DISTANCE_KM.ordinal()] = addDblColumn(TRAVEL_KM);
		travelCosts[TravelCostType.TIME.ordinal()] = addTimeColumn(TRAVEL_TIME);
		waitingTime = addTimeColumn(WAITING_TIME);

	}
	
	

}
