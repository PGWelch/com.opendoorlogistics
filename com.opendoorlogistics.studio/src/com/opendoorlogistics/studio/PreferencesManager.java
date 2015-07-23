/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.studio.utils.WindowState;

final public class PreferencesManager {
	private static final PreferencesManager singleton = new PreferencesManager();
	private static final int MAX_NB_RECENT_FILES = 10;

	private final Preferences userPreferences = Preferences.userNodeForPackage(getClass());

	public synchronized File getLastImportFile(ImportFileType fileType) {
		String s = userPreferences.get("last-" + fileType.name(), null);
		if (s == null) {
			return null;
		}
		return new File(s);
	}

	public Preferences node(String id){
		return userPreferences.node(id);
	}
	
	public synchronized void setLastImportFile(File file, ImportFileType fileType) {
		userPreferences.put("last-" + fileType.name(), file.getAbsolutePath());
	}

	public enum PrefKey {
		LAST_JRXML_TO_COMPILE(null),SCRIPTS_DIR(null), REPORT_TEMPLATES_DIR(null), LAST_IO_DIR(null), LAST_GRID_VIEW_JRXML(null);
		// private final PrefKeyType type;
		private final String defaultVal;

		private PrefKey(String defaultVal) {
			this.defaultVal = defaultVal;
		}

	}

	// public enum PrefKeyType{
	// FILE,
	// DIRECTORY,
	// }

	public String get(PrefKey key) {
		return userPreferences.get(key.name(), key.defaultVal);
	}

	public File getFile(PrefKey key) {
		String s = get(key);
		if (s != null) {
			return new File(s);
		}
		return null;
	}

	public void set(PrefKey key, String value) {
		if (value != null) {
			userPreferences.put(key.name(), value);
		} else {
			userPreferences.remove(key.name());
		}
	}

	public void setFile(PrefKey key, File file) {
		set(key, file != null ? file.getAbsolutePath() : null);
	}

	// public synchronized File getLastCustomJRXMLFile() {
	// String s= userPreferences.get("lastjrxml", "Reports");
	// if(s==null){
	// return null;
	// }
	// return new File(s);
	// }
	//
	// public synchronized void setLastCustomJRXMLFile(File file){
	// userPreferences.put("lastjrxml", file.getAbsolutePath());
	// }

	// public synchronized File getLastIODirectory() {
	// String s = userPreferences.get("lastIODir", null);
	// if(s!=null){
	// return new File(s);
	// }
	// return null;
	// }

	public synchronized void setDirectory(PrefKey keyType, File file) {
		if (file.isDirectory() == false) {
			file = file.getParentFile();
		}
		setFile(keyType, file);
	}

	public synchronized File getScriptsDirectory() {
		File ret = getFile(PrefKey.SCRIPTS_DIR);
		if (ret != null && ret.exists() && ret.isDirectory()) {
			return ret;
		}
		return new File(ScriptConstants.DIRECTORY);
	}

	// public synchronized File getReportTemplatesDirectory() {
	// String dir = userPreferences.get("reportsTemplates", "");
	//
	// if (dir != null) {
	// return new File(dir);
	// }
	// return null;
	// }

	// public synchronized void setReportTemplatesDirectory(File file) {
	// userPreferences.put("reportsTemplates", file.getAbsolutePath());
	// }

	// public synchronized void setScriptsDirectory(File file) {
	// userPreferences.put("lastscriptsdir", file.getAbsolutePath());
	// }

	public static PreferencesManager getSingleton() {
		return singleton;
	}

	public synchronized void setWindowState(String id, WindowState screenState) {
		// keys have a short max length, so use hashcode...
		id = Integer.toString(id.hashCode());

		String xml = screenState.toXMLString();
		userPreferences.put("windowstate_" + id, xml);
	}

	public synchronized WindowState getWindowState(String id) {
		// keys have a short max length, so use hashcode...
		id = Integer.toString(id.hashCode());

		String xml = userPreferences.get("windowstate_" + id, null);
		if (xml != null && xml.length() > 0) {
			return WindowState.fromXMLString(xml);
		}
		return null;
	}

	public synchronized void setScreenState(WindowState screenState) {
		setWindowState("screen", screenState);
	}

	public synchronized WindowState getScreenState() {
		return getWindowState("screen");
	}

	public synchronized void addRecentFile(File file) {
		file = file.getAbsoluteFile();
		List<File> recent = getRecentFiles();

		// take out if already in the list (all occurrences)
		while (recent.remove(file)) {
		}

		// add to top of list
		recent.add(0, file);

		while (recent.size() > MAX_NB_RECENT_FILES) {
			recent.remove(recent.size() - 1);
		}

		// turn into string form
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < recent.size(); i++) {
			if (i > 0) {
				builder.append(System.lineSeparator());
			}

			builder.append(recent.get(i).getAbsolutePath());

		}

		// save
		userPreferences.put("recentfiles", builder.toString());
	}

	public synchronized List<File> getRecentFiles() {
		String recent = userPreferences.get("recentfiles", null);
		ArrayList<File> ret = new ArrayList<>();
		if (recent != null) {
			String[] files = recent.split(System.lineSeparator());
			for (String s : files) {
				try {
					File file = new File(s);

					// validate...
					if (file.exists()) {
						boolean found = false;

						// de-duplication
						for (File other : ret) {
							if (other.equals(file)) {
								found = true;
								break;
							}
						}

						if (!found) {
							ret.add(file);
						}
					}
				} catch (Throwable e) {
				}
			}
		}
		//
		return ret;

	}

	public synchronized void clearRecentFiles() {
		userPreferences.put("recentfiles", "");
	}
	
	public Preferences get(){
		return userPreferences;
	}
}
