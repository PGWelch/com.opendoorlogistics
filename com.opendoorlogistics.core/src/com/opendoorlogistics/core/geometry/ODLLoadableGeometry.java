/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public abstract class ODLLoadableGeometry extends ODLGeomImpl{
//	private final SimpleSoftReferenceMap<Object,Object> cache = new SimpleSoftReferenceMap<>(10);
	protected volatile HashMap<Object, Rectangle2D> worldBitmapBoundsByZoomLevel;
	protected volatile HashMap<Object, Point2D> worldBitmapCentroidsByZoomLevel;	
	protected volatile Envelope wgsBounds;
	protected volatile LatLong wgsCentroid;

	
//	public synchronized String toText(){	
//		if(fullGeometry!=null){
//			return fullGeometry.toText();			
//		}
//		
//		return "";
//	}
	
//	@Override
//	public synchronized void putInCache(Object cacheKey, Object data){
//		cache.put(cacheKey, data);			
//	}
//	
//	@Override
//	public synchronized Object getFromCache(Object cacheKey){
//		return cache.get(cacheKey);			
//	}

	
	@Override
	public boolean isLoaded(){
		return true;
	}
	
	@Override
	public synchronized Point2D getWorldBitmapCentroid(LatLongToScreen latLongToScreen){
		Point2D ret = null;
		if(worldBitmapCentroidsByZoomLevel!=null){
			ret = worldBitmapCentroidsByZoomLevel.get(latLongToScreen.getZoomHashmapKey());
			if(ret!=null){
				return ret;
			}
		}
	
		// Get the wgs centroid if not yet available
		if(getWGSCentroid()==null){
			return null;
		}

		// Translate it to world bitmap for this zoom
		ret = latLongToScreen.getWorldBitmapPixelPosition(wgsCentroid);
		
		// Create cache map if not yet existing
		if(worldBitmapCentroidsByZoomLevel==null){
			worldBitmapCentroidsByZoomLevel = new HashMap<>(20);
		}
		
		worldBitmapCentroidsByZoomLevel.put(latLongToScreen.getZoomHashmapKey(), ret);
	
		return ret;
	}
	
	
	@Override
	public synchronized Rectangle2D getWorldBitmapBounds(LatLongToScreen latLongToScreen){
		Rectangle2D ret=null;
		
		// Return pre-calculated if we have it
		if(worldBitmapBoundsByZoomLevel!=null){
			ret = worldBitmapBoundsByZoomLevel.get(latLongToScreen.getZoomHashmapKey());
			if(ret!=null){
				return ret;
			}
		}
		
		// Get wgs bounds 
		if(getWGSBounds()==null){
			return null;
		}
		
		// Convert min and max points remembering x=longitude, y = latitude
		Point2D min = latLongToScreen.getWorldBitmapPixelPosition(new LatLongImpl(wgsBounds.getMinY(), wgsBounds.getMinX()));
		Point2D max = latLongToScreen.getWorldBitmapPixelPosition(new LatLongImpl(wgsBounds.getMaxY(), wgsBounds.getMaxX()));

		// I'm not sure if the coordinate directions might flip so I'm playing it safe here
		double minX = Math.min(min.getX(), max.getX());
		double minY = Math.min(min.getY(), max.getY());
		double maxX = Math.max(min.getX(), max.getX());
		double maxY = Math.max(min.getY(), max.getY());
		
		// Get bounding rectangle
		ret = new Rectangle2D.Double(minX,minY, maxX - minX , maxY - minY);
		
		// Cache it
		if(worldBitmapBoundsByZoomLevel==null){
			// OSM has 20 zoom levels by default
			worldBitmapBoundsByZoomLevel = new HashMap<>(20);
		}
		worldBitmapBoundsByZoomLevel.put(latLongToScreen.getZoomHashmapKey(), ret);

		return ret;
	}
	
	@Override
	public synchronized String toText(){
		Geometry g = getJTSGeometry();
		if(g!=null){
			return g.toText();			
		}
		
		return "";
	}
	
}
