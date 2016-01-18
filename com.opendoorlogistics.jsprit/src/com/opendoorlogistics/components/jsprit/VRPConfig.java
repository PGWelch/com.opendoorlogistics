/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.distances.DistancesConfiguration;


@XmlRootElement(name = "VRPConfig")
final public class VRPConfig implements Serializable {
	
	enum BooleanOptions{
		FLEET_SIZE_IS_INFINITE("Fleet size is infinite"),
		FORCE_ALL_DELIVERIES_BEFORE_PICKUPS("Do all deliveries before all pickups"),
		OUTPUT_STRAIGHT_LINES_BETWEEN_STOPS("Output straight lines between stops to the map"),
		;
	//	USE_PICKUP_DELIVER,
	//	USE_SERVICE_DURATIONS,		
	//	USE_STOP_TIME_WINDOWS,
	//	USE_VEHICLE_TIME_WINDOW,
	//	VEHICLE_HAS_DIFFERENT_END_LOCATION,
		
		public final String displayName;
		
		private BooleanOptions(String displayName) {
			this.displayName = displayName;
		}
	}
	
	private final boolean [] booleans = new boolean[BooleanOptions.values().length];
	private DistancesConfiguration distances = new DistancesConfiguration();
	private int nbIterations=VRPConstants.DEFAULT_NB_ITERATIONS;
	private int nbQuantities=1;
	private int nbThreads=0;
	
//	public boolean isStopTimeWindows() {
//		return getBool(BooleanOptions.USE_STOP_TIME_WINDOWS);
//	}
//	public void setStopTimeWindows(boolean timeWindows) {
//		setBool(BooleanOptions.USE_STOP_TIME_WINDOWS, timeWindows);
//	}
	
//	public boolean isLinkedPickupDeliver() {
//		return getBool(BooleanOptions.USE_PICKUP_DELIVER);
//	}
//	
//	@XmlAttribute	
//	public void setLinkedPickupDeliver(boolean pickupDeliver) {
//		setBool(BooleanOptions.USE_PICKUP_DELIVER, pickupDeliver);
//	}
	
	public int getNbQuantities() {
		return nbQuantities;
	}
	
	@XmlAttribute	
	public void setNbQuantities(int nbQuantities) {
		this.nbQuantities = nbQuantities;
	}
	
//	public boolean isServiceDurations() {
//		return getBool(BooleanOptions.USE_SERVICE_DURATIONS);
//	}
//	
//	@XmlAttribute	
//	public void setServiceDurations(boolean serviceTimes) {
//		setBool(BooleanOptions.USE_SERVICE_DURATIONS, serviceTimes);
//	}
//	
//	public boolean isVehicleTimeWindow() {
//		return getBool(BooleanOptions.USE_VEHICLE_TIME_WINDOW);
//	}
	
//	@XmlAttribute	
//	public void setVehicleTimeWindow(boolean vehicleTimeWindows) {
//		setBool(BooleanOptions.USE_VEHICLE_TIME_WINDOW, vehicleTimeWindows);
//	}
	
//	public boolean isVehicleHasDifferentEndLocation() {
//		return getBool(BooleanOptions.VEHICLE_HAS_DIFFERENT_END_LOCATION);
//	}
//	
//	@XmlAttribute	
//	public void setVehicleHasDifferentEndLocation(boolean vehicleHasDifferentEndLocation) {
//		setBool(BooleanOptions.VEHICLE_HAS_DIFFERENT_END_LOCATION, vehicleHasDifferentEndLocation);
//	}
	
	public boolean isInfiniteFleetSize() {
		return getBool(BooleanOptions.FLEET_SIZE_IS_INFINITE);
	}
	
	@XmlAttribute		
	public void setInfiniteFleetSize(boolean infiniteFleetSize) {
		setBool(BooleanOptions.FLEET_SIZE_IS_INFINITE, infiniteFleetSize);
	}
	
	public boolean isDeliveriesBeforePickups() {
		return getBool(BooleanOptions.FORCE_ALL_DELIVERIES_BEFORE_PICKUPS);
	}
	
	@XmlAttribute	
	public void setDeliveriesBeforePickups(boolean deliveriesBeforePickups) {
		setBool(BooleanOptions.FORCE_ALL_DELIVERIES_BEFORE_PICKUPS, deliveriesBeforePickups);

	}
	
	public boolean isOutputStraightLines() {
		return getBool(BooleanOptions.OUTPUT_STRAIGHT_LINES_BETWEEN_STOPS);
	}
	
	@XmlAttribute	
	public void setOutputStraightLines(boolean outputStraightLines) {
		setBool(BooleanOptions.OUTPUT_STRAIGHT_LINES_BETWEEN_STOPS, outputStraightLines);

	}
	
	public DistancesConfiguration getDistances() {
		return distances;
	}
	
	@XmlElement
	public void setDistances(DistancesConfiguration distances) {
		this.distances = distances;
	}
	
	public boolean getBool(BooleanOptions bo){
		return booleans[bo.ordinal()];
	}
	
	public void setBool(BooleanOptions bo, boolean val){
		booleans[bo.ordinal()]=val;
	}
	
	public int getNbIterations() {
		return nbIterations;
	}
	
	@XmlElement
	public void setNbIterations(int nbIterations) {
		this.nbIterations = nbIterations;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	@XmlAttribute	
	public void setNbThreads(int nbThreads) {
		this.nbThreads = nbThreads;
	}
	
	
	
}
