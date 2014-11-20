/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.tabledefinitions;

import static com.opendoorlogistics.api.components.PredefinedTags.*;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.components.jsprit.VRPConfig;
import com.opendoorlogistics.components.jsprit.VRPBuilder.TravelCostType;

/**
 * Class storing the column indices of various details for each stop on a route
 * 
 * @author Phil
 *
 */
public class StopDetailsTableDfn extends StopOrderTableDfn {
	public final int jobId;
	public final int[] arrivalQuantities;
	public final int[] arrivalCapacityViolations;
	public final int arrivalTime;
	public final int hasViolation;
	public final int incomingPath;
	public final int[] leaveQuantities;
	public final int[] leaveCapacityViolations;
	// public final int arrivalDistance;
	public final int leaveTime;
	public final int outgoingPath;
	public final int stopAddress;
	public final int stopDuration;
	public final LatLongDfn stopLatLong;
	public final int stopName;
	public final int stopNumber;
	public final int[] stopQuantities;
	public final int requiredSkills;
	public final int timeWindowEnd;
	public final int timeWindowStart;

	public final int timeWindowViolation;
	public final int[] totalTravelCosts = new int[3];
	public final int[] travelCosts = new int[3];
	public final int type;
	public final int vehicleName;
	public final int waitingTime;

	public StopDetailsTableDfn(ODLApi api,ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, VRPConfig config) {
		super(api,ds, "Stop-Details");
		jobId = addStrColumn(JOB_ID);
		
		vehicleName = addStrColumn(VEHICLE_NAME);		
		stopNumber = addColumn(ODLColumnType.LONG, STOP_NUMBER);
		stopName = addStrColumn(STOP_NAME);
		type = addColumn(ODLColumnType.STRING, TYPE);		
		stopAddress = addStrColumn(STOP_ADDRESS);
		stopLatLong = new LatLongDfn(api,table, "stop-");
		requiredSkills = addStrColumn("required-skills");
		hasViolation = addLngColumn("has-violation");
		
		arrivalTime = addTimeColumn(ARRIVAL_TIME);
		waitingTime = addTimeColumn(PredefinedTags.WAITING_TIME);
		stopDuration = addTimeColumn(STOP_DURATION);
		leaveTime = addTimeColumn(LEAVE_TIME);
		timeWindowStart = addTimeColumn(START_TIME_WINDOW);
		timeWindowEnd = addTimeColumn(END_TIME_WINDOW);
		timeWindowViolation = addTimeColumn(TIME_WINDOW_VIOLATION);
		
		arrivalQuantities = addQuantities(ARRIVAL_QUANTITY, config);
		arrivalCapacityViolations = addQuantities("arrival-capacity-violation", config);
		stopQuantities = addQuantities(STOP_QUANTITY, config);
		leaveQuantities = addQuantities(LEAVE_QUANTITY, config);
		leaveCapacityViolations = addQuantities("leave-capacity-violation", config);
				
		travelCosts[TravelCostType.COST.ordinal()] = addDblColumn(TRAVEL_COST);
		travelCosts[TravelCostType.DISTANCE_KM.ordinal()] = addDblColumn(TRAVEL_KM);
		travelCosts[TravelCostType.TIME.ordinal()] = addTimeColumn(TRAVEL_TIME);
		
		totalTravelCosts[TravelCostType.COST.ordinal()] = addDblColumn(TOTAL_TRAVEL_COST);
		totalTravelCosts[TravelCostType.DISTANCE_KM.ordinal()] = addDblColumn(TOTAL_TRAVEL_KM);
		totalTravelCosts[TravelCostType.TIME.ordinal()] = addTimeColumn(TOTAL_TRAVEL_TIME);

		incomingPath = addColumn(ODLColumnType.GEOM, PredefinedTags.INCOMING_PATH);
		outgoingPath = addColumn(ODLColumnType.GEOM, PredefinedTags.OUTGOING_PATH);
		
	}

}
