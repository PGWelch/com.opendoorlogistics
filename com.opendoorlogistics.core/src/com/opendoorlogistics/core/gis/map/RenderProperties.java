/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.strings.StringKeyValue;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class RenderProperties {
	private static final TreeMap<StringKeyValue, Long> keyValueMap;
	
	public static final long SHOW_TEXT = 1<<0;
	public static final long SHOW_BACKGROUND = 1<<1;
	public static final long SHOW_ALL = SHOW_TEXT | SHOW_BACKGROUND;
	public static final long LEGEND_TOP_LEFT = 1<<2;
	public static final long LEGEND_TOP_RIGHT = 1<<3;
	public static final long LEGEND_BOTTOM_LEFT = 1<<4;
	public static final long LEGEND_BOTTOM_RIGHT = 1<<5;
	public static final long LEGEND_TOP = 1<<6;
	public static final long LEGEND_BOTTOM = 1<<7;
//	public static final long RENDER_FADE = 1<<8;
	public static final long SKIP_BACKGROUND_COLOUR_RENDERING = 1<<9;
	public static final long DRAW_OSM_COPYRIGHT = 1<<10;
	public static final long RENDER_BORDERS_ONLY = 1<<11;
	public static final long SKIP_BORDER_RENDERING = 1<<12;
	public static final long THIN_POLYGON_BORDERS= 1<<13;
	
	public enum NumericRenderProp{
		MIN_SPAN_LONGITUDE("minSpanLongitude"),
		MIN_SPAN_LATITUDE("minSpanLatitude"),
		MIN_SPAN_DEGREES("minSpanDegrees");
		
		private NumericRenderProp(String keyword) {
			this.keyword = keyword;
		}

		private final String keyword;
		
		public String getKeyword(){
			return keyword;
		}
	}
	
	public RenderProperties(){
		
	}
	
	public RenderProperties(RenderProperties r){
		flags = r.flags;
		for(Map.Entry<NumericRenderProp, Double> entry:r.numerics.entrySet()){
			numerics.put(entry.getKey(), entry.getValue());
		}
	}
	
	public RenderProperties(String keyValueString){
		flags =0;
		List<StringKeyValue> list = StringKeyValue.parseCommaSeparated(keyValueString, true);
		
		for(StringKeyValue kv :list ){
			boolean found=false;
			Long value = keyValueMap.get(kv);
			if(value!=null){
				flags |=value;
				found = true;
			}
			
			if(!found){
				for(NumericRenderProp nrp: NumericRenderProp.values()){
					if(Strings.equalsStd(nrp.getKeyword(), kv.getKey())){
						Double number = Numbers.toDouble(kv.getValue());
						if(number==null){
							throw new RuntimeException("Unparseable number found for render property: " + nrp.getKeyword());						
						}
						numerics.put(nrp, number);
						found = true;
						break;
					}
				}				
			}
			
			if(!found){
				throw new RuntimeException("Unknown key-value render property option:" + kv);
			}
		}

	}
	
	private long flags = SHOW_ALL;
	private final HashMap<NumericRenderProp, Double> numerics = new HashMap<>();
	
	public boolean hasFlag(long flag){
		return hasFlag(this.flags, flag);
	}
	
	public long getFlags(){
		return flags;
	}
	
	public static boolean hasFlag(long flags,long flag){
		return (flags & flag) == flag;
	}
	
	public void addFlags(long l){
		flags |=l;
	}
	
	public void setFlag(long flag, boolean on){
		if(on){
			flags |= flag;
		}else{
			flags &= ~flag;
		}
	}
	
	public void toggleFlag(long flag){
		setFlag(flag, !hasFlag(flag));
	}
	
	static{
		// init keyvalue map
		keyValueMap = new TreeMap<>();
		keyValueMap.put(new StringKeyValue("legend", "topleft"), LEGEND_TOP_LEFT);
		keyValueMap.put(new StringKeyValue("legend", "topright"), LEGEND_TOP_RIGHT);
		keyValueMap.put(new StringKeyValue("legend", "bottomleft"), LEGEND_BOTTOM_LEFT);
		keyValueMap.put(new StringKeyValue("legend", "bottomright"), LEGEND_BOTTOM_RIGHT);
		keyValueMap.put(new StringKeyValue("legend", "top"), LEGEND_TOP);
		keyValueMap.put(new StringKeyValue("legend", "bottom"), LEGEND_BOTTOM);
	}
	
	public Double getNumericProperty(NumericRenderProp nrp){
		return numerics.get(nrp);
	}
}
