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
import java.util.List;

public class RogSingleton implements Closeable {

	final private static RogSingleton singleton = new RogSingleton();
	final private HashMap<File, QuadLoader> loaders = new HashMap<>();
	
	public static RogSingleton singleton(){
		return singleton;
	}

	@Override
	public synchronized void close() {
		for(QuadLoader loader:loaders.values()){
			try {
				loader.close();
			} catch (IOException e) {
			}
		}
		loaders.clear();
	}
	
	
	public synchronized QuadLoader createLoader(File file, List<ODLRenderOptimisedGeom> readObjs){
		QuadLoader ret = loaders.get(file);
		if(ret==null){
			ret = new QuadLoader(file,readObjs);
			loaders.put(file, ret);
		}else if(readObjs!=null){
			readObjs.addAll(ret.readObjects());
		}
		return ret;
	}

//	public synchronized QuadLoader createLoader(File file){
//		GeometryLoader ret = loaders.get(file);
//		if(ret==null){
//			ret = new GeometryLoaderImpl(file);
//			loaders.put(file, ret);
//		}
//		return ret;
//	}
	
}
