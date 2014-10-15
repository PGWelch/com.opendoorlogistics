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
import java.io.InputStream;
import java.net.URL;
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
		applicationProperties = new Properties();
		loadEmbedded(applicationProperties);
		loadExternal(applicationProperties);
		for(Map.Entry<Object,Object> entry:applicationProperties.entrySet()){
			System.out.println("\t" + entry.getKey() + "=" + entry.getValue());
		}		
	}

	public static Properties get(){
		return applicationProperties;
	}

	public static final String SPATIAL_KEY = "spatial";
	
	public static final String SPATIAL_RENDERER_KEY = SPATIAL_KEY + ".renderer";

	public static final String SPATIAL_RENDERER_SIMPLIFY_DISTANCE_TOLERANCE = SPATIAL_RENDERER_KEY+ ".simplify_distance_tolerance";
	
	public static final String SPATIAL_RENDERER_SIMPLIFY_DISTANCE_TOLERANCE_LINESTRING = SPATIAL_RENDERER_SIMPLIFY_DISTANCE_TOLERANCE+ ".linestring";

	public static Double getDouble(String key){
		if(applicationProperties!=null){
			Object val = applicationProperties.get(key);
			if(val!=null){
				return Numbers.toDouble(val);
			}
		}
		return null;
	}

	public static Double getDouble(String key, double defaultValueIfKeyMissing){
		Double ret = getDouble(key);
		if(ret==null){
			ret = defaultValueIfKeyMissing;
		}
		return ret;
	}
	
	private static void loadEmbedded(Properties addTo){
		InputStream stream =null;
		try {
			stream = Object.class.getResourceAsStream(AppConstants.ODL_EMBEDED_PROPERTIES_FILE);
			addTo.load(stream);
			System.out.println("Loaded embededd properties.");
		} catch (Exception e) {
		}finally{
			if(stream!=null){
				try {
					stream.close();					
				} catch (Exception e2) {
				}
			}
		}
	}
	
	private static void loadExternal(Properties addTo){
		File file = new File(AppConstants.ODL_EXTERNAL_PROPERTIES_FILE);
		if(file.exists()){
			FileInputStream in=null;
			try {
				in = new FileInputStream(file);
				addTo.load(in);
				System.out.println("Loaded properties file: " + file.getAbsolutePath());				
			} catch (Exception e) {
			}finally{
				if(in!=null){
					try {
						in.close();						
					} catch (Exception e2) {
					}
				}
			}
		}

	}
}
