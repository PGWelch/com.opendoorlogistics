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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.mapsforge.map.reader.MapDataStore;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.MultiMapDataStore;
import org.mapsforge.map.reader.MultiMapDataStore.DataPolicy;

import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.OSMTileFactoryInfo;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.Tile;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactory;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.gis.map.JXMapUtils;
import com.opendoorlogistics.core.gis.map.background.BackgroundMapConfig.BackgroundType;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.PropertiesUtils;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.io.RelativeFiles;
import com.opendoorlogistics.core.utils.strings.Strings;

public final class BackgroundTileFactorySingleton {
	private static final TileFactory singleton;

	private BackgroundTileFactorySingleton() {
	}

	@SuppressWarnings("unchecked")
	public static TileFactory getFactory() {
		return singleton;
	}

	static {
		// always ensure we initialise the local file cache

		Properties p = PropertiesUtils.loadFromFile(new File(AppConstants.ODL_BACKGROUND_MAP_PROPERTIES_FILE));
		BackgroundMapConfig c = new BackgroundMapConfig(p);
		JXMapUtils.initLocalFileCache(c.getTileserverUrl());
		singleton = createFactory(c);
		
//		// get files in directory
//		File dir = new File(AppConstants.MAPSFORGE_DIRECTORY);
//		ArrayList<File> mapFiles = new ArrayList<>();
//		if (dir.exists()) {
//			for (File child : dir.listFiles()) {
//				String ext = FilenameUtils.getExtension(child.getAbsolutePath());
//				if (Strings.equalsStd(ext, "map")) {
//					mapFiles.add(child);
//				}
//			}
//		}
//
//		Color fadeColour = new Color(255, 255, 255, 100);
//
//		// just take the first file at the moment...
//		MapsforgeTileFactory factory = null;
//		if (mapFiles.size() > 0) {
//			File mapFile = mapFiles.get(0);
//			MapDatabase mapDatabase = new MapDatabase();
//			try {
//				mapDatabase.openFile(mapFile);
//				factory = new MapsforgeTileFactory(new OSMTileFactoryInfo(), mapFile, mapDatabase, fadeColour);
//			} catch (Exception e) {
//			}
//		}
//
//		if (factory != null) {
//			singleton = factory;
//		} else {
//			singleton = new ODLWebTileFactory(new OSMTileFactoryInfo(), fadeColour);
//			JXMapUtils.initLocalFileCache();
//
//		}
	}

	private static class EmptyTileFactory extends TileFactory{
		final BufferedImage blankImage;
		
		EmptyTileFactory(TileFactoryInfo info, BackgroundMapConfig config) {
			super(info);
			blankImage = ImageUtils.createBlankImage(256, 256, BufferedImage.TYPE_INT_ARGB, Colours.lerp(Color.WHITE, config.getFade().getColour(), config.getFade().getColour().getAlpha() / 255.0));
		}



		@Override
		public BufferedImage renderSynchronously(int x, int y, int zoom) {
			return blankImage;
		}

		@Override
		public Tile getTile(int x, int y, int zoom) {
			Tile ret = new Tile(x, y, zoom) {
				@Override
				public boolean isLoaded() {
					return true;
				}

				@Override
				public boolean isLoading() {
					return false;
				}

				@Override
				public BufferedImage getImage() {
					return blankImage;
				}

			};
			return ret;
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void startLoading(Tile tile) {
			// TODO Auto-generated method stub
			
		}



		@Override
		public boolean isRenderedOffline() {
			return true;
		}
		
	}
	
	private static TileFactory createFactory(BackgroundMapConfig config) {
		TileFactoryInfo info = new OSMTileFactoryInfo(config.getTileserverUrl());

		BackgroundType type = config.getType();

		if (type == BackgroundType.MAPSFORGE) {
			MapDataStore result = openMapsforgeDb(config.getMapsforgeFilename());
			if (result != null) {
				return new MapsforgeTileFactory(info,  config.getMapsforgeXMLRenderTheme(),result, config.getFade());
			}
			type = BackgroundType.EMPTY;
		}

		if (type == BackgroundType.OSMTILESERVER) {
			return new ODLWebTileFactory(info, config.getFade());
		}

		// return empty tile factory...
		return new EmptyTileFactory(new OSMTileFactoryInfo(), config);

	}

	private static MapDataStore openMapsforgeDb(String filename) {
		File file = null;
		if (Strings.isEmpty(filename)) {
			// to do.. assume loading all files in the mapsforge directory
			file = new File(AppConstants.MAPSFORGE_DIRECTORY).getAbsoluteFile();
		}else{
			file = RelativeFiles.validateRelativeFiles(filename, AppConstants.MAPSFORGE_DIRECTORY);			
		}

		if (file == null || !file.exists()) {
			return null;
		}
		
		// if its a directory, load all the ones from the directory
		MultiMapDataStore ret = new MultiMapDataStore(DataPolicy.RETURN_ALL);
		if(file.isDirectory()){
			for(File child : file.listFiles()){
				String ext = FilenameUtils.getExtension(child.getAbsolutePath());
				if(ext!=null && ext.toLowerCase().equals("map")){
					try {
						MapFile mf= new MapFile(child);
						ret.addMapDataStore(mf, false, false);
					} catch (Exception e) {
					}			
				}
			}
		}
		else{
			try {
				MapFile mf= new MapFile(file);
				ret.addMapDataStore(mf, false, false);
			} catch (Exception e) {
			}			
		}

		return ret;
	}
	// public static void main(String[]args){
	// System.out.println(BackgroundTileFactorySingleton.getFactory());
	// }
}
