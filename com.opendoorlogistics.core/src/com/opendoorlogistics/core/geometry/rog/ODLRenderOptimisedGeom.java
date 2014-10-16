/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog;

import java.awt.geom.Point2D;
import java.io.DataInputStream;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.geometry.ODLLoadableGeometry;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.gis.map.OnscreenGeometry;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;
import com.opendoorlogistics.core.utils.Numbers;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class ODLRenderOptimisedGeom extends ODLLoadableGeometry {
	public final static int INVALID_GEOMETRY=-1;
	public final static int SUBPIXEL=-2;
	public final static int USE_LAST_LEVEL=-3;
	
	private final long id;
	private final long wgsGeomPos;
	private final long [] filePositionsByZoom; 
	private final GeometryLoader loader;
	private final int nbPoints;
	private final int pointGeomsCount;
	private final int polysGeomsCount;
	private final int linestringsGeomsCount;
	
	@Override
	public long getEstimatedSizeInBytes() {
		long ret=8 + 4 + 8 + 8*filePositionsByZoom.length + 8 + 4;
		
		if(fullGeometry!=null){
			ret += Spatial.getEstimatedSizeInBytes(fullGeometry);
		}
		return ret;
	}
	
	public ODLRenderOptimisedGeom(DataInputStream dis, GeometryLoader loader) {
		super(null);
		this.loader=  loader;
	
		try {
			// read id
			id = dis.readLong();
			
			// read number of points
			nbPoints = dis.readInt();
			pointGeomsCount = dis.readInt();
			linestringsGeomsCount = dis.readInt();
			polysGeomsCount = dis.readInt();
			
			// read bounds
			double minX = dis.readDouble();
			double minY = dis.readDouble();
			double w = dis.readDouble();
			double h = dis.readDouble();
			wgsBounds = new Envelope(minX, minX +w, minY, minY + h);
			
			// read latitude
			double lng = dis.readDouble();
			double lat = dis.readDouble();
			wgsCentroid = new LatLongImpl(lat, lng);
			
			// read wgs geometry position
			wgsGeomPos = dis.readLong();
			
			// read position array size
			byte sz = dis.readByte();
			filePositionsByZoom = new long[sz];
			for(int i =0 ; i<sz ; i++){
				filePositionsByZoom[i] = dis.readLong();
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@Override
	public synchronized Geometry getJTSGeometry() {
		// full load the geometry (should be avoided wherever possible)
		if(fullGeometry==null && wgsGeomPos!=-1){
			fullGeometry = loader.load(id,wgsGeomPos);
		}
		return fullGeometry;
	}

	@Override
	public boolean isLoaded() {
		return true;
	}

	@Override
	public OnscreenGeometry createOnscreenGeometry(LatLongToScreen converter) {
		// lookup optimised for size
		Long zoom = Numbers.toLong(converter.getZoomHashmapKey());
		if(zoom!=null && zoom>=0 && zoom<filePositionsByZoom.length){
			int iZoom = zoom.intValue();
			long pos = filePositionsByZoom[iZoom];
			Geometry g=null;
			boolean drawFilledBounds=false;
			if(pos == SUBPIXEL){
				drawFilledBounds = true;
			}
			else if(pos >=0){
				// take the exact geometry
				g = loader.load(id,pos);
			}
			else{
				// find nearest and transform
				int nearestZoom = -1;
				
				// look bigger first (lower index zoom is bigger within jxmapviewer project)
				for(int i = iZoom-1 ; i>=0 && nearestZoom==-1 ; i--){
					if(filePositionsByZoom[i]>=0){
						nearestZoom = i;
					}
				}
				
				// then look smaller
				for(int i = iZoom + 1; i < filePositionsByZoom.length && nearestZoom==-1 ;i++){
					if(filePositionsByZoom[i]>=0){
						nearestZoom = i;
					}
				}
				
				if(nearestZoom==-1){
					// this should probably never happen
					drawFilledBounds = true;
				}else{
					g = loader.loadTransform(id,filePositionsByZoom[nearestZoom], nearestZoom, iZoom);
				}
			}
			
			if(drawFilledBounds){
				// create a geometry which is just a single point at the centroid (following renderer convention)
				Point2D centroid = getWorldBitmapCentroid(converter);
				GeometryFactory factory = new GeometryFactory();
				g= factory.createPoint(new Coordinate(centroid.getX(),centroid.getY()));		
			}
			
			return new OnscreenGeometry(g, drawFilledBounds);
		}
		
		return null;
	}


	@Override
	public int getPointsCount() {
		return nbPoints;
	}

	@Override
	public LatLong getWGSCentroid() {
		return wgsCentroid;
	}

	@Override
	public Envelope getWGSBounds() {
		return wgsBounds;
	}

	@Override
	public boolean isLineString(){
		return linestringsGeomsCount==1 && pointGeomsCount==0 && polysGeomsCount==0;
	}

	@Override
	public int getAtomicGeomCount(AtomicGeomType type) {
		switch(type){
		case POINT:
			return pointGeomsCount;
			
		case LINESTRING:
			return linestringsGeomsCount;
			
		case POLYGON:
			return polysGeomsCount;
		}
		return 0;
	}

	public long getFilePosition(int zoom){
		if(zoom<0){
			return wgsGeomPos;
		}
		else if (zoom < filePositionsByZoom.length){
			return filePositionsByZoom[zoom];
		}
		return -1;
	}
	
	public long getGeomId(){
		return id;
	}
}
