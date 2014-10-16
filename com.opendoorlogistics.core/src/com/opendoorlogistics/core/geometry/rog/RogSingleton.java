/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class RogSingleton implements Closeable {
	public static final boolean USE_QUADTREE = true;
	
	final private static RogSingleton singleton = new RogSingleton();
	final private HashMap<File, GeometryLoader> loaders = new HashMap<>();
	
	public static RogSingleton singleton(){
		return singleton;
	}

	@Override
	public synchronized void close() {
		for(GeometryLoader loader:loaders.values()){
			try {
				loader.close();
			} catch (IOException e) {
			}
		}
		loaders.clear();
	}
	
	
	public synchronized GeometryLoader createLoader(File file){
		GeometryLoader ret = loaders.get(file);
		if(ret==null){
			ret = new GeometryLoaderImpl(file);
			loaders.put(file, ret);
		}
		return ret;
	}
	
}
