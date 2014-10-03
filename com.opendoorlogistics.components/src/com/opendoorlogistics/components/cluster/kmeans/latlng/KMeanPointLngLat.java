/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.kmeans.latlng;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.components.cluster.kmeans.KMeanPoint;
import com.opendoorlogistics.core.geometry.GreateCircle;

final public class KMeanPointLngLat extends KMeanPoint implements LatLong{

	public double longitude;

	public double latitude;
	
	public KMeanPointLngLat(double longitude, double latitude) {
		super();
		this.longitude = longitude;
		this.latitude = latitude;
	}
	
	public KMeanPointLngLat(){}

	/**
	 * Convert long/lat to cartesian with circle radius=1
	 * @return
	 */
	public double [] toUnitCartesian(){
		// see http://www.math.montana.edu/frankw/ccp/multiworld/multipleIVP/spherical/learn.htm
		double [] ret = new double[3];
		double latr =latAsRadians();
		double lngr =lngAsRadians();
		ret[0] = sin(latr) * cos(lngr);
		ret[1] = sin(latr) * sin(lngr);
		ret[2] = cos(latr);

		return ret;
	}

	@Override
	public String toString() {
		return "KMeanPointLngLat [longitude=" + longitude + ", latitude=" + latitude + "]";
	}
	
	private double latAsRadians(){
		return toRadians(90-latitude);
	}

	private double lngAsRadians(){
		return toRadians(longitude);
	}

	
	@Override
	public double distance(KMeanPoint km){
		KMeanPointLngLat other = (KMeanPointLngLat)km;
	//	return DefaultEllipsoid.WGS84.orthodromicDistance(longitude, latitude, other.longitude, other.latitude);
		return GreateCircle.greatCircle(this, other, false);
	}

	@Override
	public double getLatitude() {
		return latitude;
	}

	@Override
	public double getLongitude() {
		return longitude;
	}
	
}
