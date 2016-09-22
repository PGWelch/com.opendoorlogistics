/*
 * Copyright 2016 Open Door Logistics Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opendoorlogistics.speedregions.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * For a given flag encoder and regionid, the speed rule is applied
 * if the speed rule contains both the regionid in its list of regions,
 * and the flag enconder name (e.g. "car", "foot") in its list of flag encoders.
 * @author Phil
 *
 */
public class SpeedRule extends JSONToString{
	private Map<String,Integer> speedsByRoadType = new TreeMap<String, Integer>();
	private double multiplier = 1;
	private List<String> flagEncoders = new ArrayList<String>();
	private List<String> regionIds= new ArrayList<String>();
	private SpeedUnit speedUnit = SpeedUnit.DEFAULT;
	
	public Map<String, Integer> getSpeedsByRoadType() {
		return speedsByRoadType;
	}
	public void setSpeedsByRoadType(Map<String, Integer> speedsByRoadType) {
		this.speedsByRoadType = speedsByRoadType;
	}
	public double getMultiplier() {
		return multiplier;
	}
	public void setMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}

	public List<String> getFlagEncoders() {
		return flagEncoders;
	}
	public void setFlagEncoders(List<String> flagEncoders) {
		this.flagEncoders = flagEncoders;
	}
	public List<String> getRegionIds() {
		return regionIds;
	}
	public void setRegionIds(List<String> regionIds) {
		this.regionIds = regionIds;
	}
	public SpeedUnit getSpeedUnit() {
		return speedUnit;
	}
	public void setSpeedUnit(SpeedUnit speedUnit) {
		this.speedUnit = speedUnit;
	}
	

}

