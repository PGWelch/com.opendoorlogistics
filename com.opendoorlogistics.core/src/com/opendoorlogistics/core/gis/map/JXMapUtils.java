/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Set;

import com.opendoorlogistics.codefromweb.jxmapviewer2.ExpiringOSMLocalResponseCache;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.util.GeoUtil;
import com.opendoorlogistics.core.utils.Pair;

final public class JXMapUtils {
	private JXMapUtils(){}


	
	/**
	 * Find the zoom which would fit in the range of input lat and longs
	 * 
	 * @param control
	 * @param positions
	 * @param imageSize
	 * @param maxFraction
	 * @return Pair where first is the best zoom level and second is the tightness of fit (0= taking up no space, 1 = taking up all space).
	 */
	public static Pair<Integer, Double> getBestFitZoom(TileFactoryInfo info, Set<GeoPosition> positions, int imageWidth, int imageHeight, double maxFraction) {

		// set to maximum zoomed out
		final double targetWidth = imageWidth * maxFraction;
		final double targetHeight= imageHeight * maxFraction;
		int zoom = info.getMaximumZoomLevel();
		int bestZoom = zoom;
		double bestFit=0;
		if (positions.size() > 0) {

			// set to central position initially
			Rectangle2D bounds = generateBoundingRect(info, positions, zoom);

			// repeatedly zoom in until we find the first zoom level where either the width or height
			// of the points takes up more than the max fraction of the viewport
			while (zoom >= info.getMinimumZoomLevel() && positions.size() > 1) {

				// is this zoom still OK?
				bounds = generateBoundingRect(info, positions, zoom);
				if (bounds.getWidth() < targetWidth && bounds.getHeight() < targetHeight) {
					// zoom is still good
					bestZoom = zoom;
					
					// calculate 'goodness of fit', where 1 is perfect, 0 is crap..
					double horizFit = bounds.getWidth() / targetWidth;
					double vertFit = bounds.getHeight()/ targetHeight;
					bestFit= Math.max(horizFit, vertFit);
				} else {
					break;
				}
				zoom--;
			}
		}

		return new Pair<Integer, Double>(bestZoom, bestFit);
	}

	public static Rectangle2D generateBoundingRect(TileFactoryInfo info, Set<GeoPosition> positions, int zoom) {
		Rectangle2D rect = null;
		for (GeoPosition pos : positions) {
			Point2D pnt = GeoUtil.getBitmapCoordinate(pos, zoom, info);
			if (rect == null) {
				rect = new Rectangle2D.Double(pnt.getX(), pnt.getY(), 0, 0);
			} else {
				rect.add(pnt);
			}
		}

		return rect;
	}
}
