/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;

import java.util.Calendar;


/**
 * This class represents an immutable time without a time zone (i.e. wall clock time) - e.g. 21:06 - except
 * time can extend to multiple days. For example we can have 5 days, 02:04:00 or 21 days, 23:43:12 (i.e.
 * an ODLTime is more like a time duration...).
 * Precision is millisecond.
 * 
 * @author Phil
 *
 */
public class ODLTime extends Number implements Comparable<ODLTime> {
	public static final long MILLIS_IN_SEC = 1000;
	public static final long MILLIS_IN_MIN = MILLIS_IN_SEC * 60;
	public static final long MILLIS_IN_HOUR = MILLIS_IN_MIN * 60;
	public static final long MILLIS_IN_DAY = MILLIS_IN_HOUR * 24;

	private final long value;

	public static final ODLTime ZERO = new ODLTime(0);
	
	public long getValue() {
		return value;
	}

	public ODLTime(){
		Calendar cal = Calendar.getInstance();
		value = cal.get(Calendar.HOUR_OF_DAY) * MILLIS_IN_HOUR + cal.get(Calendar.MINUTE) * MILLIS_IN_MIN + cal.get(Calendar.SECOND) * MILLIS_IN_SEC + cal.get(Calendar.MILLISECOND);
	}
	
	public ODLTime(long milliseconds) {
		this.value = milliseconds;
	}
	
	public ODLTime(long hours, long minutes){
		value = hours * MILLIS_IN_HOUR + minutes * MILLIS_IN_MIN;
	}

	public ODLTime(long days, long hours, long minutes){
		this(days, hours, minutes, 0, 0);
	}

	public ODLTime(long days, long hours, long minutes, long seconds){
		this(days, hours, minutes, seconds, 0);
	}

	public ODLTime(long days, long hours, long minutes, long seconds , long milliseconds){
		value = days * MILLIS_IN_DAY + hours * MILLIS_IN_HOUR + minutes * MILLIS_IN_MIN + seconds * MILLIS_IN_SEC + milliseconds;
	}

	public ODLTime(ODLTime value) {
		this.value = value.getTotalMilliseconds();
	}
	
	public long getTotalDays() {
		return value / MILLIS_IN_DAY;
	}

	public long getTotalHours() {
		return value / MILLIS_IN_HOUR;
	}

	public long getTotalMinutes() {
		return value / MILLIS_IN_MIN;
	}

	public long getTotalSeconds() {
		return value / MILLIS_IN_SEC;
	}

	public long getTotalMilliseconds() {
		return value;
	}

	/**
	 * The hours component of the current time
	 * 
	 * @return
	 */
	public int getHour() {
		return (int) ((value % MILLIS_IN_DAY) / MILLIS_IN_HOUR);
	}

	/**
	 * The minutes component of the current time
	 * 
	 * @return
	 */
	public int getMinute() {
		return (int) ((value % MILLIS_IN_HOUR) / MILLIS_IN_MIN);
	}

	/**
	 * The seconds component of the current time
	 * 
	 * @return
	 */
	public int getSecond() {
		return (int) ((value % MILLIS_IN_MIN) / MILLIS_IN_SEC);
	}

	/**
	 * The milliseconds component of the current time
	 * 
	 * @return
	 */
	public int getMilliseconds() {
		return (int) (value % MILLIS_IN_SEC);
	}



	private static String addDigits(int val, int nbDigits) {
		StringBuilder builder = new StringBuilder();
		builder.append(Integer.toString(val));
		while(builder.length()<nbDigits){
			builder.insert(0, "0");
		}
		return builder.toString();
	}

	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		long days = getTotalDays();
		if (days >= 2) {
			builder.append(Long.toString(days) + "d ");
		} else if (days >= 1) {
			builder.append(Long.toString(days) + "d ");
		}
		
		builder.append(addDigits(getHour(),2));
		
		builder.append(":");
		
		builder.append(addDigits(getMinute(),2));

		builder.append(":");

		builder.append(addDigits(getSecond(),2));

		int millis = getMilliseconds();
		if(millis!=0){
			builder.append(".");
			builder.append(addDigits(millis,3));			
		}
		
		return builder.toString();
	}
	
	public static void main(String []args){
		for(int i =0 ; i < 64 ; i++){
			long l  = i*MILLIS_IN_HOUR;
			ODLTime time = new ODLTime(l);
			System.out.println(time);
		}
	}

	@Override
	public int intValue() {
		return (int)value;
	}

	@Override
	public long longValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return value;
	}

	@Override
	public double doubleValue() {
		return value;
	}
	
	@Override
    public int hashCode() {
        return (int)(value ^ (value >>> 32));
    }
	
	@Override
    public boolean equals(Object obj) {
        if (obj instanceof ODLTime) {
            return value == ((ODLTime)obj).longValue();
        }
        return false;
    }

	@Override
	public int compareTo(ODLTime o) {
		return Long.compare(value, o.value);
	}
	
	public ODLTime plus(ODLTime t){
		return new ODLTime(value + t.value);
	}
	
	public ODLTime minus(ODLTime t){
		return new ODLTime(value - t.value);		
	}
	
	public static ODLTime max(ODLTime a, ODLTime b){
		return a.value > b.value ? a:b;
	}
}
