/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core;

import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.distances.DistancesSingleton;
import com.opendoorlogistics.core.geometry.rog.RogSingleton;
import com.opendoorlogistics.core.gis.map.background.BackgroundTileFactorySingleton;

public class DisposeCore {
	private static boolean disposed=false;

	public synchronized static void dispose() {
		if(!disposed){
			DistancesSingleton.singleton().close();
			RogSingleton.singleton().close();
			BackgroundTileFactorySingleton.dispose();
			ApplicationCache.singleton().dispose();
			disposed = true;
		}
	}
	
}
