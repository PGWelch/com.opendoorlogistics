package com.opendoorlogistics.core.api.impl;

import java.util.prefs.Preferences;

import com.opendoorlogistics.api.app.ODLAppPreferences;

public class ODLAppPreferencesImpl implements ODLAppPreferences {
	private static final String PREFKEY_PREFIX = "pref-";
	private static final Preferences USER_PREFERENCES = Preferences.userNodeForPackage(ODLAppPreferencesImpl.class);

	@Override
	public String get(String key) {
		String s = USER_PREFERENCES.get(PREFKEY_PREFIX+key,null);
		return s;
	}
	@Override
	public void put(String key, String value) {
		USER_PREFERENCES.put(PREFKEY_PREFIX+key, value);
	}

}
