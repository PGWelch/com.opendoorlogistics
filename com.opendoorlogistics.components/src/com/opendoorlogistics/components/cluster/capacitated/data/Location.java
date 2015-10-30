/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.data;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultDoubleValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTag;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;

final public class Location extends LatLongImpl{
	private double quantity=1;
	private double costPerUnitTravel=1;
	private String id=null;
	private String clusterId;
//	private String customerId;
	
	public String getClusterId() {
		return clusterId;
	}

	@ODLNullAllowed
	@ODLColumnOrder(5)			
	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}

	public String getId() {
		return id;
	}
	
	@ODLColumnOrder(2)
	@ODLTag(PredefinedTags.LOCATION_KEY)
	public void setId(String pointId) {
		this.id = pointId;
	}
	
	public double getQuantity() {
		return quantity;
	}
	
	@ODLColumnOrder(3)
	@ODLTag(PredefinedTags.DEMAND)
	@ODLDefaultDoubleValue(1.0)
	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}
	public double getCostPerUnitTravel() {
		return costPerUnitTravel;
	}
	
	@ODLNullAllowed
	@ODLColumnOrder(4)	
	@ODLDefaultDoubleValue(1.0)
	public void setCostPerUnitTravel(double costPerUnitTravelled) {
		this.costPerUnitTravel = costPerUnitTravelled;
	}

}
