/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * API providing access to common logic for column naming, adapter naming etc
 * @author Phil
 *
 */
public interface StringConventions {

	String getVehicleName(String typeName, int numberOfVehiclesInType, int vehicleIndex);
	
	String getVehicleId(String typeId, int numberOfVehiclesInType, int vehicleIndex);
	
	Integer getVehicleIndex(String vehicleId, String vehicleTypeId);
	
	/**
	 * Return the adapter id which corresponds to the loaded spreadsheet
	 * @return
	 */
	String getSpreadsheetAdapterId();
	
	/**
	 * Returns true if string is null or has zero length
	 * @param s
	 * @return
	 */
	boolean isEmptyString(String s);

	/**
	 * Is the input string empty when standardised?
	 * @param s
	 * @return
	 */
	boolean isEmptyStandardised(String s);
	
	/**
	 * Returns a standardised version of the string used for all comparisons by ODL.
	 * This is lowercase with all trailing whitespaces removed.
	 * @param s
	 * @return
	 */
	String standardise(String s);
	
	/**
	 * Test whether 2 strings are equal when in the standard form
	 * @param a
	 * @param b
	 * @return
	 */
	boolean equalStandardised(String a, String b);
	
	
	/**
	 * Standardised comparison of two strings. 
	 * The comparison compares the standardised version of the two strings.
	 * It also handles the situation where you have a word followed by a number,
	 * e.g. "vehicle 9", "vehicle 11", and applies numeric sorting to the number part. 
	 * @param a
	 * @param b
	 * @return
	 */
	int compareStandardised(String a, String b);
	
	/**
	 * Create a map where all strings are standardised by default.
	 * Not all map methods are supported.
	 * Strings are ordered using the natural ordering in the standardised form.
	 * @return
	 */
	<T> Map<String, T> createStandardisedMap();
	
	/**
	 * Create a map where all strings are standardised by default.
	 * Not all map methods are supported.
	 * Strings are ordered using the natural ordering in the standardised form.
	 * 
	 * @param factory If non-null, then get on an empty record or will cause
	 * a record to be created using the factory to populate the value. 
	 * @return
	 */
	<T> Map<String, T> createStandardisedMap(Factory<T> factory);
	
	/**
	 * Create a set where all strings are standardised by default.
	 * Not all set methods are supported.
	 * Strings are ordered using the natural ordering in the standardised form.
	 * @return
	 */
	Set<String> createStandardisedSet();
	
	List<String> tokenise(String s);
	
	/**
	 * Get a multi-line string containing message from the exception
	 * and whatever exceptions cause it.
	 * @param t
	 * @return
	 */
	String getExceptionReport(Throwable t);

	
	/**
	 * Split a string into commas preserving strings in quotations which contain commas.
	 * Warning - this method is slow!
	 * @param s
	 * @return
	 */
	List<String> splitCommas(String s);
}

