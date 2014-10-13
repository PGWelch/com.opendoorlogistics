/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.cache;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.opendoorlogistics.core.utils.Pair;

/**
 * Cache which stores only the recently used objects. Objects are stored as soft references
 * and can hence still get dropped from the cache early if the memory is really needed.
 * @author Phil
 *
 */
final public class RecentlyUsedCache {
	private long timeIndex=0;
	private long totalBytes;
	private final long bytesLimit;
	private WeakHashMap<Object, CacheEntry> cached = new WeakHashMap<>();

	public RecentlyUsedCache(long bytesLimit){
		this.bytesLimit = bytesLimit;
	}

	private static class CacheEntry implements Comparable<CacheEntry>{
		static final int CONTAINER_OVERHEAD_BYTES = 8 + 16 + 4 + 8 + 8; // rough guess....
		final Object key;
		final SoftReference<Object> data;
		final long nbBytes;
		long lastUsed;
		
		CacheEntry(Object key,Object obj, long nbBytes) {
			this.key = key;
			this.data = new SoftReference<Object>(obj);
			this.nbBytes = nbBytes + CONTAINER_OVERHEAD_BYTES;
		}
		
		@Override
		public int compareTo(CacheEntry o) {
			// oldest first
			return Long.compare(o.lastUsed, lastUsed);
		}
	}
	
	private void clearIfNeeded(){
		if(timeIndex == Long.MAX_VALUE){
			// reset long value...
			timeIndex = 0;
			cached.clear();
		}else{
			if(totalBytes > bytesLimit ){
				// sort by last used time, latest first
				ArrayList<CacheEntry> sorted = new ArrayList<>();
				for(CacheEntry o : cached.values()){
					if(o.data.get()!=null){
						sorted.add(o);
					}
				}
				Collections.sort(sorted);
				
				// keep on adding until we reach half the bytes limit
				totalBytes = 0;
				cached.clear();
				int i =0;
				while(i<sorted.size() && totalBytes < bytesLimit /2  ){
					cached.put(sorted.get(i).key, sorted.get(i));
					totalBytes += sorted.get(i).nbBytes;
					i++;
				}
				
		//		System.out.println("CLEARED");
			}
		}
	}
	

	public synchronized void put(Object objectKey, Object value, long nbBytes){
		timeIndex++;
		CacheEntry obj = new CacheEntry(objectKey, value, nbBytes);
		obj.lastUsed = timeIndex;
		cached.put(objectKey, obj);
		totalBytes += obj.nbBytes;
		clearIfNeeded();
	}
	
	
	public synchronized Object get(Object key){
		CacheEntry c = cached.get(key);
		if(c!=null){
			c.lastUsed = timeIndex;
			Object obj = c.data.get();
			if(obj!=null){
				return obj;
			}else{
				// collected already....
				cached.remove(key);
				totalBytes -= c.nbBytes;
			}
		}
		return null;

	}
	
	public static void main(String []args){
		RecentlyUsedCache lus = new RecentlyUsedCache(10*(8 + CacheEntry.CONTAINER_OVERHEAD_BYTES));
		int n = 1000;
		for(int i =0 ; i < n;i++){
			Integer val = new Integer(i);
			lus.put(val, val, 8);
			lus.get(2);
			System.out.println("i=" + i + " - " + lus.toString());
		}
	}

	public synchronized void clear(){
		timeIndex=0;
		totalBytes=0;
		cached.clear();
	}
	
	@Override
	public synchronized String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int count=0;
		for(Map.Entry<Object, CacheEntry> entry:cached.entrySet()){
			Object val = entry.getValue().data.get();
			if(val!=null){
				if(count>0){
					builder.append(", ");
				}
				builder.append("{" + entry.getKey() + "=" + val + "}");
				count++;
			}
		}
		builder.append("]");
		return builder.toString();
	}
	
	/**
	 * Get snapshot of the keys and values stored in the cache. 
	 * This could change directly after calling this method if anything
	 * is garbage collected. Calling this method does not update the last
	 * used state on the entries.
	 * @return
	 */
	public synchronized List<Pair<Object, Object>> getSnapshot(){
		ArrayList<Pair<Object, Object>> ret = new ArrayList<>(cached.size());
		for(Map.Entry<Object, CacheEntry> entry:cached.entrySet()){
			Object val= entry.getValue().data.get();
			if(val!=null){
				ret.add(new Pair<Object, Object>(entry.getKey(), val));
			}
		}
		return ret;
	}
	
	public synchronized void remove(Object key){
		CacheEntry container = cached.get(key);
		if(container!=null){
			cached.remove(key);
			totalBytes -= container.nbBytes;
		}
	}
}
