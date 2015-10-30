/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.data;

import java.awt.Color;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnDescription;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultDoubleValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultLongValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultStringValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTag;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.gis.map.Symbols.SymbolType;
import com.opendoorlogistics.core.gis.map.annotations.ImageFormulaKey;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMappingImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping.ReadObjectFilter;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.memory.ODLTableImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;

@ODLTableName(PredefinedTags.DRAWABLES)
public class DrawableObjectImpl extends LatLongImpl implements DrawableObject{
	public static final long DEFAULT_DRAW_OUTLINE=1;
	public static final int COL_LATITUDE=0;
	public static final int COL_LONGITUDE=COL_LATITUDE+1;
	public static final int COL_GEOMETRY=COL_LONGITUDE+1;
	public static final int COL_LABEL=COL_GEOMETRY+1;
	public static final int COL_COLOUR=COL_LABEL+1;
	public static final int COL_SYMBOL=COL_COLOUR+1;
	public static final int COL_OPAQUE=COL_SYMBOL+1;
	public static final int COL_COLOUR_KEY=COL_OPAQUE+1;
	public static final int COL_PIXEL_WIDTH=COL_COLOUR_KEY+1;
	public static final int COL_FONT_SIZE=COL_PIXEL_WIDTH+1;
	public static final int COL_OUTLINE=COL_FONT_SIZE+1;
	public static final int COL_LEGEND_COLOUR=COL_OUTLINE+1;
	public static final int COL_LEGEND_KEY=COL_LEGEND_COLOUR+1;
	public static final int COL_IMAGE_FORMULA_KEY=COL_LEGEND_KEY+1;
	public static final int COL_LABEL_GROUP_KEY=COL_IMAGE_FORMULA_KEY+1;
	public static final int COL_NOPL_GROUP_KEY=COL_LABEL_GROUP_KEY+1;
	public static final int COL_TOOLTIP= COL_NOPL_GROUP_KEY + 1;
	public static final int COL_SELECTABLE= COL_TOOLTIP + 1;
	public static final int COL_LPO= COL_SELECTABLE + 1;
	public static final int COL_LABEL_COLOUR= COL_LPO + 1;
	public static final int COL_LABEL_PRIORITY= COL_LABEL_COLOUR + 1;
	public static final int COL_FLAGS= COL_LABEL_PRIORITY + 1;
	public static final int COL_MIN_ZOOM= COL_FLAGS + 1;
	public static final int COL_MAX_ZOOM= COL_MIN_ZOOM + 1;
	public static final int COL_MAX = COL_MAX_ZOOM;
	
	private static final BeanDatastoreMapping mapping;
	public static final ODLDatastore<? extends ODLTableDefinition> ACTIVE_BACKGROUND_FOREGROUND_IMAGE_DS;
	public static final ODLDatastore<? extends ODLTableDefinition> DRAWABLES_ONLY_DS;
	private static final double DEFAULT_OPAQUE = 1.0;
	
