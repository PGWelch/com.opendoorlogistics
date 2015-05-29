/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.postcodes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * TO DO - Tidy this up...
 * 
 * Standard postcode strings don't include the extra digit used in some London
 * districts - e.g. district E1 contains 'district' E1W. All postcode
 * strings include this.
 * @author Phil
 *
 */
final public class UKPostcodes {
	public static final String area =  "[a-z][a-z]?";
	public static final String stdDistrict = area + "\\d\\d?";
	public static final String allDistrict = area + "\\d\\d?[a-z]?";
	public static final String std2ndPart = "\\d[a-z][a-z]";
//	public static final String stdSector = stdDistrict + "\\s+" + "\\d";
	public static final String allSector = allDistrict + "\\s+" + "\\d";
//	public static final String stdUnit= stdSector + "[a-z][a-z]";
	public static final String allUnit= allSector + "[a-z][a-z]";
	public static final Pattern sectorFromUnit = Pattern.compile("^(" + allSector + ")[a-z][a-z]$",Pattern.CASE_INSENSITIVE);
	public static final Pattern districtFromAnyLevelPC = Pattern.compile("^(" + allDistrict + ").*",Pattern.CASE_INSENSITIVE);
	public static final Pattern areaFromAnyLevelPC = Pattern.compile("^(" + area + ").*",Pattern.CASE_INSENSITIVE);
	public static final Pattern LondonExtraDigitFormatSectorLevel = Pattern.compile("^" + "("+ stdDistrict+  ")"+ "[a-z]\\s" + "(" + std2ndPart +")"+ "$",Pattern.CASE_INSENSITIVE);
        
	public static final Pattern isArea = Pattern.compile("^(" + area + ")$",Pattern.CASE_INSENSITIVE);
	public static final Pattern isDistrict = Pattern.compile("^(" + allDistrict + ")$",Pattern.CASE_INSENSITIVE);
	public static final Pattern isSector = Pattern.compile("^(" + allSector + ")$",Pattern.CASE_INSENSITIVE);
	public static final Pattern isUnit = Pattern.compile("^(" + allDistrict + ")\\s*(" + std2ndPart + ")$",Pattern.CASE_INSENSITIVE);
	
	/**
	 * Match a UK unit postcode only, with or without a space, with groups that give the before space and after space parts separately. 
	 */
	public static final Pattern unitWithWithoutSpaceGroupedForSpace = Pattern.compile("([a-z][a-z]?\\d\\d?[a-z]?)\\s*(\\d[a-z][a-z])", Pattern.CASE_INSENSITIVE);
	
	public static String standardisePostcode(String s, boolean removeExtraDistrictLetter){
		// run basic standardise
		s = Strings.std(s);

		// if its a UK unit postcode then ensure its got the space
		Matcher ukUnit = unitWithWithoutSpaceGroupedForSpace.matcher(s);
		if(ukUnit.matches()){
			s = ukUnit.group(1) + " " + ukUnit.group(2);
		}

		// remove the extra postcode digits used for some areas in London
		if(removeExtraDistrictLetter){
			Matcher special = UKPostcodes.LondonExtraDigitFormatSectorLevel.matcher(s);
			if(special.find()){
				s = special.group(1) + " " + special.group(2);
			}					
		}
		
		// ensure one space between the last 3 characters and the first, if its a unit pc
		Matcher unitMatcher= UKPostcodes.isUnit.matcher(s);
		if(unitMatcher.matches()){
			s = unitMatcher.group(1) + " " + unitMatcher.group(2);
		}
		
		// should be upper case
		s = s.toUpperCase();
		return s;
	}
	
	public enum UKPostcodeLevel{
		Area,
		District,
		Sector,
		Unit
	}
	
	public static void main(String[]args){
		
		for(String s : new String[]{"LE119FZ" , "LE11    9FZ" , "B28BQ" , "B2A8TG"}){
			Matcher matcher = UKPostcodes.unitWithWithoutSpaceGroupedForSpace.matcher(s);
			if(matcher.matches()){
				System.out.println(s + " -> " + matcher.group(1) + " " + matcher.group(2));			
			}else{
				System.out.println(s + " -> no match");
			}	
		}

	}
}
