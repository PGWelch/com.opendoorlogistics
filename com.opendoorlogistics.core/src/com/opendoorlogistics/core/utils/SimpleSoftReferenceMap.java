/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.corba.se.impl.oa.poa.ActiveObjectMap.Key;

/**
 * A very simple map using soft references (references which are garbage-collected
 * when needed). Every nbPutsTillClear puts the map is checked for any references
 * which are no longer valid and the keys are removed.  
 * @author Phil
 *
 * @param <Key>
 * @param <Value>
 */
final public class SimpleSoftReferenceMap<Key,Value> implements Iterable<Map.Entry<Key,Value>> {
	private final HashMap<Key, SoftReference<Value>> map = new HashMap<>();
	private static final int DEFAULT_PUTS_TILL_CLEAR = 1000;
	private int nbPuts;
	private int putsTillClear = DEFAULT_PUTS_TILL_CLEAR;
	
	public SimpleSoftReferenceMap(){	
	}
	
	public SimpleSoftReferenceMap(int nbPutsTillClear){
		this.putsTillClear = nbPutsTillClear;
	}
	
	public void clear(){
		map.clear();
	}
	
	public Value get(Key key){
		Value ret=null;
		SoftReference<Value> soft = map.get(key);
		if(soft!=null){
			ret = soft.get();
			if(ret==null){
				map.remove(key);
			}
		}
		return ret;
	}
	
	public void put(Key key, Value value){
		map.put(key, new SoftReference<Value>(value));
		
		if(nbPuts>putsTillClear){
			Iterator<Map.Entry<Key,SoftReference<Value>>> it = map.entrySet().iterator();
			while(it.hasNext()){
				if(it.next().getValue().get()==null){
					it.remove();
				}
			};
			nbPuts=0;
		}else{
			nbPuts++;
		}
	}
	
	public int size(){
		return map.size();
	}
	
	public void remove(Key key){
		map.remove(key);
	}

	@Override
	public Iterator<Entry<Key, Value>> iterator() {
		HashMap<Key, Value> tmpMap = new HashMap<>();
		for(Map.Entry<Key, SoftReference<Value>> entry:map.entrySet()){
			Value val = entry.getValue().get();
			if(val!=null){
				tmpMap.put(entry.getKey(), val);
			}
		}
		return tmpMap.entrySet().iterator();
	}
}
