/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.iterators;

import java.util.ArrayList;
import java.util.List;


final public class IteratorUtils {
	private IteratorUtils(){}
	
	public static <T> boolean contains(Iterable<T> iterable, T val){
		for(T t : iterable){
			if(t.equals(val)){
				return true;
			}
		}
		return false;
	}

	public static interface ChildExtracter<TParent, TChild> {
		TChild extract(TParent parent);
	}
	
	public static <T> T first(Iterable<T> iterable){
		for(T obj :iterable){
			return obj;
		}
		return null;
	}
	
	/**
	 * Return the size of the iterable or 0 if its null
	 * @param iterable
	 * @return
	 */
	public static int size(Iterable<?> iterable){
		if(iterable==null){
			return 0;
		}
		int count=0;
		for(@SuppressWarnings("unused") Object o : iterable){
			count++;
		}
		return count;
	}
	
	/**
	 * Extract all non-null child objects from the iterable using the extracter interface
	 * @param iterable
	 * @param extracter
	 * @return
	 */
	static public <TParent, TChild>  List<TChild> extractSubset(Iterable<TParent> iterable, ChildExtracter<TParent,TChild> extracter){
		ArrayList<TChild> ret = new ArrayList<>();
		for(TParent parent : iterable){
			TChild child = extracter.extract(parent);
			if(child!=null){
				ret.add(child);
			}
		}
		return ret;
	}

	public static <T> List<T> toList(Iterable<T> iterable){
		ArrayList<T>ret = new ArrayList<>();
		for(T obj:iterable){
			ret.add(obj);
		}
		return ret;
	}
	
}
