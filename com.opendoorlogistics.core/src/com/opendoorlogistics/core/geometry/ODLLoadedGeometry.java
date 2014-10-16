/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.gis.map.OnscreenGeometry;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ODLLoadedGeometry extends ODLLoadableGeometry{
	public ODLLoadedGeometry(Geometry jtsGeometry) {
		super(jtsGeometry);
	}
	
	@Override
	public int getPointsCount() {
		if(fullGeometry!=null){
			return fullGeometry.getNumPoints();			
		}
		return 0;
	}
	
	@Override
	public synchronized LatLong getWGSCentroid() {
		if(wgsCentroid==null){
			
			// Check we have WGS geometry
			if(getJTSGeometry()==null){
				return null;
			}
		
			Point pnt = getJTSGeometry().getCentroid();
			wgsCentroid = new LatLongImpl(pnt.getY(), pnt.getX());
		}
		return wgsCentroid;
	}
	
	@Override
	public synchronized Envelope getWGSBounds(){
		// Get wgs bounds 
		if(wgsBounds==null){
	
			// Check we have WGS geometry
			if(getJTSGeometry()==null){
				return null;
			}
			
			wgsBounds = getJTSGeometry().getEnvelopeInternal();
		}
		return wgsBounds;	
	}
	
	@Override
	public OnscreenGeometry createOnscreenGeometry(LatLongToScreen converter) {
		return new OnscreenGeometry(this, converter);
	}
	
	@Override
	public long getEstimatedSizeInBytes(){
		Geometry geom = getJTSGeometry();
		if(geom==null){
			return 20;
		}
		
		return Spatial.getEstimatedSizeInBytes(geom);
	}

	
	@Override
	public boolean isLineString(){
		Geometry geometry = getJTSGeometry();
		if(geometry!=null){
			return LineString.class.isInstance(geometry);
		}
		return false;
	}

	@Override
	public int getAtomicGeomCount(AtomicGeomType type) {
		return JTSUtils.getGeomCount(fullGeometry, type);
	}


}
