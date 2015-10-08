/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.strings;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class StandardisedStringSet implements Set<String>{
	private final boolean useNumberSortationLogic;
	
	private TreeSet<String> set = new TreeSet<>(new Comparator<String>(){

		@Override
		public int compare(String o1, String o2) {
			return Strings.compareStd(o1, o2, useNumberSortationLogic);
		}
		
	});
	
	public StandardisedStringSet(boolean useNumberSortationLogic){
		this.useNumberSortationLogic = useNumberSortationLogic;
	}

	public StandardisedStringSet(boolean useNumberSortationLogic,Iterable<String> iterable){
		this.useNumberSortationLogic = useNumberSortationLogic;
		for(String s : iterable){
			add(s);
		}
	}
	
	public StandardisedStringSet(boolean useNumberSortationLogic,String ...strs){
		this.useNumberSortationLogic = useNumberSortationLogic;
		for(String s:strs){
			add(s);
		}
	}

	@Override
	public boolean add(String s){
		return set.add(Strings.std(s));
	}
	
	public boolean remove(String s){
		return set.remove(Strings.std(s));
	}
	
	public boolean contains(String s){
		if(s==null){
			return false;
		}
		return set.contains(Strings.std(s));
	}

	@Override
	public Iterator<String> iterator() {
		return set.iterator();
	}
	
	@Override
	public String toString(){
		return set.toString();
	}
	
	public String [] toArray(){
		int i=0;
		String [] ret = new String[size()];
		for(String val: set){
			ret[i++]=val; 
		}
		return ret;
	}
	
	@Override
	public int size(){
		return set.size();
	}

	@Override
	public boolean isEmpty() {
		return set.size()==0;
	}

	@Override
	public boolean contains(Object o) {
		return set.contains(Strings.std(o.toString()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		int i =0;
		for(String s: this){
			if(i<a.length){
				a[i]=(T)s;
				i++;
			}else{
				break;
			}
		}
		return a;
	}

	@Override
	public boolean remove(Object o) {
		return remove(o.toString());
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends String> c) {
		boolean added=false;
		for(String s:c){
			if(add(s)){
				added = true;
			}
		}
		return added;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		set.clear();
	}
	
//	public String [] toStringArray(){
//		return set.toArray(new String[size()]);
//	}
}
