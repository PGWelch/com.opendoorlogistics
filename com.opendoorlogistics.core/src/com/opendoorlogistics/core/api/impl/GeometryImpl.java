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
import com.opendoorlogistics.core.geometry.GreateCircle;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

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
		ODLGeomImpl geom = new ODLLoadedGeometry(geomfactory.createLineString(coords));
		return geom;
	}

	static{
		Spatial.initSpatial();
	}

	@Override
	public double calculateGreatCircleDistance(LatLong from, LatLong to) {
		return GreateCircle.greatCircleApprox(from, to);
	}

	@Override
	public ODLGeom createPolygon(LatLong [] outer, LatLong [][]holes) {
		LinearRing outerRing = geomfactory.createLinearRing(toCoords(outer));
		int nholes = holes!=null ? holes.length : 0;
		LinearRing [] innerRings = new LinearRing[nholes];
		for(int i =0 ; i < nholes ; i++){
			innerRings[i] = geomfactory.createLinearRing(toCoords(holes[i]));
		}
		
		com.vividsolutions.jts.geom.Geometry g = geomfactory.createPolygon(outerRing, innerRings);
		return new ODLLoadedGeometry(g);
	}
	
	private Coordinate toCoordinate(LatLong ll){
		return new Coordinate(ll.getLongitude(), ll.getLatitude());
	}
	
	private Coordinate [] toCoords(LatLong [] lls){
		Coordinate []ret = new Coordinate[lls.length];
		for(int i =0 ; i < lls.length ; i++){
			ret[i] = toCoordinate(lls[i]);
		}
		return ret;
	}

	@Override
	public ODLGeom createMultipolygon(ODLGeom[] polygons) {
		Polygon [] polys = new Polygon[polygons.length];
		for(int i =0 ; i < polys.length ; i++){
			polys[i] = (Polygon)((ODLGeomImpl)polygons[i]).getJTSGeometry();
		}
		
		return new ODLLoadedGeometry(geomfactory.createMultiPolygon(polys));
	}
	
}
