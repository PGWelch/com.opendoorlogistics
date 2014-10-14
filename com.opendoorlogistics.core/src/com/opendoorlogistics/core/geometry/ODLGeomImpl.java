/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.geometry;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Immutable geometry class. Geometry may not be modified after
 * its created, only replaced.
 * @author Phil
 *
 */
public final class ODLGeomImpl implements ODLGeom{
	private final ShapefileLink shapefileLink;
	private volatile GeomWithCache geomWithCache;
	private volatile boolean attemptedResolve=false;
	private volatile HashMap<Object, Rectangle2D> worldBitmapBoundsByZoomLevel;
	private volatile HashMap<Object, Point2D> worldBitmapCentroidsByZoomLevel;	
	private volatile Envelope wgsBounds;
	private volatile LatLong wgsCentroid;
	
	public ODLGeomImpl(Geometry jtsGeometry) {
		this.geomWithCache = new GeomWithCache(jtsGeometry);
		this.shapefileLink = null;
	}

	public ODLGeomImpl(GeomWithCache gwc) {
		this.geomWithCache = gwc;
		this.shapefileLink = null;
	}
	
	
	public ODLGeomImpl(ShapefileLink shapefileLink){
		this.geomWithCache = null;
		this.shapefileLink = shapefileLink;
	}
	
	public synchronized String toText(){	
		if(shapefileLink!=null){
			return shapefileLink.toString();
		}
		
		if(geomWithCache!=null && geomWithCache.getJTSGeometry()!=null){
			return geomWithCache.getJTSGeometry().toText();			
		}
		
		return "";
	}
	
	@Override
	public String toString(){
		return toText();
	}
	
	/**
	 * Geometries can have data cached with them which is used to speed up rendering.
	 * @param cacheKey
	 * @param data
	 */
	public synchronized void putInCache(Object cacheKey, Object data){
		if(resolveLink()){
			geomWithCache.putInCache(cacheKey, data);			
		}
	}
	
	public synchronized Object getFromCache(Object cacheKey){
		if(resolveLink()){
			return geomWithCache.getFromCache(cacheKey);			
		}
		return null;
	}
	
	public synchronized Geometry getJTSGeometry(){
		if(resolveLink()){
			return geomWithCache.getJTSGeometry();			
		}
		return null;
	}
	
	public synchronized boolean isValid(){
		return getJTSGeometry()!=null;
	}
	
	/**
	 * For linked geometry, return true if the geometry has been
	 * loaded OR been attempted to be loaded.
	 * @return
	 */
	public synchronized boolean isLoaded(){
		if(shapefileLink==null){
			// not linked so always loaded
			return true;
		}
		
		return attemptedResolve;
	}
	
	private synchronized boolean resolveLink(){
		if(shapefileLink==null){
			// not linked
			return true;
		}
		if(attemptedResolve && geomWithCache!=null){
			// all ok, link resolved
			return true;
		}
		
		// attempt resolve
		if(attemptedResolve==false && shapefileLink!=null){
			geomWithCache = Spatial.loadLink(shapefileLink);
			// set attempted resolve last of all; accessed by multiple threads
			attemptedResolve = true;			
			return geomWithCache!=null;
		}
		
		// bad link
		return false;
	}
	
	static{
		Spatial.initSpatial();
	}

	@Override
	public int getPointsCount() {
		Geometry jts = getJTSGeometry();
		return jts.getNumPoints();
	}
	
	public long getEstimatedSizeInBytes(){
		Geometry geom = getJTSGeometry();
		if(geom==null){
			return 20;
		}
		
		return Spatial.getEstimatedSizeInBytes(geom);
	}

	public synchronized Point2D getWorldBitmapCentroid(LatLongToScreen latLongToScreen){
		Point2D ret = null;
		if(worldBitmapCentroidsByZoomLevel!=null){
			ret = worldBitmapCentroidsByZoomLevel.get(latLongToScreen.getZoomHashmapKey());
			if(ret!=null){
				return ret;
			}
		}
	
		// Get the wgs centroid if not yet available
		if(wgsCentroid==null){
			
			// Check we have WGS geometry
			if(getJTSGeometry()==null){
				return null;
			}
		
			Point pnt = getJTSGeometry().getCentroid();
			wgsCentroid = new LatLongImpl(pnt.getY(), pnt.getX());
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
		if(wgsBounds==null){
	
			// Check we have WGS geometry
			if(getJTSGeometry()==null){
				return null;
			}
			
			wgsBounds = getJTSGeometry().getEnvelopeInternal();
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
	
	public boolean isLineString(){
		Geometry geometry = getJTSGeometry();
		if(geometry!=null){
			return LineString.class.isInstance(geometry);
		}
		return false;
	}

}
