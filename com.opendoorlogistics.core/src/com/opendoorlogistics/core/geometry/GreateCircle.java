/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.geometry;

import java.util.ArrayList;
import java.util.Random;

import org.geotools.referencing.datum.DefaultEllipsoid;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.graphhopper.*;

final public class GreateCircle {

	/**
	 * Returns great circle distance in metres
	 * @param from
	 * @param to
	 * @param highAccuracy
	 * @return
	 */
	public static double greatCircle(LatLong from, LatLong to, boolean highAccuracy) {
		double d=0;
		if(highAccuracy){
			DefaultEllipsoid elipsoid = DefaultEllipsoid.WGS84;
			d= elipsoid.orthodromicDistance(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude());
	
		}else{
			d = greatCircleApprox(from, to);
		}
		return d;
	}
	
	/**
	 * See http://en.wikipedia.org/wiki/Great-circle_distance
	 * Vincetty formula. Returns metres
	 * @param from
	 * @param to
	 * @return
	 */
	public static double greatCircleApprox(LatLong from, LatLong to) {
		return GreateCircleVincetty.greatCircleApprox(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
	}
	
//	public static double greatCircleApprox(LatLong from, LatLong to) {
//		double lat1 = FastMath.toRadians(from.getLatitude());
//		double lat2 = FastMath.toRadians(to.getLatitude());
//		double lng1 = FastMath.toRadians(from.getLongitude());
//		double lng2 = FastMath.toRadians(to.getLongitude());
//
//		double deltaLng = FastMath.abs(lng1 - lng2);
//
//		double a = FastMath.cos(lat2) * FastMath.sin(deltaLng);
//		a *= a;
//
//		double b = FastMath.cos(lat1) * FastMath.sin(lat2) - FastMath.sin(lat1) * FastMath.cos(lat2) * FastMath.cos(deltaLng);
//		b *= b;
//
//		double c = FastMath.sin(lat1) * FastMath.sin(lat2);
//
//		double d = FastMath.cos(lat1) * FastMath.cos(lat2) * FastMath.cos(deltaLng);
//
//		double numerator = FastMath.sqrt(a + b);
//		double denominator = c + d;
//
//		double centralAngle = FastMath.atan2(numerator, denominator);
//		double distance = EARTH_RADIUS_METRES * centralAngle;
//		return distance;
//	}
	
	public static void main(String[]args){
		Random random = new Random(123);
		ArrayList<LatLong> lls = new ArrayList<>();
		for(int i =0 ; i<100 ; i++){
			LatLongImpl ll = new LatLongImpl(-90 + 180*random.nextDouble(), -180 + 360*random.nextDouble());
			lls.add(ll);
		}
		
		//while(true){
			for(LatLong from:lls){
				for(LatLong to:lls){
					double a = greatCircle(from, to,false);
					
					DefaultEllipsoid elipsoid = DefaultEllipsoid.WGS84;
					double b = elipsoid.orthodromicDistance(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude());
					System.out.println("From " + from + " To "+ to + " a=" + a + " b=" + b);
				}
			}			
		//}
	}
}
