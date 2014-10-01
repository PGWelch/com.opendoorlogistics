/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.distances;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="GreatCircleConfig")
public final class GreatCircleConfiguration implements Serializable {
	private double speedMetresPerSec=22.352;
	private double distanceMultiplier=1;
	
	public double getSpeedMetresPerSec() {
		return speedMetresPerSec;
	}
	
	@XmlElement(name="SpeedMetresPerSec")
	public void setSpeedMetresPerSec(double speedMetresPerSec) {
		this.speedMetresPerSec = speedMetresPerSec;
	}
	public double getDistanceMultiplier() {
		return distanceMultiplier;
	}

	@XmlElement(name="DistanceMultiplier")
	public void setDistanceMultiplier(double distanceMultiplier) {
		this.distanceMultiplier = distanceMultiplier;
	}

	public GreatCircleConfiguration deepCopy(){
		GreatCircleConfiguration ret = new GreatCircleConfiguration();
		ret.speedMetresPerSec = speedMetresPerSec;
		ret.distanceMultiplier = distanceMultiplier;
		return ret;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(distanceMultiplier);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(speedMetresPerSec);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		GreatCircleConfiguration other = (GreatCircleConfiguration) obj;
		if (Double.doubleToLongBits(distanceMultiplier) != Double.doubleToLongBits(other.distanceMultiplier))
			return false;
		if (Double.doubleToLongBits(speedMetresPerSec) != Double.doubleToLongBits(other.speedMetresPerSec))
			return false;
		return true;
	}
	
	
}
