/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.RenderProperties.NumericRenderProp;
import com.opendoorlogistics.core.gis.map.SynchronousRenderer;
import com.opendoorlogistics.core.gis.map.View;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.SimpleSoftReferenceMap;
import com.opendoorlogistics.core.utils.images.ImageUtils;

final public class FmImage extends FunctionImpl {
	final private ODLTableReadOnly table;
	final private int groupKeyIndx;
	final private RenderProperties properties;
	final private Mode mode;

	// fmimage uses an image cache... It assumes the script execution framework takes care of refreshing etc when data changes
	//final private SimpleSoftReferenceMap<CacheKey, BufferedImage> cache = new SimpleSoftReferenceMap<>();

	public enum Mode {
		SHOW_SELECTED("1", "Show only the matching objects in the table and the background map, zooming on the matching objects."), 
		SHOW_ALL_ZOOM_SELECTED("2", "Show all objects in the table and the background map, zooming on the matching objects."),
		ZOOM_ONLY("3","Show background map only, zooming on the matching objects."),
		SHOW_SELECTED_NO_BACKGROUND("4", "Show the matching objects without the background map, zooming on the matching objects."),
		SHOW_INVERSE_SELECTED_NO_BACKGROUND("5", "Show the non-matching objects without the background map, zooming on the matching objects."),
		SHOW_INVERSE_SELECTED("6","Show the non-matching objects with the background map, zooming on the matching objects.")		
		;
		
		private Mode(String keyword,String description) {
			this.keyword = keyword;
			this.description = description;
		}

		private final String keyword;
		private final String description;
		
		public String getDescription(){
			return description;
		}
		
		public String getKeyword(){
			return keyword;
		}
	}

	private FmImage(Function foreignKeyValue, ODLTableReadOnly table, int groupKeyIndx,Mode mode, Function width, Function height, Function dotsPerCM,
			RenderProperties flags) {
		super(dotsPerCM != null ? new Function[] { foreignKeyValue, width, height, dotsPerCM } : new Function[] { foreignKeyValue, width, height });
		this.table = table;
		this.groupKeyIndx = groupKeyIndx;
		this.mode = mode;
		
		flags = new RenderProperties(flags);
		if(mode == Mode.SHOW_SELECTED_NO_BACKGROUND || mode==Mode.SHOW_INVERSE_SELECTED_NO_BACKGROUND){
			flags.setFlag(RenderProperties.SHOW_BACKGROUND, false);
			flags.setFlag(RenderProperties.SKIP_BACKGROUND_COLOUR_RENDERING, true);
		}
		this.properties = flags;
	}
	
	private RecentlyUsedCache cache(){
		return ApplicationCache.singleton().get(ApplicationCache.IMAGE_FORMULAE_CACHE);
	}

	public static FmImage createFixedPixelSize(Function foreignKeyValue, ODLTableReadOnly table, int groupKeyIndx, Mode mode,Function width, Function height,
			RenderProperties flags) {
		return new FmImage(foreignKeyValue, table, groupKeyIndx,mode, width, height, null, flags);
	}

	public static FmImage createFixedPhysicalSize(Function foreignKeyValue, ODLTableReadOnly table, int groupKeyIndx,Mode mode, Function width,
			Function height, Function dotsPerCM, RenderProperties properties) {
		return new FmImage(foreignKeyValue, table, groupKeyIndx,mode, width, height, dotsPerCM, properties);
	}

	private boolean isFixedPhysicalSize() {
		return nbChildren() == 4;
	}

	static protected class CacheKey {
		Object keyval;
		double width;
		double height;
		double dotsPerCM = -1;

		public CacheKey(Object keyval, double width, double height, double dotsPerCM) {
			super();
			this.keyval = keyval;
			this.width = width;
			this.height = height;
			this.dotsPerCM = dotsPerCM;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(dotsPerCM);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(height);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((keyval == null) ? 0 : keyval.hashCode());
			temp = Double.doubleToLongBits(width);
			result = prime * result + (int) (temp ^ (temp >>> 32));
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
			CacheKey other = (CacheKey) obj;
			if (Double.doubleToLongBits(dotsPerCM) != Double.doubleToLongBits(other.dotsPerCM))
				return false;
			if (Double.doubleToLongBits(height) != Double.doubleToLongBits(other.height))
				return false;
			if (keyval == null) {
				if (other.keyval != null)
					return false;
			} else if (!keyval.equals(other.keyval))
				return false;
			if (Double.doubleToLongBits(width) != Double.doubleToLongBits(other.width))
				return false;
			return true;
		}

	}

	private double validateImageDimension(double d) {
		if (d < 1) {
			return 1;
		}
		if (d > 10000) {
			return 10000;
		}
		return d;
	}

	private CacheKey getCacheKey(FunctionParameters parameters) {
		Object[] vals = executeChildFormulae(parameters, true);
		if (vals == null) {
			return null;
		}

		// get width and height
		int formulaIndx = 1;
		Double width = Numbers.toDouble(vals[formulaIndx++]);
		Double height = Numbers.toDouble(vals[formulaIndx++]);
		if (width == null || height == null) {
			return null;
		}

		Double dotsPerCM = 0.0;
		if (isFixedPhysicalSize()) {
			dotsPerCM = Numbers.toDouble(vals[formulaIndx++]);
		}

		// ensure sensible ranges
		width = validateImageDimension(width);
		height = validateImageDimension(height);
		CacheKey key = new CacheKey(vals[0], width.intValue(), height.intValue(), dotsPerCM.doubleValue());
		return key;
	}

