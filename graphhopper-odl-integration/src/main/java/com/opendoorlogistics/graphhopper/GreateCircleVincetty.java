/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.graphhopper;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;


final public class GreateCircleVincetty {

	public static final double EARTH_RADIUS_METRES = 6371000;
	
	/**
	 * See http://en.wikipedia.org/wiki/Great-circle_distance
	 * Vincetty formula. Returns metres
	 * @param from
	 * @param to
	 * @return
	 */
	public static double greatCircleApprox(double latFrom, double lngFrom, double latTo, double lngTo) {
		double lat1 = toRadians(latFrom);
		double lat2 = toRadians(latTo);
		double lng1 = toRadians(lngFrom);
		double lng2 = toRadians(lngTo);

		double deltaLng = Math.abs(lng1 - lng2);

		double a = cos(lat2) * sin(deltaLng);
		a *= a;

		double b = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLng);
		b *= b;

		double c = sin(lat1) * sin(lat2);

		double d = cos(lat1) * cos(lat2) * cos(deltaLng);

		double numerator = Math.sqrt(a + b);
		double denominator = c + d;

		double centralAngle = atan2(numerator, denominator);
		double distance = EARTH_RADIUS_METRES * centralAngle;
		return distance;
	}
}