	static{
		mapping = BeanMapping.buildDatastore(DrawableObjectImpl.class);
		
		ReadObjectFilter rowfilter = new ReadObjectFilter() {
			
			@Override
			public boolean acceptObject(Object obj, ODLTableReadOnly inputTable, int row, long rowId, BeanTableMappingImpl btm) {
				DrawableObject o = (DrawableObject)obj;
				if(o.getGeometry()!=null || o.getLatitude()!=0 || o.getLongitude()!=0){
					return true;
				}
				
				// Check for null latitude and longitude... if so the object really isn't drawable and should be filtered
				if(btm.getValue(inputTable, row, rowId, COL_LATITUDE)==null && btm.getValue(inputTable, row, rowId, COL_LONGITUDE)==null){
					return false;
				}
				
				return true;
			}
		};
		
		mapping.getTableMapping(0).setRowfilter(rowfilter);
		
		// Create datastore definition for active, inactive-background, inactive-foreground
		ODLTableDefinition drawablesTable = mapping.getDefinition().getTableAt(0);
		
		// build without images table
		ODLDatastoreImpl<ODLTableDefinitionAlterable> abc = new ODLDatastoreImpl<>(ODLTableImpl.ODLTableDefinitionAlterableFactory);
		DatastoreCopier.copyTableDefinition(drawablesTable, abc, PredefinedTags.DRAWABLES, -1);				
		DatastoreCopier.copyTableDefinition(drawablesTable, abc, PredefinedTags.DRAWABLES_INACTIVE_BACKGROUND, -1);				
		DatastoreCopier.copyTableDefinition(drawablesTable, abc, PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND, -1);
		for(int tableIndx = 1 ; tableIndx<=2 ; tableIndx++){
			ODLTableDefinitionAlterable alterable= abc.getTableAt(tableIndx);
			alterable.setFlags(alterable.getFlags() | TableFlags.FLAG_IS_OPTIONAL);
		}
		DRAWABLES_ONLY_DS = abc;
		
		// build with images
		ODLDatastoreImpl<ODLTableDefinitionAlterable> abci = new ODLDatastoreImpl<>(ODLTableImpl.ODLTableDefinitionAlterableFactory);
		DatastoreCopier.copyTableDefinition(drawablesTable, abci, PredefinedTags.DRAWABLES, -1);				
		DatastoreCopier.copyTableDefinition(drawablesTable, abci, PredefinedTags.DRAWABLES_INACTIVE_BACKGROUND, -1);				
		DatastoreCopier.copyTableDefinition(drawablesTable, abci, PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND, -1);
		DatastoreCopier.copyTableDefinition(BackgroundImage.BEAN_MAPPING.getTableDefinition(), abci);				
		for(int tableIndx = 1 ; tableIndx<=3 ; tableIndx++){
			ODLTableDefinitionAlterable alterable= abci.getTableAt(tableIndx);
			alterable.setFlags(alterable.getFlags() | TableFlags.FLAG_IS_OPTIONAL);
		}
		ACTIVE_BACKGROUND_FOREGROUND_IMAGE_DS = abci;
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
	private String labelPositioningOption;
	private Color labelColor;
	private long flags;
	private long labelPriority=0;
	private long minZoom = 0;
	private long maxZoom = 1000;
	
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
		this.fontSize = copyThis.getFontSize();
		this.geometry = copyThis.getGeometry();
		this.imageFormula = copyThis.getImageFormulaKey();
		this.label = copyThis.getLabel();
		this.labelColor = copyThis.getLabelColour();
		this.labelGroupKey = copyThis.getLabelGroupKey();
		this.labelPositioningOption = copyThis.getLabelPositioningOption();
		this.legend = copyThis.getLegendKey();
		this.legendColour = copyThis.getLegendColour();
		this.nonOverlappingPolygonLayerGroupKey = copyThis.getNonOverlappingPolygonLayerGroupKey();
		this.opaque =copyThis.getOpaque();
		this.pixelWidth = copyThis.getPixelWidth();
		this.selectable = copyThis.getSelectable();
		this.symbol  = copyThis.getSymbol();
		this.tooltip = copyThis.getTooltip();
		this.labelPriority = copyThis.getLabelPriority();
		this.flags = copyThis.getFlags();
		this.minZoom = copyThis.getMinZoom();
		this.maxZoom = copyThis.getMaxZoom();
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
	@ODLDefaultLongValue(DEFAULT_DRAW_OUTLINE)
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
	@ODLColumnDescription("<html>Name of the symbol used when drawing a point."
			+ "<br>If this is null a circle is used. "
			+ "<br>Available symbols are \"triangle\",\"inverted-triangle\",\"diamond\",\"square\",\"pentagon\",\"star\",\"fat-star\",\"hexagon\",\"circle\"."
			+ "</html>")		
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
	@ODLDefaultLongValue(12)	
	@ODLColumnDescription("The font size for the name. A value of 0 takes the default size. Less than zero deactivates the label.")	
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

	public String getLabelPositioningOption() {
		return labelPositioningOption;
	}

	@ODLColumnOrder(COL_LPO)
	@ODLNullAllowed
	public void setLabelPositioningOption(String lpo) {
		this.labelPositioningOption = lpo;
	}

	public Color getLabelColour() {
		return labelColor;
	}

	@ODLColumnOrder(COL_LABEL_COLOUR)
	@ODLNullAllowed
	public void setLabelColour(Color col) {
		this.labelColor = col;
	}

	@Override
	public long getFlags() {
		return flags;
	}

	@ODLColumnOrder(COL_FLAGS)
	@ODLNullAllowed
	@ODLDefaultLongValue(0)
	public void setFlags(long f) {
		this.flags = f;
	}

	public long getMinZoom() {
		return minZoom;
	}

	@ODLNullAllowed
	@ODLDefaultLongValue(0)
	@ODLColumnOrder(COL_MIN_ZOOM)
	public void setMinZoom(long minZoom) {
		this.minZoom = minZoom;
	}

	public long getMaxZoom() {
		return maxZoom;
	}

	@ODLNullAllowed
	@ODLDefaultLongValue(1000)
	@ODLColumnOrder(COL_MAX_ZOOM)
	public void setMaxZoom(long maxZoom) {
		this.maxZoom = maxZoom;
	}

	public long getLabelPriority() {
		return labelPriority;
	}

	@ODLNullAllowed
	@ODLDefaultLongValue(0)
	@ODLColumnOrder(COL_LABEL_PRIORITY)
	public void setLabelPriority(long labelPriority) {
		this.labelPriority = labelPriority;
	}

	
}
