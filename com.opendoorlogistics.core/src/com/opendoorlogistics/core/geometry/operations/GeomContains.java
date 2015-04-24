package com.opendoorlogistics.core.geometry.operations;

import gnu.trove.map.hash.TObjectByteHashMap;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;


public class GeomContains {
	private static int MAX_CACHE_POINT_SIZE_PER_RESULTS_OBJ = 100000;
	
	private static class CacheKey{
		final Geometry g;
		final String espg;
		
		CacheKey(Geometry g, String espg) {
			this.g = g;
			this.espg = espg;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((espg == null) ? 0 : espg.hashCode());
			result = prime * result + ((g == null) ? 0 : g.hashCode());
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
			if (g == null) {
				if (other.g != null)
					return false;
			} else if (!g.equals(other.g))
				return false;
			return true;
		}
		
		
	}
	
	private static class Result{
		Geometry projected;
		TObjectByteHashMap<LatLong> results = new TObjectByteHashMap<LatLong>();
	}
	
	/**
	 * Calculate if the input geometry contains the latitude and longitude point.
	 * @param g
	 * @param latitude
	 * @param longitude
	 * @param espg Can be null if just testing in lat-longs
	 * @return
	 */
	public static boolean containsPoint(Geometry g, double latitude, double longitude, String espg){
		
		// Get results object (for whole geometry) from cache if it exists
		CacheKey key = new CacheKey(g, espg);
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.GEOMETRY_CONTAINS_CACHE);
		Object cached = cache.get(key);
		Result result = (Result)cached;
		
		// Create results object if needed
		GridTransforms transforms =null;
		if(result==null){
			result = new Result();
			
			// Transform if needed
			if(espg!=null){
				transforms = GridTransforms.getAndCache(espg);
				result.projected = transforms.wgs84ToGrid(g);
			}
		}

		// Check if we already have the result
		LatLongImpl ll = new LatLongImpl(latitude, longitude);
		if(result.results.containsKey(ll)){
			return result.results.get(ll)==1;
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

		// If the results object is getting silly big, assume most of the results are old and clear them
		if(result.results.size() > MAX_CACHE_POINT_SIZE_PER_RESULTS_OBJ){
			result.results.clear();
		}
		
		// Add result to the results object
		result.results.put(ll, contains ? (byte)1 : (byte)0);
		
		// Remove existing results object from cache (if it was cached) as its size has changed
		if(cached!=null){
			cache.remove(key);			
		}
		
		// Estimate size. We assume that the geometry is wholey owned by the cache record,
		// as typically we use contains for short-lived geometry (e.g. when geometry is edited and each edit is a geom)
		// which may only be referenced from here
		long nbBytes = Spatial.getEstimatedSizeInBytes(g);
		if(transforms!=null){
			nbBytes *=2;
		}
		long llSize = 3*8;
		long mapSize = result.results.size() * (llSize + 8) + 64;
		nbBytes += mapSize;
		
		// Cache it
		cache.put(key, result, nbBytes);
		return contains;
	}
}
