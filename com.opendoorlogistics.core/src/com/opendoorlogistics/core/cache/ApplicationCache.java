/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.cache;

import java.util.HashMap;

/**
 * A class to store all other caches! Note that cache retrieval by string
 * is not standardised - e.g. lower case strings will be treated differently 
 * to upper case strings etc. This is because cache retrieval needs to be fast
 * @author Phil
 *
 */
public class ApplicationCache {
	private static final ApplicationCache singleton = new ApplicationCache();
	private final HashMap<String, RecentlyUsedCache> caches = new HashMap<>();
	
	public static final String DISTANCE_MATRIX_CACHE = "distance-matrix-cache";
	public static final String ROUTE_GEOMETRY_CACHE = "route-geometry-cache";
	public static final String GEOMETRY_MERGER_CACHE = "geometry-merge-cache";
	public static final String GEOMETRY_BORDER_CACHE = "geometry-border-cache";
	public static final String A_TO_B_DISTANCE_METRES_CACHE = "a-to-b-distance-metres-cache";
	public static final String A_TO_B_TIME_SECONDS_CACHE = "a-to-b-time-seconds-cache";
	public static final String IMPORTED_SHAPEFILE_CACHE = "imported-shapefile-cache";
	public static final String GEOM_CENTROID_CACHE = "geom-centroid-cache";
	
	public static ApplicationCache singleton(){
		return singleton;
	}
	
	private ApplicationCache(){
		long MB = 1024*1024;
		create(DISTANCE_MATRIX_CACHE, 128 * MB);
		create(ROUTE_GEOMETRY_CACHE, 64 *MB);
		create(GEOMETRY_MERGER_CACHE, 32* MB);
		create(GEOMETRY_BORDER_CACHE, 32* MB);
		create(A_TO_B_DISTANCE_METRES_CACHE, 12*MB);
		create(A_TO_B_TIME_SECONDS_CACHE, 12*MB);
		create(IMPORTED_SHAPEFILE_CACHE, 128*MB);
		create(GEOM_CENTROID_CACHE, 16 * MB);
	}
	
	public RecentlyUsedCache get(String cacheId){
		return caches.get(cacheId);
	}
	
	public RecentlyUsedCache create(String cacheId, long maxSizeInBytes){
		if(get(cacheId)!=null){
			throw new RuntimeException("Cache already exists with id: " + cacheId);
		}
		
		RecentlyUsedCache ret = new RecentlyUsedCache(maxSizeInBytes);
		caches.put(cacheId,ret );
		return ret;
	}
}
