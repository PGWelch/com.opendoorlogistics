/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.data;

import java.awt.Color;

import com.opendoorlogistics.core.geometry.ODLGeomImpl;

public class DrawableObjectDecorator implements DrawableObject {
	private DrawableObject decorated;

	public DrawableObjectDecorator(DrawableObject decorated){
		this.decorated = decorated;
	}
	
	public double getLatitude() {
		return decorated.getLatitude();
	}

	public long getGlobalRowId() {
		return decorated.getGlobalRowId();
	}

	public double getLongitude() {
		return decorated.getLongitude();
	}

	public Color getColour() {
		return decorated.getColour();
	}

	public String getColourKey() {
		return decorated.getColourKey();
	}

	public long getDrawOutline() {
		return decorated.getDrawOutline();
	}

	public String getImageFormulaKey() {
		return decorated.getImageFormulaKey();
	}

	public String getLegendKey() {
		return decorated.getLegendKey();
	}

	public Color getLegendColour() {
		return decorated.getLegendColour();
	}

	public String getLabel() {
		return decorated.getLabel();
	}

	public String getLabelGroupKey() {
		return decorated.getLabelGroupKey();
	}

	public String getSymbol() {
		return decorated.getSymbol();
	}

	public long getFontSize() {
		return decorated.getFontSize();
	}

	public long getPixelWidth() {
		return decorated.getPixelWidth();
	}

	public double getOpaque() {
		return decorated.getOpaque();
	}

	public ODLGeomImpl getGeometry() {
		return decorated.getGeometry();
	}

	public String getTooltip() {
		return decorated.getTooltip();
	}

	public DrawableObject getDecorated() {
		return decorated;
	}

	public void setDecorated(DrawableObject decorated) {
		this.decorated = decorated;
	}

	@Override
	public String getNonOverlappingPolygonLayerGroupKey() {
		return decorated.getNonOverlappingPolygonLayerGroupKey();
	}

	@Override
	public long getSelectable() {
		return decorated.getSelectable();
	}

	@Override
	public String getLabelPositioningOption() {
		return decorated.getLabelPositioningOption();
	}

	@Override
	public Color getLabelColour() {
		return decorated.getLabelColour();
	}

	
}
