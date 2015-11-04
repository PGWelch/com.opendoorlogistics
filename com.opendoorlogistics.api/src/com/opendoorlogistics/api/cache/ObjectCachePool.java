package com.opendoorlogistics.api.cache;

/**
 * A pool of multiple caches
 * @author Phil
 *
 */
public interface ObjectCachePool {
	/**
	 * Get cache or return null if doesn't exist
	 * @param cacheId
	 * @return
	 */
	ObjectCache get(String cacheId);
	
	/**
	 * Create a cache. CacheId's must be globally unique, so using package names is a good idea.
	 * @param cacheId
	 * @param maxSizeInBytes
	 * @return
	 */
	ObjectCache create(String cacheId, long maxSizeInBytes);
}
