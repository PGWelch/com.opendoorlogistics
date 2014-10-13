/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.awt.Graphics2D;

import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;

public interface ObjectRenderer {
	boolean renderObject(Graphics2D g, LatLongToScreen converter, DrawableObject obj, boolean isSelected, long renderFlags);
}
