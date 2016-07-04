/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.awt.Color;
import java.time.LocalDate;

import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class Numbers {
	private Numbers() {
	}

	public static boolean isFloatingPoint(Object o) {
		Class<?> cls = o.getClass();
		return isFloatingPoint(cls);

	}

	public static boolean isFloatingPoint(Class<?> cls) {
		return cls == Double.class || cls == Float.class || cls == Double.TYPE || cls == Float.TYPE;
	}

	public static boolean isInteger(Class<?> cls) {
		return cls == Long.class || cls == Integer.class || cls == Short.class || cls == Byte.class || cls == Long.TYPE || cls == Integer.TYPE || cls == Short.TYPE || cls == Byte.TYPE;
	}

	public static Long toLong(Object o) {
		return toLong(o, false);
	}

	public static Long toLong(Object o, boolean rejectIfFloatingPoint) {
		if (o == null) {
			return null;
		}
		Class<?> cls = o.getClass();
		if (rejectIfFloatingPoint && Numbers.isFloatingPoint(cls)) {
			return null;
		}

		if (Numbers.isInteger(cls) || (rejectIfFloatingPoint == false && Number.class.isInstance(o))) {
			return ((Number) o).longValue();
		}

		if (ODLTime.class.isInstance(o)) {
			return ((ODLTime) o).longValue();
		}

		if(LocalDate.class.isInstance(o)){
			return ((LocalDate)o).toEpochDay();
		}
		
		if (Color.class.isInstance(o)) {
			long l = ((Color) o).getRGB();
			return l;
		}

		if (Boolean.class.isInstance(o)) {
			return ((Boolean) o) ? 1L : 0L;
		}

		String s = o.toString();
		String lc = s.toLowerCase();
		if (lc.equals("true") || lc.equals("yes")|| lc.equals("oui")|| lc.equals("vrai")) {
			return 1L;
		}
		if (lc.equals("false") || lc.equals("no")|| lc.equals("faux")|| lc.equals("non")) {
			return 0L;
		}

		try {
			return toLong(s, rejectIfFloatingPoint);
		} catch (Throwable e) {
		}
		return null;
	}

	/**
	 * Converts to a long is the input object can be transformed into one and isn't a floating point number.
	 * 
	 * @param o
	 * @return
	 */
	public static Long toLongIfNotFloatingPoint(Object o) {
		return toLong(o, true);
	}

	public static Double toDouble(Object o) {
		if (o == null) {
			return null;
		}
		Class<?> cls = o.getClass();
		if (isFloatingPoint(cls) || isInteger(cls)) {
			return ((Number) o).doubleValue();
		}

		if (Color.class.isInstance(o)) {
			int i = ((Color) o).getRGB();
			return (double) i;
		}

		if (Boolean.class.isInstance(o)) {
			return ((Boolean) o) ? 1.0 : 0.0;
		}

		if (Number.class.isInstance(o)) {
			return ((Number) o).doubleValue();
		}

		if(LocalDate.class.isInstance(o)){
			return (double) ((LocalDate)o).toEpochDay();
		}
		
		String s = o.toString().toLowerCase().trim();
		if (s.equals("true") || s.equals("yes") || s.equals("vrai")) {
			return 1.0;
		}
		if (s.equals("false") || s.equals("no") || s.equals("faux")) {
			return 0.0;
		}

		if (Strings.isNumber(s)) {
			try {
				return Double.parseDouble(s);
			} catch (Throwable e) {
			}
		}
		return null;
	}

	public static Long toLong(String s) {
		return toLong(s, false);
	}

	/**
	 * Parse double or return null if non-parsable. Catches exception and tries to avoid throwing them (as they make debugging hard). If a number
	 * contains a decimal point it is parsed as a double then rounded to the nearest integer number.
	 * 
	 * @param s
	 * @return
	 */
	public static Long toLong(String s, boolean rejectIfFloatingPoint) {
		if (s == null) {
			return null;
		}

		// remove all leading / trailing whitespaces
		s = s.trim();

		if (Strings.isNumber(s)) {
			if (s.contains(".")) {
				if (rejectIfFloatingPoint) {
					return null;
				}
				try {
					Double val = Numbers.toDouble(s);
					if (val != null) {
						return (Long) Math.round(val);
					}
				} catch (Exception e) {
				}
			} else {
				try {
					return Long.parseLong(s);
				} catch (Exception e) {

				}
			}
		}

		return null;
	}

	public static void main(String[] args) {
		System.out.println(toLong(" 3   "));
	}
	
	public static double clamp(double number, double min, double max){
		if(number<min){
			number = min;
		}
		if(number>max){
			number = max;
		}
		return number;
	}
}
