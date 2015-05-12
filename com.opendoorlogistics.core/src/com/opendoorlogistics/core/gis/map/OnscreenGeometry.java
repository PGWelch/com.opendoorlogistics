/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.geotools.geometry.jts.JTS;

import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.gis.map.transforms.TransformGeomToWorldBitmap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

/**
 * Geometry transformed to world bitmap coord system
 * @author Phil
 *
 */
public final class OnscreenGeometry {
	private Point2D.Double lineStringMidpoint;
	private double linestringLength=Double.NaN;
	private final boolean drawFilledBounds;
	private final Geometry wbFinal;

	public long getSizeInBytes(){
		long ret=0;
		
		if(wbFinal!=null){
			ret += Spatial.getEstimatedSizeInBytes(wbFinal);
		}
		
		ret += 4*4 + 1 + 16 + 16;
		return ret;
	}
	
	
	public static class CachedGeomKey{
		private final ODLGeom geom;
		private final Object otherKey;
		
		public CachedGeomKey(ODLGeom geom, Object otherKey) {
			this.geom = geom;
			this.otherKey = otherKey;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((geom == null) ? 0 : geom.hashCode());
			result = prime * result + ((otherKey == null) ? 0 : otherKey.hashCode());
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
			CachedGeomKey other = (CachedGeomKey) obj;
			if (geom == null) {
				if (other.geom != null)
					return false;
			} else if (!geom.equals(other.geom))
				return false;
			if (otherKey == null) {
				if (other.otherKey != null)
					return false;
			} else if (!otherKey.equals(other.otherKey))
				return false;
			return true;
		}
		
		
	}
	
	private static final boolean isAllLineStrings(Geometry g){
		if(g==null){
			return false;
		}
		
		if(LineString.class.isInstance(g)){
			return true;
		}
		
		if(GeometryCollection.class.isInstance(g)){
			int n = g.getNumGeometries();
			for(int i =0 ; i<n;i++){
				if(!isAllLineStrings(g.getGeometryN(i))){
					return false;
				}
			}
		}
		
		return false;
	}
	
	public OnscreenGeometry(Geometry onscreen, boolean drawFilledBounds){
		this.wbFinal = onscreen;
		this.drawFilledBounds = drawFilledBounds;
	}
	
	/**
	 * Initialise the cached geometry and transform to screen coordinates
	 * so we can get the bounds. Simplifying the geometry is not done until later.
	 * @param geom
	 * @param simplify
	 * @param latLongToScreen
	 */
	public OnscreenGeometry(ODLGeomImpl geom, LatLongToScreen latLongToScreen) {

		// geometry starts in long-lat; transform to world bitmap
		Geometry initialGeometry = geom.getJTSGeometry();
		try {
			initialGeometry = JTS.transform(geom.getJTSGeometry(), new TransformGeomToWorldBitmap(latLongToScreen));
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		// get bounds ensuring we have non-zero width and height in pixel space otherwise points don't draw...
		Envelope bb = initialGeometry.getEnvelopeInternal();
		Rectangle2D wbBounds = new Rectangle2D.Double(bb.getMinX(), bb.getMinY(),Math.max( bb.getWidth(),1),Math.max( bb.getHeight(),1));

		double simpleTol = Spatial.getRendererSimplifyDistanceTolerance();
		if(isAllLineStrings(initialGeometry)){
			simpleTol = Spatial.getRendererSimplifyDistanceToleranceLineString();
		}
		if (simpleTol>0) {

			// treat as circle if really small
			if (wbBounds.getWidth() < simpleTol && wbBounds.getHeight() < simpleTol) {
				wbFinal = createSimplificationCircle(wbBounds);
				drawFilledBounds = true;
			} else {
				Geometry simplified = TopologyPreservingSimplifier.simplify(initialGeometry, simpleTol);

				// if empty (because its too small), just treat as a point
				if (simplified.getNumPoints() == 0) {
					wbFinal = createSimplificationCircle(wbBounds);
					drawFilledBounds = true;
				} else {
					wbFinal = simplified;
					drawFilledBounds = false;
				}
			}
		} else {
			// use initial unsimplified geometry
			drawFilledBounds = false;
			wbFinal = initialGeometry;
		}
	}



	private Geometry createSimplificationCircle(Rectangle2D wbBounds) {
		Geometry geometry;
		GeometryFactory factory = new GeometryFactory();
		geometry = factory.createPoint(new Coordinate(wbBounds.getCenterX(), wbBounds.getCenterY()));
		return geometry;
	}

	Geometry getJTSGeometry() {
		return wbFinal;
	}

	/**
	 * If the image is really small (i.e. 1x2 pixels), rather than draw it we just draw a filled rectangle for its bounds
	 * 
	 * @return
	 */
	boolean isDrawFilledBounds() {
		return drawFilledBounds;
	}

	public boolean isLineString(){
		Geometry geometry = getJTSGeometry();
		if(geometry!=null){
			return LineString.class.isInstance(geometry);
		}
		return false;
	}

	public synchronized double getLineStringLength(){
		if(Double.isNaN(linestringLength)==false){
			return linestringLength;
		}
		
		if(!isLineString()){
			throw new RuntimeException("Cannot calculate the length of a line for a non-line geometry.");
		}

		LineString geometry = (LineString)getJTSGeometry();
		linestringLength = geometry.getLength();
		return linestringLength;
	}
	
	public synchronized Point2D getLineStringMidPoint(){
		if(lineStringMidpoint!=null){
			return lineStringMidpoint;
		}
		
		if(!isLineString()){
			return null;
		}
		
		// check for empty geometry 
		LineString geometry = (LineString)getJTSGeometry();
		double length = geometry.getLength();
		if(length==0){
			Point pnt = geometry.getCentroid();
			lineStringMidpoint = new Point2D.Double(pnt.getX(), pnt.getY());
			return lineStringMidpoint;
		}
		
		// Calculate for general case
		Coordinate [] coords = geometry.getCoordinates();
		double midpoint = length * 0.5;
		
		double sum=0;
		for(int i =0 ; i< coords.length-1 ; i++){
			Coordinate a = coords[i];
			Coordinate b = coords[i+1];
			double segmentLength = a.distance(b);
			if( (sum<=midpoint && (sum+segmentLength)>=midpoint) || (i==coords.length-2)){
				double denominator = segmentLength>0 ? segmentLength:1;
				Coordinate unitVector = new Coordinate( (b.x-a.x) / denominator, (b.y-a.y)/ denominator);

				double lengthAlong = midpoint - sum;
				lineStringMidpoint = new Point2D.Double(a.x + unitVector.x * lengthAlong, a.y + unitVector.y * lengthAlong);
				break;
			}
			sum+=segmentLength;
		}
		return lineStringMidpoint;
	}
}
