package com.opendoorlogistics.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

public class MultiHashMap<K,V> {
	private final HashMap<K, List<V>> map;
	
	public MultiHashMap(){
		map = new HashMap<>();
	}
	
	public MultiHashMap(int initialCapacity){
		map = new HashMap<>(initialCapacity);
	}
	
	public List<V> get(K key){
		return map.get(key);
	}
	
	public void put(K key, V value){
		List<V> list = map.get(key);
		if(list==null){
			list = new ArrayList<>(1);
			map.put(key, list);
		}
		list.add(value);
	}
	
	public void remove(K key, V value){
		List<V> list = map.get(key);
		if(list!=null){
			list.remove(value);
			if(list.size()==0){
				map.remove(key);
			}
		}
	}
	
	public void remove(K key){
		map.remove(key);
	}
	
	public int keySize(){
		return map.size();
	}
	
	public Set<K> keySet(){
		return map.keySet();
	}
	
	
	public Set<Entry<K, List<V>>> entrySet(){
		return map.entrySet();
	}

}
