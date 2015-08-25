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
import com.opendoorlogistics.core.utils.Pair;

public final class UpscalerLatLongToPixelPosition implements LatLongToScreen{
	private final LatLongToScreen original;
	private final double scalingFactor;
	
	public UpscalerLatLongToPixelPosition(LatLongToScreen original, double scalingFactor) {
		this.original = original;
		this.scalingFactor = scalingFactor;
	}

	@Override
	public Point2D getOnScreenPixelPosition(LatLong latLong) {
		Point2D ret = original.getOnScreenPixelPosition(latLong);
		scalePosition(ret);
		return ret;
	}

	private void scalePosition(Point2D ret) {
		ret.setLocation(ret.getX() * scalingFactor, ret.getY() * scalingFactor);
	}

	@Override
	public LatLong getLongLat(double pixelX, double pixelY) {
		pixelX  = (int)Math.round(pixelX / scalingFactor);
		pixelY  = (int)Math.round(pixelY / scalingFactor);
		return original.getLongLat(pixelX, pixelY);
	}

	@Override
	public Object getZoomHashmapKey() {
		return new Pair<Object,Object>(original.getZoomHashmapKey(), new Double(scalingFactor));
	}

	@Override
	public Point2D getWorldBitmapPixelPosition(LatLong latLong) {
		Point2D ret = original.getWorldBitmapPixelPosition(latLong);
		scalePosition(ret);
		return ret;
	}

	@Override
	public Rectangle2D getViewportWorldBitmapScreenPosition() {
		Rectangle2D rect = original.getViewportWorldBitmapScreenPosition();
		Rectangle2D.Double ret = new Rectangle2D.Double(rect.getX() * scalingFactor, rect.getY()*scalingFactor, rect.getWidth()*scalingFactor, rect.getHeight()*scalingFactor);
		return ret;
	}

	@Override
	public int getZoomForObjectFiltering() {
		return original.getZoomForObjectFiltering();
	}
	
}
