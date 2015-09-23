// Fraunhofer Institute for Computer Graphics Research (IGD)
// Department Information Visualization and Visual Analytics
//
// Copyright (c) Fraunhofer IGD. All rights reserved.
//
// This source code is property of the Fraunhofer IGD and underlies
// copyright restrictions. It may only be used with explicit
// permission from the respective owner.

package com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx;

import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;

/**
 * Uses OpenStreetMap
 * @author Martin Dummer
 */
public class OSMTileFactoryInfo extends TileFactoryInfo
{
	private static final int max = 19;

	public OSMTileFactoryInfo(){
		this("http://tile.openstreetmap.org");
	}
	
	/**
	 * Default constructor
	 */
	public OSMTileFactoryInfo(String baseUrl)
	{
//		super("OpenStreetMap", 
//				1, max - 2, max, 
//				256, true, true, 					// tile size is 256 and x/y orientation is normal
//				baseUrl,
//	//			"http://otile1.mqcdn.com/tiles/1.0.0/osm",
//			//	"http://tile.openstreetmap.org",
//			//	"http://otile1.mqcdn.com/tiles/1.0.0/sat",
//				"x", "y", "z");
		
		super("OpenStreetMap", 
				1, max - 2, max, 
				256, true, true, 					// tile size is 256 and x/y orientation is normal
				baseUrl,
	//			"http://otile1.mqcdn.com/tiles/1.0.0/osm",
			//	"http://tile.openstreetmap.org",
			//	"http://otile1.mqcdn.com/tiles/1.0.0/sat",
				"x", "y", "z");		
	}

	@Override
	public String getTileUrl(int x, int y, int zoom)
	{
		zoom = max - zoom;
		String url = this.baseURL + "/" + zoom + "/" + x + "/" + y + ".png";
		return url;
	}

}
