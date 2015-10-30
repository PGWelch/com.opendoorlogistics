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
	
	@Override	
	public double getLatitude() {
		return decorated.getLatitude();
	}

	@Override	
	public long getGlobalRowId() {
		return decorated.getGlobalRowId();
	}

	@Override
	public double getLongitude() {
		return decorated.getLongitude();
	}

	@Override
	public Color getColour() {
		return decorated.getColour();
	}

	@Override
	public String getColourKey() {
		return decorated.getColourKey();
	}

	@Override
	public long getDrawOutline() {
		return decorated.getDrawOutline();
	}

	@Override
	public String getImageFormulaKey() {
		return decorated.getImageFormulaKey();
	}

	@Override
	public String getLegendKey() {
		return decorated.getLegendKey();
	}

	@Override
	public Color getLegendColour() {
		return decorated.getLegendColour();
	}

	@Override
	public String getLabel() {
		return decorated.getLabel();
	}

	@Override
	public String getLabelGroupKey() {
		return decorated.getLabelGroupKey();
	}

	@Override
	public String getSymbol() {
		return decorated.getSymbol();
	}

	@Override
	public long getFontSize() {
		return decorated.getFontSize();
	}

	@Override
	public long getPixelWidth() {
		return decorated.getPixelWidth();
	}

	@Override
	public double getOpaque() {
		return decorated.getOpaque();
	}

	@Override
	public ODLGeomImpl getGeometry() {
		return decorated.getGeometry();
	}

	@Override
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

	@Override
	public long getFlags() {
		return decorated.getFlags();
	}

	@Override
	public long getMinZoom() {
		return decorated.getMinZoom();
	}

	@Override
	public long getMaxZoom() {
		return decorated.getMaxZoom();
	}

	@Override
	public long getLabelPriority() {
		return decorated.getLabelPriority();
	}

	@Override
	public void setGlobalRowId(long globalRowId) {
		decorated.setGlobalRowId(globalRowId);
	}

	
}
