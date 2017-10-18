package com.opendoorlogistics.api.app;

import java.util.Properties;
import java.util.Set;

/**
 * Interface for the app-wide properties.
 * Properties are accessed by string, where all lookups use standardised strings,
 * i.e. they are insensitive to case and whitespace at start and end of key.
 * Properties are loaded from config files and may be modified in memory
 * but the modication is not persisted
 * @author Phil
 *
 */
public interface ODLAppProperties {
	Double getDouble(String key);
	Double getDouble(String key, double defaultValueIfKeyMissing);
	String getString(String key);
	void add(Properties properties);
	
	/**
	 * 
	 * @param key
	 * @return Boolean or null if key not found or not boolean
	 */
	Boolean getBool(String key);
	
	/**
	 * Returns true of the key is found, is boolean and the boolean is true
	 * @param key
	 * @return
	 */
	boolean isTrue(String key);
	
	Set<String> getKeys();
	void put(String key, Object value);
}
