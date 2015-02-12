/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.opendoorlogistics.core.geometry.ODLShapefileLinkGeom;
import com.opendoorlogistics.core.geometry.ShapefileLink;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.NullComparer;
import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

/**
 * Contains all the logic to process different values supported by column type, e.g. conversion, comparison etc...
 * 
 * @author Phil
 *
 */
public class ColumnValueProcessor {
	private ColumnValueProcessor() {
	}

	private static final WKTReader wktReader = new WKTReader();

	public static Class<?> getJavaClass(ODLColumnType colType) {
		switch (colType) {
		case STRING:
			return String.class;

		case LONG:
			return Long.class;

		case DOUBLE:
			return Double.class;

		case COLOUR:
			return Color.class;

		case IMAGE:
			return BufferedImage.class;

		case GEOM:
			return ODLGeomImpl.class;

		case TIME:
			return ODLTime.class;

		default:
			throw new RuntimeException();
		}

	}

	private static Pattern daysHourPattern = Pattern.compile("\\s*(\\d+)\\s*d[a-z]*\\s*(\\d+)\\s*", Pattern.CASE_INSENSITIVE);

	private static ODLTime parseTime(String time) {
		// Possibilities
		// 1d11:11:11.111
		// 1d11:11:11
		// 11:11:11.111
		// 11:11:11
		// 11:11

		if (time == null) {
			return null;
		}

		time = time.toLowerCase().trim();
		String[] split = time.split(":");
		if(split.length < 2){
			// must have at least one :
			return null;
		}
		
		// the first will either be days and hours or just days
		Long days = 0L;
		Long hours = 0L;
		Matcher daysHours = daysHourPattern.matcher(split[0]);
		if (daysHours.matches()) {
			days = Numbers.toLong(daysHours.group(1));
			hours = Numbers.toLong(daysHours.group(2));
			if (days == null || hours == null) {
				return null;
			}
		} else {
			hours = Numbers.toLong(split[0]);
			if (hours == null) {
				return null;
			}
		}

		// get minutes (optional)
		Long minutes = 0L;
		if (split.length > 1) {
			minutes = Numbers.toLong(split[1]);
			if (minutes == null) {
				return null;
			}
		}

		// get seconds and milliseconds (optional)
		Long seconds = 0L;
		Long millis = 0L;
		if (split.length > 2) {
			String[] split2 = split[2].split("\\.");
			seconds = Numbers.toLong(split2[0]);
			if (split2.length == 1) {
			} else if (split2.length == 2) {
				millis = Numbers.toLong(split2[1]);
				if (millis == null) {
					return null;
				}
			} else {
				return null;
			}

			if (seconds == null) {
				return null;
			}
		}

		long value = 0;
		if (days != null) {
			value += ODLTime.MILLIS_IN_DAY * days;
		}

		if (hours != null) {
			value += ODLTime.MILLIS_IN_HOUR * hours;
		}

		if (minutes != null) {
			if (minutes < 0 || minutes > 59) {
				return null;
			}
			value += ODLTime.MILLIS_IN_MIN * minutes;
		}

		if (seconds != null) {
			if (seconds < 0 || seconds > 59) {
				return null;
			}
			value += ODLTime.MILLIS_IN_SEC * seconds;
		}

		if (millis != null) {
			if (millis < 0 || millis > 999) {
				return null;
			}
			value += millis;
		}

		return new ODLTime(value);
	}

	public static Object convertToMe(ODLColumnType convertToMe, Object other) {
		
		if (other == null) {
			// null always converts to null
			return null;
		}

		if (other.getClass() == ColumnValueProcessor.getJavaClass(convertToMe)) {
			// check for same class
			return other;
		}

		// treat boolean as integer
		if (other.getClass() == Boolean.class || other.getClass() == Boolean.TYPE) {
			other = (Boolean) other ? 1 : 0;
		}

		if (ODLGeomImpl.class.isInstance(other)) {
			return convertToMe(convertToMe, other, ODLColumnType.GEOM);
		}

		if (convertToMe == ODLColumnType.GEOM) {
			if (Geometry.class.isInstance(other)) {
				return new ODLLoadedGeometry((Geometry) other);
			} else if (ShapefileLink.class.isInstance(other)) {
				return new ODLShapefileLinkGeom((ShapefileLink) other);
			}
		}

		// do conversion .. find most suitable supported type to avoid string parsing
		Class<?> otherCls = other.getClass();

		if (ODLTime.class.isInstance(other)) {
			return convertToMe(convertToMe, other, ODLColumnType.TIME);
		}

		if (Numbers.isFloatingPoint(otherCls)) {
			return convertToMe(convertToMe, ((Number) other).doubleValue(), ODLColumnType.DOUBLE);
		}

		if (Numbers.isInteger(otherCls)) {
			return convertToMe(convertToMe, ((Number) other).longValue(), ODLColumnType.LONG);
		}
		
		if(Color.class.isAssignableFrom(otherCls)){
			return convertToMe(convertToMe, other, ODLColumnType.COLOUR);			
		}

		// just try parsing, will throw exception if fails
		return convertToMe(convertToMe, other.toString(), ODLColumnType.STRING);
	}

