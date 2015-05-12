/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.operations;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;

import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.geometry.Spatial;
import com.vividsolutions.jts.geom.Geometry;

public class GridTransforms {
	private final CRSAuthorityFactory crsFac;;
	private final CoordinateReferenceSystem wgs84crs;
	private final CoordinateReferenceSystem gridcrs ;
	private final CoordinateOperation latLongToGrid ;
	private final CoordinateOperation gridToLongLat ;

	public GridTransforms(String espgCode) {
		Spatial.initSpatial();
		try {
			crsFac = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);
			wgs84crs = crsFac.createCoordinateReferenceSystem("4326");
			gridcrs = crsFac.createCoordinateReferenceSystem(espgCode);
			latLongToGrid = new DefaultCoordinateOperationFactory().createOperation(wgs84crs, gridcrs);
			gridToLongLat = new DefaultCoordinateOperationFactory().createOperation(gridcrs, wgs84crs);			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

	public CRSAuthorityFactory getCrsFac() {
		return crsFac;
	}

	public CoordinateReferenceSystem getWgs84crs() {
		return wgs84crs;
	}

	public CoordinateReferenceSystem getGridcrs() {
		return gridcrs;
	}

	public CoordinateOperation getWGS84ToGrid() {
		return latLongToGrid;
	}

	public CoordinateOperation getGridToWGS84() {
		return gridToLongLat;
	}
	
	public Geometry gridToWGS84(Geometry gridGeom){
		try {
			 return JTS.transform(gridGeom, getGridToWGS84().getMathTransform());					
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Geometry wgs84ToGrid(Geometry gridGeom){
		try {
			 return JTS.transform(gridGeom, getWGS84ToGrid().getMathTransform());					
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the transform and cache if not already cached
	 * @param espg
	 * @return
	 */
	public static synchronized GridTransforms getAndCache(String espg){
		Spatial.initSpatial();		
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.GRID_TRANSFORMS_CACHE);
		Object o = cache.get(espg);
		if(o!=null){
			return (GridTransforms)o;
		}
		
		GridTransforms ret = new GridTransforms(espg);
		cache.put(espg, ret, 1024);
		return ret;
	}
}
