/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;


final public class Time {
	private Time(){}
	
	private static long MILLIS_IN_MINUTES = (1000 * 60);
	public static String millisecsToString(long millis){
		long minutes = millis / MILLIS_IN_MINUTES;
		millis -= minutes * MILLIS_IN_MINUTES;
		long secs = millis/ 1000;
		String s = "";
		if(minutes>0){
			s += Long.toString(minutes) + " mins ";			
		}
		s += Long.toString(secs) + " secs";
		return s;
	}
}
