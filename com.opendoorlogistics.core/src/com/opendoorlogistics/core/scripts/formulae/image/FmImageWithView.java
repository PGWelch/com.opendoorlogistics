package com.opendoorlogistics.core.scripts.formulae.image;

import java.awt.image.BufferedImage;
import java.util.List;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.scripts.formulae.image.ImageFormulaUtils.FilterMode;
import com.opendoorlogistics.core.utils.Numbers;

/**
 * Formula to create an image using a view defined by points in another table
 * @author Phil
 *
 */
public class FmImageWithView extends FunctionImpl{
	final private ODLTableReadOnly tableToDraw;
	final private ODLTableReadOnly tableToProvideView;
	final private int viewTableFilterColumnIndex;
	final private RenderProperties properties;
	final private boolean isPrintable;

	enum IWVMode{
		SHOW_BACKGROUND_MAP("1", "Show the objects from the table to draw and the background map."),
		NO_BACKGROUND_MAP("2", "Show the objects from the table to draw without the background map.");
				
		private IWVMode(String keyword,String description) {
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
	};
	
	FmImageWithView(ODLTableReadOnly tableToDraw, ODLTableReadOnly tableToProvideView, int viewTableFilterColumnIndex,Function foreignKeyValue , IWVMode mode, Function width, Function height, Function dotsPerCM,
			RenderProperties flags) {
		super(dotsPerCM != null ? new Function[] { foreignKeyValue, width, height, dotsPerCM } : new Function[] { foreignKeyValue, width, height });
		this.tableToDraw = tableToDraw;
		this.tableToProvideView = tableToProvideView;
		this.viewTableFilterColumnIndex = viewTableFilterColumnIndex;
		this.isPrintable = dotsPerCM!=null;
		
		this.properties = new RenderProperties(flags);
		if(mode == IWVMode.NO_BACKGROUND_MAP){
			ImageFormulaUtils.setNotToRenderBackgroundMap(properties);
		}		
	}
	
	@Override
	public Object execute(FunctionParameters parameters) {
		
		// get cache key
		Object[] vals = executeChildFormulae(parameters, true);
		if (vals == null) {
			return Functions.EXECUTION_ERROR;
		}
		int formulaIndx = 1;
		Double width = Numbers.toDouble(vals[formulaIndx++]);
		Double height = Numbers.toDouble(vals[formulaIndx++]);
		if (width == null || height == null) {
			return Functions.EXECUTION_ERROR;
		}
		Double dotsPerCM = isPrintable ? Numbers.toDouble(vals[formulaIndx++]) : 0.0;
		ImageFormulaCacheKey key = new ImageFormulaCacheKey(this,vals[0], width.intValue(), height.intValue(), dotsPerCM.doubleValue());
		
		// try to fetch using the cache key
		BufferedImage image = (BufferedImage)cache().get(key);
		if (image != null) {
			return image;
		}

		// get the points which define the view and the points to draw
		List<DrawableObjectImpl> viewPoints = ImageFormulaUtils.getPoints(tableToProvideView,viewTableFilterColumnIndex, key.keyval,FilterMode.FILTER);
		List<DrawableObjectImpl> drawPoints = ImageFormulaUtils.getPoints(tableToDraw, -1, null, FilterMode.NONE);		
		BufferedImage ret = ImageFormulaUtils.createImage(key.width, key.height, key.dotsPerCM,properties,isPrintable, viewPoints,drawPoints);

		// add to cache
		cache().put(key, ret, (long)ret.getWidth() * (long)ret.getHeight() * 4);

		return ret;
	}

	@Override
	public Function deepCopy() {
		throw new UnsupportedOperationException();
	}

	private RecentlyUsedCache cache(){
		return ApplicationCache.singleton().get(ApplicationCache.IMAGE_WITH_VIEW_FORMULAE_CACHE);
	}

}
