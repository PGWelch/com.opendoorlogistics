/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.distances;

import java.io.Serializable;

public final class GraphhopperConfiguration implements Serializable {
	private String graphDirectory;
	private String vehicleType;
	private double timeMultiplier = 1;
	
	public String getGraphDirectory() {
		return graphDirectory;
	}

	public void setGraphDirectory(String graphDirectory) {
		this.graphDirectory = graphDirectory;
	}
	
	public GraphhopperConfiguration deepCopy(){
		GraphhopperConfiguration ret = new GraphhopperConfiguration();
		ret.setGraphDirectory(getGraphDirectory());
		ret.setTimeMultiplier(getTimeMultiplier());
		return ret;
	}

	public double getTimeMultiplier() {
		return timeMultiplier;
	}

	public void setTimeMultiplier(double timeMultiplier) {
		this.timeMultiplier = timeMultiplier;
	}

	
	public String getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(String vehicleType) {
		this.vehicleType = vehicleType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((graphDirectory == null) ? 0 : graphDirectory.hashCode());
		long temp;
		temp = Double.doubleToLongBits(timeMultiplier);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((vehicleType == null) ? 0 : vehicleType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GraphhopperConfiguration other = (GraphhopperConfiguration) obj;
		if (graphDirectory == null) {
			if (other.graphDirectory != null)
				return false;
		} else if (!graphDirectory.equals(other.graphDirectory))
			return false;
		if (Double.doubleToLongBits(timeMultiplier) != Double.doubleToLongBits(other.timeMultiplier))
			return false;
		if (vehicleType == null) {
			if (other.vehicleType != null)
				return false;
		} else if (!vehicleType.equals(other.vehicleType))
			return false;
		return true;
	}

	
}
