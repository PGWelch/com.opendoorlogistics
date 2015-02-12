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
	
	public static final int APP_VERSION_MINOR = 3;

	public static final int APP_VERSION_REVISION = 2;
	
	public static final String SHAPEFILES_DIRECTORY = "data" + File.separator + "shapefiles" + File.separator;

	public static final String GRAPHHOPPER_DIRECTORY = "data" + File.separator + "graphhopper" + File.separator;

	public static final String MAPSFORGE_DIRECTORY = "data" + File.separator + "mapsforge" + File.separator;

	public static final String OSM_COPYRIGHT = "© OpenStreetMap contributors";
	
	
	// Don't use File.separator when loading resources as its wrong!
	public static final String ODL_EMBEDED_PROPERTIES_FILE = "/resources/odl-defaults.properties";

	public static final String ODL_CONFIG_DIR = "config" + File.separator;

	public static final String ODL_EXTERNAL_PROPERTIES_FILE =ODL_CONFIG_DIR +  "odl.properties";

	public static final String ODL_BACKGROUND_MAP_PROPERTIES_FILE =ODL_CONFIG_DIR + "backgroundmap.properties";

	public static final Version getAppVersion(){
		return new Version(APP_VERSION_MAJOR,APP_VERSION_MINOR,APP_VERSION_REVISION);
	}
	
}
