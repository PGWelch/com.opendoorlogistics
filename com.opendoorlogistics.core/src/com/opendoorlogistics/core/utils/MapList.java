/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Iterator;
import java.util.List;

/**
 * A collection which supports list ordering and retrieval by id.
 * @author Phil
 *
 * @param <T>
 */
final public class MapList<T> implements Iterable<T> {
	private class MapListNode{
		final T value;
		final int id;
		
		MapListNode( int id, T value) {
			this.value = value;
			this.id = id;
		}
		
		@Override
		public String toString(){
			return Integer.toString(id);
		}
	}
	
	private final TIntObjectHashMap<MapListNode> map = new TIntObjectHashMap<>();
	private final List<MapListNode> list = new LargeList<>();

	
	public void add(int id, T obj){
		id = validateId(id);
		
		MapListNode node = new MapListNode(id,obj);
		map.put(id, node);
		list.add(node);
	}

	public boolean containsID(int id){
		return map.containsKey(id);
	}
	
	private int validateId(int id) {

		if(id==-1){
			throw new IllegalArgumentException("Invalid key");			
		}
		
		if(map.containsKey(id)){
			throw new IllegalArgumentException("Duplicate key");
		}
		return id;
	}
	
	public int size(){
		return list.size();
	}
	
	public T getAt(int i){
		return list.get(i).value;
	}
	
	public T getByID(int id){
		MapListNode node = map.get(id);
		if(node!=null){
			return node.value;
		}
		return null;
	}
	
	public void clear(){
		list.clear();
		map.clear();
	}
	
	public void insertAt(int index, int id,T obj){
		id = validateId(id);
		MapListNode node = new MapListNode(id,obj);
		map.put(id, node);
		if(index > list.size()){
			index = list.size();
		}
		list.add(index, node);
	}
	
	public int getIDAt(int index){
		return list.get(index).id;
	}
	
	public void removeAt(int index){
		int id = getIDAt(index);
		list.remove(index);
		map.remove(id);
	}
	
	public static void main(String []args){
		MapList<String> mapList = new MapList<>();
		for(int i = 0 ; i< 10000000 ; i++){
			if(i%1000==0){
				System.out.println(i);
			}
			mapList.add(i, "");
		}
	}

	@Override
	public Iterator<T> iterator() {
		final Iterator<MapListNode> nodeIt = list.iterator();
		return new Iterator<T>() {

			@Override
			public boolean hasNext() {
				return nodeIt.hasNext();
			}

			@Override
			public T next() {
				return nodeIt.next().value;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
