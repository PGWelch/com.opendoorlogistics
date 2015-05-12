/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.transforms;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;

public abstract class LatLongToScreenImpl implements LatLongToScreen{

	@Override
	public Point2D getOnScreenPixelPosition(LatLong latLong) {
		Point2D point = getWorldBitmapPixelPosition(latLong);

		// set the location relative to the viewport
		Rectangle2D viewport = getViewportWorldBitmapScreenPosition();
		point.setLocation(point.getX() - viewport.getX(), point.getY() - viewport.getY());
		return point;
	}

	//protected abstract int getZoomLevel();
	
	@Override
	public abstract Object getZoomHashmapKey();	
}
