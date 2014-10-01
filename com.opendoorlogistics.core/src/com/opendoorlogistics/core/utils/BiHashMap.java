/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.util.HashMap;

final public class BiHashMap<Ta, Tb> {
	private HashMap<Ta, Tb> a2b = new HashMap<>();
	private HashMap<Tb, Ta> b2a = new HashMap<>();
	
	public void put(Ta a, Tb b){
		if(a==null || b==null){
			throw new RuntimeException();
		}
		
		if(a2b.containsKey(a) || b2a.containsKey(b)){
			throw new RuntimeException("Cannot add the same key twice");		
		}
		
		a2b.put(a, b);
		b2a.put(b, a);
	}
	
	public Tb getB(Ta a){
		return a2b.get(a);
	}
	
	public Ta getA(Tb b){
		return b2a.get(b);
	}
	
	public void removeUsingA(Ta a){
		Tb b = a2b.get(a);
		a2b.remove(a);
		b2a.remove(b);
	}
	
	public void clear(){
		a2b.clear();
		b2a.clear();
	}
}
