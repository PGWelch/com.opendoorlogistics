/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.geometry;

/**
 * An immutable geometry type, which for the most part is just a wrapper around a JTS geometry.
 * http://www.vividsolutions.com/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html
 * @author Phil
 *
 */
public interface ODLGeom {
	int getPointsCount();
	
	  /**
	   * Returns the number of geometries in a geometry
	   * (or 1, if the geometry is not a collection).
	   * See http://www.vividsolutions.com/jts/javadoc/com/vividsolutions/jts/geom/Geometry.html
	   * @return the number of geometries contained in this geometry
	   */
	int getNbChildGeometries();
	
	ODLGeom getChildGeom(int i);
	
	enum GeomType{
		POINT(false),
		LINESTRING(false),
		POLYGON(false),
		MULTIPOINT(true),
		MULTILINESTRING(true),
		MULTIPOLYGON(true),
		COLLECTION(true),
		
		/**
		 * An invalid geometry is one which couldn't be loaded, for example.
		 */
		INVALID(false);
		
		private final boolean isCollection;

		private GeomType(boolean isCollection) {
			this.isCollection = isCollection;
		}

		public boolean isCollection() {
			return isCollection;
		}
		
		
	}
	
	GeomType getGeomType();
	
	/**
	 * Get the ith point. If the type is a polygon, the last point is guaranteed to be 
	 * equal to the first point.
	 * @param i
	 * @return
	 */
	LatLong getPoint(int i);
	
	/**
	 * Get the number of holes if the geometry is a polygon, otherwise throw an exception
	 * @return
	 */
	int getNbHoles();
	
	/**
	 * Get the exterior if the geometry is a polygon, otherwise throw an exception
	 * @return
	 */
	ODLGeom getExterior();
	
	/**
	 * Get the whole if the geometry is a polygon, otherwise throw an exception
	 * @param i
	 * @return
	 */
	ODLGeom getHole(int i);
}
