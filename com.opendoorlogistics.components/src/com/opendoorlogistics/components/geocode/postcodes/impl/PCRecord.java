/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes.impl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class PCRecord implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public enum StrField{
		COUNTRY_CODE("CountryCode"),
		POSTAL_CODE("Postcode"),
		PLACE_NAME("PlaceName"),
		ADMIN_NAME1("AdminName1"),
		ADMIN_CODE1("AdminCode1"),
		ADMIN_NAME2("AdminName2"),
		ADMIN_CODE2("AdminCode2"),
		ADMIN_NAME3("AdminName3"),
		ADMIN_CODE3("AdminCode3");
		
		private final String displayText;
		private final String [] tags;
		
		private StrField(String displayText, String...tags) {
			this.displayText = displayText;
			this.tags = tags;
		}

		public String getDisplayText() {
			return displayText;
		}

		public String[] getTags() {
			return tags;
		}
		
	}
	
	private String [] strs = new String[StrField.values().length];	
	private BigDecimal latitude;
	private BigDecimal longitude;
	private short accuracy;
	
	public String getField(StrField fld){
		return strs[fld.ordinal()];
	}
	
	public void setField(StrField fld, String val){
		strs[fld.ordinal()]=val;
	}
	
	public BigDecimal getLatitude() {
		return latitude;
	}
	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}
	public BigDecimal getLongitude() {
		return longitude;
	}
	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}
	public short getAccuracy() {
		return accuracy;
	}
	public void setAccuracy(short accuracy) {
		this.accuracy = accuracy;
	}
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		for(StrField fld: StrField.values()){
			builder.append(fld.name() +"=" + strs[fld.ordinal()]);
			builder.append(", ");
		}
		return builder.toString()+ "lat=" + latitude + ", long=" + longitude + ", accuracy=" + accuracy;
	}
	
	public static PCRecord merge(List<PCRecord> records){
		int n = records.size();
		if(n==0){
			return null;
		}
		else if(n==1){
			return records.get(0);
		}
		
		BigDecimal lat=null;
		BigDecimal lng=null;

		// create record flagging accuracy as unknown
		PCRecord ret = new PCRecord();
		ret.setAccuracy((short)-1);
		
		for(int i =0 ; i < n ; i++){
			PCRecord rec = records.get(i);
			
			if(i==0){
				// copy initially
				for(StrField fld : StrField.values()){
					ret.setField(fld, rec.getField(fld));
				}
				lat = rec.getLatitude();
				lng = rec.getLongitude();				
			}
			else{
				// sum lat and longs
				lat = lat.add(rec.getLatitude());
				lng = lng.add(rec.getLongitude());
				
				// only keep identical fields
				for(StrField fld : StrField.values()){
					if(ret.getField(fld).equals(rec.getField(fld))==false){
						ret.setField(fld, "");
					}
				}
			}
		}
		
		lat = lat.divide(new BigDecimal(n), BigDecimal.ROUND_HALF_UP);
		lng = lng.divide(new BigDecimal(n), BigDecimal.ROUND_HALF_UP);
		ret.setLatitude(lat);
		ret.setLongitude(lng);			
	
		return ret;
	}
}
