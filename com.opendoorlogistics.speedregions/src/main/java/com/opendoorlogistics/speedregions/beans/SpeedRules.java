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

import org.geojson.FeatureCollection;

public class SpeedRules extends JSONToString{
	private List<SpeedRule> rules = new ArrayList<SpeedRule>();
	private FeatureCollection features = new FeatureCollection();
	private String countryCode;
	
	public List<SpeedRule> getRules() {
		return rules;
	}
	public void setRules(List<SpeedRule> rules) {
		this.rules = rules;
	}
	public FeatureCollection getFeatures() {
		return features;
	}
	public void setFeatures(FeatureCollection features) {
		this.features = features;
	}
	
	/**
	 * 2 digit country code. Must be set. See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
	 * @return
	 */
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	
	
}
