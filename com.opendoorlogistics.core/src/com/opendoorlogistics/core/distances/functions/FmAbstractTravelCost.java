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
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.vividsolutions.jts.geom.Point;

public abstract class FmAbstractTravelCost extends FunctionImpl {
	public FmAbstractTravelCost(Function...children) {
		super(children);
	}

	protected Object execute(FunctionParameters parameters, boolean isLatLongs) {
		Object[] childExe = executeChildFormulae(parameters, false);
		if (childExe == null) {
			return Functions.EXECUTION_ERROR;
		}

		// just return null if any of the inputs are null
		for (int i = 0; i < childExe.length; i++) {
			if (childExe[i] == null) {
				return null;
			}
		}

		LatLong []lls = new LatLong[2];
		for (int i = 0; i < 2; i++) {
			if (isLatLongs) {
				// get lat longs directly
				Object lat = ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, childExe[i * 2]);
				Object lng = ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, childExe[i * 2 + 1]);
				if (lat == null || lng == null) {
					return Functions.EXECUTION_ERROR;
				}

				lls[i] = new LatLongImpl((Double) lat, (Double) lng);
			} else {
				
				// convert geometry using centroid (to do.. should probaly be in grid frame)
				ODLGeom g = (ODLGeom) ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, childExe[i]);
				if (g == null) {
					return Functions.EXECUTION_ERROR;
				}

				Point p = ((ODLGeomImpl) g).getJTSGeometry().getCentroid();
				lls[i] = new LatLongImpl(p.getY(), p.getX());
			}
		}

	//	System.out.println(getClass().getName());
		Object ret = calculateTravel(parameters,lls,isLatLongs,childExe);
		
		return ret;
	}

	protected abstract Object calculateTravel(FunctionParameters parameters,LatLong[] lls, boolean isLatLongs,Object[] childExe );

	@Override
	public Function deepCopy() {
		// TODO Auto-generated method stub
		return null;
	}

}
