/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.distances.functions;

import com.opendoorlogistics.api.distances.DistancesConfiguration;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.distances.DistancesSingleton;
import com.opendoorlogistics.core.distances.DistancesSingleton.CacheOption;
import com.opendoorlogistics.core.formulae.Function;

public class FmDrivingTime extends FmAbstractDrivingCost{
	
	public FmDrivingTime(Function geom1, Function geom2, Function map) {
		super(geom1, geom2, map, CacheOption.USE_CACHING);
	}

	public FmDrivingTime(Function lat1, Function lng1, Function lat2, Function lng2, Function map) {
		super(lat1, lng1, lat2, lng2, map, CacheOption.USE_CACHING);
	}


	@Override
	protected Object calculateDrivingTravel(LatLong[] lls, DistancesConfiguration config) {
		return DistancesSingleton.singleton().calculateDrivingTime(config, lls[0], lls[1],cacheOption, null);
	}

	public static class FmDrivingTimeUncached extends FmDrivingTime{

		public FmDrivingTimeUncached(Function lat1, Function lng1, Function lat2, Function lng2, Function map) {
			super(lat1, lng1, lat2, lng2, map);
			setCacheOption(CacheOption.NO_CACHING);
		}

		public FmDrivingTimeUncached(Function geom1, Function geom2, Function map) {
			super(geom1, geom2, map);
			setCacheOption(CacheOption.NO_CACHING);
		}
		
	}
}
