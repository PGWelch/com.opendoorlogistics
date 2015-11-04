/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.cache;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.opendoorlogistics.api.cache.ObjectCachePool;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.utils.Pair;

/**
 * A class to store all other caches! Note that cache retrieval by string
 * is not standardised - e.g. lower case strings will be treated differently 
 * to upper case strings etc. This is because cache retrieval needs to be fast
 * @author Phil
 *
 */
public class ApplicationCache implements Disposable, ObjectCachePool{
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
	public static final String PROJECTED_RENDERER_GEOMETRY = "projected-renderer-geometry";
	public static final String ROG_QUADTREE_BLOCKS = "render-optimised-geometry-quadtree-blocks";
	public static final String ROG_FULL_GEOMETRY = "render-optimised-geometry-full-geometry";
	public static final String MAPSFORGE_BACKGROUND_TILES = "mapsforge-background-tiles";
	public static final String TEXT_LAYOUT_CACHE = "text-layout-cache";
	public static final String SYNCHRONOUS_RETRIEVED_TILE_CACHE = "synchronous-retrieved-tile-cache";
	public static final String LOOKUP_NEAREST_TRANSFORMED_GEOMS = "lookup-nearest-transformed-geoms";
	public static final String IMAGE_FORMULAE_CACHE = "image-formulae-cache";
	public static final String IMAGE_WITH_VIEW_FORMULAE_CACHE = "image-with-view-formulae-cache";
	public static final String PROJECTABLE_GEOMETRY_CONTAINS_CACHE = "projectable-geometry-contains-cache";
	public static final String PROJECTED_GEOMETRY_CONTAINS_CACHE = "projected-geometry-contains-cache";
	public static final String GRID_TRANSFORMS_CACHE = "grid-transforms-cache";
	public static final String FAST_CONTAINED_POINTS_QUADTREE= "fast-contained-points-quadtree";
	public static final String FUNCTION_IMPORTED_DATASTORES= "function-imported-datastores";
	
	public void clearCache(){
		for(RecentlyUsedCache cache : caches.values()){
			cache.clear();
		}
	}
	
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
		create(PROJECTED_RENDERER_GEOMETRY, 256 * MB);
		create(ROG_QUADTREE_BLOCKS, 128 * MB);
		create(ROG_FULL_GEOMETRY, 64 * MB);
		create(MAPSFORGE_BACKGROUND_TILES, 64 * MB);
		create(TEXT_LAYOUT_CACHE, 32 * MB);
		create(SYNCHRONOUS_RETRIEVED_TILE_CACHE, 32 * MB);
		create(LOOKUP_NEAREST_TRANSFORMED_GEOMS, 256 * MB);
		create(IMAGE_FORMULAE_CACHE, 64 * MB);
		create(IMAGE_WITH_VIEW_FORMULAE_CACHE, 64 * MB);
		create(PROJECTABLE_GEOMETRY_CONTAINS_CACHE, 64 * MB);
		create(PROJECTED_GEOMETRY_CONTAINS_CACHE, 64 * MB);
		create(GRID_TRANSFORMS_CACHE, 6 * MB);
		create(FAST_CONTAINED_POINTS_QUADTREE, 64 * MB);
		create(FUNCTION_IMPORTED_DATASTORES, 512 * MB);
		
		
		
				
	}
	
	@Override
	public RecentlyUsedCache get(String cacheId){
		return caches.get(cacheId);
	}
	
	@Override
	public RecentlyUsedCache create(String cacheId, long maxSizeInBytes){
		if(get(cacheId)!=null){
			throw new RuntimeException("Cache already exists with id: " + cacheId);
		}
		
		RecentlyUsedCache ret = new RecentlyUsedCache(cacheId,maxSizeInBytes);
		caches.put(cacheId,ret );
		return ret;
	}
	
	public String getUsageReport(){
		StringBuilder builder = new StringBuilder();
		long total=0;
		
		ArrayList<Pair<Long, String>> list = new ArrayList<Pair<Long,String>>();
		for(Map.Entry<String,RecentlyUsedCache> entry : caches.entrySet()){
			long bytes = entry.getValue().getEstimatedTotalBytes();
			list.add(new Pair<Long, String>(bytes, entry.getKey()));
			total += bytes;
		}
		
		Collections.sort(list, new Comparator<Pair<Long, String>>() {

			@Override
			public int compare(Pair<Long, String> o1, Pair<Long, String> o2) {
				int diff = o2.getFirst().compareTo(o1.getFirst());
				if(diff==0){
					diff = o1.getSecond().compareTo(o2.getSecond());
				}
				return diff;
			}
		});
		
		class ToMbString{
			String toMB(long bytes){
				double val =(double) bytes / (1024*1024);
				DecimalFormat df = new DecimalFormat("0.00");
				return df.format(val);
			}
		}
		
		ToMbString toMB = new ToMbString();
		
		builder.append("Estimated total usage is " + toMB.toMB(total) + " MB" + System.lineSeparator());
		for(Pair<Long, String> pair : list){
			builder.append(pair.getSecond() + " estimated " + toMB.toMB(pair.getFirst()) + " MB" + System.lineSeparator());
		}
		return builder.toString();
	}

	@Override
	public void dispose() {
		
	}
}
