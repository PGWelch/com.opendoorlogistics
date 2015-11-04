package com.opendoorlogistics.api.cache;

/**
 * A simple cache for objects which clears objects when it runs out of memory,
 * preserving the most recent objects
 * @author Phil
 *
 */
public interface ObjectCache {
	void clear();
	Object get(Object key);
	void put(Object objectKey, Object value, long nbBytes);
	void remove(Object key);

}
