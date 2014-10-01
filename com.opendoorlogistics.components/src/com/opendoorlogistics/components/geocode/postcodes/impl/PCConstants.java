/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes.impl;

import com.opendoorlogistics.core.utils.Version;

final public class PCConstants {
	private PCConstants(){}
	
	public static final String DBNAME_INT2ST = "int2str";
	public static final String DBNAME_PCS = "pcs";
	public static final String DBFILE_EXTENSION = "gdf";
	public static final String PC_SPREADSHEET_FIELD_NAME = "postcode";
	
	public static String IS_MATCHED_YES = "yes";
	
	public static String IS_MATCHED_NO = "no";

	public static String TEMP_FILE_EXTENSION = ".tmp";
	
	public static String DBNAME_COUNTRYCODE = "countrycode";
	
	public static String DBNAME_VERSION = "version";
	
	public static final Version pcgeocode_file_version = new Version(0, 0, 1);

	static final String GEOCODED_COUNT = "Number of rows geocoded"; 
	static final String NOT_GEOCODED_COUNT = "Number of rows not geocoded"; 
}
