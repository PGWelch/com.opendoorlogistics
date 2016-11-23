/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.solution;

import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.components.jsprit.VRPBuilder.BuiltStopRec;
import com.opendoorlogistics.components.jsprit.VRPBuilder.TravelCostType;
import com.opendoorlogistics.components.jsprit.RowWriter;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopDetailsTableDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.RowVehicleIndex;

public class StopDetail extends StopOrder {
	public class TemporaryStopInfo {
		public double earliestArrival = Double.NEGATIVE_INFINITY;
		public double latestArrival = Double.POSITIVE_INFINITY;
		public String locationId;
		public RowVehicleIndex rowVehicleIndex;
	//	public boolean isUnbalancedPickupDelivery;
		public BuiltStopRec builtStopRec;
		public int rowNumberInStopOrderTable;
		public TourActivity jspritTourActivity;
	}

	final public long[] arrivalQuantities;
	final public long[] arrivalCapacityViolation;
	public double arrivalTime;
	public Double endTimeWindow;
	public ODLGeom incomingPath;
	public String type;
	final public long[] leaveQuantities;
	final public long[] leaveCapacityViolation;
	
	public String jobId;
	
	public double leaveTime;
	public ODLGeom outgoingPath;
	public Double startTimeWindow;
	public String stopAddress;
	public double stopDuration;
	public LatLong stopLatLong;
	public String stopName;
	public long stopNumber = -1;
	final public long[] stopQuantities;
	final public TemporaryStopInfo temporary = new TemporaryStopInfo();
	public double timeWindowViolation;
	final public double[] totalTravelCost = new double[TravelCostType.values().length];
	final public double[] travelCost = new double[TravelCostType.values().length];
	public String vehicleName;
	public double waitingTime;
	public long hasViolation;
	public String requiredSkills;
	
	public StopDetail(int nbQuantities) {
		stopQuantities = new long[nbQuantities];
		arrivalQuantities = new long[nbQuantities];
		arrivalCapacityViolation = new long[nbQuantities];
		leaveQuantities = new long[nbQuantities];
		leaveCapacityViolation= new long[nbQuantities];
	}

	// public void writeRouteOrder(RouteOrderTableDfn r)

	public void writeDetails(StopDetailsTableDfn dfn, RowWriter writer) {
		writeRouteOrder(dfn, writer);
		
		writer.write(jobId, dfn.jobId);
		
		// travel costs
		for(TravelCostType type:TravelCostType.values()){
			double value =travelCost[type.ordinal()]; 
			double total = totalTravelCost[type.ordinal()];
			if(type == TravelCostType.DISTANCE_KM){
				value /=1000;
				total/=1000;
			}
			writer.write(value, dfn.travelCosts[type.ordinal()]);
			writer.write(total, dfn.totalTravelCosts[type.ordinal()]);
		}		
//		writer.write(travelCost, dfn.travelCosts);
//		writer.write(totalTravelCost, dfn.totalTravelCosts);

		writer.write(hasViolation, dfn.hasViolation);
		
		// quantities
		writer.write(stopQuantities, dfn.stopQuantities);
		writer.write(arrivalQuantities, dfn.arrivalQuantities);
		writer.write(arrivalCapacityViolation, dfn.arrivalCapacityViolations);
		writer.write(leaveQuantities, dfn.leaveQuantities);
		writer.write(leaveCapacityViolation, dfn.leaveCapacityViolations);

		// time windows
		writer.write(startTimeWindow, dfn.timeWindowStart);
		writer.write(endTimeWindow, dfn.timeWindowEnd);
		writer.write(timeWindowViolation, dfn.timeWindowViolation);

		// misc
		writer.write(type, dfn.type);
		writer.write(vehicleName, dfn.vehicleName);
		writer.write(stopName, dfn.stopName);
		writer.write(stopNumber != -1 ? new Long(stopNumber) : null, dfn.stopNumber);
		writer.write(stopAddress, dfn.stopAddress);
		writer.write(arrivalTime, dfn.arrivalTime);
		writer.write(leaveTime, dfn.leaveTime);
		writer.write(waitingTime, dfn.waitingTime);
		writer.write(stopDuration, dfn.stopDuration);
		writer.write(requiredSkills, dfn.requiredSkills);
		
		if(stopLatLong!=null){
			writer.write(stopLatLong.getLatitude(), dfn.stopLatLong.latitude);
			writer.write(stopLatLong.getLongitude(), dfn.stopLatLong.longitude);			
		}
		writer.write(incomingPath, dfn.incomingPath);
		writer.write(outgoingPath, dfn.outgoingPath);

	}
}
