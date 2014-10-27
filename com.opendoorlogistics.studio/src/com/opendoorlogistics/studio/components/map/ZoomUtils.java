/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

import com.opendoorlogistics.codefromweb.jxmapviewer2.DesktopPaneMapViewer;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactory;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.core.gis.map.JXMapUtils;
import com.opendoorlogistics.core.gis.map.data.LatLongBoundingBox;
import com.opendoorlogistics.core.utils.SetUtils;

final public class ZoomUtils {
	private static double DEFAULT_EDGE_SIZE_DEGREES = 0.1;
	
	private ZoomUtils() {
	}

	public static void zoomToBestFit(DesktopPaneMapViewer jxMap, LatLongBoundingBox positions, double maxFraction, boolean resetViewIfEmpty) {
		zoomToBestFit(jxMap, positions, maxFraction, resetViewIfEmpty, DEFAULT_EDGE_SIZE_DEGREES);
	}

	/**
	 * Zoom and centre the map to a best fit around the input GeoPositions. Best
	 * fit is defined as the most zoomed-in possible view where both the width
	 * and height of a bounding box around the positions take up no more than
	 * maxFraction of the viewport width or height respectively.
	 * 
	 * @param jxMap
	 * @param positions
	 * @param maxFraction
	 */
	public static void zoomToBestFit(DesktopPaneMapViewer jxMap, LatLongBoundingBox llbb, double maxFraction, boolean resetViewIfEmpty, double defaultEdgeSizeDegrees) {
		TileFactory tileFactory = jxMap.getTileFactory();
		TileFactoryInfo info = tileFactory.getInfo();

		Set<GeoPosition> positions = llbb.getCornerSet();
		if (positions.size() > 0) {
			if(positions.size()==1){
				GeoPosition pos = SetUtils.getFirst(positions);
				positions = createGeopositionsSquareAroundPoint(pos.getLatitude(), pos.getLongitude(), defaultEdgeSizeDegrees);
			}
			
			if (info != null) {
				Rectangle viewBounds = jxMap.getViewportBounds();
				int bfz = JXMapUtils.getBestFitZoom(info, positions, viewBounds.width, viewBounds.height, maxFraction).getFirst();
				jxMap.setZoom(bfz);
				Rectangle2D bounds = JXMapUtils.generateBoundingRect(info, positions, jxMap.getZoom());
				GeoPosition centre = tileFactory.pixelToGeo(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()), jxMap.getZoom());
				jxMap.setCenterPosition(centre);
			}
		} else if (resetViewIfEmpty) {
			int maxZoom = info.getMaximumZoomLevel();
			jxMap.setZoom(maxZoom);
			Point2D pixelPos = tileFactory.geoToPixel(new GeoPosition(0, 0), maxZoom);
			jxMap.setCenter(pixelPos);
		}
	}

	private static Set<GeoPosition> createGeopositionsSquareAroundPoint(double latitude, double longitude, double edgeSizeDegrees) {
		HashSet<GeoPosition> dummies = new HashSet<>();
		for (int lat = -1; lat <= 1; lat += 2) {
			for (int lng = -1; lng <= 1; lng += 2) {
				dummies.add(new GeoPosition(latitude + lat * edgeSizeDegrees, longitude + lng * edgeSizeDegrees));
			}
		}
		return dummies;
	}
}
