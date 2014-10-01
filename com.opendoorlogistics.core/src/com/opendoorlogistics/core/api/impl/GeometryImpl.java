/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl;

import com.opendoorlogistics.api.geometry.Geometry;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.gis.GeoUtils;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class GeometryImpl implements Geometry {
	private final GeometryFactory geomfactory = new GeometryFactory();
	
	@Override
	public LatLong createLatLong(double latitude, double longitude) {
		return new LatLongImpl(latitude, longitude);
	}

	@Override
	public ODLGeom createLineGeometry(LatLong... latLongs) {
		Coordinate[] coords = new Coordinate[latLongs.length];
		for(int i =0 ; i<latLongs.length;i++){
			coords[i] = new Coordinate(latLongs[i].getLongitude(), latLongs[i].getLatitude());
		}
		ODLGeomImpl geom = new ODLGeomImpl(geomfactory.createLineString(coords));
		return geom;
	}

	static{
		Spatial.initSpatial();
	}

	@Override
	public double calculateGreatCircleDistance(LatLong from, LatLong to) {
		return GeoUtils.greatCircleApprox(from, to);
	}
}
