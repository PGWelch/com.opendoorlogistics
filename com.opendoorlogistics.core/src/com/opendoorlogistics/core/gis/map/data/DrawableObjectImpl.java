/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.data;

import java.awt.Color;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.gis.map.Symbols.SymbolType;
import com.opendoorlogistics.core.gis.map.annotations.ImageFormulaKey;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnDescription;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.core.tables.beans.annotations.ODLDefaultDoubleValue;
import com.opendoorlogistics.core.tables.beans.annotations.ODLDefaultLongValue;
import com.opendoorlogistics.core.tables.beans.annotations.ODLDefaultStringValue;
import com.opendoorlogistics.core.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTag;

@ODLTableName(PredefinedTags.DRAWABLES)
public class DrawableObjectImpl extends LatLongImpl implements DrawableObject{
	private static final int COL_LATITUDE=0;
	private static final int COL_LONGITUDE=COL_LATITUDE+1;
	private static final int COL_GEOMETRY=COL_LONGITUDE+1;
	private static final int COL_LABEL=COL_GEOMETRY+1;
	private static final int COL_COLOUR=COL_LABEL+1;
	private static final int COL_SYMBOL=COL_COLOUR+1;
	private static final int COL_OPAQUE=COL_SYMBOL+1;
	private static final int COL_COLOUR_KEY=COL_OPAQUE+1;
	private static final int COL_PIXEL_WIDTH=COL_COLOUR_KEY+1;
	private static final int COL_FONT_SIZE=COL_PIXEL_WIDTH+1;
	private static final int COL_OUTLINE=COL_FONT_SIZE+1;
	private static final int COL_LEGEND_COLOUR=COL_OUTLINE+1;
	private static final int COL_LEGEND_KEY=COL_LEGEND_COLOUR+1;
	private static final int COL_IMAGE_FORMULA_KEY=COL_LEGEND_COLOUR+1;
	private static final int COL_LABEL_GROUP_KEY=COL_IMAGE_FORMULA_KEY+1;
	private static final int COL_NOPL_GROUP_KEY=COL_LABEL_GROUP_KEY+1;
	private static final int COL_TOOLTIP= COL_NOPL_GROUP_KEY + 1;
	private static final int COL_SELECTABLE= COL_TOOLTIP + 1;

	private static final BeanDatastoreMapping mapping;
	private static final double DEFAULT_OPAQUE = 1.0;
	
	static{
		mapping = BeanMapping.buildDatastore(DrawableObjectImpl.class);
	}
	
	public static BeanDatastoreMapping getBeanMapping(){
		return mapping;
	}
	private Color Colour = Color.BLACK;
	private String colourKey;
	private long drawOutline = 1;
	private String imageFormula=null;
	private String legend;
	private String label = "";
	private String labelGroupKey = "";
	private Color legendColour;
	private long fontSize;
	private ODLGeomImpl geometry;
	private long pixelWidth = 5;
	private double opaque= DEFAULT_OPAQUE;
	private String symbol = SymbolType.CIRCLE.getKeyword();
	private String tooltip;
	private String nonOverlappingPolygonLayerGroupKey;
	private long selectable=1;
	
	public DrawableObjectImpl(){}
	
	/**
	 * Copy constructor
	 * @param copyThis
	 */
	public DrawableObjectImpl(DrawableObject copyThis){
		super(copyThis.getLatitude(), copyThis.getLongitude());
		setGlobalRowId(copyThis.getGlobalRowId());
		this.Colour = copyThis.getColour();
		this.colourKey = copyThis.getColourKey();
		this.drawOutline = copyThis.getDrawOutline();
		this.imageFormula = copyThis.getImageFormulaKey();
		this.legend = copyThis.getLegendKey();
		this.label = copyThis.getLabel();
		this.fontSize = copyThis.getFontSize();
		this.geometry = copyThis.getGeometry();
		this.pixelWidth = copyThis.getPixelWidth();
		this.opaque =copyThis.getOpaque();
	}
	

	
	public DrawableObjectImpl(double latitude, double longitude, Color color, String name){
		super(latitude, longitude);
		this.Colour = color;
		this.label = name;
	}
	
	@Override
	public Color getColour() {
		return Colour;
	}
	
	@Override
	public String getColourKey() {
		return colourKey;
	}
	
	@Override
	public long getDrawOutline() {
		return drawOutline;
	}
	
	@Override
	public String getImageFormulaKey() {
		return imageFormula;
	}
	
