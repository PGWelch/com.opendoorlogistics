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

public abstract class FmDrivingDistance extends FmAbstractDrivingCost{
	private final double multiplyMetresBy;
	
	public FmDrivingDistance(Function geom1, Function geom2, Function map,double multiplyMetresBy) {
		super(geom1, geom2, map, CacheOption.USE_CACHING);
		this.multiplyMetresBy = multiplyMetresBy;		
	}

	public FmDrivingDistance(Function lat1, Function lng1, Function lat2, Function lng2, Function map,double multiplyMetresBy) {
		super(lat1, lng1, lat2, lng2, map, CacheOption.USE_CACHING);
		this.multiplyMetresBy = multiplyMetresBy;		
	}

	@Override
	protected Object calculateDrivingTravel(LatLong[] lls, DistancesConfiguration config) {
		return DistancesSingleton.singleton().calculateDistanceMetres(config, lls[0], lls[1], null)  * multiplyMetresBy;
	}

	public static class Metres extends FmDrivingDistance{

		public Metres(Function geom1, Function geom2,Function map) {
			super(geom1, geom2,map, 1);
		}

		public Metres(Function lat1, Function lng1, Function lat2, Function lng2,Function map) {
			super(lat1, lng1, lat2, lng2,map, 1);
		}
		
	}
	public static class Km extends FmDrivingDistance{

		public Km(Function geom1, Function geom2,Function map) {
			super(geom1, geom2,map, DistancesConsts.METRES_TO_KM);
		}

		public Km(Function lat1, Function lng1, Function lat2, Function lng2,Function map) {
			super(lat1, lng1, lat2, lng2, map,DistancesConsts.METRES_TO_KM);
		}
		
	}
	
	public static class Miles extends FmDrivingDistance{

		public Miles(Function geom1, Function geom2,Function map) {
			super(geom1, geom2,map, DistancesConsts.METRES_TO_MILES);
		}

		public Miles(Function lat1, Function lng1, Function lat2, Function lng2,Function map) {
			super(lat1, lng1, lat2, lng2,map, DistancesConsts.METRES_TO_MILES);
		}
		
	}

}
