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

public class RouteDetailsTableDfn extends TableDfn{
	public final int []capacity;
	public final int []capacityViolation;
	public final int deliveriesCount;
	public final int []deliveredQuantities;
	public final int endTime;
	public final int endTimeWindow;
	public final int [] pickedUpQuantities;
	public final int pickupsCount;
	public final int [] startQuantities;
	public final int startTime;
	public final int startTimeWindow;
	public final int stopCount;
	public final int time;
	public final int timeWindowViolation;
	public final int [] travelCosts = new int[TravelCostType.values().length];
	public final int vehicleId;
	public final int vehicleName;	
	public final int waitingTime;
	public final int hasViolation;
	public final int skills;
	public final int speedMultiplier;

	public RouteDetailsTableDfn(ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, VRPConfig config) {
		super(ds, "Route-Details");
		vehicleName = addStrColumn(VEHICLE_NAME);
		vehicleId = addStrColumn(VEHICLE_ID);
		skills = addStrColumn("skills");
		speedMultiplier = addDblColumn("speed-multiplier");
		table.setColumnFlags(vehicleId, table.getColumnFlags(vehicleId) | TableFlags.FLAG_IS_REPORT_KEYFIELD);
		stopCount = addLngColumn(STOPS_COUNT);
		startTime = addTimeColumn(START_TIME);
		endTime = addTimeColumn(END_TIME);
		hasViolation = addLngColumn("has-violation");
		
		capacity = addQuantities(CAPACITY, config);
		capacityViolation = addQuantities("capacity-violation", config);
		deliveriesCount = addLngColumn(DELIVERIES_COUNT);
		deliveredQuantities	= addQuantities("delivered-quantity", config);
		endTimeWindow = addTimeColumn(END_TIME_WINDOW);
		pickedUpQuantities = addQuantities("picked-up-quantity", config);
		pickupsCount = addLngColumn(PICKUPS_COUNT);
		startQuantities = addQuantities("start-quantity", config);
		startTimeWindow = addTimeColumn(START_TIME_WINDOW);
		time = addTimeColumn(TIME);
		timeWindowViolation = addTimeColumn(TIME_WINDOW_VIOLATION);
		travelCosts[TravelCostType.COST.ordinal()] = addDblColumn(TRAVEL_COST);
		travelCosts[TravelCostType.DISTANCE_KM.ordinal()] = addDblColumn(TRAVEL_KM);
		travelCosts[TravelCostType.TIME.ordinal()] = addTimeColumn(TRAVEL_TIME);
		waitingTime = addTimeColumn(WAITING_TIME);

	}
	
	

}
