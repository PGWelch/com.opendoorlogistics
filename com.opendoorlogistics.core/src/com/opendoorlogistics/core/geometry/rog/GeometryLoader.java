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

import com.vividsolutions.jts.geom.Geometry;

public interface GeometryLoader extends Closeable {
	/**
	 * Load geometry at the file position
	 * @param position
	 * @return
	 */
	Geometry load(long geomId,long position);
	
	/**
	 * Load the geometry at the file position and modify its zoom from sourceZoom to targetZoom
	 * @param position
	 * @param sourceZoom
	 * @param TargetZoom
	 * @return
	 */
	Geometry loadTransform(long geomId,long position, int sourceZoom, int targetZoom);
}
