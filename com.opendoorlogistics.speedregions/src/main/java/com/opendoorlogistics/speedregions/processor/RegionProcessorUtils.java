/*
 * Copyright 2016 Open Door Logistics Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opendoorlogistics.speedregions.processor;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Miscellaneous utils used to create the region lookup
 * @author Phil
 *
 */
public class RegionProcessorUtils {
	private static final double EARTH_RADIUS_METRES = 6371000;
	private static final ObjectMapper JACKSON_MAPPER;
	static {
		JACKSON_MAPPER = new ObjectMapper();
		JACKSON_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		JACKSON_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public static void toJSONFile(Object o, File file){
		String json = toJSON(o);
		try {
			internalStringToFile(file, json);			
		} catch (Exception e) {
			throw asUncheckedException(e);
		}
	}

	private static void internalStringToFile(File file, String s) throws FileNotFoundException {
		try(  PrintWriter out = new PrintWriter(file )  ){
		    out.println( s );
		}
	}
	
	public static String toJSON(Object o) {
		StringWriter writer = new StringWriter();
		try {
			JACKSON_MAPPER.writeValue(writer, o);
		} catch (Exception e) {
			throw asUncheckedException(e);
		}
		return writer.toString();
	}
	
	public static RuntimeException asUncheckedException(Throwable e){
		if(RuntimeException.class.isInstance(e)){
			return (RuntimeException)e;
		}
		return new RuntimeException(e);
	}
	

	public static <T> T fromJSON(File textFile, Class<T> cls) {
		return fromJSON(readTextFile(textFile), cls);
	}
	
	public static <T> T fromJSON(String json, Class<T> cls) {

		try {
			return JACKSON_MAPPER.readValue(json, cls);
		} catch (Exception e) {
			
			throw asUncheckedException(e);
		}

	}
	
	public static String stdString(String s){
		if(s==null){
			return "";
		}
		return s.toLowerCase().trim();
	}

	public static String readTextFile(File file){
		try {
			return internalReadTextFile(file);
		} catch (Exception e) {
			throw asUncheckedException(e);
		}
	}
	
	private static String internalReadTextFile(File file)throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(file));
		try {
		    StringBuilder builder = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        builder.append(line);
		        builder.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    return builder.toString();
		} finally {
		    br.close();
		}
		
	}
	
	
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
	
	/**
	 * We may need to change the precision model in the geometry factory later-on,
	 * so we keep the creation of a geometry factory in one place
	 * @return
	 */
	public static GeometryFactory newGeomFactory() {
		GeometryFactory factory = new GeometryFactory();
		return factory;
	}
	
}
