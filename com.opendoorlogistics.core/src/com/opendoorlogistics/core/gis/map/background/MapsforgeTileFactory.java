/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.background;

import gnu.trove.map.hash.TByteIntHashMap;
import gnu.trove.map.hash.TIntByteHashMap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.io.FilenameUtils;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapDataStore;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.MultiMapDataStore;
import org.mapsforge.map.reader.MultiMapDataStore.DataPolicy;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.Tile;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactory;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.utils.images.CompressedImage;
import com.opendoorlogistics.core.utils.images.CompressedImage.CompressedType;
import com.opendoorlogistics.core.utils.io.RelativeFiles;
import com.opendoorlogistics.core.utils.strings.Strings;

class MapsforgeTileFactory extends TileFactory {
	private static final int TILE_SIZE = 256;
	private static final float TEXT_SCALE = 1.0f;

	private final MapDataStore mapDatabase;
	private final LinkedList<Tile> toCreate = new LinkedList<>();
	private final DatabaseRenderer databaseRenderer;
	private final XmlRenderTheme renderTheme;
	private final DisplayModel model;
	private final FadeConfig fadeColour;
	private final ZoomLevelConverter zoomLevelConverter;
	private ExecutorService service;

	private static XmlRenderTheme getRenderTheme(String xmlRenderThemeFilename){
		if(Strings.isEmpty(xmlRenderThemeFilename)==false){
			File renderThemeFile = RelativeFiles.validateRelativeFiles(xmlRenderThemeFilename, AppConstants.ODL_CONFIG_DIR);
			if (renderThemeFile != null) {
				try {
					return new ExternalRenderTheme(renderThemeFile.getAbsoluteFile());					
				} catch (Exception e) {
					// just return the default theme
				}
			}			
		}		
		return InternalRenderTheme.OSMARENDER;
	}
	
	MapsforgeTileFactory(TileFactoryInfo info, String xmlRenderThemeFilename,MapDataStore mapDatabase, FadeConfig fadeColour) {
		super(info);
		this.fadeColour =fadeColour;
		this.mapDatabase = mapDatabase;

		zoomLevelConverter = new ZoomLevelConverter(info);
		databaseRenderer = new DatabaseRenderer(mapDatabase, AwtGraphicFactory.INSTANCE,createDummyTileCacheForMapsforgeLabelPlacementAlgorithm());
		renderTheme =getRenderTheme(xmlRenderThemeFilename);

		model = new DisplayModel();
		model.setFixedTileSize(TILE_SIZE);
		model.setBackgroundColor(backgroundMapColour().getRGB());

		// use single thread at the moment as DatabaseRenderer is probably single threaded
		service = Executors.newFixedThreadPool(1, new ThreadFactory() {
			private int count = 0;

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "mapsforge-tile-pool-" + count++);
				t.setPriority(Thread.MIN_PRIORITY);
				t.setDaemon(true);
				return t;
			}
		});
	}

	private static Color backgroundMapColour() {
		return Color.BLUE;
	}


