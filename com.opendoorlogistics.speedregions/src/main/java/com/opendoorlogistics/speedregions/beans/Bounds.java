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

import com.opendoorlogistics.speedregions.processor.RegionProcessorUtils;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Simple bean, easily serialised to JSON etc
 * @author Phil
 *
 */
public class Bounds extends JSONToString{
	private double minLng;
	private double maxLng;
	private double minLat;
	private double maxLat;
	
	public Bounds(){
		
	}
	
	public Bounds(Bounds deepCopyThis){
		this(deepCopyThis.minLng, deepCopyThis.maxLng, deepCopyThis.minLat, deepCopyThis.maxLat);
	}
	
	public Bounds(double minLng, double maxLng, double minLat, double maxLat) {
		this.minLng = minLng;
		this.maxLng = maxLng;
		this.minLat = minLat;
		this.maxLat = maxLat;
	}


	public double getMinLng() {
		return minLng;
	}
	public void setMinLng(double minLng) {
		this.minLng = minLng;
	}
	public double getMaxLng() {
		return maxLng;
	}
	public void setMaxLng(double maxLng) {
		this.maxLng = maxLng;
	}
	public double getMinLat() {
		return minLat;
	}
	public void setMinLat(double minLat) {
		this.minLat = minLat;
	}
	public double getMaxLat() {
		return maxLat;
	}
	public void setMaxLat(double maxLat) {
		this.maxLat = maxLat;
	}
	
	public Envelope asEnvelope(){
		return new Envelope(minLng, maxLng, minLat, maxLat);
	}
	
	public static Bounds createGlobal(){
		return new Bounds(-180, 180, -90, 90);
	}
	
	@Override
	public String toString() {
		return RegionProcessorUtils.toJSON(this);
	}

}
