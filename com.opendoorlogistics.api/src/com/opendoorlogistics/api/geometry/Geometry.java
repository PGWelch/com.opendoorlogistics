/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.geometry;

public interface Geometry {
	LatLong createLatLong(double latitude, double longitude);
	ODLGeom createLineGeometry(LatLong ... latLongs);
	
	/**
	 * Calculate distance in metres
	 * @param from
	 * @param to
	 * @return
	 */
	double calculateGreatCircleDistance(LatLong from, LatLong to);
	
	/**
	 * Create a polygon. The first point and last point each input array
	 * must always match
	 * @param outer
	 * @param holes
	 * @return
	 */
	ODLGeom createPolygon(LatLong [] outer, LatLong [][]holes);
	
	/**
	 * Create a multipolygon. The input polygons must all be of type polygon
	 * @param polygons
	 * @return
	 */
	ODLGeom createMultipolygon(ODLGeom [] polygons);
}