	@Override
	public Object execute(FunctionParameters parameters) {

		CacheKey key = getCacheKey(parameters);
		if (key == null) {
			return Functions.EXECUTION_ERROR;
		}

		// try to fetch using the cache key
		BufferedImage image = (BufferedImage)cache().get(key);
		if (image != null) {
			return image;
		}

		// find matching values in the other table
		List<DrawableObjectImpl> matchingPoints = getPoints(key.keyval,FilterMode.FILTER);

		BufferedImage ret = null;
		switch(mode){
		case SHOW_ALL_ZOOM_SELECTED:
			ret = createImage(key.width, key.height, key.dotsPerCM, matchingPoints,getPoints(null, FilterMode.NONE));
			break;
			
		case SHOW_SELECTED:
		case SHOW_SELECTED_NO_BACKGROUND:
			ret = createImage(key.width, key.height, key.dotsPerCM, matchingPoints,matchingPoints);
			break;
			
		case SHOW_INVERSE_SELECTED:
		case SHOW_INVERSE_SELECTED_NO_BACKGROUND:
			ret = createImage(key.width, key.height, key.dotsPerCM, matchingPoints,getPoints(key.keyval,FilterMode.INVERSE_FILTER));
			break;
			
		case ZOOM_ONLY:
			ret = createImage(key.width, key.height, key.dotsPerCM, matchingPoints,null);
			break;
		}


		// add to cache
		cache().put(key, ret, (long)ret.getWidth() * (long)ret.getHeight() * 4);

		return ret;
	}

	private enum FilterMode{
		NONE,
		FILTER,
		INVERSE_FILTER
	}
	
	private List<DrawableObjectImpl> getPoints(Object valueToMatch, FilterMode filterMode) {
		int nr = table.getRowCount();
		ArrayList<DrawableObjectImpl> points = new ArrayList<>();
		BeanDatastoreMapping mapping = DrawableObjectImpl.getBeanMapping();
		for (int row = 0; row < nr; row++) {
			if(filterMode !=FilterMode.NONE){
				Object val = table.getValueAt(row, groupKeyIndx);
				boolean match =val != null && ColumnValueProcessor.isEqual(val, valueToMatch); 
				if ((filterMode==FilterMode.FILTER && match) || (filterMode==FilterMode.INVERSE_FILTER && !match)) {
					// each matching row gets turned into a point using the bean mapping
					DrawableObjectImpl pnt = (DrawableObjectImpl) mapping.getTableMapping(0).readObjectFromTableByRow(table, row);
					if (pnt != null) {
						points.add(pnt);
					}
				}				
			}else{
				DrawableObjectImpl pnt = (DrawableObjectImpl) mapping.getTableMapping(0).readObjectFromTableByRow(table, row);
				if (pnt != null) {
					points.add(pnt);
				}				
			}
		}
		return points;
	}

	private BufferedImage createImage(double width, double height, double dotsPerCM, List<DrawableObjectImpl> zoomPoints, List<DrawableObjectImpl> displayPoints) {
		BufferedImage ret;
		if (zoomPoints.size() > 0) {
			// get min spans if we have them
			Double minSpanLat=properties.getNumericProperty(NumericRenderProp.MIN_SPAN_DEGREES);
			Double minSpanLng=properties.getNumericProperty(NumericRenderProp.MIN_SPAN_DEGREES);
			if(properties.getNumericProperty(NumericRenderProp.MIN_SPAN_LATITUDE)!=null){
				minSpanLat =properties.getNumericProperty(NumericRenderProp.MIN_SPAN_LATITUDE); 
			}
			if(properties.getNumericProperty(NumericRenderProp.MIN_SPAN_LONGITUDE)!=null){
				minSpanLng =properties.getNumericProperty(NumericRenderProp.MIN_SPAN_LONGITUDE); 
			}
			
			// draw using a bounding box...
			View view = null;
			if(minSpanLat!=null || minSpanLng!=null){
				view = View.createViewWithMinSpans(zoomPoints, minSpanLat!=null?minSpanLat:0, minSpanLng!=null?minSpanLng:0);	
			}else{
				view = View.createView(zoomPoints);				
			}

			if (isFixedPhysicalSize()) {
				ret = SynchronousRenderer.singleton().drawPrintableAtLatLongCentre(view, width, height, dotsPerCM, displayPoints, properties.getFlags())
						.getFirst();
			} else {
				ret = SynchronousRenderer.singleton().drawAtLatLongCentre(view, (int) width, (int) height, properties.getFlags(), displayPoints);
			}

		} else {
			// return blank image..
			ret = ImageUtils.createBlankImage((int) width, (int) height, Color.WHITE);
		}
		return ret;
	}

	@Override
	public Function deepCopy() {
		throw new UnsupportedOperationException();
	}


}
