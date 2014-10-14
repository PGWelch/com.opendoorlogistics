/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;

import com.opendoorlogistics.core.utils.Numbers;

/**
 * Class for the app-wide properties
 * @author Phil
 *
 */
public class AppProperties {
	private static Properties applicationProperties;
	
	static{
		try {
			File file = new File(AppConstants.ODL_PROPERTIES_FILES);
			if(file.exists()){
				applicationProperties = new Properties();
				FileInputStream in = new FileInputStream(file);
				applicationProperties.load(in);
				in.close();									
				System.out.println("Loaded properties file: " + file.getAbsolutePath());
				for(Map.Entry<Object,Object> entry:applicationProperties.entrySet()){
					System.out.println("\t" + entry.getKey() + "=" + entry.getValue());
				}
			}else{
				System.out.println("No properties file found at " + file.getAbsolutePath());				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Properties get(){
		return applicationProperties;
	}

	public static final String SPATIAL_KEY = "spatial";
	
	public static final String SPATIAL_RENDERER_KEY = SPATIAL_KEY + ".renderer";

	public static final String SPATIAL_RENDERER_SIMPLIFY_DISTANCE_TOLERANCE_PIXELS = SPATIAL_RENDERER_KEY+ ".simplify_distance_tolerance_pixels";
	
	
	public static Double getDouble(String key){
		if(applicationProperties!=null){
			Object val = applicationProperties.get(key);
			if(val!=null){
				return Numbers.toDouble(val);
			}
		}
		return null;
	}

}
