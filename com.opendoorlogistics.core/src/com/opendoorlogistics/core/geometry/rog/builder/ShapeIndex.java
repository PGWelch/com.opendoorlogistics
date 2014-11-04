/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog.builder;

import java.util.Arrays;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.rog.ODLRenderOptimisedGeom;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ShapeIndex {
	
	private final Envelope wgsBounds;
	private final LatLong wgsCentroid;
	
	final int rowNb;	
//	final long []positions;
	final int [] blockNb;
	final int [] geomNbInBlock;
	final int [] nbPoints;
	final int nbPointsFullGeometry;
	byte [] binaryJSONData;
	
	int originalWGS84BlockNb;
	int originalWGS84GeomNbInBlock;
	
	int pointsCount;
	int polysCount;
	int linestringsCount;
	
	public ShapeIndex(int rowNb,int maxZoomLevel, ODLGeom geom) {
		this.rowNb = rowNb;
		this.nbPointsFullGeometry = ((ODLGeomImpl)geom).getJTSGeometry().getNumPoints();
		countGeoms(((ODLGeomImpl)geom).getJTSGeometry());

//		positions = new long[maxZoomLevel+1];
//		Arrays.fill(positions, ODLRenderOptimisedGeom.INVALID_GEOMETRY);
		blockNb = new int[maxZoomLevel+1];
		geomNbInBlock = new int[maxZoomLevel+1];
		Arrays.fill(blockNb, ODLRenderOptimisedGeom.INVALID_GEOMETRY);
		Arrays.fill(geomNbInBlock, ODLRenderOptimisedGeom.INVALID_GEOMETRY);
		
		nbPoints = new int[maxZoomLevel+1];
		this.wgsBounds = ((ODLGeomImpl)geom).getWGSBounds();
		this.wgsCentroid = ((ODLGeomImpl)geom).getWGSCentroid();
		
	}
	
	public void setBlock(int blockNb ,int geomNbInBlock, int zoom){
		if(zoom < 0){
			originalWGS84BlockNb = blockNb;
			originalWGS84GeomNbInBlock = geomNbInBlock;
		}else{
			this.blockNb[zoom] = blockNb;
			this.geomNbInBlock[zoom] = geomNbInBlock;
		}
	}
	
	private void countGeoms(Geometry g){
		if(g!=null){
			if(GeometryCollection.class.isInstance(g)){
				int n = g.getNumGeometries();
				for(int i =0 ; i<n ; i++){
					countGeoms(g.getGeometryN(i));
				}
			}else if(Point.class.isInstance(g)){
				pointsCount++;
			}
			else if (LineString.class.isInstance(g)){
				linestringsCount++;
			}else if (Polygon.class.isInstance(g)){
				pointsCount++;
			}
		}
	}

	public int findLastDefinedLevel(int minZoomLevel, int startLevel){
		
		int ret = startLevel-1;
		while(ret >=minZoomLevel && blockNb[ret] == ODLRenderOptimisedGeom.USE_LAST_LEVEL){
			ret--;
		}
		
		if(ret >=minZoomLevel && blockNb[ret]!=ODLRenderOptimisedGeom.USE_LAST_LEVEL){
			return ret;
		}
		return -1;
	}

	public Envelope getWgsBounds() {
		return wgsBounds;
	}

	public LatLong getWgsCentroid() {
		return wgsCentroid;
	}


//	public long getOriginalWGS84GeomPosition() {
//		return originalWGS84GeomPosition;
//	}
//
//	public void setOriginalWGS84GeomPosition(long originalWGS84GeomPosition) {
//		this.originalWGS84GeomPosition = originalWGS84GeomPosition;
//	}
	
	
}
