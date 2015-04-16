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
import java.io.File;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.geometry.ODLLoadableGeometry;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.gis.map.OnscreenGeometry;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
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
	private final int wgsBlockNb;
	private final int wgsGeomNbInBlock;
	
	private final int [] blockNbsByZoom; 
	private final int [] geomNbInBlockByZoom; 
	
	private final QuadLoader loader;
	private final int nbPoints;
	private final int pointGeomsCount;
	private final int polysGeomsCount;
	private final int linestringsGeomsCount;
	
	private final byte[] bjsonBytes;
	
	@Override
	public long getEstimatedSizeInBytes() {
		long ret=8 + 4 + 8 + 4*blockNbsByZoom.length + 4 + geomNbInBlockByZoom.length*4 + 4;
		
//		if(fullGeometry!=null){
//			ret += Spatial.getEstimatedSizeInBytes(fullGeometry);
//		}
		return ret;
	}
	
	public ODLRenderOptimisedGeom(DataInputStream dis, QuadLoader loader) {
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
			wgsBlockNb = dis.readInt();
			wgsGeomNbInBlock = dis.readInt();

			// read position array size
			byte sz = dis.readByte();
			blockNbsByZoom = new int[sz];
			geomNbInBlockByZoom = new int[sz];
			for(int i =0 ; i<sz ; i++){
				blockNbsByZoom[i] = dis.readInt();
			}
			for(int i =0 ; i<sz ; i++){
				geomNbInBlockByZoom[i] = dis.readInt();
			}
			
			// binary json
			bjsonBytes = RogReaderUtils.readBytesArray(dis);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private static class ROGGeomKey{
		final File file;
		final long id;
		
		ROGGeomKey(File file, long id) {
			this.file = file;
			this.id = id;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			result = prime * result + (int) (id ^ (id >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ROGGeomKey other = (ROGGeomKey) obj;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			if (id != other.id)
				return false;
			return true;
		}

	}

	@Override
	public synchronized Geometry getJTSGeometry() {
		// This loads the full geometry and should be avoided wherever possible.
		// The geometry is cached with a limited cache size.

		// Try getting from the cache first
		ROGGeomKey key = new ROGGeomKey(getFileInformation().getFile(), id);
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.ROG_FULL_GEOMETRY);
		Geometry ret = (Geometry)cache.get(key);
		
		// Load if not cached
		if(ret==null && wgsBlockNb!=-1){
			ret = loader.loadGeometry(id,wgsBlockNb,wgsGeomNbInBlock );
			if(ret!=null){
				cache.put(key, ret, Spatial.getEstimatedSizeInBytes(ret));				
			}
		}
		
		return ret;
	}

	@Override
	public boolean isLoaded() {
		return true;
	}

	@Override
	public OnscreenGeometry createOnscreenGeometry(LatLongToScreen converter) {
		// lookup optimised for size
		Long zoom = Numbers.toLong(converter.getZoomHashmapKey());
		if(zoom!=null && zoom>=0 && zoom<blockNbsByZoom.length){
			int iZoom = zoom.intValue();
			int blockNb = blockNbsByZoom[iZoom];
			Geometry g=null;
			boolean drawFilledBounds=false;
			if(blockNb == SUBPIXEL){
				drawFilledBounds = true;
			}
			else if(blockNb >=0){
				// take the exact geometry
				g = loader.loadGeometry(id,blockNb, geomNbInBlockByZoom[iZoom]);
			}
			else{
				// find nearest and transform
				int nearestZoom = -1;
				
				// look bigger first (lower index zoom is bigger within jxmapviewer project)
				for(int i = iZoom-1 ; i>=0 && nearestZoom==-1 ; i--){
					if(blockNbsByZoom[i]>=0){
						nearestZoom = i;
					}
				}
				
				// then look smaller
				for(int i = iZoom + 1; i < blockNbsByZoom.length && nearestZoom==-1 ;i++){
					if(blockNbsByZoom[i]>=0){
						nearestZoom = i;
					}
				}
				
				if(nearestZoom==-1){
					// this should probably never happen
					drawFilledBounds = true;
				}else{
					g = loader.loadTransformedGeometry(id,blockNbsByZoom[nearestZoom],geomNbInBlockByZoom[nearestZoom], nearestZoom, iZoom);
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
		else{
			// have to use the full geometry (slow)
			return new OnscreenGeometry(this, converter);
		}
		//return null;
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

	public int[] getFilePosition(int zoom){
		if(zoom<0){
			return new int[]{wgsBlockNb,wgsGeomNbInBlock};
		}
		else if (zoom < blockNbsByZoom.length){
			return new int[]{blockNbsByZoom[zoom], geomNbInBlockByZoom[zoom]};
		}
		return null;
	}
	
	public long getGeomId(){
		return id;
	}
	
	public byte[] getBjsonBytes(){
		return bjsonBytes;
	}
	
	public RogFileInformation getFileInformation(){
		return loader;
	}


}
