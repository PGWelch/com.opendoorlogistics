/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;

import java.util.ArrayList;
import java.util.List;

final public class IntUtils {
	/**
	 * Get the modal (most common) int value in the list
	 * @param list
	 * @return
	 */
	public static int getModal(TIntArrayList list){
		if(list.size()==0){
			throw new IllegalArgumentException();
		}
		TIntIntHashMap map = new TIntIntHashMap();
		int n = list.size();
		for(int i =0;i<n;i++){
			int val = list.get(i);
			int count=0;
			if(map.containsKey(val)){
				count = map.get(val);
			}
			count++;
			map.put(val, count);
		}
		
		class Max{
			int val=-1;
			int count=-1;
		}
		final Max max = new Max();
		
		map.forEachEntry(new TIntIntProcedure() {
			
			@Override
			public boolean execute(int val, int count) {
				if(count > max.count){
					max.val = val;
					max.count = count;
				}
				return true;
			}
		});
		
		return max.val;
	}
	
	public static ArrayList<Integer> toArrayList(int ...vals){
		ArrayList<Integer> ret = new ArrayList<>(vals.length);
		for(int i : vals){
			ret.add(i);
		}
		return ret;
	}
	
	public static boolean contains(int [] arr, int val){
		for(int i :arr){
			if(i==val){
				return true;
			}
		}
		return false;
	}
	
	public static int[] toArray(List<Integer> list){
		int n = list.size();
		int [] ret = new int[n];
		for(int i =0;i<n;i++){
			ret[i]=list.get(i);
		}
		return ret;
	}
	
	/**
	 * Create a filled array with values going from min to max-1
	 * (i.e. min is inclusive but max is exclusive).
	 * @param min
	 * @param max
	 * @return
	 */
	public static int [] fillArray(int min, int max){
		int nb = max - min;
		int[] ret = new int[nb];
		for(int i = 0 ; i<nb;i++){
			ret[i] = min + i;
		}
		return ret;
	}
}
