/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.reports;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.gis.map.MapUtils;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.SynchronousRenderer;
import com.opendoorlogistics.core.gis.map.View;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.utils.images.ImageUtils;
/**
 * DO NOT RENAME THIS CLASS OR MOVE IT TO ANOTHER PACKAGE AS ITS ACCESSED VIA REFLECTION
 * IN THE EXISTING REPORTS!
 * @author Phil
 *
 */
final public class ImageProvider {
	final List<? extends DrawableObject> objects;
	
	public ImageProvider(ODLTableReadOnly pointsTable) {
		objects = MapUtils.getDrawables(pointsTable);
	}
	
	public BufferedImage createImage(double widthCm, double heightCm, double dotsPerCm){
		BufferedImage ret;
		if (objects.size() > 0) {
			// draw using a bounding box...
			ret = SynchronousRenderer.singleton().drawPrintableAtLatLongCentre( View.createView(objects), widthCm, heightCm, dotsPerCm, objects,RenderProperties.SHOW_ALL).getFirst();
		} else {
			// return blank image..
			ret = ImageUtils.createBlankImage(100, 100, Color.WHITE);
		}
		return ret;
	}
}
