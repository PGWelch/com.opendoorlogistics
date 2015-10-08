/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution.adapters;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;

final public class BuiltAdapters {
	private final StandardisedStringTreeMap<ODLDatastore<? extends ODLTable>> adapters = new StandardisedStringTreeMap<>(false);

	public void addAdapter(String id, ODLDatastore<? extends ODLTable> adapter){
		if(id==null){
			return;
		}
		if(adapters.get(id)!=null){
			throw new RuntimeException();
		}
		
		adapters.put(id,adapter);
	}
	
	public ODLDatastore<? extends ODLTable> getAdapter(String id){
		return adapters.get(id);
	}

//	public String createUniqueAdapterId(String prefix){
//
//		return Strings.makeUnique(prefix, new DoesStringExist() {
//			
//			@Override
//			public boolean isExisting(String s) {
//				return adapters.containsKey(s);
//			}
//		});
//	}
}
