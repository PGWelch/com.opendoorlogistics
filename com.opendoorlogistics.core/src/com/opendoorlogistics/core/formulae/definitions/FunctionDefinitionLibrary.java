/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.formulae.definitions;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.opendoorlogistics.core.distances.functions.FmDistance;
import com.opendoorlogistics.core.distances.functions.FmDrivingDistance;
import com.opendoorlogistics.core.distances.functions.FmDrivingRouteGeom;
import com.opendoorlogistics.core.distances.functions.FmDrivingRouteGeom.FmDrivingRouteGeomUncached;
import com.opendoorlogistics.core.distances.functions.FmDrivingRouteGeomFromLine;
import com.opendoorlogistics.core.distances.functions.FmDrivingTime;
import com.opendoorlogistics.core.distances.functions.FmDrivingTime.FmDrivingTimeUncached;
import com.opendoorlogistics.core.formulae.FmLegendRangeText;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionFactory;
import com.opendoorlogistics.core.formulae.FunctionUtils;
import com.opendoorlogistics.core.formulae.Functions.FmAbs;
import com.opendoorlogistics.core.formulae.Functions.FmAcos;
import com.opendoorlogistics.core.formulae.Functions.FmAnd;
import com.opendoorlogistics.core.formulae.Functions.FmAppProperty;
import com.opendoorlogistics.core.formulae.Functions.FmAsin;
import com.opendoorlogistics.core.formulae.Functions.FmAtan;
import com.opendoorlogistics.core.formulae.Functions.FmBitwiseOr;
import com.opendoorlogistics.core.formulae.Functions.FmCeil;
import com.opendoorlogistics.core.formulae.Functions.FmColour;
import com.opendoorlogistics.core.formulae.Functions.FmColourImage;
import com.opendoorlogistics.core.formulae.Functions.FmColourMultiply;
import com.opendoorlogistics.core.formulae.Functions.FmConcatenate;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.formulae.Functions.FmContains;
import com.opendoorlogistics.core.formulae.Functions.FmCos;
import com.opendoorlogistics.core.formulae.Functions.FmCreateUUID;
import com.opendoorlogistics.core.formulae.Functions.FmDarken;
import com.opendoorlogistics.core.formulae.Functions.FmDecimalFormat;
import com.opendoorlogistics.core.formulae.Functions.FmDecimalHours;
import com.opendoorlogistics.core.formulae.Functions.FmDivide;
import com.opendoorlogistics.core.formulae.Functions.FmEquals;
import com.opendoorlogistics.core.formulae.Functions.FmFadeImage;
import com.opendoorlogistics.core.formulae.Functions.FmFirstNonNull;
import com.opendoorlogistics.core.formulae.Functions.FmFloor;
import com.opendoorlogistics.core.formulae.Functions.FmGreaterThan;
import com.opendoorlogistics.core.formulae.Functions.FmGreaterThanEqualTo;
import com.opendoorlogistics.core.formulae.Functions.FmGreyscale;
import com.opendoorlogistics.core.formulae.Functions.FmIfThenElse;
import com.opendoorlogistics.core.formulae.Functions.FmIndexOf;
import com.opendoorlogistics.core.formulae.Functions.FmLeft;
import com.opendoorlogistics.core.formulae.Functions.FmLen;
import com.opendoorlogistics.core.formulae.Functions.FmLerp;
import com.opendoorlogistics.core.formulae.Functions.FmLessThan;
import com.opendoorlogistics.core.formulae.Functions.FmLessThanEqualTo;
import com.opendoorlogistics.core.formulae.Functions.FmLighten;
import com.opendoorlogistics.core.formulae.Functions.FmLineStringEnd;
import com.opendoorlogistics.core.formulae.Functions.FmLineStringFraction;
import com.opendoorlogistics.core.formulae.Functions.FmLn;
import com.opendoorlogistics.core.formulae.Functions.FmLog10;
import com.opendoorlogistics.core.formulae.Functions.FmLower;
import com.opendoorlogistics.core.formulae.Functions.FmMax;
import com.opendoorlogistics.core.formulae.Functions.FmMin;
import com.opendoorlogistics.core.formulae.Functions.FmMod;
import com.opendoorlogistics.core.formulae.Functions.FmMultiply;
import com.opendoorlogistics.core.formulae.Functions.FmNotEqual;
import com.opendoorlogistics.core.formulae.Functions.FmOr;
import com.opendoorlogistics.core.formulae.Functions.FmPostcodeUKFormatUnit;
import com.opendoorlogistics.core.formulae.Functions.FmPostcodeUk;
import com.opendoorlogistics.core.formulae.Functions.FmPow;
import com.opendoorlogistics.core.formulae.Functions.FmRand;
import com.opendoorlogistics.core.formulae.Functions.FmRandColour;
import com.opendoorlogistics.core.formulae.Functions.FmRandData;
import com.opendoorlogistics.core.formulae.Functions.FmTileFactory;
import com.opendoorlogistics.core.formulae.Functions.FmRandData.RandDataType;
import com.opendoorlogistics.core.formulae.Functions.FmRandPalletColour;
import com.opendoorlogistics.core.formulae.Functions.FmRandomSymbol;
import com.opendoorlogistics.core.formulae.Functions.FmRegExpMatchedGroup;
import com.opendoorlogistics.core.formulae.Functions.FmRegExpMatches;
import com.opendoorlogistics.core.formulae.Functions.FmReplace;
import com.opendoorlogistics.core.formulae.Functions.FmRound;
import com.opendoorlogistics.core.formulae.Functions.FmRound2Second;
import com.opendoorlogistics.core.formulae.Functions.FmSin;
import com.opendoorlogistics.core.formulae.Functions.FmSqrt;
import com.opendoorlogistics.core.formulae.Functions.FmStringDateTimeStamp;
import com.opendoorlogistics.core.formulae.Functions.FmStringFormat;
import com.opendoorlogistics.core.formulae.Functions.FmSubtract;
import com.opendoorlogistics.core.formulae.Functions.FmSum;
import com.opendoorlogistics.core.formulae.Functions.FmSwitch;
import com.opendoorlogistics.core.formulae.Functions.FmTan;
import com.opendoorlogistics.core.formulae.Functions.FmTemperatureColours;
import com.opendoorlogistics.core.formulae.Functions.FmTime;
import com.opendoorlogistics.core.formulae.Functions.FmUpper;
import com.opendoorlogistics.core.formulae.StringTokeniser;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.ArgumentType;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionArgument;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionType;
import com.opendoorlogistics.core.geometry.functions.FmCentroid;
import com.opendoorlogistics.core.geometry.functions.FmGeom;
import com.opendoorlogistics.core.geometry.functions.FmGeom.GeomType;
import com.opendoorlogistics.core.geometry.functions.FmGeomArea;
import com.opendoorlogistics.core.geometry.functions.FmGeomBorder;
import com.opendoorlogistics.core.geometry.functions.FmGeomContains;
import com.opendoorlogistics.core.geometry.functions.FmLatitude;
import com.opendoorlogistics.core.geometry.functions.FmLongitude;
import com.opendoorlogistics.core.geometry.functions.FmShapefileLookup;
import com.opendoorlogistics.core.gis.map.data.UserRenderFlags;
import com.opendoorlogistics.core.gis.postcodes.UKPostcodes.UKPostcodeLevel;
import com.opendoorlogistics.core.scripts.TableReference;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;

