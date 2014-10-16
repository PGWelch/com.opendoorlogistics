/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry;

import com.opendoorlogistics.core.geometry.ODLGeomImpl.AtomicGeomType;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class JTSUtils {
	
	public static int getGeomCount(Geometry g,AtomicGeomType type){
		int ret=0;
		if(g!=null){
			if(GeometryCollection.class.isInstance(g)){
				int n = g.getNumGeometries();
				for(int i =0 ; i<n ; i++){
					ret+=getGeomCount(g.getGeometryN(i),type);
				}
			}else if(type == AtomicGeomType.POINT && Point.class.isInstance(g)){
				ret++;
			}
			else if (type == AtomicGeomType.LINESTRING && LineString.class.isInstance(g)){
				ret++;
			}else if (type == AtomicGeomType.POLYGON && Polygon.class.isInstance(g)){
				ret++;
			}
		}
		return ret;
	}
}
