/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder.model;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;

public enum GeocodeAverageType{
	TOP_ONLY("Top result"),
	ALL("Average all results"),
	SELECTED("Average selected");
	
	private GeocodeAverageType(String prefix) {
		this.prefix = prefix;
	}
	
	private final String prefix;

	public LatLong getAverage(GeocodeModel model){
		return getAverage(model, null);
	}
	
	public LatLong getAverage(GeocodeModel model, int [] countOut){
		LatLongImpl sum = new LatLongImpl();
		int count=0;
		if(model.getSearchResults()!=null && model.getSearchResults().size()>0){
			ArrayList<SearchResultPoint> results = new ArrayList<>();
			switch(this){
			case TOP_ONLY:
				results.add(model.getSearchResults().get(0));
				break;
				
			case ALL:
				results.addAll(model.getSearchResults());
				break;
				
			case SELECTED:
				if(model.getSelectedResultIndices()!=null){
					for(int i : model.getSelectedResultIndices()){
						results.add(model.getSearchResults().get(i));
					}					
				}
				break;
			}
			
			for(SearchResultPoint pnt : results){
				sum.add(pnt);
				count++;
			}		
		}
		
		if(count>0){
			sum.multiply(1.0/count);
		}	
		
		if(countOut!=null){
			countOut[0] = count;
		}
		
		return sum;
	}
	
	public boolean isAvailable(GeocodeModel model){
		if(model.getSearchResults().size()==0){
			return false;
		}
		
		if(this == SELECTED && model.getSelectedResultsCount()==0){
			return false;
		}
		
		return true;
	}
	
	public String getText(GeocodeModel model, boolean lineBreak){
		if(model!=null && isAvailable(model)){
			int [] count = new int[1];
			LatLong pnt = getAverage(model, count);
			NumberFormat f = DecimalFormat.getInstance();
			f.setMaximumFractionDigits(4);				
			return "<html><center>" + prefix + 
			//	(this!=TOP_ONLY? " " + count[0]:"") +
				(lineBreak? "<br>": " ")
				+ "(" + f.format(pnt.getLatitude()) + "," + f.format(pnt.getLongitude())+")" + "</center></html>";					
		}else{
			return  "<html><center>" + prefix + "<br>(Unavailable)</center></html>";
		}
	}
}
