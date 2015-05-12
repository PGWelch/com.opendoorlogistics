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
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.geometry.ODLGeom.GeomType;
import com.opendoorlogistics.core.gis.map.OnscreenGeometry;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Immutable geometry class. An ODLGeom may not be modified after creation.
 * Internally this class uses the JTS geometry class.
 * @author Phil
 *
 */
public abstract class ODLGeomImpl implements ODLGeom{
	private ODLGeom [] children;

	public abstract String toText();
	
	@Override
	public String toString(){
		return toText();
	}
	

	//public abstract  void putInCache(Object cacheKey, Object data);
	
	//public abstract Object getFromCache(Object cacheKey);
	
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
	
	public int getNbChildGeometries(){
		Geometry jts = getJTSGeometry();
		return jts!=null ? jts.getNumGeometries():0;
	}
	
	public synchronized ODLGeom getChildGeom(int i){

		Geometry jts = getJTSGeometry();
		if(!GeometryCollection.class.isInstance(jts)){
			if(i>0){
				throw new RuntimeException();
			}
			return this;
		}

		int n = getNbChildGeometries();
		if(children==null){
			children = new ODLGeom[n];
		}
		
		if(children[i]==null){
			children[i] =new ODLLoadedGeometry(getJTSGeometry().getGeometryN(i));
		}
		
		return children[i];
	}
	
	public GeomType getGeomType(){
		Geometry jts = getJTSGeometry();
		if(jts!=null){
			if(Point.class.isInstance(jts)){
				return GeomType.POINT;
			}
			if(LineString.class.isInstance(jts)){
				return GeomType.LINESTRING;
			}
			if(Polygon.class.isInstance(jts)){
				return GeomType.POLYGON;
			}
			if(MultiPoint.class.isInstance(jts)){
				return GeomType.MULTIPOINT;
			}
			if(MultiLineString.class.isInstance(jts)){
				return GeomType.MULTILINESTRING;
			}
			if(MultiPolygon.class.isInstance(jts)){
				return GeomType.MULTIPOLYGON;
			}
			if(GeometryCollection.class.isInstance(jts)){
				return GeomType.COLLECTION;
			}
		}
		
		return GeomType.INVALID;
	}

	public LatLong getPoint(int i){
		Geometry jts = getJTSGeometry();
		if(jts!=null){
			Coordinate c = null;
			if(LineString.class.isInstance(jts)){
				// more efficient doing this...
				c = ((LineString)jts).getCoordinateN(i);
			}else{
				c = jts.getCoordinates()[i];				
			}
			return new LatLongImpl(c.y, c.x);
		}
		return null;
	}
	
	@Override
	public int getNbHoles(){
		throwIfNotPolygon();
		return ((Polygon)getJTSGeometry()).getNumInteriorRing();
	}

	private void throwIfNotPolygon() {
		if(getGeomType() != GeomType.POLYGON){
			throw new RuntimeException("Cannot call polygon method on non-polygon geometry");			
		}
	}
	
	@Override
	public ODLGeom getExterior(){
		throwIfNotPolygon();		
		allocatePolygonPartsArray();
		if(children[0]==null){
			children[0] = new ODLLoadedGeometry(((Polygon)getJTSGeometry()).getExteriorRing());
		}
		return children[0];
	}
	
	private synchronized void allocatePolygonPartsArray(){
		if(children==null){
			int nholes = getNbHoles();
			children = new ODLGeom[nholes+1];
		}
		
	}
	
	@Override
	public ODLGeom getHole(int i){
		throwIfNotPolygon();		
		allocatePolygonPartsArray();
		if(children[i+1]==null){
			children[i+1] = new ODLLoadedGeometry(((Polygon)getJTSGeometry()).getInteriorRingN(i));
		}
		return children[i+1];
	}
}
