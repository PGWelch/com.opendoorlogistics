/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts;

import com.opendoorlogistics.core.utils.strings.Strings;

final public class ScriptConstants {
	private ScriptConstants(){}
	
	public static final String EXTERNAL_DS_NAME = "external";

	public static final String INTERNAL_GLOBAL_DS = "scriptglobal";
	
	public static final String SCRIPT_EMBEDDED_TABLE_DATA_DS = "embeddedData";

//	public static final String IMPORT_LINK_POSTFIX =":\\";
	
//	public static final String SHAPEFILE_DS_NAME_PREFIX = "shapefile" + IMPORT_LINK_POSTFIX;

	public static final String FILE_EXT = "odlx";

	public static final String DIRECTORY = "scripts";

	public static final String SCRIPT_XML_NODE_NAME = "Script";

	public static final int SCRIPT_VERSION_MAJOR = 1;
	
	public static final int SCRIPT_VERSION_MINOR = 0;

	public static final int SCRIPT_VERSION_REVISION = 0;

	public static boolean isExternalDs(String s){
		return Strings.equalsStd(s, EXTERNAL_DS_NAME);
	}
	
	public static final String FORMULA_PREFIX = ":=";
}
