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

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.core.gis.map.OnscreenGeometry;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class ODLShapefileLinkGeom extends ODLGeomImpl {
	private final ShapefileLink shapefileLink;
	private volatile boolean attemptedResolve=false;
	private volatile ODLGeomImpl loadedLink;
	
	public ODLShapefileLinkGeom( ShapefileLink shapefileLink) {
		this.shapefileLink = shapefileLink;
	}

	@Override
	public Geometry getJTSGeometry() {
		if(resolveLink()){
			return loadedLink.getJTSGeometry();			
		}
		return null;
	}

	@Override
	public boolean isLoaded() {
		return attemptedResolve;
	}

	@Override
	public OnscreenGeometry createOnscreenGeometry(LatLongToScreen converter) {
		if(resolveLink()){
			return loadedLink.createOnscreenGeometry(converter);
		}
		return null;
	}

	private synchronized boolean resolveLink(){

		if(attemptedResolve && loadedLink!=null){
			// all ok, link resolved
			return true;
		}
		
		// attempt resolve
		if(attemptedResolve==false && shapefileLink!=null){
			loadedLink =  (ODLGeomImpl)Spatial.loadLink(shapefileLink);

			// set attempted resolve last of all; accessed by multiple threads
			attemptedResolve = true;			
			return loadedLink!=null;
		}
		
		// bad link
		return false;
	}

	public synchronized String toText(){	
		return shapefileLink.toString();
	}
	


//	@Override
//	public void putInCache(Object cacheKey, Object data) {
//		if(resolveLink()){
//			loadedLink.putInCache(cacheKey, data);;
//		}
//	}
//
//	@Override
//	public Object getFromCache(Object cacheKey) {
//		if(resolveLink()){
//			return loadedLink.getFromCache(cacheKey);
//		}
//		return null;
//	}



	@Override
	public int getPointsCount() {
		if(resolveLink()){
			return loadedLink.getPointsCount();
		}
		return 0;
	}

	@Override
	public long getEstimatedSizeInBytes() {
		if(resolveLink()){
			return loadedLink.getEstimatedSizeInBytes();
		}
		return 0;
	}

	@Override
	public Point2D getWorldBitmapCentroid(LatLongToScreen latLongToScreen) {
		if(resolveLink()){
			return loadedLink.getWorldBitmapCentroid(latLongToScreen);
		}
		return null;
	}

	@Override
	public LatLong getWGSCentroid() {
		if(resolveLink()){
			return loadedLink.getWGSCentroid();
		}
		return null;
	}

	@Override
	public Envelope getWGSBounds() {
		if(resolveLink()){
			return loadedLink.getWGSBounds();
		}
		return null;
	}

	@Override
	public Rectangle2D getWorldBitmapBounds(LatLongToScreen latLongToScreen) {
		if(resolveLink()){
			return loadedLink.getWorldBitmapBounds(latLongToScreen);
		}
		return null;
	}

	@Override
	public boolean isLineString() {
		if(resolveLink()){
			return loadedLink.isLineString();
		}
		return false;
	}

	@Override
	public int getAtomicGeomCount(AtomicGeomType type) {
		if(resolveLink()){
			return loadedLink.getAtomicGeomCount(type);
		}
		return 0;
	}
}
