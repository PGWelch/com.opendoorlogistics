/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.tiled;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreenImpl;

class TilePosition {
	static final int TILE_SIZE = 256;

	final int ix;
	final int iy;
	final Object zoomKey;
	
	TilePosition(int x, int y, Object zoomKey) {
		this.ix = x;
		this.iy = y;
		this.zoomKey = zoomKey;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ix;
		result = prime * result + iy;
		result = prime * result + ((zoomKey == null) ? 0 : zoomKey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TilePosition other = (TilePosition) obj;
		if (ix != other.ix)
			return false;
		if (iy != other.iy)
			return false;
		if (zoomKey == null) {
			if (other.zoomKey != null)
				return false;
		} else if (!zoomKey.equals(other.zoomKey))
			return false;
		return true;
	}
	

	@Override
	public String toString() {
		return "[x=" + ix + ", y=" + iy + ", z=" + zoomKey + "]";
	}
	
	Rectangle2D createBounds() {
		return new Rectangle2D.Double(ix * TILE_SIZE, iy * TILE_SIZE, TILE_SIZE, TILE_SIZE);
	}


	LatLongToScreen createConverter(final LatLongToScreen originalConverter) {
		final Rectangle2D viewport = createBounds();
		LatLongToScreenImpl converter = new LatLongToScreenImpl() {

			@Override
			public Point2D getWorldBitmapPixelPosition(LatLong latLong) {
				return originalConverter.getWorldBitmapPixelPosition(latLong);
			}

			@Override
			public Rectangle2D getViewportWorldBitmapScreenPosition() {
				return viewport;
			}

			@Override
			public LatLong getLongLat(double pixelX, double pixelY) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Object getZoomHashmapKey() {
				return originalConverter.getZoomHashmapKey();
			}

			@Override
			public int getZoomForObjectFiltering() {
				return originalConverter.getZoomForObjectFiltering();
			}
		};
		return converter;
	}

}
