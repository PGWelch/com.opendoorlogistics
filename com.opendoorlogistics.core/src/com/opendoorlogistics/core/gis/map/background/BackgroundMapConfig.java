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

public class BackgroundMapConfig {
	private static final Color DEFAULT_FADE_COLOUR =new Color(255, 255, 255, 100); 
	private FadeConfig fade = new FadeConfig(DEFAULT_FADE_COLOUR , 0);
	private String tileserverUrl;
	private BackgroundType type;
	private String mapsforgeFilename;
	private String mapsforgeXMLRenderTheme;

	public BackgroundMapConfig() {
		tileserverUrl = "http://tile.openstreetmap.org";
		type = BackgroundType.OSMTILESERVER;
	}
	
	public BackgroundMapConfig(BackgroundMapConfig config){
		this.fade = config.getFade()!=null ? new FadeConfig(config.getFade()):null;
		this.tileserverUrl = config.getTileserverUrl();
		this.type = config.getType();
		this.mapsforgeFilename = config.getMapsforgeFilename();
		this.mapsforgeXMLRenderTheme = config.getMapsforgeXMLRenderTheme();
	}

	public BackgroundMapConfig(Properties properties) {
		// call this to fill in standard values
		this();

		if (properties != null) {
			StandardisedStringTreeMap<String> map = StandardisedStringTreeMap.fromProperties(properties);
			readMap(map);
		}
	}
	
	public BackgroundMapConfig(StandardisedStringTreeMap<String> map){
		this();
		if(map!=null){
			readMap(map);
		}
	}

	private void readMap(StandardisedStringTreeMap<String> map) {
		fade.setColour( new Color(getInt(map, "fade.r", DEFAULT_FADE_COLOUR.getRed()), getInt(map, "fade.g", DEFAULT_FADE_COLOUR.getGreen()), getInt(map, "fade.b", DEFAULT_FADE_COLOUR.getBlue()), getInt(map, "fade.a", DEFAULT_FADE_COLOUR.getAlpha())));
		fade.setGreyscale(getDbl(map, "greyscale", 0));
		tileserverUrl = getStr(map, "tileserver.url", tileserverUrl);

		// get mapsforge filename and remove any speech marks
		String tmp  = getStr(map, "mapsforge.file", mapsforgeFilename);
		if(tmp!=null){
			tmp=tmp.replaceAll("\"", "");
		}
		mapsforgeFilename = tmp;

		// and for the render theme
		tmp = getStr(map, "mapsforge.xmlrendertheme", mapsforgeXMLRenderTheme);
		if(tmp!=null){
			tmp=tmp.replaceAll("\"", "");	
		}
		mapsforgeXMLRenderTheme = tmp;
		
		String typeString = getStr(map, "type", type.name());
		for (BackgroundType t : BackgroundType.values()) {
			if (Strings.equalsStd(t.name(), typeString)) {
				type = t;
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

	private static double getDbl(StandardisedStringTreeMap<String> map, String key, double defaultValue) {
		Double d = Numbers.toDouble(map.get(key));
		if (d != null) {
			return d;
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

	public FadeConfig getFade() {
		return fade;
	}

	public void setFade(FadeConfig fade) {
		this.fade = fade;
	}

	public String getMapsforgeXMLRenderTheme() {
		return mapsforgeXMLRenderTheme;
	}

	public void setMapsforgeXMLRenderTheme(String mapsforgeXMLRenderTheme) {
		this.mapsforgeXMLRenderTheme = mapsforgeXMLRenderTheme;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fade == null) ? 0 : fade.hashCode());
		result = prime * result + ((mapsforgeFilename == null) ? 0 :Strings.std(mapsforgeFilename).hashCode());
		result = prime * result + ((mapsforgeXMLRenderTheme == null) ? 0 : Strings.std(mapsforgeXMLRenderTheme).hashCode());
		result = prime * result + ((tileserverUrl == null) ? 0 : Strings.std(tileserverUrl).hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BackgroundMapConfig other = (BackgroundMapConfig) obj;
		if (fade == null) {
			if (other.fade != null)
				return false;
		} else if (!fade.equals(other.fade))
			return false;
		if (mapsforgeFilename == null) {
			if (other.mapsforgeFilename != null)
				return false;
		} else if (!Strings.equalsStd(mapsforgeFilename,other.mapsforgeFilename))
			return false;
		if (mapsforgeXMLRenderTheme == null) {
			if (other.mapsforgeXMLRenderTheme != null)
				return false;
		} else if (!Strings.equalsStd(mapsforgeXMLRenderTheme,other.mapsforgeXMLRenderTheme))
			return false;
		if (tileserverUrl == null) {
			if (other.tileserverUrl != null)
				return false;
		} else if (!Strings.equalsStd(tileserverUrl,other.tileserverUrl))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	
	
}
