package com.opendoorlogistics.core.geometry.operations;

import gnu.trove.map.hash.TObjectByteHashMap;

import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.utils.ObjectDefaultSystemHashingDecorator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;


public class GeomContains {
	private static int MAX_CACHE_POINT_SIZE_PER_RESULTS_OBJ = 100000;
	
	private static class CacheKey{
		final Geometry geometry;
		final ObjectDefaultSystemHashingDecorator geomHashingDecorator;
		final String espg;
		
		CacheKey(Geometry g, String espg) {
			this.geomHashingDecorator = new ObjectDefaultSystemHashingDecorator(g);
			this.geometry = g;
			this.espg = espg;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((espg == null) ? 0 : espg.hashCode());
			result = prime * result + ((geomHashingDecorator == null) ? 0 : geomHashingDecorator.hashCode());
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
			if (espg == null) {
				if (other.espg != null)
					return false;
			} else if (!espg.equals(other.espg))
				return false;
			if (geomHashingDecorator == null) {
				if (other.geomHashingDecorator != null)
					return false;
			} else if (!geomHashingDecorator.equals(other.geomHashingDecorator))
				return false;
			return true;
		}
		
		
	}
	
	private static class CachedResultRecord{
		Geometry projected;
		TObjectByteHashMap<Coordinate> results = new TObjectByteHashMap<Coordinate>();
	}
	
	/**
	 *  Calculate if the input geometry contains the input point, using the projection of both
	 * @param g
	 * @param c
	 * @return
	 */
	public static boolean containsPoint(Geometry g, Coordinate c){
		// Get results object (for whole geometry) from cache if it exists
		CacheKey key = new CacheKey(g, null);
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.PROJECTED_GEOMETRY_CONTAINS_CACHE);
		Object cached = cache.get(key);
		CachedResultRecord result = (CachedResultRecord)cached;
		
		if(result==null){
			result = new CachedResultRecord();
		}
		
		// Check if we already have the result
		if(result.results.containsKey(c)){
			return result.results.get(c)==1;
		}
		
		// calculate it
		GeometryFactory factory = new GeometryFactory();
		Geometry p = factory.createPoint(c);
		boolean contains = g.contains(p);
		
		cacheResult(key, new Coordinate(c), contains, false, cached!=null, result, cache);
		return contains;
	}
	
	/**
	 * Calculate if the input geometry contains the latitude and longitude point.
	 * Calculate in a projection if needed
	 * @param g Geometry in WGS84
	 * @param latitude
	 * @param longitude
	 * @param espg Can be null if just testing in lat-longs
	 * @return
	 */
	public static boolean containsPoint(Geometry g, double latitude, double longitude, String espg){
		
		// Get results object (for whole geometry) from cache if it exists
		CacheKey key = new CacheKey(g, espg);
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.PROJECTABLE_GEOMETRY_CONTAINS_CACHE);
		Object cached = cache.get(key);
		CachedResultRecord result = (CachedResultRecord)cached;
		
		// Create results object if needed
		GridTransforms transforms =null;
		if(result==null){
			result = new CachedResultRecord();
			
			// Transform if needed
			if(espg!=null){
				transforms = GridTransforms.getAndCache(espg);
				result.projected = transforms.wgs84ToGrid(g);
			}
		}

		// Check if we already have the result
		Coordinate coordinate = new Coordinate(longitude, latitude, 0);
		if(result.results.containsKey(coordinate)){
			return result.results.get(coordinate)==1;
		}

		// Calculate the contains, projecting as needed
		Geometry polygonGeom = g;
		GeometryFactory factory = new GeometryFactory();
		Geometry pointGeom = factory.createPoint(new Coordinate(longitude, latitude));
		if(transforms!=null){
			polygonGeom = result.projected;
			pointGeom = transforms.wgs84ToGrid(pointGeom);
		}
		boolean contains = polygonGeom.contains(pointGeom);

		cacheResult(key, coordinate, contains, transforms!=null, cached!=null, result, cache);
		return contains;
	}

	private static void cacheResult(CacheKey key, Coordinate coordinate, boolean isContained, boolean hasTransform, boolean wasCached, CachedResultRecord cachedResultRecord, RecentlyUsedCache cache) {
		// If the results object is getting silly big, assume most of the results are old and clear them
		if(cachedResultRecord.results.size() > MAX_CACHE_POINT_SIZE_PER_RESULTS_OBJ){
			cachedResultRecord.results.clear();
		}
		
		// Add result to the results object
		cachedResultRecord.results.put(coordinate, isContained ? (byte)1 : (byte)0);
		
		// Remove existing results object from cache (if it was cached) as its size has changed
		if(wasCached){
			cache.remove(key);			
		}
		
		// Estimate size. We assume that the geometry is wholey owned by the cache record,
		// as typically we use contains for short-lived geometry (e.g. when geometry is edited and each edit is a geom)
		// which may only be referenced from here
		long nbBytes = Spatial.getEstimatedSizeInBytes(key.geometry);
		if(hasTransform){
			nbBytes *=2;
		}
		long llSize = 3*8;
		long mapSize = cachedResultRecord.results.size() * (llSize + 8) + 64;
		nbBytes += mapSize;
		
		// Cache it
		cache.put(key, cachedResultRecord, nbBytes);
	}
}
