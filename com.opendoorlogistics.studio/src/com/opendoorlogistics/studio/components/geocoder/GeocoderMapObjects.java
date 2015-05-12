/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.core.gis.map.Legend;
import com.opendoorlogistics.core.gis.map.Legend.LegendAlignment;
import com.opendoorlogistics.core.gis.map.MapUtils;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.utils.ObjectConverter;
import com.opendoorlogistics.core.utils.iterators.IteratorAdapter;
import com.opendoorlogistics.core.utils.iterators.IteratorUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModel;
import com.opendoorlogistics.studio.components.geocoder.model.SearchResultPoint;
import com.opendoorlogistics.studio.components.map.LayeredDrawables;

final public class GeocoderMapObjects {
	private final static int RESULT_WIDTH=15;
	private final static int GEOCODE_WIDTH=16;

//	public static LegendCreator createLegendCreator(){
//		return new LegendCreator() {
//			
//			@Override
//			public BufferedImage createLegend(Iterable<? extends DrawableObject> pnts) {
//				return Legend.createLegendImageFromDrawables(createLegendData());
//			}
//		};
//	}
	
	public static ODLDatastore<? extends ODLTable> createDrawableDs(GeocodeModel model){
		return MapUtils.convertToDatastore(createObjects(model), false);
	}
	
//	public static LayeredDrawables createDrawable(final GeocodeModel model) {
//		return new LayeredDrawables(null, new Iterable<DrawableObject>() {
//
//			@Override
//			public Iterator<DrawableObject> iterator() {
//				ArrayList<DrawableObjectImpl> ret = createObjects(model);
//				return new IteratorAdapter<DrawableObjectImpl, DrawableObject>(ret.iterator(), new ObjectConverter<DrawableObjectImpl, DrawableObject>() {
//
//					@Override
//					public DrawableObject convert(DrawableObjectImpl from) {
//						return from;
//					}
//				});
//				
//			}
//
//	
//		},null);
//	}

	private static DrawableObjectImpl createResultDrawable(double lat, double lng, boolean isSelected) {
		DrawableObjectImpl drawable = new DrawableObjectImpl();
		drawable.setLatitude(lat);
		drawable.setLongitude(lng);
		drawable.setPixelWidth(RESULT_WIDTH);
		drawable.setColour(Color.BLUE);
		drawable.setDrawOutline(1);
		drawable.setLegendKey("Search result (unselected)");
		
		if(isSelected){
			drawable.setPixelWidth(RESULT_WIDTH);
			drawable.setDrawOutline(0);
			drawable.setColour(Color.RED);
			drawable.setLegendKey("Search result (selected)");
		}
		return drawable;
	}
	
	private static ArrayList<DrawableObjectImpl> createObjects(final GeocodeModel model) {
		ArrayList<DrawableObjectImpl> ret = new ArrayList<>();
		
		// add search results first
		int count=0;
		if(model.getSearchResults()!=null){
			for(SearchResultPoint pnt : model.getSearchResults()){
				boolean isSelected = model.getSelectedResultIndices()!=null && IteratorUtils.contains(model.getSelectedResultIndices(), count);
				DrawableObjectImpl drawable = createResultDrawable( pnt.getLatitude(),  pnt.getLongitude(), isSelected);
				drawable.setGlobalRowId(count);
				
				String name = "("+ Integer.toString(count+1) + ") ";
				int maxLetters = 15;
				String address = Strings.getLeftWithoutWordSplitting(pnt.getAddress(), maxLetters);
				if(address.length() < pnt.getAddress().length()){
					address +="...";
				}
				drawable.setLabel(name + address);	
				
				// add in reverse order so top ones render first
				ret.add(0, drawable);
				
				count++;
			}
		}
		
		// then current geocode if non null
		if(model.getLatitude()!=null && model.getLongitude()!=null){
			double lat = model.getLatitude();
			double lng = model.getLongitude();
			DrawableObjectImpl item = createGeocodeDrawable(lat, lng);
			item.setGlobalRowId(count++);
			item.setLabel(model.getAddress());
			ret.add(item);
		}
		return ret;
	}

	private static DrawableObjectImpl createGeocodeDrawable(double lat, double lng) {
		DrawableObjectImpl item = new DrawableObjectImpl();
		item.setColour(Color.GREEN);
		item.setLegendKey("Geocoded address");
		item.setLatitude(lat);
		item.setLongitude(lng);
		item.setPixelWidth(GEOCODE_WIDTH);
		return item;
	}

	private static Iterable<DrawableObject> createLegendData() {
		Collection<DrawableObject> tmp = new ArrayList<>();
		tmp.add(createResultDrawable(0, 0, false));
		tmp.add(createResultDrawable(0, 0, true));
		tmp.add(createGeocodeDrawable(0, 0));
		return tmp;
	}
	
	public static BufferedImage createLegend(){
		return Legend.createLegendImageFromDrawables(createLegendData(), 12, LegendAlignment.HORIZONTAL);
	}
}
