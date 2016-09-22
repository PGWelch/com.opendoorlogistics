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
package com.opendoorlogistics.speedregions;

import com.opendoorlogistics.speedregions.beans.SpeedRule;
import com.vividsolutions.jts.geom.Geometry;

public interface SpeedRegionLookup {
	/**
	 * For a given edge, find what region it sits within and returned
	 * the standardised string of the region id.
	 * @param edge
	 * @return Region id (standardised) or null if none found.
	 */
	String findRegionId(Geometry edge);

	
	public interface SpeedRuleLookup{
		SpeedRule getSpeedRule(String standardisedRegionId);
	}
	
	/**
	 * For a given encoder, create a {@link SpeedRuleLookup} object 
	 * @param encoder
	 * @return
	 */
	SpeedRuleLookup createLookupForEncoder(String encoder);
}
