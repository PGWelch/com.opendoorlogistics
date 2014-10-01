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

@XmlRootElement(name="DistancesOutput")
public final class DistancesOutputConfiguration implements Serializable{
	private OutputDistanceUnit outputDistanceUnit = OutputDistanceUnit.KILOMETRES;
	private OutputTimeUnit outputTimeUnit = OutputTimeUnit.HOURS;
	private OutputType outputType = OutputType.DISTANCE;
	private double distanceWeighting=1;
	private double timeWeighting=1;

	public enum OutputDistanceUnit {
		METRES, KILOMETRES, MILES;
	}

	public enum OutputTimeUnit {
		MILLISECONDS, SECONDS, MINUTES, HOURS;
	}
	
	public DistancesOutputConfiguration deepCopy(){
		DistancesOutputConfiguration ret = new DistancesOutputConfiguration();
		ret.outputDistanceUnit = outputDistanceUnit;
		ret.outputTimeUnit = outputTimeUnit;
		ret.outputType = outputType;
		ret.distanceWeighting = distanceWeighting;
		ret.timeWeighting = timeWeighting;
		return ret;
	}
	
	/**
	 * Note - ordering of output type can effect the default value because of the validate output method
	 * @author Phil
	 *
	 */
	public enum OutputType{
		DISTANCE(true,false),
		TIME(false,true),
		SUMMED(true,true);
	//	BOTH(true,true)
		
		private final boolean usesDistance;
		private final boolean usesTime;

		private OutputType(boolean usesDistance,boolean usesTime) {
			this.usesDistance = usesDistance;
			this.usesTime = usesTime;
		}
		
		public boolean isUsesTime(){
			return usesTime;
		}
		
		public boolean isUsesDistance(){
			return usesDistance;
		}
		
	}

	public OutputDistanceUnit getOutputDistanceUnit() {
		return outputDistanceUnit;
	}

	@XmlElement(name="DistanceOutputUnit")
	public void setOutputDistanceUnit(OutputDistanceUnit outputDistanceUnit) {
		this.outputDistanceUnit = outputDistanceUnit;
	}

	public OutputTimeUnit getOutputTimeUnit() {
		return outputTimeUnit;
	}

	@XmlElement(name="TimeOutputUnit")
	public void setOutputTimeUnit(OutputTimeUnit outputTimeUnit) {
		this.outputTimeUnit = outputTimeUnit;
	}

	public OutputType getOutputType() {
		return outputType;
	}

	@XmlElement(name="OutputTime")
	public void setOutputType(OutputType outputType) {
		this.outputType = outputType;
	}

	public double getDistanceWeighting() {
		return distanceWeighting;
	}

	@XmlElement(name="DistanceWeighting")
	public void setDistanceWeighting(double distanceWeighting) {
		this.distanceWeighting = distanceWeighting;
	}

	public double getTimeWeighting() {
		return timeWeighting;
	}

	@XmlElement(name="TimeWeighting")
	public void setTimeWeighting(double timeWeighting) {
		this.timeWeighting = timeWeighting;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(distanceWeighting);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((outputDistanceUnit == null) ? 0 : outputDistanceUnit.hashCode());
		result = prime * result + ((outputTimeUnit == null) ? 0 : outputTimeUnit.hashCode());
		result = prime * result + ((outputType == null) ? 0 : outputType.hashCode());
		temp = Double.doubleToLongBits(timeWeighting);
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
		DistancesOutputConfiguration other = (DistancesOutputConfiguration) obj;
		if (Double.doubleToLongBits(distanceWeighting) != Double.doubleToLongBits(other.distanceWeighting))
			return false;
		if (outputDistanceUnit != other.outputDistanceUnit)
			return false;
		if (outputTimeUnit != other.outputTimeUnit)
			return false;
		if (outputType != other.outputType)
			return false;
		if (Double.doubleToLongBits(timeWeighting) != Double.doubleToLongBits(other.timeWeighting))
			return false;
		return true;
	}

}
