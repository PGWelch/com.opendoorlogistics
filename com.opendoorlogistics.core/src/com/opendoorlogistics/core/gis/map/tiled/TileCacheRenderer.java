/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.tiled;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.codefromweb.BlockingLifoQueue;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.gis.map.CachedGeomImageRenderer;
import com.opendoorlogistics.core.gis.map.DatastoreRenderer;
import com.opendoorlogistics.core.gis.map.ObjectRenderer;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.tiled.DrawableObjectLayer.LayerType;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.images.CompressedImage;
import com.opendoorlogistics.core.utils.images.CompressedImage.CompressedType;

import gnu.trove.set.hash.TLongHashSet;

final public class TileCacheRenderer implements Disposable {
	private static final int MAX_GEOM_POINTS_FOR_EDT_RENDER = 5000;
	//private static final int MAX_GEOM_POINTS_FILL_FOR_EDT_RENDER = 5000;
	private final DatastoreRenderer EDTrenderer = new DatastoreRenderer();
	// private final DatastoreRenderer workerThreadRenderer = new DatastoreRenderer(true, RecentImageCache.ZipType.LZ4);
	private final ObjectRenderer workerThreadRenderer = new CachedGeomImageRenderer();
	private final ExecutorService service;
	private final RecentlyUsedCache updatedCompletedTileMap = new RecentlyUsedCache("updated-completed-tile-map",64 * 1024 * 1024);
	private final RecentlyUsedCache outdatedCompleteTileMap = new RecentlyUsedCache("outdated-complete-tile-map",16 * 1024 * 1024);
	private final ConcurrentHashMap<Object, CachedTile> processingTileMap = new ConcurrentHashMap<>();
	private final HashSet<TileReadyListener> tileReadyListeners = new HashSet<>();
	private final BufferedImage loadingImage = createLoadingImage();
	// private TLongHashSet lastSelectedObjectIds = new TLongHashSet();
	private List<DrawableObjectLayer> drawableLayers;	
	private volatile boolean isDisposed = false;
	private final ChangedObjectsCalculator calculateChangedObjects = new ChangedObjectsCalculator();
	private LatLongToScreen lastConverter;
	private boolean edtRender = false;
	private NOPLManager noplmanager = new NOPLManager();
	
	@SuppressWarnings("unused")
	/**
	 * We store the last required tiles so we have a strong reference to them
	 */
	private LinkedList<CachedTile> lastUsedTiles = new LinkedList<>();