public final class FunctionDefinitionLibrary {
	public static FunctionDefinitionLibrary DEFAULT_LIB = new FunctionDefinitionLibrary().buildStd();
	
	private StandardisedStringTreeMap<List<FunctionDefinition>> map = new StandardisedStringTreeMap<>(false);
	private int nextOperatorPrecedence = 0;

	public void add(FunctionDefinition definition) {
		List<FunctionDefinition> list = map.get(definition.getName());
		if (list == null) {
			list = new ArrayList<>();
			map.put(definition.getName(), list);
		} else if (definition.getType() == FunctionType.OPERATOR) {
			throw new RuntimeException("Cannot overload operators");
		}

		list.add(definition);
		
		// keep sorted so method with smallest number of arguments is shown first
		Collections.sort(list, new Comparator<FunctionDefinition>() {

			@Override
			public int compare(FunctionDefinition o1, FunctionDefinition o2) {
				return Integer.compare(o1.nbArgs(), o2.nbArgs());
			}
		});
	}

	public FunctionDefinitionLibrary(){}
	
	public FunctionDefinitionLibrary(FunctionDefinitionLibrary copyFromThis){
		for(Map.Entry<String,List< FunctionDefinition>> entry:copyFromThis.map.entrySet()){
			map.put(entry.getKey(), entry.getValue());
		}
		nextOperatorPrecedence = copyFromThis.nextOperatorPrecedence;
	}
	