	public static Object convertToMe(ODLColumnType convertToMe, Object other, ODLColumnType otherType) {
		return convertToMe(convertToMe, other, otherType, false);
	}
	
	/**
	 * Does the string start with 00, 01, 02, ... , 09?
	 * Useful for detecting French zip codes which are 5 digit, starting with 0 sometimes
	 * and should not be converted to numeric. 
	 * @param s
	 * @return
	 */
	private static boolean startsWith0AndOtherDigit(String s){
		String stdVal = Strings.std(s);	
		if(stdVal.length()>=2 && stdVal.charAt(0)=='0' && Character.isDigit(stdVal.charAt(1))){
			return true;
		}
		return false;
	}

	public static Object convertToMe(ODLColumnType convertToMe, Object other, ODLColumnType otherType, boolean onlyConvertStringIfFormatMatches) {
		if (otherType == convertToMe) {
			return other;
		}

		if (other == null) {
			// null always converts to null
			return null;
		}

		// NOTE - the automatic type identification when loading an excel without a schema
		// uses this convertToMe, so we shouldn't return default values for unparsable strings
		switch (convertToMe) {
		case GEOM:
			// conversion to geom only supported from wkt string or shapefilelink
			try {
				ShapefileLink link = ShapefileLink.parse(other.toString());
				if (link != null) {
					return new ODLShapefileLinkGeom(link);
				}
				Geometry geometry = wktReader.read(other.toString());
				return new ODLLoadedGeometry(geometry);
			} catch (Throwable e) {
				return null;
			}

		case COLOUR:
			switch (otherType) {
			case DOUBLE:
			case LONG:
			case TIME:
				int intVal = ((Number) other).intValue();
				return new Color(intVal);

			case STRING:
				String sOther = ((String) other).trim();
				Color ret = Colours.getColourByName(sOther);
				if (ret != null) {
					return ret;
				}
				if (sOther.startsWith("#") == false) {
					if(onlyConvertStringIfFormatMatches){
						// must start with # in this case...
						return null;
					}
					sOther = "#" + sOther;
				}
				try {
					return Color.decode(sOther);
				} catch (Throwable e) {
					return null;
				}

			default:
				return null;
			}

		case DOUBLE:
			switch (otherType) {
			case COLOUR:
				return (double) ((Color) other).getRGB();

			case LONG:
			case TIME:
				return ((Number) other).doubleValue();

			case STRING:
				String sOther = ((String) other).trim();

				if(onlyConvertStringIfFormatMatches && startsWith0AndOtherDigit((String)other)){
					return null;
				}
				
				try {
					
					// Test if we have a . in the number and if so, use java's parsedouble which always uses .
					double number=0;
					if(sOther.indexOf(".")!=-1){
						number = Double.parseDouble(sOther);
					}else{
						// If not, use the number format which takes account of localisation and will use commas in the correct country.	
						NumberFormat nf = NumberFormat.getInstance();
						number = nf.parse((String) sOther).doubleValue();
						return number;	
					}
					
					return number;
				} catch (Throwable e) {
					return null;
				}

			default:
				return null;
			}

		case LONG:
			switch (otherType) {
			case COLOUR:
				return (long) ((Color) other).getRGB();

			case DOUBLE:
			case TIME:
				return ((Number) other).longValue();

			case STRING:
				if(onlyConvertStringIfFormatMatches && startsWith0AndOtherDigit((String)other)){
					return null;
				}
				return Numbers.toLong((String) other, onlyConvertStringIfFormatMatches);

			default:
				return null;
			}

		case STRING:
			switch (otherType) {
			case GEOM:
				return ((ODLGeomImpl) other).toText();

			case COLOUR:
				return Colours.toHexString((Color) other);

			case IMAGE:
				return ImageUtils.imageToBase64String((RenderedImage) other, "png");
			default:
				return other.toString();
			}

		case IMAGE:
			switch (otherType) {
			case STRING:
				return ImageUtils.base64StringToImage((String) other);
			default:
				return null;
			}

		case TIME:
			switch (otherType) {
			case LONG:
				return new ODLTime(((Number) other).longValue());

			case DOUBLE:
				return new ODLTime((long) Math.round(((Double) other).doubleValue()));

			case STRING:
				return parseTime(other.toString());

			default:
				return null;
			}

		default:
			return null;
		}
	}

