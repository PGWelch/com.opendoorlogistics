package com.opendoorlogistics.core.utils;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;

public class MultiTreeSet<T> {
	private int size;
	private TreeMap<T , LinkedList<T>> map = new TreeMap<T, LinkedList<T>>();
	
	public void add(T obj){
		LinkedList<T> list = map.get(obj);
		if(list == null){
			list = new LinkedList<T>();
			map.put(obj, list);
		}
		list.add(obj);
		size++;
	}

	public T pollFirst(){
		Entry<T,LinkedList<T>> entry = map.firstEntry();
		T ret = entry.getValue().removeFirst();
		if(entry.getValue().size()==0){
			map.pollFirstEntry();
		}
		size--;
		return ret;
	}
	
	public int size(){
		return size;
	}
	
	public T last(){
		Entry<T,LinkedList<T>> entry = map.lastEntry();		
		if(entry!=null){
			return entry.getValue().getLast();
		}
		return null;
	}
	
	public T first(){
		Entry<T,LinkedList<T>> entry = map.firstEntry();		
		if(entry!=null){
			return entry.getValue().getFirst();
		}
		return null;
	}
}