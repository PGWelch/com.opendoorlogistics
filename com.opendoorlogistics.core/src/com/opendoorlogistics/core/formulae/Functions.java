/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.formulae;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.core.AppProperties;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.opendoorlogistics.core.geometry.operations.LinestringFraction;
import com.opendoorlogistics.core.gis.map.Symbols.SymbolType;
import com.opendoorlogistics.core.gis.map.background.BackgroundMapConfig;
import com.opendoorlogistics.core.gis.map.background.BackgroundTileFactorySingleton;
import com.opendoorlogistics.core.gis.postcodes.UKPostcodes;
import com.opendoorlogistics.core.gis.postcodes.UKPostcodes.UKPostcodeLevel;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class Functions {
	public static final Object EXECUTION_ERROR = new Object() {
		@Override
		public String toString() {
			return "Formula execution error";
		}
	};

	private static abstract class Fm2ParamBase extends FunctionImpl {

		public Fm2ParamBase(Function a, Function b) {
			super(a, b);
		}

		@Override
		public Function deepCopy() {
			throw new UnsupportedOperationException();
		}

		protected abstract Object execute(Object a, Object b);

		@Override
		public Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			Object b = child(1).execute(parameters);
			if (a == EXECUTION_ERROR || b == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}
			return execute(a, b);
		}
	}

	public static class FmDecimalHours extends FunctionImpl{
		public FmDecimalHours(Function time){
			super(time);
		}
		
		@Override
		public Object execute(FunctionParameters parameters) {
			Object v = child(0).execute(parameters);
			if(v==Functions.EXECUTION_ERROR){
				return Functions.EXECUTION_ERROR;
			}
			
			v = ColumnValueProcessor.convertToMe(ODLColumnType.LONG, v);
			if (v==null){
				return null;
			}			
			
			Long l = (Long)v;
			return ((double)l) / ODLTime.MILLIS_IN_HOUR;
		}

		@Override
		public Function deepCopy() {
			return new FmDecimalHours(child(0).deepCopy());
		}
		
	}
	
	public static class FmTime extends FunctionImpl {

		public FmTime(Function... components) {
			super(components);
			if (components.length > 5) {
				throw new RuntimeException("Too many arguments for the function time: " + components.length);
			}
		}

		/**
		 * Non-var args construction for the function definition library.
		 */
		public FmTime() {
			super();
		}

		/**
		 * Non-var args construction for the function definition library.
		 * 
		 * @param p1
		 */
		public FmTime(Function p1) {
			super(p1);
		}

		/**
		 * Non-var args construction for the function definition library.
		 * 
		 * @param p1
		 * @param p2
		 */
		public FmTime(Function p1, Function p2) {
			super(p1, p2);
		}

		/**
		 * Define non-var args construction for the function definition library.
		 * 
		 * @param p1
		 * @param p2
		 * @param p3
		 */
		public FmTime(Function p1, Function p2, Function p3) {
			super(p1, p2, p3);
		}

		/**
		 * Define non-var args construction for the function definition library.
		 * 
		 * @param p1
		 * @param p2
		 * @param p3
		 * @param p4
		 */
		public FmTime(Function p1, Function p2, Function p3, Function p4) {
			super(p1, p2, p3, p4);
		}

		/**
		 * Define non-var args construction for the function definition library.
		 * 
		 * @param p1
		 * @param p2
		 * @param p3
		 * @param p4
		 * @param p5
		 */
		public FmTime(Function p1, Function p2, Function p3, Function p4, Function p5) {
			super(p1, p2, p3, p4, p5);
		}

		// private FmTime(Function... children) {
		// super(children);
		// // TODO Auto-generated constructor stub
		// }

		@Override
		public Object execute(FunctionParameters parameters) {
			if (children == null || children.length == 0) {
				return new ODLTime();
			}

			Object[] res = executeChildFormulae(parameters, true);
			Long[] longs = new Long[res.length];
			for (int i = 0; i < res.length; i++) {
				longs[i] = Numbers.toLong(res[i]);
				if (longs[i] == null) {
					return EXECUTION_ERROR;
				}
			}

			try {
				switch (res.length) {

				case 1:
					return new ODLTime(longs[0]);

				case 2:
					return new ODLTime(longs[0], longs[1]);

				case 3:
					return new ODLTime(longs[0], longs[1], longs[2]);

				case 4:
					return new ODLTime(longs[0], longs[1], longs[2], longs[3]);

				case 5:
					return new ODLTime(longs[0], longs[1], longs[2], longs[3], longs[4]);

				default:
					break;
				}

			} catch (Exception e) {
				return EXECUTION_ERROR;
			}

			return EXECUTION_ERROR;
		}

		@Override
		public Function deepCopy() {
			return new FmTime(deepCopy(children));
		}

		@Override
		public String toString() {
			return super.toString("time");
		}
	}

	private static abstract class Fm1ParamBase extends FunctionImpl {

		public Fm1ParamBase(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			throw new UnsupportedOperationException();
		}

		protected abstract double execute(double d);

		protected abstract long execute(long l);

		@Override
		public Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			if (a == EXECUTION_ERROR || a == null) {
				return EXECUTION_ERROR;
			}

			// try treating as long first to preserve integer numbers where possible
			Long l = Numbers.toLongIfNotFloatingPoint(a);
			if (l != null) {
				return execute(l);
			}

			// then try double
			Double d = Numbers.toDouble(a);
			if (d != null) {
				return execute(d);
			}

			return EXECUTION_ERROR;

		}

	}

	public static final class FmAbs extends Fm1ParamBase {

		public FmAbs(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmAbs(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "abs(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double d) {
			return Math.abs(d);
		}

		@Override
		protected long execute(long l) {
			return Math.abs(l);
		}
	}

	public static abstract class Fm1DoubleParam extends Fm1ParamBase {

		public Fm1DoubleParam(Function a) {
			super(a);
		}

		@Override
		protected long execute(long l) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			if (a == EXECUTION_ERROR || a == null) {
				return EXECUTION_ERROR;
			}

			// always treat as double
			Double d = Numbers.toDouble(a);
			if (d != null) {
				return execute(d);
			}

			return EXECUTION_ERROR;

		}
	}

	public static final class FmLn extends Fm1DoubleParam {

		public FmLn(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmLn(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "ln(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double d) {
			return Math.log(d);
		}
	}

	public static final class FmDecimalFormat extends FunctionImpl{
		private final DecimalFormat format;

		public FmDecimalFormat(Function pattern,Function number){
			super(pattern,number);
			format = new DecimalFormat(pattern.toString());
		}
		
		@Override
		public Object execute(FunctionParameters parameters) {
			Object child = child(1).execute(parameters);
			if(child==EXECUTION_ERROR){
				return EXECUTION_ERROR;
			}
			Double d = Numbers.toDouble(child);
			if(d!=null){
				return format.format(d);
			}
			return null;
		}

		@Override
		public Function deepCopy() {
			return new FmDecimalFormat(child(0).deepCopy(), child(1).deepCopy());
		}
		
	}
	
	public static final class FmLog10 extends Fm1DoubleParam {

		public FmLog10(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmLog10(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "log10(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double d) {
			return Math.log10(d);
		}
	}

	public static final class FmSin extends Fm1DoubleParam {

		public FmSin(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmSin(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "sin(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double d) {
			return Math.sin(d);
		}
	}

	public static final class FmAsin extends Fm1DoubleParam {

		public FmAsin(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmAsin(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "asin(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double d) {
			return Math.asin(d);
		}
	}

	public static final class FmCos extends Fm1DoubleParam {

		public FmCos(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmCos(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "cos(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double d) {
			return Math.cos(d);
		}
	}

	public static final class FmSqrt extends Fm1DoubleParam {

		public FmSqrt(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmSqrt(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return super.toString("sqrt");
		}

		@Override
		protected double execute(double d) {
			return Math.sqrt(d);
		}
	}

	public static final class FmTemperatureColours extends FunctionImpl {
		public FmTemperatureColours(Function f) {
			super(f);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Double val = Numbers.toDouble(child(0).execute(parameters));
			if (val == null) {
				return EXECUTION_ERROR;
			}
			return Colours.temperature(val);
		}

		@Override
		public Function deepCopy() {
			return new FmTemperatureColours(child(0).deepCopy());
		}

	}

	public static final class FmLerp extends FunctionImpl {

		public FmLerp(Function from, Function to, Function fraction) {
			super(from, to, fraction);
		}

		@Override
		public Function deepCopy() {
			return new FmLerp(child(0).deepCopy(), child(1).deepCopy(), child(2).deepCopy());
		}

		@Override
		public String toString() {
			return super.toString("lerp");
		}

		private double lerp(double from, double to, double fraction) {
			if (fraction <= 0) {
				return from;
			}

			if (fraction >= 1) {
				return to;
			}

			return from * (1.0 - fraction) + to * fraction;
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object[] children = executeChildFormulae(parameters, true);
			if (children == null) {
				return EXECUTION_ERROR;
			}

			Double fraction = Numbers.toDouble(children[2]);
			if (fraction == null) {
				return EXECUTION_ERROR;
			}

			// check for colour
			if (Color.class.isInstance(children[0]) || Color.class.isInstance(children[1])) {
				Color c1 =(Color) ColumnValueProcessor.convertToMe(ODLColumnType.COLOUR, children[0]);
				Color c2 =(Color) ColumnValueProcessor.convertToMe(ODLColumnType.COLOUR, children[1]);
				if(c1 == null || c2 == null){
					return null;
				}
				return Colours.lerp(c1, c2, fraction);
			}

			Double low = Numbers.toDouble(children[0]);
			Double high = Numbers.toDouble(children[1]);
			if (low == null || high == null) {
				return EXECUTION_ERROR;
			}

			return lerp(low, high, fraction);
		}
	}

	public static final class FmAcos extends Fm1DoubleParam {

		public FmAcos(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmAcos(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "acos(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double d) {
			return Math.acos(d);
		}

	}

	public static final class FmTan extends Fm1DoubleParam {

		public FmTan(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmTan(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "tan(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double d) {
			return Math.tan(d);
		}
	}

	public static final class FmAtan extends Fm1DoubleParam {

		public FmAtan(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmAtan(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "atan(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double d) {
			return Math.atan(d);
		}
	}

	public static final class FmCeil extends Fm1ParamBase {

		public FmCeil(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmCeil(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "ceil(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double d) {
			return Math.ceil(d);
		}

		@Override
		protected long execute(long l) {
			return l;
		}

	}

	public static final class FmFloor extends Fm1ParamBase {

		public FmFloor(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmFloor(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "floor(" + child(0).toString() + ")";
		}

		@Override
		protected double execute(double a) {
			return Math.floor(a);
		}

		@Override
		protected long execute(long l) {
			return l;
		}

	}

	public static final class FmRound extends FunctionImpl {

		public FmRound(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmRound(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "round(" + child(0).toString() + ")";
		}

		@Override
		public boolean hasBrackets() {
			return true;
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			if (a == EXECUTION_ERROR ) {
				return EXECUTION_ERROR;
			}

			Double d = Numbers.toDouble(a);
			if (d != null) {
				return (long) Math.round(d);
			}

			return null;

		}
	}

	public static final class FmAnd extends FunctionImpl {

		public FmAnd(Function a, Function b) {
			super(a, b);
		}

		@Override
		public Function deepCopy() {
			return new FmAnd(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			int nbChildren = nbChildren();
			for (int j = 0; j < nbChildren; j++) {
				Object val = child(j).execute(parameters);
				if (val == EXECUTION_ERROR) {
					return EXECUTION_ERROR;
				}
				Double d = Numbers.toDouble(val);
				if (d == null) {
					// treat null as false
					d= 0.0;
				}

				if (d != 1) {
					// booleans should treated as long..
					return 0L;
				}
			}
			return 1L;
		}

		@Override
		public String toString() {
			return toStringWithChildOp("&&");
		}
	}

	public static final class FmIndexOf extends FunctionImpl {
		public FmIndexOf(Function findText, Function findWithin) {
			super(findText, findWithin);
		}

		@Override
		public Function deepCopy() {
			return new FmIndexOf(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public String toString() {
			return super.toString("indexof");
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			return execute(child(0).execute(parameters), child(1).execute(parameters));
		}

		static Object execute(Object a, Object b) {
			if (a == null || a == EXECUTION_ERROR || b == null || b == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}

			String sa = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, a);
			String sb = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, b);
			if (a == null || b == null) {
				return EXECUTION_ERROR;
			}

			sa = sa.toLowerCase();
			sb = sb.toLowerCase();
			return sb.indexOf(sa);
		}
	}

	public static final class FmContains extends FunctionImpl {
		public FmContains(Function findText, Function findWithin) {
			super(findText, findWithin);
		}

		@Override
		public Function deepCopy() {
			return new FmContains(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public String toString() {
			return super.toString("contains");
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object o = FmIndexOf.execute(child(0).execute(parameters), child(1).execute(parameters));
			if (o == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}
			return ((Integer) o) != -1 ? 1 : 0;
		}
	}

	/**
	 * Parent class for formula which return a single string
	 * 
	 * @author Phil
	 * 
	 */
	public abstract static class FmSingleString extends FunctionImpl {
		protected FmSingleString(Function child) {
			super(child);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			if (a == null || a == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}

			String sa = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, a);
			if (sa == null) {
				return null;
			}
			return execute(sa);
		}

		protected abstract Object execute(String s);
	}

	public static final class FmUpper extends FmSingleString {

		public FmUpper(Function child) {
			super(child);
		}

		@Override
		public Function deepCopy() {
			return new FmUpper(child(0).deepCopy());
		}

		@Override
		protected Object execute(String s) {
			return s.toUpperCase();
		}

		@Override
		public String toString() {
			return super.toString("upper");
		}
	}

	public static final class FmLower extends FmSingleString {

		public FmLower(Function child) {
			super(child);
		}

		@Override
		public Function deepCopy() {
			return new FmLower(child(0).deepCopy());
		}

		@Override
		protected Object execute(String s) {
			return s.toLowerCase();
		}

		@Override
		public String toString() {
			return super.toString("lower");
		}
	}

	public static final class FmCreateUUID extends FunctionImpl{
		public FmCreateUUID(Function text) {
			super(text);
		}
		
		@Override
		public Object execute(FunctionParameters parameters) {
			Object val = child(0).execute(parameters);
			if(val == Functions.EXECUTION_ERROR || val==null){
				return Functions.EXECUTION_ERROR;
			}
			
			return UUID.nameUUIDFromBytes(ColumnValueProcessor.convertToMe(ODLColumnType.STRING , val).toString().getBytes());
		}

		@Override
		public Function deepCopy() {
			return new FmCreateUUID(child(0).deepCopy());
		}
		
	}
	
	public static final class FmLeft extends FunctionImpl {
		public FmLeft(Function text, Function number_of_characters) {
			super(text, number_of_characters);
		}

		@Override
		public Function deepCopy() {
			return new FmLeft(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public String toString() {
			return super.toString("left");
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			Object b = child(1).execute(parameters);
			if (a == null || a == EXECUTION_ERROR || b == null || b == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}

			String sa = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, a);

			Double l = Numbers.toDouble(b);
			if (l == null) {
				return EXECUTION_ERROR;
			}
			return sa.substring(0, Math.min(l.intValue(), sa.length()));
		}
	}

	public static final class FmReplace extends FunctionImpl {
		public FmReplace(Function findWithin, Function oldText, Function newText) {
			super(findWithin, oldText, newText);
		}

		@Override
		public Function deepCopy() {
			return new FmReplace(child(0).deepCopy(), child(1).deepCopy(), child(2).deepCopy());
		}

		@Override
		public String toString() {
			return super.toString("replace");
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			Object b = child(1).execute(parameters);
			Object c = child(2).execute(parameters);
			if (a == null || a == EXECUTION_ERROR || b == null || b == EXECUTION_ERROR || c == null || c == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}

			String sa = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, a);
			String sb = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, b);
			String sc = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, c);
			if (sa == null || sb == null || sc == null) {
				return EXECUTION_ERROR;
			}

			// always case-insensitive and be sure to process the string so it can be matched literally
			sb = Pattern.quote(sb);
			return Pattern.compile(sb, Pattern.CASE_INSENSITIVE).matcher(sa).replaceAll(sc);
		}
	}

	public static final class FmRound2Second extends FunctionImpl {
		public FmRound2Second(Function time) {
			super(time);
		}

		@Override
		public Function deepCopy() {
			return new FmRound2Second(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return super.toString("roundmilliseconds");
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			if (a == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}

			Long l = Numbers.toLong(a);
			if(l==null){
				return EXECUTION_ERROR;
			}
			
			ODLTime time = new ODLTime(l);
			if(time.getMilliseconds()>=500){
				time = new ODLTime(time.getTotalMilliseconds() + 1000 - time.getMilliseconds());
			}else{
				time = new ODLTime(time.getTotalMilliseconds()  - time.getMilliseconds());
			}
			return time;
		}
	}

	
	public static final class FmLen extends FunctionImpl {
		public FmLen(Function text) {
			super(text);
		}

		@Override
		public Function deepCopy() {
			return new FmLen(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return super.toString("len");
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			if (a == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}

			String sa = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, a);

			if (sa == null) {
				return new Long(0);
			} else {
				return new Long(sa.length());
			}
		}
	}

	public static final class FmStringFormat extends FunctionImpl{

		
		public FmStringFormat(Function format, Function ...args){
			super(FunctionUtils.toSingleArray(format, args));
		}
		
		private FmStringFormat(Function ...fncs){
			super(fncs);
		}
		
		@Override
		public Object execute(FunctionParameters parameters) {
			Object [] chdl = executeChildFormulae(parameters, false);
			if(chdl==null || chdl[0] == null){
				return Functions.EXECUTION_ERROR;
			}
			
			String s = ColumnValueProcessor.convertToMe(ODLColumnType.STRING, chdl[0]).toString();
			Object [] args = Arrays.copyOfRange(chdl, 1, chdl.length);
			String ret = String.format(s, args);
			return ret;
		}

		@Override
		public Function deepCopy() {
			return new FmStringFormat(deepCopy(children));
		}
		
	}
	
	/**
	 * Base class for all comparisons, e.g. <, <=, >, >=
	 * 
	 * @author Phil
	 * 
	 */
	public static abstract class FmRelativeComparisonBase extends FunctionImpl {
		public FmRelativeComparisonBase(Function a, Function b) {
			super(a, b);
		}

		protected abstract Object compare(double a, double b);

		@Override
		public final Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			Object b = child(1).execute(parameters);
			if (a == EXECUTION_ERROR || b == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}

			Double da = Numbers.toDouble(a);
			Double db = Numbers.toDouble(b);
			if (da == null || db == null) {
				// comparing nulls always gives false
				return false;
			}

			return compare(da, db);
		}

		public abstract String operator();

		@Override
		public String toString() {
			return toStringWithChildOp(operator());
		}
	}

	public static final class FmConst extends FunctionImpl {
		private final Object val;

		public static final FmConst NULL = new FmConst((Object)null);
		
		public FmConst(Object val) {
			super();
			this.val = val;
		}

		/**
		 * Create a constant out of the input function's toString value; used to qualify when a formula containing a string i.e. "name" should refer
		 * to a table's field or a string constant.
		 * 
		 * @param function
		 */
		public FmConst(Function function) {
			this(function.toString());
		}

		@Override
		public Function deepCopy() {
			return new FmConst(val);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			return val;
		}

		@Override
		public String toString() {
			if (val != null) {
				if (Color.class.isInstance(val)) {
					Color col = (Color) val;
					return new FmColour(new FmConst(col.getRed() / 255.0), new FmConst(col.getGreen() / 255.0), new FmConst(col.getBlue() / 255.0)).toString();
				} else {
					return val.toString();
				}
			}
			return "null";
		}

		public Object value() {
			return val;
		}

	}

	public static class FmRand extends FunctionImpl {
		protected final Random random = new Random();

		public FmRand() {
			super();
		}

		@Override
		public Function deepCopy() {
			return new FmRand();
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			return random.nextDouble();
		}

		@Override
		public String toString() {
			return "rand()";
		}
	}
	
	public static final class FmRandData extends FmRand{
		private final RandDataType rdt;
		
		public enum RandDataType{
			PERSON_NAME,
			COMPANY_NAME,
			STREET_NAME
		}
		
		public FmRandData(RandDataType rdt){
			this.rdt = rdt;
		}
		
		@Override
		public Object execute(FunctionParameters parameters) {
			switch(rdt){
			case PERSON_NAME:
				return ExampleData.getRandomPersonName(random);
				
			case COMPANY_NAME:
				return ExampleData.getRandomBusinessName(random);
				
			case STREET_NAME:
				return ExampleData.getRandomStreet(random);
				
			default:
				return null;
			}
		}
		
		@Override
		public Function deepCopy() {
			return new FmRandData(rdt);
		}
	}

	public static final class FmRandomSymbol extends FunctionImpl {
		private final Random random = new Random();

		public FmRandomSymbol() {
			super();
		}

		@Override
		public Function deepCopy() {
			return new FmRandomSymbol();
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			int index = random.nextInt(SymbolType.values().length);
			return SymbolType.values()[index].getKeyword();
		}

		@Override
		public String toString() {
			return "randomsymbol()";
		}
	}

	public static final class FmDivide extends Fm2ParamBase {

		public FmDivide(Function a, Function b) {
			super(a, b);
		}

		@Override
		public Function deepCopy() {
			return new FmDivide(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		protected Object execute(Object a, Object b) {
			Double da = Numbers.toDouble(a);
			Double db = Numbers.toDouble(b);
			if (da == null || db == null) {
				return EXECUTION_ERROR;
			}
			return da / db;
		}

		@Override
		public String toString() {
			return toStringWithChildOp("/");
		}
	}

	public abstract static class FmImageFilter extends FunctionImpl {
		public FmImageFilter(Function... parameters) {
			super(parameters);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object[] executed = executeChildFormulae(parameters, true);
			if (executed == null) {
				return Functions.EXECUTION_ERROR;
			}

			if (BufferedImage.class.isInstance(executed[0]) == false) {
				return Functions.EXECUTION_ERROR;
			}

			BufferedImage img = (BufferedImage) executed[0];
			ImageFilter filter = createFilter(executed);
			if (filter == null) {
				return Functions.EXECUTION_ERROR;
			}

			ImageProducer ip = new FilteredImageSource(img.getSource(), filter);
			Image ret = Toolkit.getDefaultToolkit().createImage(ip);
			return ImageUtils.toBufferedImage(ret);
		}

		protected abstract ImageFilter createFilter(Object[] executed);
	}

	/**
	 * Fade the image, making it transparent by multiplying its alpha channel by the fade value.
	 * 
	 * @author Phil
	 *
	 */
	public static final class FmFadeImage extends FmImageFilter {

		public FmFadeImage(Function image, Function fadeValue) {
			super(image, fadeValue);
		}

		@Override
		protected ImageFilter createFilter(Object[] executed) {
			Double alpha = Numbers.toDouble(executed[1]);
			if (alpha == null) {
				return null;
			}
			if (alpha < 0) {
				alpha = 0.0;
			}
			if (alpha > 1) {
				alpha = 1.0;
			}
			final double finalAlpha = alpha;

			ImageFilter filter = new RGBImageFilter() {

				@Override
				public int filterRGB(int x, int y, int rgb) {
					// pass directly if completely transparent
					Color pixelCol = new Color(rgb);
					int alpha = pixelCol.getAlpha();
					if (alpha == 0) {
						return rgb;
					}

					double dAlpha = alpha * (1.0 / 255);
					dAlpha *= finalAlpha;

					int finalAlpha = Colours.ensureRange((int) Math.round(dAlpha * 255));
					Color ret = new Color(pixelCol.getRed(), pixelCol.getGreen(), pixelCol.getBlue(), finalAlpha);
					return ret.getRGB();
				}
			};
			return filter;
		}

		@Override
		public Function deepCopy() {
			return new FmFadeImage(child(0).deepCopy(), child(1).deepCopy());
		}

	}

	/**
	 * Apply the colour filter to the image. All non-transparent pixels are linearly interpolated (lerped) between their original colour and the input
	 * colour, according to the lerp fraction.
	 * 
	 * @author Phil
	 *
	 */
	public static final class FmColourImage extends FmImageFilter {

		public FmColourImage(Function image, Function col, Function lerp) {
			super(image, col, lerp);
		}

		@Override
		protected ImageFilter createFilter(Object[] executed) {
			final Color filterCol = Colours.toColour(executed[1]);
			if (filterCol == null) {
				return null;
			}

			final Double lerpValue = Numbers.toDouble(executed[2]);
			if (lerpValue == null) {
				return null;
			}

			ImageFilter filter = new RGBImageFilter() {

				@Override
				public int filterRGB(int x, int y, int rgb) {
					// pass directly if completely transparent
					Color pixelCol = new Color(rgb, true);
					if (pixelCol.getAlpha() == 0) {
						return rgb;
					}

					// lerp between the two according to the lerp value
					Color lerped = Colours.lerp(pixelCol, filterCol, lerpValue);
					return lerped.getRGB();
				}
			};
			return filter;
		}

		@Override
		public Function deepCopy() {
			return new FmColourImage(child(0).deepCopy(), child(1).deepCopy(), child(2).deepCopy());
		}

	}

	public static final class FmEquals extends FunctionImpl {

		public FmEquals(Function a, Function b) {
			super(a, b);
		}

		@Override
		public final Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			Object b = child(1).execute(parameters);
			if (a == EXECUTION_ERROR || b == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}

			boolean equals = ColumnValueProcessor.isEqual(a, b);
			return equals ? 1L : 0L;
		}

		@Override
		public Function deepCopy() {
			return new FmEquals(child(0).deepCopy(), child(1).deepCopy());
		}



		@Override
		public String toString() {
			return toStringWithChildOp("=");
		}
		
	
	}

	public static final class FmNotEqual extends FunctionImpl {
		public FmNotEqual(Function a, Function b) {
			super(a, b);
		}

		@Override
		public final Object execute(FunctionParameters parameters) {
			Object a = child(0).execute(parameters);
			Object b = child(1).execute(parameters);
			if (a == EXECUTION_ERROR || b == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}

			boolean equals = ColumnValueProcessor.isEqual(a, b);
			return equals ? 0L : 1L;
		}

		@Override
		public Function deepCopy() {
			return new FmNotEqual(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public String toString() {
			return toStringWithChildOp("!=");
		}

	}
	
	public static final class FmGreaterThan extends FmRelativeComparisonBase {

		public FmGreaterThan(Function a, Function b) {
			super(a, b);
		}

		@Override
		protected Object compare(double a, double b) {
			return a > b ? 1L : 0L;
		}

		@Override
		public Function deepCopy() {
			return new FmGreaterThan(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public String operator() {
			return ">";
		}
	}

	public static final class FmGreaterThanEqualTo extends FmRelativeComparisonBase {

		public FmGreaterThanEqualTo(Function a, Function b) {
			super(a, b);
		}

		@Override
		protected Object compare(double a, double b) {
			return a >= b ? 1L : 0L;
		}

		@Override
		public Function deepCopy() {
			return new FmGreaterThanEqualTo(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public String operator() {
			return ">=";
		}
	}

	public static final class FmIfThenElse extends FunctionImpl {

		public FmIfThenElse(Function fmIf, Function fmThen, Function fmElse) {
			super(fmIf, fmThen, fmElse);
		}

		@Override
		public Function deepCopy() {
			return new FmIfThenElse(child(0).deepCopy(), child(1).deepCopy(), child(2).deepCopy());
		}

		@Override
		public Object execute(FunctionParameters parameters) {

			Object o = child(0).execute(parameters);
			if (o == null || o == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}
			Double d = Numbers.toDouble(o);
			if (d == null) {
				return EXECUTION_ERROR;
			}

			if (d == 1) {
				return child(1).execute(parameters);
			} else {
				return child(2).execute(parameters);
			}

		}

		@Override
		public String toString() {
			return super.toString("if");
		}

		@Override
		public boolean hasBrackets() {
			return true;
		}
	}

	public static final class FmLessThan extends FmRelativeComparisonBase {

		public FmLessThan(Function a, Function b) {
			super(a, b);
		}

		@Override
		protected Object compare(double a, double b) {
			return a < b ? 1L : 0L;
		}

		@Override
		public Function deepCopy() {
			return new FmLessThan(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public String operator() {
			return "<";
		}
	}

	public static final class FmLessThanEqualTo extends FmRelativeComparisonBase {

		public FmLessThanEqualTo(Function a, Function b) {
			super(a, b);
		}

		@Override
		protected Object compare(double a, double b) {
			return a <= b ? 1L : 0L;
		}

		@Override
		public Function deepCopy() {
			return new FmLessThanEqualTo(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public String operator() {
			return "<=";
		}
	}

	public static final class FmSwitch extends FunctionImpl {

		public FmSwitch(Function... formula) {
			super(formula);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object expression = child(0).execute(parameters);
			if (expression == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}
			
			int n = nbChildren();
			for (int i = 1; (i + 1) < n; i += 2) {
				Object oCase = child(i).execute(parameters);
				if (oCase == EXECUTION_ERROR) {
					return EXECUTION_ERROR;
				}
				if (ColumnValueProcessor.isEqual(expression, oCase)) {
					return child(i + 1).execute(parameters);
				}
			}

			// Return the else at the end if we have one
			if(n%2==0){
				return child(n-1).execute(parameters);	
			}
			
			return null;
		}

		@Override
		public Function deepCopy() {
			return new FmSwitch(deepCopy(children));
		}

	}

	
	public static final class FmFirstNonNull extends FunctionImpl {

		public FmFirstNonNull(Function... formula) {
			super(formula);
		}

		@Override
		public Function deepCopy() {
			return new FmFirstNonNull(deepCopy(children));
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object [] children = executeChildFormulae(parameters, false);
			if(children==null){
				return Functions.EXECUTION_ERROR;
			}
			
			for(Object child:children){
				if(child!=null){
					return child;
				}
			}
			return null;
		}


		@Override
		public boolean hasBrackets() {
			return true;
		}

		@Override
		public String toString() {
			return toString("firstNonNull");
		}
	}
	
	public static final class FmMax extends FunctionImpl {

		public FmMax(Function... formula) {
			super(formula);
		}

		@Override
		public Function deepCopy() {
			return new FmMax(deepCopy(children));
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			return maxMin(children, parameters, true);
		}

		static Object maxMin(Function[] children, FunctionParameters parameters, boolean isMax) {
			Object ret = children[0].execute(parameters);
			if (ret == null || ret == EXECUTION_ERROR) {
				return EXECUTION_ERROR;
			}

			for (int j = 1; j < children.length; j++) {
				Object val = children[j].execute(parameters);
				if (val == null || val == EXECUTION_ERROR) {
					return EXECUTION_ERROR;
				}

				Double dCurrent = Numbers.toDouble(ret);
				Double dVal = Numbers.toDouble(val);
				if (dCurrent == null || dVal == null) {
					return EXECUTION_ERROR;
				}

				if (isMax) {
					if (dVal > dCurrent) {
						ret = val;
					}
				} else {
					if (dVal < dCurrent) {
						ret = val;
					}
				}
			}
			return ret;
		}

		@Override
		public boolean hasBrackets() {
			return true;
		}

		@Override
		public String toString() {
			return toString("max");
		}
	}

	public static final class FmMin extends FunctionImpl {

		public FmMin(Function... formula) {
			super(formula);
		}

		@Override
		public Function deepCopy() {
			return new FmMin(deepCopy(children));
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			return FmMax.maxMin(children, parameters, false);
		}

		@Override
		public boolean hasBrackets() {
			return true;
		}

		@Override
		public String toString() {
			return toString("min");
		}
	}

	public static final class FmMod extends Fm2ParamBase {

		public FmMod(Function a, Function b) {
			super(a, b);
		}

		@Override
		public Function deepCopy() {
			return new FmMod(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		protected Object execute(Object a, Object b) {
			Double da = Numbers.toDouble(a);
			Double db = Numbers.toDouble(b);
			if (da == null || db == null) {
				return EXECUTION_ERROR;
			}
			long la = da.longValue();
			long lb = db.longValue();
			if (lb == 0) {
				return EXECUTION_ERROR;
			}
			return la % lb;
		}

		@Override
		public String toString() {
			return toStringWithChildOp("/");
		}
	}

	public static final class FmPow extends Fm2ParamBase {

		public FmPow(Function a, Function b) {
			super(a, b);
		}

		@Override
		public Function deepCopy() {
			return new FmMod(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		protected Object execute(Object a, Object b) {
			Double da = Numbers.toDouble(a);
			Double db = Numbers.toDouble(b);
			if (da == null || db == null) {
				return EXECUTION_ERROR;
			}
			return Math.pow(da, db);
		}

		@Override
		public String toString() {
			return toString("pow");
		}
	}

	
	public static final class FmMultiply extends FunctionImpl {

		public FmMultiply(Function... formulae) {
			super(formulae);
		}

		@Override
		public Function deepCopy() {
			return new FmMultiply(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			// treat all as doubles
			double ret = 1;
			for (int i = 0; i < children.length; i++) {
				Object o = child(i).execute(parameters);
				if (o == EXECUTION_ERROR || o == null) {
					return EXECUTION_ERROR;
				}
				Double d = Numbers.toDouble(o);
				if (d == null) {
					return EXECUTION_ERROR;
				}

				ret *= d;
			}
			return ret;
		}

		@Override
		public String toString() {
			return toStringWithChildOp("*");
		}
	}

	public static final class FmNot extends Fm1ParamBase {

		public FmNot(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmNot(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "!" + child(0).toString();
		}

		@Override
		protected double execute(double d) {
			return d == 1 ? 1 : 0;
		}

		@Override
		protected long execute(long l) {
			return l == 1 ? 1 : 0;
		}
	}

	public static final class FmNegate extends Fm1ParamBase {

		public FmNegate(Function a) {
			super(a);
		}

		@Override
		public Function deepCopy() {
			return new FmNegate(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "-" + child(0).toString();
		}

		@Override
		protected double execute(double d) {
			return -d;
		}

		@Override
		protected long execute(long l) {
			return -l;
		}

	}

	public static final class FmRandColour extends FunctionImpl {
		public FmRandColour(Function seed) {
			super(seed);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object o = child(0).execute(parameters);
			if (o == Functions.EXECUTION_ERROR) {
				return Functions.EXECUTION_ERROR;
			}

			if (o == null) {
				o = new Double(0);
			}

			Double d = Numbers.toDouble(o);
			if (d != null && d.longValue() == d.doubleValue()) {
				return Colours.getRandomColour(d.longValue());
			}

			// get case-insensitive random colour
			return Colours.getRandomColour(o.toString());
		}

		@Override
		public Function deepCopy() {
			return new FmRandColour(child(0).deepCopy());
		}

		@Override
		public String toString() {
			return "randcolour(" + child(0).toString() + ")";
		}
	}

	public static final class FmRandPalletColour extends FunctionImpl{
		private final Random r = new Random();
		
		@Override
		public Object execute(FunctionParameters parameters) {
			return Colours.getRandomColorFromPredefinedPallet(r);
		}

		@Override
		public Function deepCopy() {
			return new FmRandPalletColour();
		}
		
	}
	
	public static final class FmOr extends FunctionImpl {

		public FmOr(Function a, Function b) {
			super(a, b);
		}

		@Override
		public Function deepCopy() {
			return new FmOr(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public Object execute(FunctionParameters parameters) {

			// treat all as doubles
			for (int i = 0; i < children.length; i++) {
				Object o = child(i).execute(parameters);
				if (o == EXECUTION_ERROR || o == null) {
					return EXECUTION_ERROR;
				}
				Double d = Numbers.toDouble(o);
				if (d == null) {
					// treat null as false 
					d=0.0;
				}

				if (d == 1) {
					return 1L;
				}
			}
			return 0L;
		}

		@Override
		public String toString() {
			return toStringWithChildOp("||");
		}

	}

	public static final class FmBitwiseOr extends FunctionImpl {

		public FmBitwiseOr(Function a, Function b) {
			super(a, b);
		}

		@Override
		public Function deepCopy() {
			return new FmBitwiseOr(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object [] childVals = executeChildFormulae(parameters, false);
			if(childVals==null){
				return Functions.EXECUTION_ERROR;
			}
			
			Long l1 = Numbers.toLong(childVals[0]);
			Long l2 = Numbers.toLong(childVals[1]);
			l1 = l1!=null ? l1:0L;
			l2 = l2!=null ? l2:0L;
			return l1 | l2;
		}

		@Override
		public String toString() {
			return toStringWithChildOp("|");
		}

	}
	
	public static final class FmSubtract extends Fm2ParamBase {

		public FmSubtract(Function a, Function b) {
			super(a, b);
		}

		@Override
		public Function deepCopy() {
			return new FmSubtract(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		public String toString() {
			return toStringWithChildOp("-");
		}

		@Override
		protected Object execute(Object a, Object b) {
			Double da = Numbers.toDouble(a);
			Double db = Numbers.toDouble(b);
			if (da == null || db == null) {
				return EXECUTION_ERROR;
			}
			return da - db;
		}
	}

	public static final class FmSum extends FunctionImpl {

		public FmSum(Function... formula) {
			super(formula);
		}

		@Override
		public Function deepCopy() {
			return new FmSum(deepCopy(children));
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			if (children.length == 0) {
				return 0.0;
			}

			// execute child formulae checking if any null
			Object[] executed = executeChildFormulae(parameters, true);
			if (executed == null) {
				return EXECUTION_ERROR;
			}

			// look out for the case where we have images
			int nbImages = 0;
			for (int i = 0; i < children.length; i++) {
				if (BufferedImage.class.isInstance(executed[0])) {
					nbImages++;
				}
			}

			if (nbImages > 0) {
				if (nbImages != children.length) {
					return EXECUTION_ERROR;
				}

				// combine images
				BufferedImage[] images = new BufferedImage[executed.length];
				for (int i = 0; i < images.length; i++) {
					images[i] = (BufferedImage) executed[i];
				}
				BufferedImage ret = ImageUtils.addImages(images);
				return ret;
			} else {
				// treat all as doubles
				double ret = 0;
				for (int i = 0; i < children.length; i++) {
					Double d = Numbers.toDouble(executed[i]);
					if (d == null) {
						return EXECUTION_ERROR;
					}

					ret += d;
				}
				return ret;

			}

		}

		@Override
		public String toString() {
			return toStringWithChildOp("+");
		}

	}

	public static final class FmConcatenate extends FunctionImpl {

		public FmConcatenate(Function... formula) {
			super(formula);
		}

		@Override
		public Function deepCopy() {
			return new FmConcatenate(deepCopy(children));
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			if (children.length == 0) {
				return 0.0;
			}

			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < children.length; i++) {
				Object o = child(i).execute(parameters);
				if (o == EXECUTION_ERROR) {
					return EXECUTION_ERROR;
				}

				// treat null as empty
				if (o == null) {
					o = "";
				}

				String s = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, o);
				if (s != null) {
					builder.append(s);
				}
			}
			return builder.toString();
		}

		@Override
		public String toString() {
			return toStringWithChildOp("&");
		}

	}

	public static final class FmColourMultiply extends FunctionImpl {
		public FmColourMultiply(Function colour, Function factor) {
			super(colour, factor);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object [] executed =executeChildFormulae(parameters, true);
			if(executed==null){
				return EXECUTION_ERROR;
			}
			
			Color col = Colours.toColour(executed[0]);
			if (col == null) {
				return EXECUTION_ERROR;
			}

			Double factor = Numbers.toDouble(executed[1]);
			if (factor == null) {
				return EXECUTION_ERROR;
			}

			return Colours.multiplyNonAlpha(col, factor.floatValue());
		}

		@Override
		public Function deepCopy() {
			return new FmColourMultiply(child(0).deepCopy(), child(1).deepCopy());

		}

	}
	
	public static class FmAppProperty extends FunctionImpl{
		public FmAppProperty(Function param){
			super(param);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object o = child(0).execute(parameters);
			if(o==Functions.EXECUTION_ERROR){
				return Functions.EXECUTION_ERROR;
			}
			if(o==null){
				return null;
			}
			
			String key = (String)ColumnValueProcessor.convertToMe(ODLColumnType.STRING, o);
			if(key!=null){
				return AppProperties.getString(key);
			}
			return null;
		}

		@Override
		public Function deepCopy() {
			throw new UnsupportedOperationException();
		}
		
		
	}
	

	public static abstract class FmLerpToDefinedColour extends FunctionImpl{
		public FmLerpToDefinedColour(Function colour, Function ammount){
			super(colour, ammount);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object [] vals = executeChildFormulae(parameters, true);
			if(vals == null){
				return Functions.EXECUTION_ERROR;
			}
			
			Color col = (Color)ColumnValueProcessor.convertToMe(ODLColumnType.COLOUR, vals[0]);
			Double lerp = (Double)ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, vals[1]);
			if(col== null || lerp == null){
				return Functions.EXECUTION_ERROR;				
			}
			
			Color other = getColour(col);
			return Colours.lerp(col, other, lerp);
		}
		
		protected abstract Color getColour(Color source);

		@Override
		public Function deepCopy() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	public static final class FmGreyscale extends FmLerpToDefinedColour{
		public FmGreyscale(Function colour, Function ammount){
			super(colour, ammount);
		}

		@Override
		protected Color getColour(Color source) {
			return Colours.toGrey(source);
		}
		
	}
	
	public static final class FmLighten extends FmLerpToDefinedColour{
		public FmLighten(Function colour, Function ammount){
			super(colour, ammount);
		}

		@Override
		protected Color getColour(Color source) {
			return Color.WHITE;
		}
		
	}
	
	public static final class FmDarken extends FmLerpToDefinedColour{
		public FmDarken(Function colour, Function ammount){
			super(colour, ammount);
		}

		@Override
		protected Color getColour(Color source) {
			return Color.BLACK;
		}
		
	}
	
	public static final class FmColour extends FunctionImpl {
		public FmColour(Function red, Function green, Function blue) {
			super(red, green, blue);
		}

		public FmColour(Function red, Function green, Function blue, Function alpha) {
			super(red, green, blue, alpha);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			int n = children.length;
			Object[] oarr = new Object[n];
			float[] cols = new float[n];
			for (int i = 0; i < oarr.length; i++) {
				oarr[i] = child(i).execute(parameters);
				if (oarr[i] == null || oarr[i] == EXECUTION_ERROR) {
					return EXECUTION_ERROR;
				}

				Double d = Numbers.toDouble(oarr[i]);
				if (d == null) {
					return EXECUTION_ERROR;
				}
				if (d < 0)
					d = 0.0;
				if (d > 1)
					d = 1.0;
				cols[i] = d.floatValue();

			}

			if (n == 3) {
				return new Color(cols[0], cols[1], cols[2]);
			} else {
				return new Color(cols[0], cols[1], cols[2], cols[3]);
			}
		}

		@Override
		public Function deepCopy() {
			if (children.length == 3) {
				return new FmColour(child(0).deepCopy(), child(1).deepCopy(), child(2).deepCopy());
			}
			return new FmColour(child(0).deepCopy(), child(1).deepCopy(), child(2).deepCopy(), child(3).deepCopy());

		}

	}

	public static abstract class Fm1GeometryParam extends FunctionImpl {
		public Fm1GeometryParam(Function geometry) {
			super(geometry);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object child = child(0).execute(parameters);
			if (child == null || child == Functions.EXECUTION_ERROR) {
				return Functions.EXECUTION_ERROR;
			}

			ODLGeomImpl geom = (ODLGeomImpl) ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, child);
			if (geom == null) {
				// if there is no geometry, return null as (a) the property of a null geometry must obviously
				// be null and (b) it allows to avoid having to use formula like this:  if(geom!=null, centroid(geom),null)
				return null;
			}

			Geometry geometry = geom.getJTSGeometry();
			if (geometry == null) {
				return Functions.EXECUTION_ERROR;
			}

			return execute(geometry);
		}

		protected abstract Object execute(Geometry geometry);
	}

	public static final class FmPostcodeUKFormatUnit extends FmSingleString{

		public FmPostcodeUKFormatUnit(Function child) {
			super(child);
		}

		@Override
		public Function deepCopy() {
			return new FmPostcodeUKFormatUnit(child(0).deepCopy());
		}

		@Override
		protected Object execute(String s) {
			Matcher matcher = UKPostcodes.unitWithWithoutSpaceGroupedForSpace.matcher(s);
			if(matcher.matches()){
				return matcher.group(1) + " " + matcher.group(2);
			}
			return null;
		}
		
	}
	

	public static abstract class FmAbstractRegExp extends FunctionImpl{

		public FmAbstractRegExp(Function...functions) {
			super(functions);
		}

		@Override
		public Object execute(FunctionParameters parameters) {
			Object [] childs = executeChildFormulae(parameters, false);
			if(childs==null){
				return Functions.EXECUTION_ERROR;
			}
			if(childs[0]==null){
				return Functions.EXECUTION_ERROR;
			}
			
			String regExp = (String)ColumnValueProcessor.convertToMe(ODLColumnType.STRING, childs[0]);
			if(regExp==null){				
				return Functions.EXECUTION_ERROR;
			}
			String str = (String)ColumnValueProcessor.convertToMe(ODLColumnType.STRING, childs[1]);
			if(str==null){
				// no match
				return 0L;
			}
			
			try {
				Pattern pattern = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE);	
				Matcher matcher = pattern.matcher(str);
				return execute(parameters,childs, matcher);
			} catch (Exception e) {
				return Functions.EXECUTION_ERROR;
			}
		}

		protected abstract Object execute(FunctionParameters parameters, Object [] childs,Matcher matcher);
		
	}
	
//	public static final class FmRegExpMatchedText extends FmAbstractRegExp{
//
//		public FmRegExpMatchedText(Function regExp, Function str) {
//			super(regExp, str);
//		}
//
//		@Override
//		public Function deepCopy() {
//			return new FmRegExpMatches(child(0).deepCopy(), child(1).deepCopy());
//		}
//
//		@Override
//		protected Object execute(FunctionParameters parameters,Object [] childs, Matcher matcher){
//			if( matcher.find() ){
//				return matcher.group();
//			}
//			else{
//				return null;
//			}
//		}
//		
//	}
	
	public static final class FmRegExpMatchedGroup extends FmAbstractRegExp{

		public FmRegExpMatchedGroup(Function regExp, Function str, Function indx) {
			super(regExp, str, indx);
		}

		@Override
		public Function deepCopy() {
			return new FmRegExpMatches(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		protected Object execute(FunctionParameters parameters,Object [] childs, Matcher matcher){
			Long indx = Numbers.toLong(childs[2],false);
			if(indx==null){
				return Functions.EXECUTION_ERROR;
			}
			
			if( matcher.matches() ){
				if(indx.intValue() < matcher.groupCount()){
					return matcher.group(indx.intValue()+1);					
				}
				return Functions.EXECUTION_ERROR;
			}
			else{
				return null;
			}
		}
		
	}
	
	public static final class FmRegExpMatches extends FmAbstractRegExp{

		public FmRegExpMatches(Function regExp, Function str) {
			super(regExp, str);
		}

		@Override
		public Function deepCopy() {
			return new FmRegExpMatches(child(0).deepCopy(), child(1).deepCopy());
		}

		@Override
		protected Object execute(FunctionParameters parameters,Object [] childs, Matcher matcher){
			return matcher.matches() ? 1L : 0L;
		}
		
	}
	
	
	
	public static final class FmPostcodeUk extends FmSingleString {

		private static final Pattern unit = UKPostcodes.unitWithWithoutSpaceGroupedForSpace; // Pattern.compile("(" + UKPostcodes.allUnit + ")", Pattern.CASE_INSENSITIVE);
		private static final Pattern sector = Pattern.compile("(" + UKPostcodes.allSector + ")", Pattern.CASE_INSENSITIVE);
		private static final Pattern district = Pattern.compile("(" + UKPostcodes.allDistrict + ")", Pattern.CASE_INSENSITIVE);
		private static final Pattern area = Pattern.compile("(" + UKPostcodes.area + ")", Pattern.CASE_INSENSITIVE);
		private final UKPostcodeLevel level;

		public FmPostcodeUk(UKPostcodeLevel level, Function child) {
			super(child);
			this.level = level;
		}

		@Override
		public Function deepCopy() {
			return new FmPostcodeUk(level, child(0).deepCopy());
		}

		@Override
		protected Object execute(String s) {
			if (s == null) {
				return null;
			}
			
			// try cleaning to readd to the space first if not already unit
			if(level != UKPostcodeLevel.Unit){
				s = Strings.std(s);
				Matcher ukUnit = unit.matcher(s);
				if(ukUnit.matches()){
					s = ukUnit.group(1) + " " + ukUnit.group(2);
				}
				s = s.toUpperCase();
			}

			Pattern pattern = null;
			switch (level) {
			case Unit:
				pattern = unit;
				break;

			case Sector:
				pattern = sector;
				break;

			case District:
				pattern = district;
				break;

			case Area:
				pattern = area;
				break;

			}
			Matcher matcher = pattern.matcher(s);
			String ret = null;
			if (matcher.find()) {

				ret = matcher.group();

				// call standardising on the UK postcode
				ret = UKPostcodes.standardisePostcode(ret, true);

			}

			return ret;
		}

		@Override
		public String toString() {
			return toString("postcodeuk" + level.name().toLowerCase());
		}

	}

	public static class FmStringDateTimeStamp extends FunctionImpl{
		private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss") ;

		@Override
		public Object execute(FunctionParameters parameters) {
			Date date = new Date() ;
			return dateFormat.format(date);
		}

		@Override
		public Function deepCopy() {
			return new FmStringDateTimeStamp();
		}		
	}
	
	public static class FmLineStringFraction extends FunctionImpl{
		public FmLineStringFraction(Function geom, Function fraction) {
			super(geom,fraction);
		}
		
		@Override
		public Object execute(FunctionParameters parameters) {
			Object []vals = executeChildFormulae(parameters, true);
			if(vals==null){
				return Functions.EXECUTION_ERROR;
			}
			
			ODLGeom geom = (ODLGeom)ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, vals[0]);
			Double fraction = (Double)ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, vals[1]);
			if(geom== null || fraction == null){
				return Functions.EXECUTION_ERROR;				
			}
			
			ODLGeomImpl geomImpl = (ODLGeomImpl)geom;
			Geometry jtsGeometry = geomImpl.getJTSGeometry();
			if(jtsGeometry!=null && LineString.class.isInstance(jtsGeometry)){
				return new ODLLoadedGeometry(LinestringFraction.calculateFraction((LineString)jtsGeometry, fraction));
			}
			return null;
			
		}

		@Override
		public Function deepCopy() {
			return new FmLineStringFraction(child(0).deepCopy(), child(1).deepCopy());
		}
		
	}
	
	public static class FmLineStringEnd extends FunctionImpl{
		public FmLineStringEnd(Function geom) {
			super(geom);
		}
		
		@Override
		public Object execute(FunctionParameters parameters) {
			Object val = child(0).execute(parameters);
			if(val==null){
				return null;
			}
			
			ODLGeom geom = (ODLGeom)ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, val);
			if(geom== null ){
				return null;				
			}
			
			ODLGeomImpl geomImpl = (ODLGeomImpl)geom;
			Geometry jtsGeometry = geomImpl.getJTSGeometry();
			if(jtsGeometry!=null && LineString.class.isInstance(jtsGeometry)){
				LineString ls = (LineString)jtsGeometry;
				Coordinate coord = ls.getCoordinateN(ls.getNumPoints()-1);
				Geometry pnt = new GeometryFactory().createPoint(coord);
				return new ODLLoadedGeometry(pnt);
			}
			return null;
			
		}

		@Override
		public Function deepCopy() {
			return new FmLineStringFraction(child(0).deepCopy(), child(1).deepCopy());
		}
		
	}
	
	public static class FmTileFactory extends FunctionImpl{
		public FmTileFactory(Function param) {
			super(param);
		}
		
		@Override
		public Object execute(FunctionParameters parameters) {
			Object val = child(0).execute(parameters);
			if(val == null || val == Functions.EXECUTION_ERROR){
				return Functions.EXECUTION_ERROR;
			}
			
			String s =(String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, val);
			if(s==null){
				return Functions.EXECUTION_ERROR;				
			}
			
			StandardisedStringTreeMap<String> map = new StandardisedStringTreeMap<String>(false);
			String [] split = s.split(",");
			for(String keyvalue : split){
				String[] extraSplit = keyvalue.split("=");
				for(int i =0 ; i < extraSplit.length ; i++){
					extraSplit[i] = extraSplit[i].trim();
				}
				if(extraSplit.length!=2 || extraSplit[0].length()==0){
					return Functions.EXECUTION_ERROR;
				}
				map.put(extraSplit[0], extraSplit[1]);
			}
			
			BackgroundMapConfig config = new BackgroundMapConfig(map);
			return BackgroundTileFactorySingleton.createTileFactory(config);
		}

		@Override
		public Function deepCopy() {
			throw new UnsupportedOperationException();
		}
		
	}
}
