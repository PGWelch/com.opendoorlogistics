/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.utils;

import java.util.HashMap;

import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class ColumnIndexLookup {
	private final HashMap<String, Integer> map = new HashMap<>();
	
	public ColumnIndexLookup(ODLTableDefinition dfn){
		int nc = dfn.getColumnCount();
		for(int i =0 ;i< nc ; i++){
			String std = Strings.std(dfn.getColumnName(i));
			if(map.containsKey(std)){
				throw new RuntimeException();
			}
			map.put(std, i);
		}
	}
	
	public int getColumnIndx(String name){
		name = Strings.std(name);
		Integer ret = map.get(name);
		if(ret!=null){
			return ret;
		}
		return -1;
	}
}
