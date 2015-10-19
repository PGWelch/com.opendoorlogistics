/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.data;

import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnDescription;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultDoubleValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultLongValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.components.cluster.BasicCluster;

final public class Cluster extends BasicCluster {
	private double capacity=1;
	private long fixedLocation=0;
	
	private String locationKey=null;
	
	// output only
	private double assignedQuantity;			
	private double assignedCapacityViolation;		
	private double assignedTravelCost;
	
	public double getCapacity() {
		return capacity;
	}
	
	@ODLColumnOrder(1)
	@ODLDefaultDoubleValue(100)
	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	
	public long getFixedLocation() {
		return fixedLocation;
	}
	
	@ODLColumnOrder(2)	
	@ODLDefaultLongValue(0)
	@ODLColumnDescription("Set to true if you want the location to be fixed, otherwise algorithm may change it.")
	@ODLColumnName("is-fixed-location")			
	public void setFixedLocation(long fixedLocation) {
		this.fixedLocation = fixedLocation;
	}
	
	public String getLocationKey() {
		return locationKey;
	}
	
	@ODLColumnOrder(5)
	@ODLColumnDescription("Identifier of the location this cluster is assigned to.")
	@ODLNullAllowed
	public void setLocationKey(String locationKey) {
		this.locationKey = locationKey;
	}

	public double getAssignedQuantity() {
		return assignedQuantity;
	}

	@ODLColumnOrder(6)
	@ODLNullAllowed
	@ODLColumnName("assigned-quantity")		
	public void setAssignedQuantity(double assignedQuantity) {
		this.assignedQuantity = assignedQuantity;
	}

	public double getAssignedCapacityViolation() {
		return assignedCapacityViolation;
	}

	@ODLColumnOrder(8)
	@ODLNullAllowed
	@ODLColumnName("assigned-capacity-violation")
	public void setAssignedCapacityViolation(double assignedOvercapacity) {
		this.assignedCapacityViolation = assignedOvercapacity;
	}

	public double getAssignedTravelCost() {
		return assignedTravelCost;
	}

	@ODLColumnOrder(9)	
	@ODLNullAllowed
	@ODLColumnName("assigned-travel-cost")
	public void setAssignedTravelCost(double assignedTravelCost) {
		this.assignedTravelCost = assignedTravelCost;
	}


}
