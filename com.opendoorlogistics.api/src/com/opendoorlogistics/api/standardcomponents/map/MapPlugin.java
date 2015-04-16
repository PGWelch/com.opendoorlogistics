package com.opendoorlogistics.api.standardcomponents.map;


public interface MapPlugin extends net.xeoh.plugins.base.Plugin {
	String getId();
	void initMap(MapApi api);
}
