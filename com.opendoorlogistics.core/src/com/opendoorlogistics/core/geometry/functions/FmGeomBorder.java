/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.functions;

import java.util.LinkedList;
import java.util.List;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

public class FmGeomBorder extends FunctionImpl {

	public FmGeomBorder(Function geometry, Function includeHoles) {
		super(geometry, includeHoles);
	}

	private Object createCacheKey(ODLGeom geom, boolean includeHoles){

		class CacheKey{
			ODLGeom geom;
			boolean includeHoles;
			public CacheKey(ODLGeom geom, boolean includeHoles) {
				super();
				this.geom = geom;
				this.includeHoles = includeHoles;
			}
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((geom == null) ? 0 : geom.hashCode());
				result = prime * result + (includeHoles ? 1231 : 1237);
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
				CacheKey other = (CacheKey) obj;
				if (geom == null) {
					if (other.geom != null)
						return false;
				} else if (!geom.equals(other.geom))
					return false;
				if (includeHoles != other.includeHoles)
					return false;
				return true;
			}	
		}
		
		Object cacheKey = new CacheKey(geom, includeHoles);
		return cacheKey;
	}
	
	@Override
	public Object execute(FunctionParameters parameters) {

		// get input parameters
		Object[] childEx = executeChildFormulae(parameters, false);
		for(Object o :childEx){
			if(o==null){
				return null;
			}
		}
		
		Object converted0 = ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, childEx[0]);
		Object converted1 = ColumnValueProcessor.convertToMe(ODLColumnType.LONG, childEx[1]);
		if (converted0 == null || converted1 == null) {
			return Functions.EXECUTION_ERROR;
		}
		ODLGeom geom = (ODLGeom) converted0;
		boolean includeHoles = ((Long) converted1) == 1;
		
		// check for cached result
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.GEOMETRY_BORDER_CACHE);
		Object cacheKey = createCacheKey(geom, includeHoles);
		ODLGeom ret = (ODLGeom)cache.get(cacheKey);
		if(ret!=null){
			return ret;
		}
		
		Geometry g = ((ODLGeomImpl) geom).getJTSGeometry();
		if (g == null) {
			return null;
		}

		LinkedList<LineString> lineStrings = new LinkedList<>();
		recurseFetchLineStrings(g, includeHoles, lineStrings);
		if(lineStrings.size()==0){
			// return null rather than an empty linestring as empty linestrings create problems
			return null;
		}
		
		// create return object and cache it
		Geometry gBorders = new GeometryFactory().createMultiLineString(lineStrings.toArray(new LineString[lineStrings.size()]));
		ret = new ODLLoadedGeometry(gBorders);
		cache.put(cacheKey, ret, ((ODLGeomImpl)ret).getEstimatedSizeInBytes() + 24);
		
		return ret;
	}

	private static void recurseFetchLineStrings(Geometry g, boolean includeHoles, List<LineString> ret) {
		if (g == null) {
			return;
		}

		if (GeometryCollection.class.isInstance(g)) {
			int nGeoms = g.getNumGeometries();
			for (int i = 0; i < nGeoms; i++) {
				recurseFetchLineStrings(g.getGeometryN(i), includeHoles, ret);
			}
		} else if (LineString.class.isInstance(g)) {
			ret.add((LineString) g);
		} else if (Polygon.class.isInstance(g)) {

			Polygon poly = (Polygon) g;
			ret.add(poly.getExteriorRing());

			if (includeHoles) {
				int nHoles = poly.getNumInteriorRing();
				for (int i = 0; i < nHoles; i++) {
					ret.add(poly.getInteriorRingN(i));
				}
			}
		}
	}

	@Override
	public Function deepCopy() {
		return new FmGeomBorder(child(0).deepCopy(), child(1).deepCopy());
	}

}
