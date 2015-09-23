/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae.image;

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
import com.opendoorlogistics.core.scripts.formulae.image.ImageFormulaUtils.FilterMode;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.SimpleSoftReferenceMap;
import com.opendoorlogistics.core.utils.images.ImageUtils;

/**
 * fmimage uses an image cache... It assumes the script execution framework takes care of refreshing etc when data changes
 * @author Phil
 *
 */
final public class FmImage extends FunctionImpl {
	final private ODLTableReadOnly table;
	final private int groupKeyIndx;
	final private RenderProperties properties;
	final private Mode mode;

	public enum Mode {
		SHOW_SELECTED("1", "Show only the matching objects in the table and the background map, zooming on the matching objects."), 
		SHOW_ALL_ZOOM_SELECTED("2", "Show all objects in the table and the background map, zooming on the matching objects."),
		ZOOM_ONLY("3","Show background map only, zooming on the matching objects."),
		SHOW_SELECTED_NO_BACKGROUND("4", "Show the matching objects without the background map, zooming on the matching objects."),
		SHOW_INVERSE_SELECTED_NO_BACKGROUND("5", "Show the non-matching objects without the background map, zooming on the matching objects."),
		SHOW_INVERSE_SELECTED("6","Show the non-matching objects with the background map, zooming on the matching objects."),
		SHOW_MATCHING_OR_EMPTY_ZOOM_MATCHING("7","Show the matching objects or those objects with an empty or null image formula key, but zooming only on the matching objects.")
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
			ImageFormulaUtils.setNotToRenderBackgroundMap(flags);			
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

	private ImageFormulaCacheKey getCacheKey(FunctionParameters parameters) {
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

		ImageFormulaCacheKey key = new ImageFormulaCacheKey(this,vals[0], width.intValue(), height.intValue(), dotsPerCM.doubleValue());
		return key;
	}

	@Override
	public Object execute(FunctionParameters parameters) {

		ImageFormulaCacheKey key = getCacheKey(parameters);
		if (key == null) {
			return Functions.EXECUTION_ERROR;
		}

		// try to fetch using the cache key
		BufferedImage image = (BufferedImage)cache().get(key);
		if (image != null) {
			return image;
		}

		// find matching values in the other table
		List<DrawableObjectImpl> matchingPoints = ImageFormulaUtils.getPoints(table,groupKeyIndx, key.keyval,FilterMode.FILTER);

		BufferedImage ret = null;
		switch(mode){
		case SHOW_ALL_ZOOM_SELECTED:
			ret = ImageFormulaUtils.createImage(key.width, key.height, key.dotsPerCM,properties,isFixedPhysicalSize(), matchingPoints,ImageFormulaUtils.getPoints(table,-1, null, FilterMode.NONE));
			break;
			
		case SHOW_SELECTED:
		case SHOW_SELECTED_NO_BACKGROUND:
			ret = ImageFormulaUtils.createImage(key.width, key.height, key.dotsPerCM,properties,isFixedPhysicalSize(), matchingPoints,matchingPoints);
			break;
			
		case SHOW_INVERSE_SELECTED:
		case SHOW_INVERSE_SELECTED_NO_BACKGROUND:
			ret = ImageFormulaUtils.createImage(key.width, key.height, key.dotsPerCM,properties,isFixedPhysicalSize(), matchingPoints,ImageFormulaUtils.getPoints(table,groupKeyIndx, key.keyval,FilterMode.INVERSE_FILTER));
			break;
			
		case ZOOM_ONLY:
			ret = ImageFormulaUtils.createImage(key.width, key.height, key.dotsPerCM,properties,isFixedPhysicalSize(), matchingPoints,null);
			break;
			
		case SHOW_MATCHING_OR_EMPTY_ZOOM_MATCHING:
			List<DrawableObjectImpl> matchingOrNull = ImageFormulaUtils.getPoints(table,groupKeyIndx, key.keyval,FilterMode.FILTER_MATCH_OR_NULL_VALUE);			
			ret = ImageFormulaUtils.createImage(key.width, key.height, key.dotsPerCM,properties,isFixedPhysicalSize(), matchingPoints, matchingOrNull);			
			break;
		}


		// add to cache
		cache().put(key, ret, (long)ret.getWidth() * (long)ret.getHeight() * 4);

		return ret;
	}




	@Override
	public Function deepCopy() {
		throw new UnsupportedOperationException();
	}


}