	public FunctionDefinitionLibrary buildStd() {
		// add operators IN ORDER OF PRECEDENCE
		addVargsOperator(FmMultiply.class, "*", "Multiply values together.");
		addStandardOperator(FmDivide.class, "/",  "Divide one value by the other.");
		addStandardOperator(FmMod.class, "%", "Remainder of dividing first value by second.");
		addVargsOperator(FmSum.class , "+","Add values together.");
		addVargsOperator(FmConcatenate.class, "&", "Concatenate strings.");
		for(String s:StringTokeniser.minuses()){
			addStandardOperator(FmSubtract.class, s, "Subtract second value from first.");	
		}
		addStandardOperator(FmLessThanEqualTo.class, "<=", "Test if first value is less than or equal to second.");
		addStandardOperator(FmGreaterThanEqualTo.class, ">=", "Test if first value is greater than or equal to second.");
		addStandardOperator(FmLessThan.class, "<", "Test if first value is less than second.");
		addStandardOperator(FmGreaterThan.class, ">", "Test if first value is greater than second.");
		addStandardOperator(FmEquals.class, "==" ,"Test if first value is equal to second." );
		addStandardOperator(FmEquals.class,  "=","Test if first value is equal to second." );
		addStandardOperator(FmNotEqual.class, "!=" ,"Test if first value is not equal to second." );
		addStandardOperator(FmNotEqual.class,  "<>","Test if first value is not equal to second" );
		addStandardOperator(FmBitwiseOr.class, "|", "Bitwise or");
		addStandardOperator(FmAnd.class, "&&", "Boolean and function.");
		addStandardOperator(FmOr.class, "||", "Boolean or function.");
		
		// functions
		addStandardFunction(FmIfThenElse.class, "if", "Test the first value and if this is true return the second value, otherwise return the third.","condition", "value_if_true", "value_if_false");
		addStandardFunction(FmRand.class, "rand", "Random number between 0 and 1.");
		addStandardFunction(FmRandomSymbol.class, "randsymbol", "Return a randomly chosen symbol name.");
		addStandardFunction(FmAcos.class, "acos","Inverse cosine function", "value");
		addStandardFunction(FmAsin.class, "asin","Inverse sine function", "value");
		addStandardFunction(FmAtan.class, "atan","Inverse tan function", "value");
		addStandardFunction(FmCos.class, "cos", "Cosine function","value");
		addStandardFunction(FmLn.class, "ln","Natural logarithm", "value");
		addStandardFunction(FmLog10.class, "log10","Logarithm to base to", "value");
		addStandardFunction(FmSqrt.class, "sqrt","Calculate the square root of the value", "value");
		addStandardFunction(FmSin.class, "sin","Sine function", "value");
		addStandardFunction(FmTan.class, "tan","Tan function", "value");
		addStandardFunction(FmAbs.class, "abs", "Get the absolute of the value","value");
		addStandardFunction(FmCeil.class, "ceil","", "value");
		addStandardFunction(FmFloor.class, "floor","Return the integer part of the number, e.g. 2.3 returns 2.", "value");
		addStandardFunction(FmPow.class, "pow","Return the value of the 1st number raised to the power of the 2nd number.", "value1","value2");
		addStandardFunction(FmRound.class, "round","Round to the nearest integer value.", "value");
		for(String constName : new String[]{"const", "c"}){
			addStandardFunction(FmConst.class, constName,"Create a constant string value from the input value. This is used to distinguish between string constants and source columns. For example, if your data adapter's source table has a column called \"name\" and you want to create a string constant also containing \"name\", you should use const(\"name\") as \"name\" on its own will automatically be converted to a source column reference.", "value");					
		}
		
		for(String spelling : new String[]{"colour", "color"}){
			addStandardFunction(FmColour.class, spelling,"Create a colour object from red, green and blue values in the range 0 to 1.", "red", "green", "blue");			
			addStandardFunction(FmColour.class, spelling,"Create a colour object from red, green, blue and alpha (transparency) values in the range 0 to 1.", "red", "green", "blue", "alpha");			
			addStandardFunction(FmColourImage.class,spelling +  "image", "Apply the colour filter to the image. All non-transparent pixels are linearly interpolated (lerped) between their original colour and the input colour, according to the lerp fraction.", "image", "colour", "lerpFraction");
		}
		
		addStandardFunction(FmAppProperty.class, "property", "Retrieve an application property from the application's properties file. Property is returned as a string.", "propertykey");
		addStandardFunction(FmFadeImage.class, "fadeimage", "Fade the image, making it transparent by multiplying its alpha channel by the fade value.", "image", "fadevalue");
		addStandardFunction(FmRandColour.class, "randcolor", "Create a random colour based on the input value.","seed_value");
		addStandardFunction(FmRandColour.class, "randcolour", "Create a random colour based on the input value.","seed_value");
		addStandardFunction(FmColourMultiply.class, "colourmultiply", "Multiply the input colour by the factor, making it lighter or darker.","colour", "factor");
		addStandardFunction(FmColourMultiply.class, "colormultiply", "Multiply the input color by the factor, making it lighter or darker.","color", "factor");
		addStandardFunction(FmGreyscale.class, "greyscale", "Convert the input colour to grey, based on the factor (between 0 and 1).", "colour", "factor");
		addStandardFunction(FmGreyscale.class, "grayscale", "Convert the input color to gray, based on the factor (between 0 and 1).", "color", "factor");
		addStandardFunction(FmLighten.class, "lighten", "Lighten the input colour based on the factor (between 0 and 1).", "colour", "factor");
		addStandardFunction(FmDarken.class, "darken", "Darken the input colour based on the factor (between 0 and 1).", "colour", "factor");
		addStandardFunction(FmRandPalletColour.class, "randPalletColour", "Choose a random colour from ODL's internal pallet.");
		addStandardFunction(FmRandPalletColour.class, "randPalletColor", "Choose a random color from ODL's internal pallet.");
		addStandardFunction(FmLerp.class, "lerp", "Linearly interpolate between value a and value b based on value c (which is in the range 0 to 1).", "a","b","c");
		addStandardFunction(FmTemperatureColours.class, "cold2hot", "Return a colour from cold (blue) to hot (red) based on the input number, which should be in the range 0 to 1.", "fraction");
		
	
		// add distance functions
		addStandardFunction(FmDrivingRouteGeomFromLine.class, "routegeom", "Given an input line geometry, calculate the road network route between the start and end." , "line_geometry" , "road_network_graph_directory");
		String [] lat1lng1latlng2 = new String[]{"latitude1", "longitude1", "latitude2", "longitude2"};
		for(String [] params : new String[][]{
				new String[]{"geometry1",  "geometry2"},
				lat1lng1latlng2,

		}){
			addStandardFunction(FmDistance.Km.class, "distanceKm", "Calculate distance in kilometres between points.", params).setGroup("Distance");
			addStandardFunction(FmDistance.Miles.class, "distanceMiles", "Calculate distance in miles between points.", params).setGroup("Distance");;
			addStandardFunction(FmDistance.Metres.class, "distanceMetres", "Calculate distance in metres between points.", params).setGroup("Distance");;
			
			String []extParams = Strings.addToArray(params, "road_network_graph_directory");
			addStandardFunction(FmDrivingDistance.Km.class, "drivingDistanceKm", "Calculate driving distance in kilometres between points.", extParams).setGroup("DrivingDistance");
			addStandardFunction(FmDrivingDistance.Miles.class, "drivingDistanceMiles", "Calculate driving distance in miles between points.", extParams).setGroup("DrivingDistance");
			addStandardFunction(FmDrivingDistance.Metres.class, "drivingDistanceMetres", "Calculate driving distance in metres between points.", extParams).setGroup("DrivingDistance");
			addStandardFunction(FmDrivingTime.class, "drivingTime", "Calculate driving time between points.", extParams);
			addStandardFunction(FmDrivingTimeUncached.class, "drivingTimeUncached", "Calculate driving time between points without using caching.", extParams);	
			addStandardFunction(FmDrivingRouteGeom.class, "routegeom", "Calculate the road network route between points.", extParams);
			addStandardFunction(FmDrivingRouteGeomUncached.class, "routegeomuncached", "Calculate the road network route between points without using caching", extParams);
						
		}
		
	//	addStandardFunction(FmDrivingTime.class, "drivingTimeUncached", "Calculate driving time between points, do not cache results.", lat1lng1latlng2);_
		
		
		// create time functions
		addStandardFunction(FmTime.class, "time", "Create a time form the current system time.");			
		for(String [] params : new String[][]{
				new String[]{"milliseconds"},
				new String[]{"hours",  "minutes"},
				new String[]{"days",  "hours",  "minutes"},
				new String[]{"days",  "hours",  "minutes",  "seconds"},
				new String[]{"days",  "hours",  "minutes",  "seconds",  "milliseconds"},

		}){
			addStandardFunction(FmTime.class, "time", "Create a time using the input components.",params).setGroup("Time");;			
		}
		addStandardFunction(FmRound2Second.class, "round2second","Round the time to the nearest second.", "time").setGroup("Time");

		// create geometry functions
		for(final FmGeom.GeomType type:FmGeom.GeomType.values()){
			FunctionDefinition dfn = new FunctionDefinition(FunctionType.FUNCTION, type.name().toLowerCase());
			dfn.setDescription("Create a geometry object of type " + type.name().toLowerCase() + " from longitude and latitudes.");
			if(type == GeomType.POINT){
				dfn.addArg("Longitude");
				dfn.addArg("Latitude");
			}else{
				dfn.addVarArgs("longAndLat", ArgumentType.GENERAL, "Pairs of longitude and latitude values.");				
			}
			dfn.setFactory(new FunctionFactory() {
				
				@Override
				public Function createFunction(Function... children) {
					return new FmGeom(type,children);
				}
			});
			add(dfn);
		}
		addStandardFunction(FmLatitude.class, "latitude", "Return the latitude of a geometry. If the geometry is not a point this returns its centroid's latitude.","geometry");
		addStandardFunction(FmLongitude.class, "longitude", "Return the longitude of a geometry. If the geometry is not a point this returns its centroid's longitude.","geometry");
		addStandardFunction(FmCentroid.class, "centroid", "Return the centroid of the geometry, as a point geometry.","geometry");
		addStandardFunction(FmShapefileLookup.class, "shapefilelookup", "Lookup a geometry in a shapefile on disk. For the input filename and type_name in the file, the search_value is searched for in the search_field and the geometry of the first matching record is returned.", "filename","search_value", "type_name", "search_field");
		addStandardFunction(FmShapefileLookup.class, "shapefilelookup", "Lookup a geometry in a shapefile on disk. For the input filename and type_name in the file, the search_value is searched for in the search_field and the geometry of the first matching record is returned.", "filename","search_value", "search_field");
		addStandardFunction(FmGeomBorder.class, "geomborder", "Return the borders of the geometry as lines.", "geometry", "include_holes");
		addStandardFunction(FmGeomContains.class, "geomcontains", "Return whether the geometry contains the point, using the input EPSG grid projection.", "geometry", "latitude", "longitude", "EPSG");
		addStandardFunction(FmGeomContains.class, "geomcontains", "Return whether the geometry contains the point, using a WGS84 latitude-longitude projection.", "geometry", "latitude", "longitude");
		addStandardFunction(FmLineStringFraction.class, "linestringfraction", "Return a fraction (0 to 1) of the input linestring.", "linestring", "fraction");
		addStandardFunction(FmLineStringEnd.class, "linestringend", "Return the end of a linestring (as another geometry).", "linestring");
		addStandardFunction(FmTileFactory.class, "tileprovider", "Create a map tile provider", "String containing a comma-separated list of key-value pairs defining the background map - e.g. \"fade.r=255, type=MAPSFORGE\".");		
		addStandardFunction(FmDecimalHours.class, "decimalHours", "Return the number of decimal hours in a time.","time");
		addStandardFunction(FmGeomArea.class, "geomarea", "Calculate the area of the geometry in the units of the input EPSG projection. The projection must be an equal area projection or the calculation will be wrong.", "geometry", "EPSGCodeEqualArea");
		
		// uk postcodes
		for(final UKPostcodeLevel level: UKPostcodeLevel.values()){
			FunctionDefinition dfn = addStandardFunction(FmPostcodeUk.class, "postcodeuk" + level.name().toLowerCase(), "Find and return the first UK postcode " + level.name().toLowerCase() + " from the input string, or null if not found.", "input_string");
			dfn.setFactory(new FunctionFactory() {
				
				@Override
				public Function createFunction(Function... children) {
					return new FmPostcodeUk(level, children[0]);
				}
			});
			dfn.setGroup("postcodeUK");
							
		}
		
		// min / max
		class MinMaxHelper {
			void build(final Class<? extends Function> cls, final String name, String description) {
				FunctionDefinition dfn = new FunctionDefinition(name);
				dfn.addVarArgs("values", ArgumentType.GENERAL, "");
				dfn.setFactory(new FunctionFactory() {

					@Override
					public Function createFunction(Function... args) {
						try {
							Constructor<? extends Function> constructor = cls.getConstructor(Function[].class);
							return constructor.newInstance((Object) args);
						} catch (Throwable e) {
							throw new RuntimeException(e);
						}
					}
				});
				dfn.setDescription(description);
				add(dfn);
			}
		}
		MinMaxHelper minMaxHelper = new MinMaxHelper();
		minMaxHelper.build(FmMax.class, "max", "Return the maximum of the input arguments.");
		minMaxHelper.build(FmMin.class, "min", "Return the minimum of the input arguments.");

		// random data
		class RandDataHelper{
			void build( final String name, String description,final RandDataType type) {
				FunctionDefinition dfn = new FunctionDefinition(name);
				dfn.setFactory(new FunctionFactory() {

					@Override
					public Function createFunction(Function... args) {
						try {
							return new FmRandData(type);
						} catch (Throwable e) {
							throw new RuntimeException(e);
						}
					}
				});
				dfn.setDescription(description);
				add(dfn);
			}	
		}
		RandDataHelper rDataHelper = new RandDataHelper();
		rDataHelper.build("randPersonName", "Randomly generate a person's name", RandDataType.PERSON_NAME);
		rDataHelper.build("randCompanyName", "Randomly generate a company's name", RandDataType.COMPANY_NAME);
		rDataHelper.build("randStreetName", "Randomly generate a street name", RandDataType.STREET_NAME);
		
		// first non null
		FunctionDefinition firstNonNull = new FunctionDefinition("firstNonNull");
		firstNonNull.addVarArgs("values", ArgumentType.GENERAL, "");
		firstNonNull.setFactory(new FunctionFactory() {

			@Override
			public Function createFunction(Function... args) {
				return new FmFirstNonNull(args);
			}
		});
		firstNonNull.setDescription("Return the first non-null value");
		add(firstNonNull);
		
		// switch function
		FunctionDefinition switchDfn = new FunctionDefinition("switch");
		switchDfn.addVarArgs("expression, value1, result1, value2, result2, .... valueN, resultN, ... else", ArgumentType.GENERAL, "");
		switchDfn.setDescription("Do a switch over the expression returning the result corresponding to the matching value or the else term at the end if provided.");
		switchDfn.setFactory(new FunctionFactory() {
			
			@Override
			public Function createFunction(Function... children) {
				if(children.length < 3){
					throw new RuntimeException("Switch function must have at least 3 arguments.");
				}
				return new FmSwitch(children);
			}
		});
		add(switchDfn);
		
		// string functions
		addStandardFunction(FmLegendRangeText.class, "legendrangetext", "Create a text description for a value according to the bin it sits in", "minValue","maxValue","nbBins","thisValue");
		addStandardFunction(FmUpper.class, "upper", "Convert string to upper case.", "string_value");
		addStandardFunction(FmLower.class, "lower",  "Convert string to lower case.","string_value");
		addStandardFunction(FmCreateUUID.class, "createUUID",  "Create a type 3 (name based) UUID using the input string.","string_value");
		addStandardFunction(FmLen.class, "len", "Get length of the string or 0 if string is null.","string_value");
		addStandardFunction(FmLeft.class, "left","", "text", "number_of_chars");
		addStandardFunction(FmContains.class, "contains","", "find_string", "find_within_string");
		addStandardFunction(FmIndexOf.class, "indexof","", "find_string", "find_within_string");
		addStandardFunction(FmReplace.class, "replace","", "find_within_string", "old_string", "new_string");
		addStandardFunction(FmPostcodeUKFormatUnit.class, "postcodeukreformatunit", "Format a UK unit postcode, ensuring a single space between the two parts.", "raw_postcode").setGroup("postcodeUK");
		addStandardFunction(FmRegExpMatches.class, "regexpmatch", "Return true if the string matched the regular expression.", "regular-expression" , "string");
	//	addStandardFunction(FmRegExpMatchedText.class, "regexpmatchedstring", "Return the string which matched the regular expression or null if no match.", "regular-expression" , "string");
		addStandardFunction(FmRegExpMatchedGroup.class, "regexpmatchedgroup", "Return the ith group in the regular expression, assuming it matched, or null if no match. This uses a zero-based index.", "regular-expression" , "string", "group_index");
		FunctionDefinition formatDfn = new FunctionDefinition(FunctionType.FUNCTION, "stringformat");
		formatDfn.addArg("FormatString");
		formatDfn.addVarArgs("Args", ArgumentType.GENERAL, "Format arguments");
		formatDfn.setDescription("String formatter equivalent to Java's String.Format.");
		formatDfn.setFactory(new FunctionFactory() {
			
			@Override
			public Function createFunction(Function... children) {
				return new FmStringFormat(children[0], Arrays.copyOfRange(children, 1, children.length));
			}
		});
		add(formatDfn);
		addStandardFunction(FmStringDateTimeStamp.class, "Timestamp", "Creates a string timestamp suitable for use in filenames.");
		
		// decimal format
		FunctionDefinition dfn = new FunctionDefinition(FunctionType.FUNCTION, "decimalformat");
		dfn.setDescription("Format a double number using the input formatting pattern, which is the same as Java's DecimalFormat class");
		dfn.addArg("format_string", ArgumentType.STRING_CONSTANT);
		dfn.addArg("number");
		dfn.setFactory(createReflectionFactory(FmDecimalFormat.class, "decimalformat"));
		add(dfn);
		
		// constants
		addConstant("true", new FmConst(1L), null);
		addConstant("false", new FmConst(0L), null);
		addConstant("pi", new FmConst(Math.PI), null);
		addConstant("e", new FmConst(Math.E), null);
		addConstant("null", new FmConst((Object)null), null);
		
		// map flag labels
		addConstant("mfAlwaysShowLabel", new FmConst(UserRenderFlags.ALWAYS_SHOW_LABEL), "Flag which forces the map to always show a label even if it overlaps others.");
		addConstant("mfDotDashLine", new FmConst(UserRenderFlags.DOT_DASH_LINE), "Flag which forces lines to be drawn with dots and dashes _ . _ . _ .");
		addConstant("mfDottedLine", new FmConst(UserRenderFlags.DOTTED_LINE), "Flag which forces lines to be drawn with dots. . . . . .");
		
		for(Map.Entry<String, Color> entry : Colours.getStandardColoursMap().entrySet()){
			addConstant(entry.getKey(),new FmConst( entry.getValue()), "Standard colour").setGroup("colours");
		}
		
	//	addTagConstants();
		return this;
	}

//	private void addTagConstants(){
//		for(Field field : PredefinedTags.class.getDeclaredFields()){
//			if(field.getAnnotation(PredefinedTags.ODLConstFunction.class)!=null){
//				String name = field.getName().toLowerCase();
//				try {
//					String value = (String)field.get(null);
//					addConstant(name, new FmConst(value), "Predefined tag, equal to the string \"" + value + "\".").setGroup("predefined tags");
//				} catch (Exception e) {
//				
//				}
//			}
//		}
//	}
	