//	private XmlRenderTheme changeBackgroundInRenderTheme(final XmlRenderTheme theme) {
//
//		InputStream inputStream = null;
//		try {
//			inputStream = theme.getRenderThemeAsStream();
//
//			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//
//			dbf.setValidating(false);
//			dbf.setIgnoringComments(false);
//			dbf.setIgnoringElementContentWhitespace(true);
//			dbf.setNamespaceAware(true);
//
//			DocumentBuilder db = null;
//			db = dbf.newDocumentBuilder();
//			Document doc = db.parse(inputStream);
//			if (doc.getChildNodes().getLength() > 0) {
//				Node node = doc.getChildNodes().item(0);
//
//				if (Strings.equalsStd(node.getNodeName(), "renderTheme")) {
//					NamedNodeMap attributes = node.getAttributes();
//					for (int i = 0; i < attributes.getLength(); i++) {
//						Node attribNode = attributes.item(i);
//						if (attribNode != null && Attr.class.isInstance(attribNode)) {
//							Attr attr = (Attr) attribNode;
//							if (Strings.equalsStd(attr.getNodeName(), "map-background")) {
//								attr.setValue("#1010ff");
//							}
//						}
//					}
//
//					DOMSource source = new DOMSource(doc);
//					StringWriter xmlAsWriter = new StringWriter();
//
//					StreamResult result = new StreamResult(xmlAsWriter);
//
//					TransformerFactory.newInstance().newTransformer().transform(source, result);
//
//					// write changes
//					final ByteArrayInputStream stream = new ByteArrayInputStream(xmlAsWriter.toString().getBytes("UTF-8"));
//
//					return new XmlRenderTheme() {
//
//						@Override
//						public InputStream getRenderThemeAsStream() throws FileNotFoundException {
//							return stream;
//						}
//
//						@Override
//						public String getRelativePathPrefix() {
//							return theme.getRelativePathPrefix();
//						}
//					};
//				}
//			}
//
//			return theme;
//
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		} finally {
//			// if (renderThemeHandler.renderTheme != null) {
//			// renderThemeHandler.renderTheme.destroy();
//			// }
//			IOUtils.closeQuietly(inputStream);
//		}
//
//	}

	private String getTileId(int x, int y, int zoom) {
		StringBuilder builder = new StringBuilder();
		builder.append(x);
		builder.append(",");
		builder.append(y);
		builder.append(",");
		builder.append(zoom);
		return builder.toString();
	}

	@Override
	public synchronized Tile getTile(int x, int y, int zoom) {
		// get from cache if exists
		BufferedImage img = getCachedTileImage(x, y, zoom);

		// create tile (which can have a null image initially)
		Tile ret = createTileFromImg(x, y, zoom, img);

		if (img == null) {
			startLoading(ret);
		}
		return ret;

	}
	
	@Override
	public BufferedImage renderSynchronously(int x, int y, int zoom) {
		BufferedImage ret = getCachedTileImage(x, y, zoom);
		if(ret==null){
			Future<BufferedImage> future = submitAsCallable(new Tile(x, y, zoom));
			try {
				ret = future.get();				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return ret;
	}

	/**
	 * @param x
	 * @param y
	 * @param zoom
	 * @param img
	 * @return
	 */
	private Tile createTileFromImg(int x, int y, int zoom, final BufferedImage img) {
		Tile ret = new Tile(x, y, zoom) {
			@Override
			public BufferedImage getImage() {
				return img;
			}

			@Override
			public synchronized boolean isLoaded() {
				return img != null;
			}

			@Override
			public synchronized boolean isLoading() {
				return img == null;
			}
		};
		return ret;
	}

	@Override
	public void dispose() {
		if (service != null) {
			service.shutdown();
			service = null;
		}

		databaseRenderer.destroy();
		mapDatabase.close();
	}

	@Override
	protected synchronized void startLoading(Tile tile) {
		// check not already pending
		for (Tile pending : toCreate) {
			if (isSameTile(tile, pending)) {
				return;
			}
		}

		// add to front
		toCreate.addFirst(tile);

		service.submit(new MultiTileCreator());
	}

	/**
	 * Submit the tile creation task as a callable so we can get its result in the calling thread
	 * @param tile
	 * @return
	 */
	private synchronized Future<BufferedImage> submitAsCallable(Tile tile){
		return service.submit(new SingleTileCreator(tile));
	}
	
	/**
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean isSameTile(Tile a, Tile b) {
		return b.getX() == a.getX() && b.getY() == a.getY() && b.getZoom() == a.getZoom();
	}

	private class SingleTileCreator implements Callable<BufferedImage>{
		final Tile tile;

		SingleTileCreator(Tile tile) {
			this.tile = tile;
		}

		@Override
		public BufferedImage call()  {
			// get mapsforge zoom from jxmapviewer2 zoom (they use different conventions)
			byte mapsforgeZoom = zoomLevelConverter.getMapsforge(tile.getZoom());

			// load the render them
			RenderThemeFuture rtf = new RenderThemeFuture(AwtGraphicFactory.INSTANCE, renderTheme, model);
			rtf.run();

			// render the mapsforge tile
			org.mapsforge.core.model.Tile mtile = new org.mapsforge.core.model.Tile(tile.getX(), tile.getY(), mapsforgeZoom, TILE_SIZE);
			RendererJob job = new RendererJob(mtile, mapDatabase, rtf, model, TEXT_SCALE, true, false);
			TileBitmap bitmap = databaseRenderer.executeJob(job);

			// copy it over onto an image (CompressedImage needs TYPE_INT_ARGB and anyway we can't access the buffered image internal to the tile)
			BufferedImage image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D)image.getGraphics();
			g.setClip(0, 0, TILE_SIZE, TILE_SIZE);
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
			Canvas canvas = (Canvas) AwtGraphicFactory.createGraphicContext(g);
			canvas.drawBitmap(bitmap, 0, 0);
			if(fadeColour!=null){
				BackgroundMapUtils.renderFade(g,fadeColour.getColour());				
			}
			
			g.dispose();
			if(fadeColour!=null){
				image = BackgroundMapUtils.greyscale(image, fadeColour.getGreyscale());				
			}
			
			// TEST save to file
		//	ImageUtils.toPNGFile(image, new File("C:\\temp\\MapsforgeOutput\\" + System.currentTimeMillis() + ".png"));
			
			// add to cache
			CompressedImage compressed = new CompressedImage(image, CompressedType.LZ4);
			cacheImage(tile.getX(), tile.getY(), tile.getZoom(), compressed);

			// remove from pending after adding from cache (so can't be added twice)
			removeTile(tile);
			
			return image;
		}
		
	}
	
	private class MultiTileCreator implements Runnable {

		@Override
		public void run() {
			// Keep on getting tiles until there's no more...
			
			Tile tile = pollTopPending();
			while (tile != null) {

				BufferedImage image = new SingleTileCreator(tile).call();

				// tell listeners
				fireTileLoadedEvent(createTileFromImg(tile.getX(), tile.getY(), tile.getZoom(), image));

				// get next
				tile = pollTopPending();
			}


		}

	}

	private synchronized Tile pollTopPending() {
		if (toCreate.size() > 0) {
			return toCreate.poll();
		}
		return null;
	}

	private synchronized void removeTile(Tile tile) {
		Iterator<Tile> it = toCreate.iterator();
		while (it.hasNext()) {
			Tile other = it.next();
			if (isSameTile(tile, other)) {
				it.remove();
			}
		}
	}

	private synchronized BufferedImage getCachedTileImage(int x, int y, int zoom) {
		String id = getTileId(x, y, zoom);
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.MAPSFORGE_BACKGROUND_TILES);
		CompressedImage compressed = (CompressedImage) cache.get(id);
		final BufferedImage img = compressed != null ? compressed.getBufferedImage() : null;
		return img;
	}

	// private final HashMap<String, CompressedImage> cache = new HashMap<>();

	private synchronized void cacheImage(int x, int y, int zoom, CompressedImage compressed) {
		RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.MAPSFORGE_BACKGROUND_TILES);
		cache.put(getTileId(x, y, zoom), compressed, compressed.getSizeBytes());
		// cache.put(getTileId(x,y,zoom), compressed);
	}

	/**
	 * The mapsforge label placement algorithm needs to know what tiles are cached.
	 * We create a dummy tile cache object which links to our real cache, so we can test this.
	 * @return
	 */
	private TileCache createDummyTileCacheForMapsforgeLabelPlacementAlgorithm() {

		TileCache dummyCache = new TileCache() {
			
			@Override
			public void setWorkingSet(Set<Job> workingSet) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void put(Job key, TileBitmap bitmap) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public TileBitmap getImmediately(Job key) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public int getCapacityFirstLevel() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public int getCapacity() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public TileBitmap get(Job key) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public void destroy() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public boolean containsKey(Job key) {
				int odlZoom = zoomLevelConverter.getODL(key.tile.zoomLevel);
				return getCachedTileImage(key.tile.tileX, key.tile.tileY, odlZoom)!=null;
			}

			@Override
			public void purge() {
				// TODO Auto-generated method stub
				
			}
		};
		
		return dummyCache;
	}

//	private byte getMapsforgeInternalZoomLevel(int ODLZoomLevel) {
//		byte mapsforgeZoom;
//		long nbTiles = getInfo().getMapWidthInTilesAtZoom(ODLZoomLevel);
//		for (mapsforgeZoom = 0; mapsforgeZoom < 255; mapsforgeZoom++) {
//			long maxMapsforgeTileNb = org.mapsforge.core.model.Tile.getMaxTileNumber(mapsforgeZoom);
//			if (maxMapsforgeTileNb == nbTiles - 1) {
//				break;
//			}
//		}
//
//		if (mapsforgeZoom == 255) {
//			throw new RuntimeException("Cannot match zoom levels between mapforge and jxmapviewer2.");
//		}
//		return mapsforgeZoom;
//	}

//	private int getODLZoomLevel(int mapsforgeInternalZoomLevel){
//		TileFactoryInfo info = getInfo();
//		for(int i =info.getMinimumZoomLevel() ; i < info.getMaximumZoomLevel() ; i++){
//			
//		}
//	}

	private static class ZoomLevelConverter{
		TIntByteHashMap odlToMapsforge = new TIntByteHashMap();
		TByteIntHashMap mapsforgeToODL = new TByteIntHashMap();
		
		ZoomLevelConverter(TileFactoryInfo info){
			for(int i =info.getMinimumZoomLevel() ; i <= info.getMaximumZoomLevel() ; i++){
				byte mapsforgeZoom;
				long nbTiles = info.getMapWidthInTilesAtZoom(i);
				for (mapsforgeZoom = 0; mapsforgeZoom < 255; mapsforgeZoom++) {
					long maxMapsforgeTileNb = org.mapsforge.core.model.Tile.getMaxTileNumber(mapsforgeZoom);
					if (maxMapsforgeTileNb == nbTiles - 1) {
						break;
					}
				}

				if (mapsforgeZoom == 255) {
					throw new RuntimeException("Cannot match zoom levels between mapforge and jxmapviewer2.");
				}
				
				odlToMapsforge.put(i, mapsforgeZoom);
				mapsforgeToODL.put(mapsforgeZoom, i);
			}	
		}
		
		byte getMapsforge(int odl){
			return odlToMapsforge.get(odl);
		}
		
		int getODL(byte mapsforge){
			return mapsforgeToODL.get(mapsforge);
		}
	}

	@Override
	public boolean isRenderedOffline() {
		return true;
	}
	
	static MapDataStore openMapsforgeDb(String filename) {
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
}
