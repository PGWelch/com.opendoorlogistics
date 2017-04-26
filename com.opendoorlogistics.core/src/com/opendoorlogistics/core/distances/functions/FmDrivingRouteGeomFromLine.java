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
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.distances.DistancesSingleton;
import com.opendoorlogistics.core.distances.DistancesSingleton.CacheOption;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class FmDrivingRouteGeomFromLine extends FunctionImpl{
	public FmDrivingRouteGeomFromLine(Function linestringgeom, Function graphhopperFile){
		super(linestringgeom, graphhopperFile);
	}

	@Override
	public Function deepCopy() {
		return new FmDrivingRouteGeomFromLine(child(0).deepCopy(), child(1).deepCopy());
	}

	@Override
	public String toString() {
		return toString("routegeom");
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		// get the geometry
		Spatial.initSpatial();
		
		Object child = child(0).execute(parameters);
		if(child==null || child == Functions.EXECUTION_ERROR){
			return Functions.EXECUTION_ERROR;
		}
		
		child = ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, child);
		if(child==null){
			return Functions.EXECUTION_ERROR;				
		}
		
		Geometry geometry = ((ODLGeomImpl)child).getJTSGeometry();
		if(geometry==null || LineString.class.isInstance(geometry)==false){
			return Functions.EXECUTION_ERROR;								
		}
		
		LineString ls = ((LineString)geometry);
		Coordinate start = ls.getCoordinateN(0);
		Coordinate end = ls.getCoordinateN(ls.getNumPoints()-1);
		
		LatLongImpl llStart = new LatLongImpl(start.y, start.x);
		LatLongImpl llEnd = new LatLongImpl(end.y, end.x);
		
		Object dir = child(1).execute(parameters);
		if(dir==null || dir == Functions.EXECUTION_ERROR){
			return Functions.EXECUTION_ERROR;
		};
		
		DistancesConfiguration config = new DistancesConfiguration();
		config.setMethod(CalculationMethod.ROAD_NETWORK);
		config.getGraphhopperConfig().setGraphDirectory(dir.toString());
		return DistancesSingleton.singleton().calculateRouteGeom(config, llStart, llEnd,CacheOption.USE_CACHING, null);
	}
}