	private void addStandardOperator(Class<? extends Function> cls, String symbol, String description) {
		addOperator(createReflectionFactory(cls, symbol), symbol,description);
	}


	
	private void addOperator(FunctionFactory factory, String symbol, String description) {
		FunctionDefinition dfn = new FunctionDefinition(FunctionType.OPERATOR, symbol);
		dfn.setOperatorPrecendence(nextOperatorPrecedence++);
		dfn.setFactory(factory);
		dfn.setDescription(description);
		
		// operators always have 2 arguments...
		for(int i = 1 ; i<=2 ; i++){
			dfn.addArg("value" + Integer.toString(i));
		}
		add(dfn);
	}

	public FunctionDefinition identifyOperator(final String name) {
		final List<FunctionDefinition> list = map.get(name);
		if (list == null) {
			return null;
		}
		for(FunctionDefinition dfn:list){
			if(dfn.getType() == FunctionType.OPERATOR){
				return dfn;
			}
		}
		return null;
	}
	
	public FunctionFactory identify(final String name, final FunctionType type) {
		// Get the list of function definitions with this name
		final List<FunctionDefinition> list = map.get(name);
		if (list == null) {
			return null;
		}
		
		// If we have one or more functions then return a functionfactory which chooses the one with the correct number of parameters 
		return new FunctionFactory() {

			@Override
			public Function createFunction(Function... children) {
				// find function with correct number of arguments
				int nbCorrectType = 0;
				for (FunctionDefinition dfn : list) {
					if (dfn.getType() == type) {
						nbCorrectType++;
						if (children.length == dfn.nbArgs()) {
							
							// check each argument
							for(int i =0 ; i<children.length ; i++){
								FunctionArgument arg = dfn.getArg(i);
								
								// check constant strings are correct
								if(arg.isConstantString() && FunctionUtils.getConstantString(children[i])==null){
									throw new RuntimeException("Argument " + (i+1) + " passed into function " + name + " is not a constant string.");
								}
								
								// check table references are formatted correctly (a table ref is a type of constant string)
								if(arg.getArgumentType() == ArgumentType.TABLE_REFERENCE_CONSTANT){
									ExecutionReportImpl res = new ExecutionReportImpl();
									String s = FunctionUtils.getConstantString(children[i]);
									if(TableReference.create(s, res)==null){
										throw new RuntimeException(res.getReportString(false,true));
									}
								}
							}
				
							// every is OK
							return dfn.getFactory().createFunction(children);
							
						} else if (dfn.hasVarArgs()) {
							// we assume a function with variable arguments is *entirely* variable arguments..
							return dfn.getFactory().createFunction(children);
						}

					}
				}

				if (nbCorrectType > 0) {
					throw new RuntimeException("Incorrect number of arguments passed into function: " + Strings.std(name));
				}
				return null;
			}
		};
	}

