/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.collections.iterators.IteratorChain;

import com.opendoorlogistics.codefromweb.IteratorChainApacheCollections440;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;

public class LayeredDrawables implements Iterable<DrawableObject>{
	private final Iterable<? extends DrawableObject> inactiveBackground;
	private final Iterable<? extends DrawableObject> active;
	private final Iterable<? extends DrawableObject> inactiveForeground;
	
	public LayeredDrawables(Iterable<? extends DrawableObject> inactiveBackground, Iterable<? extends DrawableObject> active, Iterable<? extends DrawableObject> inactiveForeground) {
		this.inactiveBackground = inactiveBackground;
		this.active = active;
		this.inactiveForeground = inactiveForeground;
	}

	public Iterable<? extends DrawableObject> getInactiveBackground() {
		return inactiveBackground;
	}

	public Iterable<? extends DrawableObject> getActive() {
		return active;
	}

	public Iterable<? extends DrawableObject> getInactiveForeground() {
		return inactiveForeground;
	}

	@Override
	public Iterator<DrawableObject> iterator() {
		ArrayList<Iterator<? extends DrawableObject>> its = new ArrayList<>();
		if(inactiveBackground!=null){
			its.add(inactiveBackground.iterator());
		}
		if(active!=null){
			its.add(active.iterator());
		}
		if(inactiveForeground!=null){
			its.add(inactiveForeground.iterator());
		}

		return new IteratorChainApacheCollections440<DrawableObject>(its);

	}
	
}
