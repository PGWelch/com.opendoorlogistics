package com.opendoorlogistics.api.app;

/**
 * Preferences are persisted unlike properties
 * @author Phil
 *
 */
public interface ODLAppPreferences {
	/**
	 * Key should be app-wide unique
	 * @param key
	 * @return
	 */
	String get(String key);
	
	/**
	 * Key should be app-wide unique
	 * @param key
	 * @param value
	 */
	void put(String key, String value);
}
