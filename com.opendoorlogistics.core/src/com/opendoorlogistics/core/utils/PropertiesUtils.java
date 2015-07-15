/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.io.File;
import java.io.StringReader;
import java.util.Properties;
import java.util.logging.Logger;

import com.opendoorlogistics.core.utils.strings.Strings;

public class PropertiesUtils {
	private static final Logger logger = Logger.getLogger(PropertiesUtils.class.getName());
	
	public static Properties loadFromFile( File file){
		Properties p = new Properties();
		loadFromFile(file, p);
		return p;
	}
	
	public static void loadFromFile(File file, Properties addTo){
		if(file.exists()){
			
			// see http://stackoverflow.com/questions/5784895/java-properties-backslash
			String propertyFileContents = Strings.readFile(file.getAbsolutePath());
			try {
				addTo.load(new StringReader(propertyFileContents.replace("\\","\\\\")));				
				logger.info("Loaded properties file: " + file.getAbsolutePath());				
			} catch (Exception e) {
			}

		}

	}
}
