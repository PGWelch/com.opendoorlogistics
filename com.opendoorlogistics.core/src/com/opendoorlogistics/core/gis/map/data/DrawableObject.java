/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.data;

import java.awt.Color;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;

public interface DrawableObject extends LatLong, BeanMappedRow{
	/**
	 * We use grey to signify nothing is set...
	 */
	public final static Color DEFAULT_COLOUR = Color.DARK_GRAY;
	public final static String DEFAULT_COLOUR_STRING = "DarkGray";
	
	Color getColour();

	String getColourKey();

	long getDrawOutline();

	String getImageFormulaKey();

	String getLegendKey();

	Color getLegendColour();
	
	String getLabel();
	
	String getLabelGroupKey();
	
	String getSymbol();
	
	long getFontSize();

	long getPixelWidth();
	
	long getSelectable();
	
	double getOpaque();
	
	String getNonOverlappingPolygonLayerGroupKey();
	
	String getLabelPositioningOption();
	
	Color getLabelColour();
	
	long getLabelPriority();
	
	/**
	 * Get the geometry. If this is null a point
	 * is drawn at the long-lat instead. 
	 * @return
	 */
	ODLGeomImpl getGeometry();
	
	String getTooltip();
	
	long getFlags();
	
	long getMinZoom();
	
	long getMaxZoom();
}
