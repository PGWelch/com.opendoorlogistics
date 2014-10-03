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
import com.opendoorlogistics.api.distances.DistancesConfiguration.CalculationMethod;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.Spatial;

public abstract class FmAbstractDrivingCost extends FmAbstractTravelCost {
	public FmAbstractDrivingCost(Function lat1, Function lng1, Function lat2, Function lng2, Function map) {
		super(lat1, lng1, lat2, lng2,map);
	}

	public FmAbstractDrivingCost(Function geom1, Function geom2, Function map) {
		super(geom1, geom2,map);
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		Spatial.initSpatial();
		boolean isLatLongs = nbChildren()== 5;
		return execute(parameters, isLatLongs);
	}

	@Override
	protected Object calculateTravel(FunctionParameters parameters,LatLong[] lls, boolean isLatLongs,Object[] childExe ) {
		DistancesConfiguration config = new DistancesConfiguration();
		config.setMethod(CalculationMethod.ROAD_NETWORK);
		
		Object dir = isLatLongs? childExe[4]: childExe[2];
		if(dir==null || dir == Functions.EXECUTION_ERROR){
			return Functions.EXECUTION_ERROR;
		};
		
		config.getGraphhopperConfig().setGraphDirectory(dir.toString());		
		return calculateDrivingTravel(lls, config);
	}

	protected abstract Object calculateDrivingTravel(LatLong[] lls, DistancesConfiguration config );
	

}
