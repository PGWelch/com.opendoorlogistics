/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.background;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;
import org.jdesktop.swingx.OSMTileFactoryInfo;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.mapsforge.map.reader.MapDatabase;

import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.gis.map.JXMapUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

public final class BackgroundTileFactorySingleton {
	private static final TileFactory singleton;
	
	private BackgroundTileFactorySingleton(){}
	
	@SuppressWarnings("unchecked")
	public static <TFactory extends TileFactory & ODLSynchronousTileFactory> TFactory getFactory(){
		return (TFactory)singleton;
	}
	

	static {
		// get files in directory
		File dir = new File(AppConstants.MAPSFORGE_DIRECTORY);
		ArrayList<File> mapFiles = new ArrayList<>();
		if (dir.exists()) {
			for (File child : dir.listFiles()) {
				String ext = FilenameUtils.getExtension(child.getAbsolutePath());
				if (Strings.equalsStd(ext, "map")) {
					mapFiles.add(child);
				}
			}
		}

		// just take the first file at the moment...
		MapsforgeTileFactory factory = null;
		if (mapFiles.size() > 0) {
			File mapFile = mapFiles.get(0);
			MapDatabase mapDatabase = new MapDatabase();
			try {
				mapDatabase.openFile(mapFile);
				factory = new MapsforgeTileFactory(new OSMTileFactoryInfo(), mapFile, mapDatabase);
			} catch (Exception e) {
			}
		}

		if(factory!=null){
			singleton = factory;			
		}
		else{
			singleton = new ODLWebTileFactory(new OSMTileFactoryInfo());
			JXMapUtils.initLocalFileCache();
			
		}
	}

//	public static void main(String[]args){
//		System.out.println(BackgroundTileFactorySingleton.getFactory());
//	}
}
