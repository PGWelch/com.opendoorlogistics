/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.ODLComponentProvider;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;

public final class ODLComponentsList implements ODLComponentProvider , Iterable<ODLComponent>{
	private final StandardisedStringTreeMap<ODLComponent> components = new StandardisedStringTreeMap<>(false);
	
	@Override
	public synchronized void register(ODLComponent component){

		// replace existing component if already registered
		components.put(component.getId(), component);

	}
	

	@Override
	public ODLComponent getComponent(String id) {
		return components.get(id);
	}


	@Override
	public Iterator<ODLComponent> iterator() {
		// returned sorted by name by default..
		ArrayList<ODLComponent> ret = new ArrayList<>(components.values());
		Collections.sort(ret, new Comparator<ODLComponent>() {

			@Override
			public int compare(ODLComponent o1, ODLComponent o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return ret.iterator();
	}
	
	public int size(){
		return components.size();
	}


	@Override
	public ODLComponent remove(String id) {
		return components.remove(id);
	}
	
	
}
