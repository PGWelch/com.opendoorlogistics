package com.opendoorlogistics.core.scripts.formulae.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.RenderProperties.NumericRenderProp;
import com.opendoorlogistics.core.gis.map.SynchronousRenderer;
import com.opendoorlogistics.core.gis.map.View;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

public class ImageFormulaUtils {

	static BufferedImage createImage(double width, double height, double dotsPerCM,RenderProperties properties, boolean isPhysicalSize, List<DrawableObjectImpl> zoomPoints, List<DrawableObjectImpl> displayPoints) {
		BufferedImage ret;
		if (zoomPoints.size() > 0) {
			View view = createViewFromPoints(zoomPoints,properties);

			if (isPhysicalSize) {
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

	static View createViewFromPoints(List<DrawableObjectImpl> zoomPoints, RenderProperties properties) {
		// get min spans if we have them
		Double minSpanLat=null;
		Double minSpanLng=null;
		if(properties!=null){
			minSpanLat=properties.getNumericProperty(NumericRenderProp.MIN_SPAN_DEGREES);
			minSpanLng=properties.getNumericProperty(NumericRenderProp.MIN_SPAN_DEGREES);
			if(properties.getNumericProperty(NumericRenderProp.MIN_SPAN_LATITUDE)!=null){
				minSpanLat =properties.getNumericProperty(NumericRenderProp.MIN_SPAN_LATITUDE); 
			}
			if(properties.getNumericProperty(NumericRenderProp.MIN_SPAN_LONGITUDE)!=null){
				minSpanLng =properties.getNumericProperty(NumericRenderProp.MIN_SPAN_LONGITUDE); 
			}				
		}
		
		// draw using a bounding box...
		View view = null;
		if(minSpanLat!=null || minSpanLng!=null){
			view = View.createViewWithMinSpans(zoomPoints, minSpanLat!=null?minSpanLat:0, minSpanLng!=null?minSpanLng:0);	
		}else{
			view = View.createView(zoomPoints);				
		}
		return view;
	}
	
	enum FilterMode{
		NONE,
		FILTER,
		INVERSE_FILTER,
		FILTER_MATCH_OR_NULL_VALUE,
	}
	
	
	static List<DrawableObjectImpl> getPoints(ODLTableReadOnly drawablesTable, int filterColumnIndx, Object valueToMatch, FilterMode filterMode) {
		int nr = drawablesTable.getRowCount();
		ArrayList<DrawableObjectImpl> points = new ArrayList<>();
		BeanDatastoreMapping mapping = DrawableObjectImpl.getBeanMapping();
		for (int row = 0; row < nr; row++) {
			if(filterMode !=FilterMode.NONE){
				Object val = drawablesTable.getValueAt(row, filterColumnIndx);
				boolean match =val != null && ColumnValueProcessor.isEqual(val, valueToMatch); 
				if ((filterMode==FilterMode.FILTER && match) || (filterMode==FilterMode.INVERSE_FILTER && !match)
					||(filterMode == FilterMode.FILTER_MATCH_OR_NULL_VALUE && (match || Strings.isEmptyWhenStandardised(val)))) {
					// each matching row gets turned into a point using the bean mapping
					DrawableObjectImpl pnt = (DrawableObjectImpl) mapping.getTableMapping(0).readObjectFromTableByRow(drawablesTable, row);
					if (pnt != null) {
						points.add(pnt);
					}
				}				
			}else{
				DrawableObjectImpl pnt = (DrawableObjectImpl) mapping.getTableMapping(0).readObjectFromTableByRow(drawablesTable, row);
				if (pnt != null) {
					points.add(pnt);
				}				
			}
		}
		return points;
	}

	static double validateImageDimension(double d) {
		if (d < 1) {
			return 1;
		}
		if (d > 10000) {
			return 10000;
		}
		return d;
	}
	
	static void setNotToRenderBackgroundMap(RenderProperties properties){
		properties.setFlag(RenderProperties.SHOW_BACKGROUND, false);
		properties.setFlag(RenderProperties.SKIP_BACKGROUND_COLOUR_RENDERING, true);
		
	}

}