	@Override
	public String getLegendKey() {
		return legend;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
	
	@Override
	public long getPixelWidth() {
		return pixelWidth;
	}

	@ODLNullAllowed
	@ODLColumnOrder(COL_COLOUR)
	@ODLDefaultStringValue(DrawableObject.DEFAULT_COLOUR_STRING)
	public void setColour(Color colour) {
		Colour = colour;
	}
	
	public double getOpaque() {
		return opaque;
	}

	@ODLColumnOrder(COL_OPAQUE)
	@ODLNullAllowed
	@ODLDefaultDoubleValue(DEFAULT_OPAQUE)
	@ODLColumnDescription("Set to less than 1 if you want the object to be partially see-through, 0 for completely invisible.")
	public void setOpaque(double opaque) {
		this.opaque = opaque;
	}

	@ODLNullAllowed
	@ODLColumnOrder(COL_COLOUR_KEY)
	public void setColourKey(String colourKey) {
		this.colourKey = colourKey;
	}

	@ODLNullAllowed
	@ODLColumnOrder(COL_OUTLINE)	
	@ODLDefaultLongValue(1)
	@ODLColumnDescription("Set this to 1 to draw an outline around the point or 0 otherwise.")	
	public void setDrawOutline(long drawOutline) {
		this.drawOutline = drawOutline;
	}

	@ODLColumnOrder(COL_IMAGE_FORMULA_KEY)	
	@ODLColumnDescription("This key field is used to select objects when you are drawing the map by calling the image formula.")	
	@ODLNullAllowed
	@ImageFormulaKey
	public void setImageFormulaKey(String imageFormula) {
		this.imageFormula = imageFormula;
	}

	@ODLColumnOrder(COL_LEGEND_KEY)	
	@ODLColumnDescription("This key field is used to populate the map's legend.")	
	@ODLNullAllowed
	public void setLegendKey(String legendKey) {
		this.legend = legendKey;
	}

	@ODLColumnOrder(COL_LEGEND_COLOUR)	
	@ODLColumnDescription("Use this field to override the colour shown in the legend for the object.")	
	@ODLNullAllowed
	public void setLegendColour(Color legendColour) {
		this.legendColour = legendColour;
	}
	
	@Override
	public Color getLegendColour() {
		return legendColour;
	}
	
	

	@ODLNullAllowed
	@ODLColumnOrder(COL_LABEL)
	@ODLColumnDescription("The text of this field will be shown on the map beside the point.")
	public void setLabel(String name) {
		label = name;
	}
	
	public String getSymbol(){
		return symbol;
	}
	
	@ODLNullAllowed
	@ODLColumnOrder(COL_SYMBOL)
	@ODLColumnDescription("Name of the symbol used when drawing a point. If this is null a circle is used.")		
	public void setSymbol(String symbol){
		this.symbol = symbol;
	}
	
	@ODLNullAllowed
	@ODLColumnOrder(COL_PIXEL_WIDTH)
	@ODLDefaultLongValue(10)
	@ODLColumnDescription("The width in pixels of the point.")	
	public void setPixelWidth(long pixelWidth) {
		this.pixelWidth = pixelWidth;
	}

	@Override
	public long getFontSize() {
		return fontSize;
	}

	@ODLNullAllowed
	@ODLColumnOrder(COL_FONT_SIZE)
	@ODLDefaultLongValue(0)	
	@ODLColumnDescription("The font size for the name. A value of 0 takes the default size.")	
	public void setFontSize(long fontSize) {
		this.fontSize = fontSize;
	}

	public ODLGeomImpl getGeometry() {
		return geometry;
	}
	
	@ODLNullAllowed
	@ODLColumnOrder(COL_GEOMETRY)
	@ODLColumnDescription("Geometry of the object. If this is not set a point is draw at the latitude-longitude instead.")
	@ODLTag(PredefinedTags.GEOMETRY)
	public void setGeometry(ODLGeomImpl geometry) {
		this.geometry = geometry;
	}

	@Override
	public String toString(){
		String ret = getLabel()!=null ? "name=" + getLabel() + ", ": "";
		ret += super.toString();
		return ret;
	}
	
	
	@Override
//	@ODLDefaultDoubleValue(Double.NaN) // NaN will stop invalid points rendering
	@ODLColumnOrder(COL_LATITUDE)
	@ODLNullAllowed	// can use geom or lat/long so null is allowed
	@ODLTag(PredefinedTags.LATITUDE)
	public void setLatitude(double latitude) {
		super.setLatitude(latitude);
	}
	
	@Override
	//@ODLDefaultDoubleValue(Double.NaN) 
	@ODLColumnOrder(COL_LONGITUDE)
	@ODLNullAllowed // can use geom or lat/long so null is allowed
	@ODLTag(PredefinedTags.LONGITUDE)
	public void setLongitude(double longitude) {
		super.setLongitude(longitude);
	}

	@Override
	public String getLabelGroupKey() {
		return labelGroupKey;
	}

	@ODLNullAllowed 
	@ODLColumnOrder(COL_LABEL_GROUP_KEY)
	public void setLabelGroupKey(String labelGroupKey) {
		this.labelGroupKey = labelGroupKey;
	}

	@Override
	public String getTooltip() {
		return tooltip;
	}

	@ODLNullAllowed 
	@ODLColumnOrder(COL_TOOLTIP)
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	@Override
	public String getNonOverlappingPolygonLayerGroupKey() {
		return nonOverlappingPolygonLayerGroupKey;
	}

	@ODLNullAllowed 
	@ODLColumnOrder(COL_NOPL_GROUP_KEY)
	public void setNonOverlappingPolygonLayerGroupKey(String nonOverlappingPolygonLayerGroupKey) {
		this.nonOverlappingPolygonLayerGroupKey = nonOverlappingPolygonLayerGroupKey;
	}

	public long getSelectable() {
		return selectable;
	}

	@ODLColumnOrder(COL_SELECTABLE)
	@ODLDefaultLongValue(1)
	@ODLNullAllowed
	public void setSelectable(long selectable) {
		this.selectable = selectable;
	}


}
