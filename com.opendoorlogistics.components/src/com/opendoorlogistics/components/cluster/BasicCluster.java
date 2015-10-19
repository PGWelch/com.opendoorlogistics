/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnDescription;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;

public class BasicCluster extends BeanMappedRowImpl implements LatLong {

	private String clusterId = "";
	private double latitude;
	private double longitude;
	private long assignedLocationsCount;

	public BasicCluster() {
		super();
	}

	public String getClusterId() {
		return clusterId;
	}

	@ODLColumnOrder(0)
	@ODLColumnDescription("Identifier of this cluster; for example the (unique) name of a salesperson assigned to it.")
	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}

	@Override
	public double getLatitude() {
		return latitude;
	}

	@ODLColumnOrder(3)
	@ODLNullAllowed
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	@Override
	public double getLongitude() {
		return longitude;
	}

	@ODLColumnOrder(4)
	@ODLNullAllowed
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public long getAssignedLocationsCount() {
		return assignedLocationsCount;
	}

	@ODLColumnOrder(7)
	@ODLNullAllowed
	@ODLColumnName("assigned-locations-count")
	public void setAssignedLocationsCount(long assignedLocationsCount) {
		this.assignedLocationsCount = assignedLocationsCount;
	}

	@Override
	public String toString() {
		return "BasicCluster [clusterId=" + clusterId + ", latitude=" + latitude + ", longitude=" + longitude + ", assignedLocationsCount=" + assignedLocationsCount + "]";
	}

}
