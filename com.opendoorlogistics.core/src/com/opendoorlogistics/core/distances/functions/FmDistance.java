/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.distances.functions;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.geometry.GreateCircle;
import com.opendoorlogistics.core.geometry.Spatial;

public abstract class FmDistance extends FmAbstractTravelCost {
	private final double multiplyMetresBy;
	
	public FmDistance(Function lat1, Function lng1, Function lat2, Function lng2,double multiplyMetresBy) {
		super(lat1, lng1, lat2, lng2);
		this.multiplyMetresBy = multiplyMetresBy;
	}

	public FmDistance(Function geom1, Function geom2,double multiplyMetresBy) {
		super(geom1, geom2);
		this.multiplyMetresBy = multiplyMetresBy;
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		Spatial.initSpatial();
		boolean isLatLongs = nbChildren()== 4;
		return execute(parameters, isLatLongs);
	}

	@Override
	protected Object calculateTravel(FunctionParameters parameters,LatLong[] lls, boolean isLatLongs,Object[] childExe ) {
		double ret =  GreateCircle.greatCircleApprox(lls[0], lls[1]);
		return ret * multiplyMetresBy;
	}

	public static class Metres extends FmDistance{

		public Metres(Function geom1, Function geom2) {
			super(geom1, geom2, 1);
		}

		public Metres(Function lat1, Function lng1, Function lat2, Function lng2) {
			super(lat1, lng1, lat2, lng2, 1);
		}
		
	}
	public static class Km extends FmDistance{

		public Km(Function geom1, Function geom2) {
			super(geom1, geom2, DistancesConsts.METRES_TO_KM);
		}

		public Km(Function lat1, Function lng1, Function lat2, Function lng2) {
			super(lat1, lng1, lat2, lng2, DistancesConsts.METRES_TO_KM);
		}
		
	}
	
	public static class Miles extends FmDistance{

		public Miles(Function geom1, Function geom2) {
			super(geom1, geom2, DistancesConsts.METRES_TO_MILES);
		}

		public Miles(Function lat1, Function lng1, Function lat2, Function lng2) {
			super(lat1, lng1, lat2, lng2, DistancesConsts.METRES_TO_MILES);
		}
		
	}

}
