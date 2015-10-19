/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.data;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTag;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;

@ODLTag(PredefinedTags.TRAVEL_COSTS)
final public class Travel extends BeanMappedRowImpl{
	private String fromPositionId=null;
	private String toPositionId=null;
	private double cost=1;
	
	public String getFromLocation() {
		return fromPositionId;
	}
	
	@ODLColumnOrder(0)
	@ODLTag(PredefinedTags.FROM_LOCATION)
	public void setFromLocation(String from) {
		this.fromPositionId = from;
	}
	
	public String getToLocation() {
		return toPositionId;
	}
	
	@ODLColumnOrder(1)
	@ODLTag(PredefinedTags.TO_LOCATION)
	public void setToLocation(String to) {
		this.toPositionId = to;
	}
	public double getCost() {
		return cost;
	}
	
	@ODLColumnOrder(2)
	@ODLTag(PredefinedTags.TRAVEL_COST)
	public void setCost(double cost) {
		this.cost = cost;
	}
	
}
