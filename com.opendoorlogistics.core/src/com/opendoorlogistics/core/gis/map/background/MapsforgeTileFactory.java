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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.Tile;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactory;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.utils.images.CompressedImage;
import com.opendoorlogistics.core.utils.images.CompressedImage.CompressedType;

class MapsforgeTileFactory extends TileFactory {
	private static final int TILE_SIZE = 256;
	private static final float TEXT_SCALE = 0.5f;

	private final MapDatabase mapDatabase;
	private final LinkedList<Tile> toCreate = new LinkedList<>();
	private final DatabaseRenderer databaseRenderer;
	private final XmlRenderTheme renderTheme;
	private final DisplayModel model;
	private final File mapFile;
	private final Color fadeColour;
	private ExecutorService service;

	MapsforgeTileFactory(TileFactoryInfo info, File mapFile, MapDatabase mapDatabase, Color fadeColour) {
		super(info);
		this.fadeColour =fadeColour;
		this.mapDatabase = mapDatabase;

		databaseRenderer = new DatabaseRenderer(mapDatabase, AwtGraphicFactory.INSTANCE,createTileCache());
		renderTheme = InternalRenderTheme.OSMARENDER;
		model = new DisplayModel();
		model.setFixedTileSize(TILE_SIZE);
		model.setBackgroundColor(Color.BLUE.getRGB());
		this.mapFile = mapFile;

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
		mapDatabase.closeFile();
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
			byte mapsforgeZoom;
			long nbTiles = getInfo().getMapWidthInTilesAtZoom(tile.getZoom());
			for (mapsforgeZoom = 0; mapsforgeZoom < 255; mapsforgeZoom++) {
				long maxMapsforgeTileNb = org.mapsforge.core.model.Tile.getMaxTileNumber(mapsforgeZoom);
				if (maxMapsforgeTileNb == nbTiles - 1) {
					break;
				}
			}

			if (mapsforgeZoom == 255) {
				throw new RuntimeException("Cannot match zoom levels between mapforge and jxmapviewer2.");
			}

			// render the mapsforge tile
			org.mapsforge.core.model.Tile mtile = new org.mapsforge.core.model.Tile(tile.getX(), tile.getY(), mapsforgeZoom, TILE_SIZE);
			RendererJob job = new RendererJob(mtile, mapFile, renderTheme, model, TEXT_SCALE, true, false);
			TileBitmap bitmap = databaseRenderer.executeJob(job);

			// copy it over onto an image (CompressedImage needs TYPE_INT_ARGB and anyway we can't access the buffered image internal to the tile)
			BufferedImage image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D)image.getGraphics();
			g.setClip(0, 0, TILE_SIZE, TILE_SIZE);
			Canvas canvas = (Canvas) AwtGraphicFactory.createGraphicContext(g);
			canvas.drawBitmap(bitmap, 0, 0);
			BackgroundMapUtils.renderFade(g,fadeColour);
			
			g.dispose();
			
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

	private TileCache createTileCache() {
		TileCache firstLevelTileCache = new InMemoryTileCache(128);
		File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge");
		TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory,  AwtGraphicFactory.INSTANCE);
		return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
	}


}
