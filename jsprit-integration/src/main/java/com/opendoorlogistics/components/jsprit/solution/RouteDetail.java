/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.solution;

import java.util.ArrayList;
import java.util.List;

import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;

import com.opendoorlogistics.components.jsprit.RowWriter;
import com.opendoorlogistics.components.jsprit.VRPBuilder.TravelCostType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.RouteDetailsTableDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.RowVehicleIndex;

public class RouteDetail {
	final public long [] capacity;
	final public long [] capacityViolation;
	final public long [] deliveredQuantities;
	public double costPerHour;
	public double costPerKm;
	public long deliveriesCount;
	public double endTime;
	public Double endTimeWindow;
	final public long [] pickedUpQuantities;
	public long pickupsCount;
	public long hasViolation;	
	final public long [] startQuantities;
	public double startTime;
	public Double startTimeWindow;
	final public List<StopDetail> stops = new ArrayList<>();
	public long stopsCount;
	public double time;
	public double timeWindowViolation=0;
	public double [] travelCosts = new double[TravelCostType.values().length];
	public String vehicleId;
	public String vehicleName;	
	public double waitingTime=0;
	public TemporaryRouteInto temp = new TemporaryRouteInto();
	public String skills;
	public double speedMultiplier;
	
	public RouteDetail(int nbQuantities) {
		capacity= new long[nbQuantities];
		capacityViolation = new long[nbQuantities];
		startQuantities = new long[nbQuantities];
		pickedUpQuantities = new long[nbQuantities];
		deliveredQuantities = new long[nbQuantities];
	}
	
	public static class TemporaryRouteInto{
		public RowVehicleIndex rvi;
		public Vehicle jspritVehicle;
		public VehicleRoute jspritRoute;
	}
	
	public void writeDetails(RouteDetailsTableDfn dfn, RowWriter w) {
		w.write(capacity, dfn.capacity);
		w.write(capacityViolation, dfn.capacityViolation);
		w.write(deliveredQuantities, dfn.deliveredQuantities);
		w.write(deliveriesCount, dfn.deliveriesCount);
		w.write(endTime, dfn.endTime);
		w.write(endTimeWindow, dfn.endTimeWindow);
		w.write(pickupsCount, dfn.pickupsCount);
		w.write(pickedUpQuantities, dfn.pickedUpQuantities);
		w.write(startQuantities, dfn.startQuantities);
		w.write(startTime, dfn.startTime);
		w.write(startTimeWindow, dfn.startTimeWindow);
		w.write(stopsCount, dfn.stopCount);
		w.write(time, dfn.time);
		w.write(timeWindowViolation, dfn.timeWindowViolation);
		for(TravelCostType type:TravelCostType.values()){
			double value =travelCosts[type.ordinal()]; 
			if(type == TravelCostType.DISTANCE_KM){
				value /=1000;
			}
			w.write(value, dfn.travelCosts[type.ordinal()]);
		}
		w.write(vehicleId, dfn.vehicleId);
		w.write(vehicleName, dfn.vehicleName);
		w.write(waitingTime, dfn.waitingTime);
		w.write(hasViolation, dfn.hasViolation);
		w.write(skills, dfn.skills);
		w.write(speedMultiplier, dfn.speedMultiplier);
	}
	
	
}
