/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.geometry;

import com.opendoorlogistics.core.utils.SimpleSoftReferenceMap;
import com.vividsolutions.jts.geom.Geometry;

public final class GeomWithCache {
	private final SimpleSoftReferenceMap<Object,Object> cache = new SimpleSoftReferenceMap<>(10);
	//private final WeakHashMap<Object,Object> cache = new WeakHashMap<>();
	private final Geometry jtsGeometry;
	
	public GeomWithCache(Geometry jtsGeometry) {
		this.jtsGeometry = jtsGeometry;
	}

	/**
	 * Geometries can have data cached with them which is used to speed up rendering.
	 * @param cacheKey
	 * @param data
	 */
	public synchronized void putInCache(Object cacheKey, Object data){
		cache.put(cacheKey, data);
	}
	
	public synchronized Object getFromCache(Object cacheKey){
		return cache.get(cacheKey);
	}
	
	public Geometry getJTSGeometry(){
		return jtsGeometry;
	}
	
}
