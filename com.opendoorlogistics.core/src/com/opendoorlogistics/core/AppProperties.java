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
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.PropertiesUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * Class for the app-wide properties
 * @author Phil
 *
 */
public class AppProperties  {
	private static final Logger logger = Logger.getLogger(AppProperties.class.getName());
	
	private static Properties applicationProperties;
	
	static{
		applicationProperties = new Properties();
		loadEmbedded(applicationProperties);
		PropertiesUtils.loadFromFile(new File(AppConstants.ODL_EXTERNAL_PROPERTIES_FILE),applicationProperties);
		for(Map.Entry<Object,Object> entry:applicationProperties.entrySet()){
			logger.info("\t" + entry.getKey() + "=" + entry.getValue());
		}		
	}

//	public static Properties get(){
//		return applicationProperties;
//	}

	public static final String SPATIAL_KEY = "spatial";
	
	public static final String SPATIAL_RENDERER_KEY = SPATIAL_KEY + ".renderer";

	public static final String SPATIAL_RENDERER_SIMPLIFY_DISTANCE_TOLERANCE = SPATIAL_RENDERER_KEY+ ".simplify_distance_tolerance";
	
	public static final String SPATIAL_RENDERER_SIMPLIFY_DISTANCE_TOLERANCE_LINESTRING = SPATIAL_RENDERER_SIMPLIFY_DISTANCE_TOLERANCE+ ".linestring";

	public synchronized static Double getDouble(String key){
		if(applicationProperties!=null){
			Object val = getValue(key);
			if(val!=null){
				return Numbers.toDouble(val);
			}
		}
		return null;
	}

	public synchronized static Object getValue(String key){
		key = Strings.std(key);
		if(applicationProperties!=null){
			return applicationProperties.get(key);
		}
		return null;
	}
	
	public synchronized static Double getDouble(String key, double defaultValueIfKeyMissing){
		Double ret = getDouble(key);
		if(ret==null){
			ret = defaultValueIfKeyMissing;
		}
		return ret;
	}
	
	public synchronized static String getString(String key){
		if(applicationProperties!=null){
			Object val = getValue(key);
			if(val!=null){
				return val.toString();
			}
		}
		return null;
	}
	
	private synchronized static void loadEmbedded(Properties addTo){
		InputStream stream =null;
		try {
			// Use own class loader to prevent problems when jar loaded by reflection
			stream = AppProperties.class.getResourceAsStream(AppConstants.ODL_EMBEDED_PROPERTIES_FILE);
			Properties tmp = new Properties();
			tmp.load(stream);
			addTo(tmp, addTo);
			logger.info("Loaded embedded properties.");
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
	
	private synchronized static void addTo(Properties source, Properties addTo){
		for(String key:source.stringPropertyNames()){
			String std = Strings.std(key);
			Object val = source.get(key);
			addTo.put(std, val);
		}
	}
	
	public synchronized static void add(Properties properties){
		if(applicationProperties==null){
			applicationProperties = new Properties();
		}
		
		addTo(properties, applicationProperties);
	}
	
	public synchronized static void main(String []args){
		loadEmbedded(new Properties());
	}

	public synchronized static Boolean getBool(String key){

		String s =getString(key);
		if(s!=null){
			if(Strings.equalsStd("true", s) || Strings.equalsStd("1", s)){
				return true;
			}
			if(Strings.equalsStd("false", s)){
				return false;
			}
		}
		return null;
	}
	
	public synchronized static Set<String> getKeys(){
		return applicationProperties.stringPropertyNames();
	
	}
	
	public synchronized static void put(String key,Object value){
		applicationProperties.put(Strings.std(key), value);
	}
}
