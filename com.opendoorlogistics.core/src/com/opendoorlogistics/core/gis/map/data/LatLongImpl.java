/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.data;

import java.util.Random;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTag;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;

public class LatLongImpl extends BeanMappedRowImpl implements LatLong{
	private double latitude;
	private double longitude;
	
	public LatLongImpl(){}
	
	public LatLongImpl(LatLong ll){
		this(ll.getLatitude(),ll.getLongitude());
	}

	
	public LatLongImpl(double latitude, double longitude) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Override
	public double getLatitude() {
		return latitude;
	}
	
	@ODLColumnOrder(0)
	@ODLTag(PredefinedTags.LATITUDE)
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	
	@Override
	public double getLongitude() {
		return longitude;
	}
	
	@ODLColumnOrder(1)
	@ODLTag(PredefinedTags.LONGITUDE)
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
//	public static ODLDatastore<? extends ODLTableDefinition> getDsDefinition() {
//		return BeanMapping.buildDatastore(LatLongImpl.class).getDefinition();
//	}

	public void add(LatLong ll){
		latitude += ll.getLatitude();
		longitude += ll.getLongitude();
	}
	
	public void multiply(double f){
		latitude *=f;
		longitude *=f;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitude);
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
		LatLongImpl other = (LatLongImpl) obj;
		if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
			return false;
		if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
			return false;
		return true;
	}
	
	@Override
	public String toString(){
	//	StringBuilder builder = new StringBuilder();
		return "(lat=" + getLatitude() + ", long=" + getLongitude() + ")";
	}
	

	public static LatLongImpl random(Random random){
		return new LatLongImpl(-90 + 180*random.nextDouble(), -180 + 360*random.nextDouble());
	}
	
//	public static LatLongImpl ensureRange(LatLong ll){
//		double lat = ll.getLatitude();
//		while(lat < -90){
//			lat += 180;
//		}
//		double lng = ll.getLongitude();
//		return null;
//	}
}