	/**
	 * Add a standard function. A standard function can be instantiated using reflection
	 * with a constructor taking a fixed number of Function objects.
	 * @param cls
	 * @param name
	 * @param argNames
	 */
	public FunctionDefinition addStandardFunction(Class<? extends Function> cls, String name,String description, String... argNames) {
		FunctionDefinition dfn = new FunctionDefinition(FunctionType.FUNCTION, name);
		dfn.setDescription(description);
		for (String argName : argNames) {
			dfn.addArg(argName);
		}
		dfn.setFactory(createReflectionFactory(cls, name));
		add(dfn);
		return dfn;
	}

	private FunctionDefinition addConstant(final String name, final FmConst val, final String description) {
		FunctionDefinition dfn = new FunctionDefinition(FunctionType.CONSTANT, name);
		if(description!=null){
			dfn.setDescription(description);
		}else{
			dfn.setDescription("Constant value equal to " + val.toString());			
		}
		dfn.setFactory(new FunctionFactory() {

			@Override
			public Function createFunction(Function... children) {
				return val.deepCopy();
			}
		});
		add(dfn);
		return dfn;
	}

	private FunctionFactory createReflectionFactory(final Class<? extends Function> cls, final String name) {
		return new FunctionFactory() {

			@Override
			public Function createFunction(Function... args) {
				try {
					// create arrays holding arg classes and args cast to objects
					Class<?>[] constructorArgsCls = new Class<?>[args.length];
					Object[] oargs = new Object[args.length];
					for (int i = 0; i < args.length; i++) {
						constructorArgsCls[i] = Function.class;
						oargs[i] = args[i];
					}

					// get constructor
					Constructor<?> constructor = cls.getConstructor(constructorArgsCls);
					Function formula = (Function) constructor.newInstance(oargs);
					return formula;

				} catch (Throwable e) {
					throwIncorrectNbArgs(name);
					return null;
				}
			}

		};
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (FunctionType type : FunctionType.values()) {
			for (List<FunctionDefinition> list : map.values()) {
				for (FunctionDefinition dfn : list) {
					if (dfn.getType() == type) {
						builder.append(dfn.toString() + System.lineSeparator());
					}
				}
			}
		}
		return builder.toString();
	}

