/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core;

import java.io.File;

import com.opendoorlogistics.core.utils.Version;


final public class AppConstants {
	public static final String ORG_NAME = "Open Door Logistics";

	public static final String WEBSITE = "www.opendoorlogistics.com";
	
	public static final int APP_VERSION_MAJOR = 1;
	
	public static final int APP_VERSION_MINOR = 4;

	public static final int APP_VERSION_REVISION = 1;

	public static final String DATA_DIRECTORY = "data" + File.separator;
			
	public static final String SHAPEFILES_DIRECTORY = DATA_DIRECTORY + "shapefiles" + File.separator;

	public static final String EXTERNAL_MATRIX_DIRECTORY = DATA_DIRECTORY + "travelmatrices" + File.separator;

	public static final String GRAPHHOPPER_DIRECTORY = DATA_DIRECTORY + "graphhopper" + File.separator;

	public static final String MAPSFORGE_DIRECTORY = DATA_DIRECTORY + "mapsforge" + File.separator;

	public static final String OSM_COPYRIGHT = "© OpenStreetMap contributors";
	
	public static final String EXTERNAL_MATRIX_TEXTFILE_EXTENSION = ".matrix.txt";
	
	// Don't use File.separator when loading resources as its wrong!
	public static final String ODL_EMBEDED_PROPERTIES_FILE = "/resources/odl-defaults.properties";

	public static final String ODL_CONFIG_DIR = "config" + File.separator;

	public static final String SCRIPTS_DIRECTORY = "scripts" + File.separator;

	public static final String ODL_EXTERNAL_PROPERTIES_FILE =ODL_CONFIG_DIR +  "odl.properties";

	public static final String ODL_BACKGROUND_MAP_PROPERTIES_FILE =ODL_CONFIG_DIR + "backgroundmap.properties";

	public static final String ODL_LOGGING_CONFIG =ODL_CONFIG_DIR + "logging.properties";

	public static final Version getAppVersion(){
		return new Version(APP_VERSION_MAJOR,APP_VERSION_MINOR,APP_VERSION_REVISION);
	}
	
}
