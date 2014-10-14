/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.geometry;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Immutable geometry class. Geometry may not be modified after
 * its created, only replaced.
 * @author Phil
 *
 */
public final class ODLGeomImpl implements ODLGeom{
	private final ShapefileLink shapefileLink;
	private GeomWithCache geomWithCache;
	private volatile boolean attemptedResolve=false;
	
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


}
