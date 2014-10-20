/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.tiled;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;

/**
 * Class holding a layer of objects to be drawn together, in order
 * @author Phil
 *
 */
class DrawableObjectLayer implements Iterable<DrawableObject>{
	final private LinkedList<DrawableObject> drawables = new LinkedList<>();
	final private String NOVLPLGroupId;
	final private DrawableObjectLayer.LayerType type;
	final private HashSet<ODLGeom> geomSet = new HashSet<>();
	
	DrawableObjectLayer(String NOVLPLGroupId) {
		this.NOVLPLGroupId = NOVLPLGroupId;
		this.type = LayerType.NOVLPL;
	}
	
	DrawableObjectLayer() {
		NOVLPLGroupId = null;
		this.type = LayerType.NORMAL;
	}

	void add(DrawableObject o){
		drawables.add(o);
		if(o.getGeometry()!=null){
			geomSet.add(o.getGeometry());
		}
	}
	
	boolean hasGeom(ODLGeom geom){
		return geomSet.contains(geom);
	}
	
	int size(){
		return drawables.size();
	}
	
	enum LayerType{
		NORMAL,
		NOVLPL
	}
	
	DrawableObjectLayer.LayerType getType(){
		return type;
	}
	
	String getNOVLPLGroupId() {
		return NOVLPLGroupId;
	}

	@Override
	public Iterator<DrawableObject> iterator() {
		return drawables.iterator();
	}
	

	
	static LinkedList<DrawableObject> layers2SingleList(Iterable<? extends DrawableObjectLayer> layers){
		LinkedList<DrawableObject> ret = new LinkedList<>();
		for(DrawableObjectLayer layer:layers){
			for(DrawableObject obj:layer){
				ret.add(obj);
			}
		}
		return ret;
	}

	
}