	public TileCacheRenderer() {
		// Create with a single rendering thread so we don't have to worry about synchronisation issues
		// We use a LIFO queue so last requested is executed first. If the user zooms around a lot
		// their most recent viewpoint should therefore generally be prioritised (unless they've
		// zoomed back and forth quickly...)
		int nThreads = 1;
		service = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new BlockingLifoQueue<Runnable>(), new ThreadFactory() {
			ThreadFactory factory = Executors.defaultThreadFactory();
			
			@Override
			public Thread newThread(Runnable r) {
				Thread ret = factory.newThread(r);
				ret.setName("TileCacheRendererThread-" + UUID.randomUUID().toString());
				return ret;
			}
		});

	}

	private static BufferedImage createLoadingImage() {
		BufferedImage ret = new BufferedImage(TilePosition.TILE_SIZE, TilePosition.TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = ret.createGraphics();
		g.setClip(0, 0, TilePosition.TILE_SIZE, TilePosition.TILE_SIZE);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		// String message = "drawing...";
		// Font loadingFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
		// TextLayout textLayout = new TextLayout(message, loadingFont, g.getFontRenderContext());
		// int width = (int)textLayout.getBounds().getWidth();
		// int x = (TILE_SIZE - width)/2;
		// int y = TILE_SIZE/2 - 8;
		// AffineTransform affineTransform = new AffineTransform();
		// affineTransform.translate(x,y);
		// Shape shape = textLayout.getOutline(affineTransform);
		// BasicStroke innerStroke = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND, 0, null, 0);
		// g.setStroke(innerStroke);
		// g.setColor(Color.WHITE);
		// g.draw(shape);
		// Color fontColour = new Color(0, 0, 100);
		// g.setColor(fontColour);
		// textLayout.draw(g,x,y);

		// Color fade = new Color(50, 50, 50, 255);
		// Rectangle bounds = g.getClipBounds();
		// g.setColor(fade);
		// g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

		g.dispose();
		return ret;
	}

	public static interface TileReadyListener {
		void tileReady(Rectangle2D worldBitmapBounds, Object zoom);
	}

	/**
	 * Information required for rendering
	 * 
	 * @author Phil
	 * 
	 */
	private static class RenderInformation {
		final List<DrawableObjectLayer> layers;
		final LatLongToScreen originalConverter;
	//	final long renderFlags;
		final TLongHashSet selectedObjectIds;

		RenderInformation(List<DrawableObjectLayer> layers, LatLongToScreen converter,  TLongHashSet selectedObjectIds) {
			this.layers = layers;
			this.originalConverter = converter;
		//	this.renderFlags = renderFlags;
			this.selectedObjectIds = selectedObjectIds;
		}

	}
	
	private class CachedTile implements Runnable {
		final TilePosition position;
		final RenderInformation renderInfo;
		CompressedImage finalImage;
		volatile boolean invalid = false;

		@Override
		public String toString() {
			return position.toString();
		}

		public CachedTile(int x, int y, Object zoomKey, RenderInformation renderInfo) {
			this.position = new TilePosition(x, y, zoomKey);
			this.renderInfo = renderInfo;
		}


		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((position == null) ? 0 : position.hashCode());
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
			CachedTile other = (CachedTile) obj;
			if (position == null) {
				if (other.position != null)
					return false;
			} else if (!position.equals(other.position))
				return false;
			return true;
		}

		@Override
		public synchronized void run() {

			BufferedImage workImg = new BufferedImage(TilePosition.TILE_SIZE, TilePosition.TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
		//	ImageUtils.fillImage(workImg, new Color(200, 200, 255));
			Graphics2D g = workImg.createGraphics();
			try {

				g.setClip(0, 0, TilePosition.TILE_SIZE, TilePosition.TILE_SIZE);
				
				// create a lat-long to onscreen converter which gives gives the viewable viewport bounds as the tile
				LatLongToScreen converter = position.createConverter(renderInfo.originalConverter);

				// render objects
				for(DrawableObjectLayer layer:renderInfo.layers){
					
					if(layer.getType() == LayerType.NOVLPL){
						// draw the layer tile
						NOVLPolyLayerTile tile = noplmanager.getTile(layer.getNOVLPLGroupId(), position, converter);
						
						// check poly layer tile exists, if not then this tile is probably out-of-date 
						if(tile!=null){
							tile.draw(layer, g,renderInfo.selectedObjectIds, 0, 0, converter.getZoomForObjectFiltering());							
						}
					}
					else{
						// draw each object one-by-one
						for(DrawableObject obj:layer){

							// check for quitting (flagged from other thread)
							if (invalid || isDisposed) {
								return;
							}
							
							try {
								workerThreadRenderer.renderObject(g, converter, obj, renderInfo.selectedObjectIds.contains(obj.getGlobalRowId()),0);
							} catch (Throwable e) {
							}
						}						
					}

				}

				// we've finished
				finalImage = new CompressedImage(DatastoreRenderer.postProcessImage(workImg), CompressedType.LZ4);
				if (!invalid && !isDisposed) {
					updatedCompletedTileMap.put(this, this, this.finalImage.getSizeBytes());
					outdatedCompleteTileMap.put(this, this, this.finalImage.getSizeBytes());
				}
				
				fireTileReadyListeners(this);

			} finally {
				g.dispose();
				processingTileMap.remove(this);
			}

		}

		void setInvalid() {
			invalid = true;
		}

	}

	static class MinMaxTileIndices {
		final Point minTileIndex;
		final Point maxTileIndex;

		MinMaxTileIndices(LatLongToScreen converter) {
			Rectangle2D view = converter.getViewportWorldBitmapScreenPosition();
			Point minPixelPoint = new Point((int) Math.floor(view.getX()), (int) Math.floor(view.getY()));
			Point maxPixelPoint = new Point((int) Math.ceil(view.getX() + view.getWidth()), (int) Math.ceil(view.getY() + view.getHeight()));

			// get min and max in tile indices
			minTileIndex = toTileIndexPoint(minPixelPoint);
			maxTileIndex = toTileIndexPoint(maxPixelPoint);
		}

		boolean isWithin(int ix, int iy) {
			return ix >= minTileIndex.x && ix <= maxTileIndex.x && iy >= minTileIndex.y && iy <= maxTileIndex.x;
		}
	}

	public synchronized void renderObjects(Graphics2D g, LatLongToScreen converter, long renderFlags, TLongHashSet selectedObjectIds) {

		// always take fresh copy of selection object so our internal record can
		// be considered immutable
		if (selectedObjectIds == null) {
			selectedObjectIds = new TLongHashSet();
		}

		clearDirtyTiles(calculateChangedObjects.updateSelected(selectedObjectIds), lastConverter);

		LinkedList<CachedTile> newLastUsedTiles = new LinkedList<>();
		if (edtRender == false) {
			// get the world bitmap for the current zoom
			MinMaxTileIndices tileRange = new MinMaxTileIndices(converter);
			Rectangle2D view = converter.getViewportWorldBitmapScreenPosition();

			// save information required for rendering in an object
			RenderInformation renderInformation = new RenderInformation(drawableLayers, converter, selectedObjectIds);

			// loop over all visible tile positions
			Object zoomKey = converter.getZoomHashmapKey();
			for (int ix = tileRange.minTileIndex.x; ix <= tileRange.maxTileIndex.x; ix++) {
				for (int iy = tileRange.minTileIndex.y; iy <= tileRange.maxTileIndex.y; iy++) {

					// create empty tile then use as a key to see if already exists
					CachedTile tile = new CachedTile(ix, iy, zoomKey, renderInformation);
					CachedTile working = processingTileMap.get(tile);
					CachedTile complete = (CachedTile) updatedCompletedTileMap.get(tile);
					Image image = loadingImage;
					if (complete != null) {
						image = complete.finalImage.get();
						// newLastUsedTiles.add(complete);
					} else {

						// use an old tile if we have it; will look better than completely redrawing
						// the screen on each small change...
						CachedTile outdated = (CachedTile) outdatedCompleteTileMap.get(tile);
						if (outdated != null && outdated.finalImage != null) {
							image = outdated.finalImage.get();
						}

						// // Use the tile from the last frame if still around; it will be wrong
						// // but better than nothing and won't confuse the user as it will be exactly
						// // the same as the last frame (i.e. instead of seeing a blank they'll see nothing change).
						// if(lastUsedTiles!=null){
						// for(Tile lut : lastUsedTiles){
						// if(lut.ix == ix && lut.iy == iy && lut.zoomKey.equals(zoomKey)){
						// System.out.println("xy zoom key match");
						// }
						//
						// if(lut.zoomKey.equals(zoomKey) && lut.ix == ix && lut.iy == iy && lut.finalImage!=null){
						// image = lut.finalImage.get();
						// }
						// }
						// }
						//
						if (working == null) {
							// we need to create it
							processingTileMap.put(tile, tile);
							service.submit(tile);
							newLastUsedTiles.add(tile);
						} else {
							newLastUsedTiles.add(working);
						}
					}

					g.drawImage(image, (int) (tile.position.ix * TilePosition.TILE_SIZE - view.getX()), (int) (tile.position.iy * TilePosition.TILE_SIZE - view.getY()), null);
				}

			}
		}

		// we maintain a non-soft reference to the last tiles used so they can't
		// get garbage collected after being completed and before being drawn to screen
		lastUsedTiles = newLastUsedTiles;

		if (edtRender) {
			EDTrenderer.renderAll(g, DrawableObjectLayer.layers2SingleList(drawableLayers), converter, renderFlags|RenderProperties.DRAW_OSM_COPYRIGHT, selectedObjectIds);
		} else {
			// Render text uncached without tiles at the moment. As texts can stretch over two tiles we need to
			// compare results across multiple tiles to see what text doesn't overlap what, and hence
			// what should be visible. As this is problematic we don't render text in the tiles.
			if ((renderFlags & RenderProperties.SHOW_TEXT) == RenderProperties.SHOW_TEXT) {
				EDTrenderer.renderTexts(g, DrawableObjectLayer.layers2SingleList(drawableLayers), converter, RenderProperties.DRAW_OSM_COPYRIGHT);
			}
		}

		// Save the last converter
		lastConverter = converter;

	}
	

	private static Point toTileIndexPoint(Point pixelPoint) {
		return new Point(pixelPoint.x / TilePosition.TILE_SIZE, pixelPoint.y / TilePosition.TILE_SIZE);
	}

	@Override
	public void dispose() {
		isDisposed = true;
		service.shutdownNow();
		// service.shutdown();
	}

//	/**
//	 * Test if the geometry contains one or more polygons
//	 * 
//	 * @param geometry
//	 * @return
//	 */
//	private static boolean hasPolygon(Geometry geometry) {
//		if (geometry != null) {
//			if (GeometryCollection.class.isInstance(geometry)) {
//				int n = geometry.getNumGeometries();
//				for (int i = 0; i < n; i++) {
//					if (hasPolygon(geometry.getGeometryN(i))) {
//						return true;
//					}
//				}
//			} else {
//				return Polygon.class.isInstance(geometry);
//			}
//		}
//		return false;
//	}

	public synchronized void setObjects(Iterable<? extends DrawableObject> pnts) {

		// Clear all tiles which are no longer correct
		clearDirtyTiles(calculateChangedObjects.updateObjects(pnts), lastConverter);

		// Count the number of geometry points. We render on the EDT for a small number
		// of points as this makes the map more responsive
		long geomPointsCount = 0;
		edtRender = false;
		for (DrawableObject obj : pnts) {
			ODLGeom geom = obj.getGeometry();
			if (geom == null) {
				geomPointsCount++;
			} else {
				geomPointsCount += geom.getPointsCount();
			}
		}
		
		// Choose EDT render or not
		if (geomPointsCount < MAX_GEOM_POINTS_FOR_EDT_RENDER ) {
			edtRender = true;
		}
		
		// Update the layer manager
		drawableLayers = noplmanager.update(pnts);
	}

	private synchronized void clearTiles() {
		// set invalid so the running tile can't be added to completed later
		clearProcessingTiles();
		updatedCompletedTileMap.clear();
	}

	private void clearProcessingTiles() {
		for (CachedTile tile : processingTileMap.values()) {
			tile.setInvalid();
		}

		processingTileMap.clear();
	}

	public synchronized void addTileReadyListener(TileReadyListener listener) {
		tileReadyListeners.add(listener);
	}

	public synchronized void removeTileReadyListener(TileReadyListener listener) {
		tileReadyListeners.remove(listener);
	}

	private synchronized void fireTileReadyListeners(CachedTile tile) {
		Rectangle2D bounds = tile.position.createBounds();
		for (TileReadyListener listener : tileReadyListeners) {
			listener.tileReady(bounds, tile.position.zoomKey);
		}
	}

	public static void main(String[] args) {
		// ImageUtils.createImageFrame(createLoadingImage()).setVisible(true);
	}

	private void clearDirtyTiles(List<DrawableObject> changeset, final LatLongToScreen currentView) {

		boolean clearAll = false;

		// clear all if haven't got current view
		if (currentView == null) {
			clearAll = true;
		}

		// calculate change area for the current view
		Rectangle2D changeArea = null;
		if (!clearAll) {
			for (DrawableObject obj : changeset) {
				Rectangle2D bounds = DatastoreRenderer.getRenderedWorldBitmapBounds(obj, currentView);

				if (bounds != null) {
					if (changeArea == null) {
						changeArea = bounds;
					} else {
						changeArea = changeArea.createUnion(bounds);
					}
				}
			}
		}

		if (clearAll) {
			clearTiles();
		} else {

			class TileTester{
				boolean isInvalid(CachedTile tile, Rectangle2D changeArea){
					// keep those in current view outside of change area

					boolean clearTile = false;
					// clear tile if not in current zoom as change area only calculated for this
					if (tile.position.zoomKey.equals(currentView.getZoomHashmapKey()) == false) {
						clearTile = true;
					}

					// clear tile if bounds intersect change area
					if (!clearTile && changeArea != null && tile.position.createBounds().intersects(changeArea)) {
						clearTile = true;
					}
					return clearTile;
				}
			}
			
			TileTester tileTester = new TileTester();
			
			// parse completed tiles
			for (Pair<Object, Object> pair : updatedCompletedTileMap.getSnapshot()) {
				CachedTile tile = (CachedTile) pair.getSecond();

				// clear tile if not in current zoom as change area only calculated for this
				boolean clearTile = tileTester.isInvalid(tile, changeArea);

				if (clearTile) {
					updatedCompletedTileMap.remove(tile);
				}
			}
			
			// parse processing tiles as well
			Iterator<Map.Entry<Object,CachedTile>> it=processingTileMap.entrySet().iterator();
			while(it.hasNext()){
				CachedTile tile = it.next().getValue();
				if(tileTester.isInvalid(tile, changeArea)){
					tile.invalid = true;
					it.remove();
				}
			}

		}
	}

	public boolean isDisposed() {
		return isDisposed;
	}
	
}