	public static boolean isNumeric(ODLColumnType type) {
		switch (type) {
		case DOUBLE:
		case LONG:
			return true;

		default:
			return false;

		}
	}

	public static boolean isBatchKeyCompatible(ODLColumnType type) {
		switch (type) {
		case COLOUR:
		case DOUBLE:
		case LONG:
		case STRING:
		case TIME:
			return true;

		default:
			return false;

		}
	}

	/**
	 * Compare values of the same column type
	 * 
	 * @param type
	 * @param valueA
	 * @param valueB
	 * @return
	 */
	public static int compareSameType(ODLColumnType type, Object val1, Object val2) {
		int diff = NullComparer.compare(val1, val2);

		switch (type) {
		case STRING:
			diff = ((String) val1).compareTo((String) val2);
			break;

		case LONG:
			diff = ((Long) val1).compareTo((Long) val2);
			break;

		case DOUBLE:
			diff = ((Double) val1).compareTo((Double) val2);
			break;

		case COLOUR:
			diff = Colours.compare((Color) val1, (Color) val2);
			break;

		case TIME:
			diff = ((ODLTime) val1).compareTo((ODLTime) val2);
			break;

		default:
			throw new RuntimeException();
		}
		return diff;
	}

	public static boolean isEqualSameType(ODLColumnType type, Object val1, Object val2) {
		if(val1 == val2){
			return true;
		}
		
		if (NullComparer.compare(val1, val2) != 0) {
			return false;
		}

		if (val1 != null) {
			switch (type) {
			case LONG:
			case DOUBLE:
			case STRING:
			case COLOUR:
			case TIME:
				if (val1.equals(val2) == false) {
					return false;
				}
				break;
			default:
				// convert to string and compare
				if (val1.toString().equals(val2.toString()) == false) {
					return false;
				}
				break;
			}
		}

		return true;
	}

	public static boolean isEqual(Object a, Object b) {
		boolean equals = false;
		if (a == null && b == null) {
			// both null are equal
			equals = true;

		} else if ((a == null && b != null) || (a != null && b == null)) {
			// one null and one not null are not equal
			equals = false;

		} else if (a.getClass() == b.getClass()) {
			// do same class-internal comparison
			if (a.getClass() == String.class) {
				equals = Strings.equalsStd(a.toString(), b.toString());
			} else {
				equals = a.equals(b);
			}

		} else if (Number.class.isInstance(a) && Number.class.isInstance(b)) {
			// try treating both as double if they are both numbers
			Double da = Numbers.toDouble(a);
			Double db = Numbers.toDouble(b);
			if (da == null || db == null) {
				return false;
			}
			equals = da.equals(db);

		} else {
			// test the string-representation values
			equals = Strings.equalsStd(a.toString(), b.toString());
		}

		return equals;
	}

	private static boolean isEmpty(Object o) {
		return o == null || o.toString().length() == 0;
	}

	public static int compareValues(Object val1, Object val2, boolean isNumeric) {
		int diff;
		boolean empty1 = isEmpty(val1);
		boolean empty2 = isEmpty(val2);
		diff = Boolean.compare(empty1, empty2);

		// if both empty then return 0 
		if(diff==0 && empty1){
			return 0;
		}
		
		if (diff == 0 && val1 != null) {
			if (isNumeric) {
				Double d1 = (Double) ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, val1);
				Double d2 = (Double) ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, val2);
				diff = d1.compareTo(d2);
			} else if (ODLTime.class.isInstance(val1) && ODLTime.class.isInstance(val2)) {
				diff = ((ODLTime) val1).compareTo((ODLTime) val2);
			} else {
				// ignore case in string compare
				String s1 = val1.toString().toLowerCase();
				String s2 = val2.toString().toLowerCase();
				diff = s1.compareTo(s2);
			}
		}
		return diff;
	}

	public static void main(String[] args) {
		for (String s : new String[] { "07:52:200", "07:87:12", "31:01:02", "09:01:02", "1", "2days3:42", "2 day 3:42", "-1 day 12:05", "12:78:12", "12:23:23.121", "12:23:23.001", "12:23:23.021", "12days 12:23:23.021" }) {
			ODLTime parsed = parseTime(s);
			String stringed = null;
			ODLTime reparsed = null;
			if (parsed != null) {
				stringed = parsed.toString();
				reparsed = parseTime(stringed);
			}

			System.out.println(s + "  [" + parsed + "]  [" + stringed + "]  [" + reparsed + "]");
		}
	}
}
