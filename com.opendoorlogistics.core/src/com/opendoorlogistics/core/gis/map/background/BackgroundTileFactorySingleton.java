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
import java.net.ResponseCache;
import java.util.HashMap;
import java.util.Properties;

import org.mapsforge.map.reader.MapDataStore;

import com.opendoorlogistics.codefromweb.jxmapviewer2.ExpiringOSMLocalResponseCache;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.OSMTileFactoryInfo;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.Tile;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactory;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.gis.map.background.BackgroundMapConfig.BackgroundType;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.PropertiesUtils;
import com.opendoorlogistics.core.utils.images.ImageUtils;

public final class BackgroundTileFactorySingleton {
	private static final ODLTileFactory SINGLETON;
	private static final ExpiringOSMLocalResponseCache LOCAL_RESPONSE_CACHE;
	private static final TileFactoryCache FADELESS_TILE_FACTORY_CACHE = new TileFactoryCache();

	private BackgroundTileFactorySingleton() {
	}

	public static ODLTileFactory getFactory() {
		return SINGLETON;
	}

	static {
		// always ensure we initialise the local file cache
		File cacheDir = new File(System.getProperty("user.home") + File.separator + ".jxmapviewer2");
		LOCAL_RESPONSE_CACHE = new ExpiringOSMLocalResponseCache( cacheDir, false);
		ResponseCache.setDefault(LOCAL_RESPONSE_CACHE);
		
		// Read properties and create default background map
		Properties p = PropertiesUtils.loadFromFile(new File(AppConstants.ODL_BACKGROUND_MAP_PROPERTIES_FILE));
		BackgroundMapConfig c = new BackgroundMapConfig(p);
		SINGLETON = createTileFactory(c);
	}

	public static ODLTileFactory createTileFactory(BackgroundMapConfig config){
		// Create config copy without the fade
		BackgroundMapConfig deepCopy = new BackgroundMapConfig(config);
		deepCopy.setFade(null);
		
		// Try to get factory from the fadeless-cache
		ODLTileFactory noFade= FADELESS_TILE_FACTORY_CACHE.get(deepCopy);

		// Create factory without the fade if needed
		if(noFade==null){
			TileFactory tileFactory = createFactory(deepCopy);
			if(tileFactory!=null){
				noFade = new TileFactory2ODL(tileFactory, true, null);
				FADELESS_TILE_FACTORY_CACHE.put(deepCopy, noFade);	
				
				// ensure its cached properly on-disk
				if(deepCopy.getType() == BackgroundType.OSMTILESERVER){
					LOCAL_RESPONSE_CACHE.addAcceptedBasedURL(deepCopy.getTileserverUrl());
				}
			}			
		}
		
		if(noFade!=null){
			return new ODLTileFactoryDecorator(noFade, config.getFade());
		}
		
		return null;
	}
	
	private static class EmptyTileFactory extends TileFactory {
		final BufferedImage blankImage;

		EmptyTileFactory(TileFactoryInfo info, BackgroundMapConfig config) {
			super(info);
			Color colour = Color.WHITE;
			if (config.getFade() != null && config.getFade().getColour() != null) {
				colour = Colours.lerp(Color.WHITE, config.getFade().getColour(), config.getFade().getColour().getAlpha() / 255.0);
			}
			blankImage = ImageUtils.createBlankImage(256, 256, BufferedImage.TYPE_INT_ARGB, colour);
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
			MapDataStore result = MapsforgeTileFactory.openMapsforgeDb(config.getMapsforgeFilename());
			if (result != null) {
				return new MapsforgeTileFactory(info, config.getMapsforgeXMLRenderTheme(), result, config.getFade());
			}
			type = BackgroundType.EMPTY;
		}

		if (type == BackgroundType.OSMTILESERVER) {
			return new ODLWebTileFactory(info, config.getFade());
		}

		// return empty tile factory...
		return new EmptyTileFactory(new OSMTileFactoryInfo(), config);

	}

	// public static void main(String[]args){
	// System.out.println(BackgroundTileFactorySingleton.getFactory());
	// }

	private static class TileFactoryCache {
		private HashMap<BackgroundMapConfig, ODLTileFactory> cached = new HashMap<BackgroundMapConfig, ODLTileFactory>();

		public synchronized void put(BackgroundMapConfig config, ODLTileFactory factory) {
			checkNoFade(config);
			ODLTileFactory pre = get(config);
			if (pre != null) {
				pre.dispose();
			}
			cached.put(config, factory);
		}

		private void checkNoFade(BackgroundMapConfig config) {
			if (config.getFade() != null) {
				throw new IllegalArgumentException("Fade cannot be set on a cached tile factory; fade should be applied afterwards");
			}
		}

		public synchronized ODLTileFactory get(BackgroundMapConfig config) {
			checkNoFade(config);
			return cached.get(config);
		}

		public synchronized void clear() {
			for (ODLTileFactory factory : cached.values()) {
				factory.dispose();
			}
			cached.clear();
		}
	}

	public static void dispose(){
		FADELESS_TILE_FACTORY_CACHE.clear();
	}
}
