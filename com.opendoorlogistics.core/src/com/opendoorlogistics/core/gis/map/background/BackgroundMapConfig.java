/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.background;

import java.awt.Color;
import java.util.Properties;

import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;

class BackgroundMapConfig {
	private Color fade;
	private String tileserverUrl;
	private BackgroundType type;
	private String mapsforgeFilename;

	public BackgroundMapConfig() {
		fade = new Color(255, 255, 255, 100);
		tileserverUrl = "http://tile.openstreetmap.org";
		type = BackgroundType.OSMTILESERVER;
	}

	public BackgroundMapConfig(Properties properties) {
		// call this to fill in standard values
		this();

		if (properties != null) {
			StandardisedStringTreeMap<String> map = StandardisedStringTreeMap.fromProperties(properties);
			fade = new Color(getInt(map, "fade.r", fade.getRed()), getInt(map, "fade.g", fade.getGreen()), getInt(map, "fade.b", fade.getBlue()), getInt(map, "fade.a", fade.getAlpha()));

			tileserverUrl = getStr(map, "tileserver.url", tileserverUrl);

			// get mapsforge filename and remove any speech marks
			String tmp  = getStr(map, "mapsforge.file", mapsforgeFilename);
			if(tmp!=null){
				tmp=tmp.replaceAll("\"", "");
			}
			mapsforgeFilename = tmp;

			String typeString = getStr(map, "type", type.name());
			for (BackgroundType t : BackgroundType.values()) {
				if (Strings.equalsStd(t.name(), typeString)) {
					type = t;
				}
			}
		}
	}

	private static String getStr(StandardisedStringTreeMap<String> map, String key, String defaultValue) {
		String ret = map.get(key);
		if (ret == null) {
			return defaultValue;
		}
		return ret;
	}

	private static int getInt(StandardisedStringTreeMap<String> map, String key, int defaultValue) {
		Long l = Numbers.toLong(map.get(key));
		if (l != null) {
			return l.intValue();
		}
		return defaultValue;
	}

	public enum BackgroundType {
		EMPTY, OSMTILESERVER, MAPSFORGE
	}

	public String getTileserverUrl() {
		return tileserverUrl;
	}

	public void setTileserverUrl(String tileserverUrl) {
		this.tileserverUrl = tileserverUrl;
	}

	public BackgroundType getType() {
		return type;
	}

	public void setType(BackgroundType type) {
		this.type = type;
	}

	public String getMapsforgeFilename() {
		return mapsforgeFilename;
	}

	public void setMapsforgeFilename(String mapsforgeFilename) {
		this.mapsforgeFilename = mapsforgeFilename;
	}

	public Color getFade() {
		return fade;
	}

	public void setFade(Color fade) {
		this.fade = fade;
	}

}
