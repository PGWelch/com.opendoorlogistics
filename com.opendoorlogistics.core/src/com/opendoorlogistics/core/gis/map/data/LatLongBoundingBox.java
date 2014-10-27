/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.data;

import java.util.HashSet;
import java.util.Set;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.utils.DoubleRange;
import com.vividsolutions.jts.geom.Envelope;

public class LatLongBoundingBox {
	private double minLat = Double.POSITIVE_INFINITY;
	private double minLng= Double.POSITIVE_INFINITY;
	private double maxLat = Double.NEGATIVE_INFINITY;
	private double maxLng = Double.NEGATIVE_INFINITY;
	
	public LatLongBoundingBox(){
		
	}
	
	public void add(double lat, double lng){
		minLat = Math.min(minLat, lat);
		minLng = Math.min(minLng, lng);
		maxLat = Math.max(maxLat, lat);
		maxLng = Math.max(maxLng, lng);
	}

	public void add(Envelope envelope){
		add(envelope.getMinY(),envelope.getMinX());
		add(envelope.getMaxY(),envelope.getMaxX());
	}
	
	public void add(LatLong ll){
		add(ll.getLatitude(),ll.getLongitude());
	}
	
	public boolean isValid(){
		return minLat<=maxLat && minLng <=maxLng;
	}
	
	public double getMinLat() {
		return minLat;
	}

	public void setMinLat(double minLat) {
		this.minLat = minLat;
	}

	public double getMinLng() {
		return minLng;
	}

	public void setMinLng(double minLng) {
		this.minLng = minLng;
	}

	public double getMaxLat() {
		return maxLat;
	}

	public void setMaxLat(double maxLat) {
		this.maxLat = maxLat;
	}

	public double getMaxLng() {
		return maxLng;
	}

	public void setMaxLng(double maxLng) {
		this.maxLng = maxLng;
	}
	
	public DoubleRange getLatRange(){
		return new DoubleRange(minLat, maxLat);
	}
	
	public DoubleRange getLngRange(){
		return new DoubleRange(minLng, maxLng);
	}
	
	public Set<GeoPosition> getCornerSet(){
		HashSet<GeoPosition> ret = new HashSet<>();
		if(isValid()){
			ret.add(new GeoPosition(minLat, minLng));
			ret.add(new GeoPosition(minLat, maxLng));
			ret.add(new GeoPosition(maxLat, minLng));
			ret.add(new GeoPosition(maxLat, maxLng));			
		}
		
		return ret;
	}
	
	static{
		Spatial.initSpatial();
	}
}
