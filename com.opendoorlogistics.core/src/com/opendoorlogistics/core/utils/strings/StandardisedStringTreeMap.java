/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.strings;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class StandardisedStringTreeMap <T> implements Map<String, T> {
	private TreeMap<String, T> map = new TreeMap<>(new Comparator<String>(){

		@Override
		public int compare(String o1, String o2) {
			return Strings.compareStd(o1, o2);
		}
		
	});
	
	@Override
	public T put(String id, T o){
		T ret = get(id);
		map.put(canonical(id), o);
		return ret;
	}

	public T remove(String id){
		return map.remove(canonical(id));
	}
	
	public T get(String id){
		return map.get(canonical(id));
	}
	
	@Override
	public int size(){
		return map.size();
	}


	@Override
	public Set<Map.Entry<String, T>> entrySet(){
		return map.entrySet();
	}
	
	protected String canonical(String s){
		return Strings.std(s);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
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
		StandardisedStringTreeMap other = (StandardisedStringTreeMap) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}

	@Override
	 public Collection<T> values(){
		 return map.values();
	 }

	@Override
	public boolean isEmpty() {
		return map.size()==0;
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(canonical(key.toString()));
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T get(Object key) {
		return get(key.toString());
	}

	@Override
	public T remove(Object key) {
		return remove(key.toString());
	}

	@Override
	public void putAll(Map<? extends String, ? extends T> m) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Set<String> keySet() {
		return map.keySet();
	}
	
}
