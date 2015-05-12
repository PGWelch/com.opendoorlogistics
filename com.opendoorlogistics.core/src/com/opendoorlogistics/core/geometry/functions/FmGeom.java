/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.functions;

import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

/**
 * Function to create a geometry
 * @author Phil
 *
 */
public final class FmGeom extends FunctionImpl {
	private final FmGeom.GeomType type;

	public enum GeomType {
		POINT, LINESTRING, POLYGON,
	}

	public FmGeom(FmGeom.GeomType type, Function... longlatcoords) {
		super(longlatcoords);
		this.type = type;
		if (longlatcoords.length == 0 || longlatcoords.length % 2 != 0) {
			throw new RuntimeException("Incorrect number of longitude-latitude arguments into formula: " + Strings.convertEnumToDisplayFriendly(type));
		}
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		Spatial.initSpatial();
		
		Object[] children = executeChildFormulae(parameters, true);
		if (children == null) {
			return Functions.EXECUTION_ERROR;
		}

		// turn into coordinates
		Coordinate[] coords = new Coordinate[children.length / 2];
		for (int i = 0; i < coords.length; i++) {
			Double longObj = Numbers.toDouble(children[i * 2]);
			Double latObj = Numbers.toDouble(children[i * 2 + 1]);
			if (longObj == null || latObj == null) {
				return Functions.EXECUTION_ERROR;
			}

			coords[i] = new Coordinate(longObj, latObj);
		}

		GeometryFactory factory = new GeometryFactory();
		Geometry geometry = null;
		switch (type) {
		case POINT:
			geometry = factory.createPoint(coords[0]);
			break;
		case LINESTRING:
				geometry = factory.createLineString(coords);
			break;
		case POLYGON:
			// ensure joined up
			if (coords[0].equals(coords[coords.length - 1]) == false) {
				Coordinate[] tmp = new Coordinate[coords.length + 1];
				System.arraycopy(coords, 0, tmp, 0, coords.length);
				tmp[coords.length] = coords[0];
				coords = tmp;
			}
			LinearRing lr = factory.createLinearRing(coords);
			geometry = factory.createPolygon(lr, null);
			break;
		}

		ODLGeomImpl ret = new ODLLoadedGeometry(geometry);
		return ret;
	}

	@Override
	public Function deepCopy() {
		return new FmGeom(type, deepCopy(children));
	}

	@Override
	public String toString() {
		return toString(type.name().toLowerCase());
	}
}