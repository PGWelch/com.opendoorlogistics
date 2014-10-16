/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.geometry;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.core.gis.map.OnscreenGeometry;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Immutable geometry class. An ODLGeom may not be modified after creation.
 * Internally this class uses the JTS geometry class.
 * @author Phil
 *
 */
public abstract class ODLGeomImpl implements ODLGeom{


	public abstract String toText();
	
	@Override
	public String toString(){
		return toText();
	}
	

	public abstract  void putInCache(Object cacheKey, Object data);
	
	public abstract Object getFromCache(Object cacheKey);
	
	public abstract Geometry getJTSGeometry();
	
	/**
	 * For linked geometry, return true if the geometry has been
	 * loaded OR been attempted to be loaded.
	 * @return
	 */
	public abstract boolean isLoaded();
	
	static{
		Spatial.initSpatial();
	}

	@Override
	public abstract int getPointsCount() ;
	
	public abstract long getEstimatedSizeInBytes();

	public abstract Point2D getWorldBitmapCentroid(LatLongToScreen latLongToScreen);

	public abstract LatLong getWGSCentroid() ;
	
	public abstract Envelope getWGSBounds();
	
	public abstract Rectangle2D getWorldBitmapBounds(LatLongToScreen latLongToScreen);
	
	public abstract boolean isLineString();

	public abstract OnscreenGeometry createOnscreenGeometry(LatLongToScreen converter) ;
	
	public enum AtomicGeomType{
		POINT,
		LINESTRING,
		POLYGON
	}
	
	public abstract int getAtomicGeomCount(AtomicGeomType type);
}