	/**
	 * Some of the operator formula classes can take variable arguments BUT we currently only allow the user formula to pass 2 parameters. This method
	 * deals with this case.
	 * 
	 * @param cls
	 * @return
	 */
	private void addVargsOperator(final Class<? extends Function> cls, final String symbol, String description) {
		addOperator(new FunctionFactory() {

			@Override
			public Function createFunction(Function... args) {
				if (args.length != 2) {
					throw new RuntimeException("Operator requires 2 arguments: " + symbol);
				}

				try {
					Constructor<? extends Function> constructor = cls.getConstructor(Function[].class);
					return constructor.newInstance((Object) args);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			}
		}, symbol,description);
	}

	private void throwIncorrectNbArgs(final String name) {
		throw new RuntimeException("Could not instantiate function " + name + ". Is the number of arguments correct?");
	}
	
	/**
	 * Get all definitions in a list, ordered by type first
	 * @return
	 */
	public List<FunctionDefinition> toList(){
		ArrayList<FunctionDefinition> ret= new ArrayList<>();
		
		for (FunctionType type : FunctionType.values()) {
			for (List<FunctionDefinition> list : map.values()) {
				for (FunctionDefinition dfn : list) {
					if (dfn.getType() == type) {
						ret.add(dfn);
					}
				}
			}
		}
	
		return ret;
	}

}
