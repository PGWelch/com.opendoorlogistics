/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.util.GeoUtil;
import com.opendoorlogistics.core.gis.map.background.BackgroundTileFactorySingleton;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreenImpl;
import com.opendoorlogistics.core.gis.map.transforms.UpscalerLatLongToPixelPosition;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.images.ImageUtils;

/**
 * A simple renderer which only renders synchronously in the current thread. This is used for taking snapshots of the map. It is based on JxMapViewer2
 * https://github.com/msteiger/jxmapviewer2 source code.
 * 
 * @author Phil
 * 
 */
final public class SynchronousRenderer {
	//private final TileFactoryInfo info;
	private final DatastoreRenderer renderer = new DatastoreRenderer();
	private final RecentImageCache recentImageCache = new RecentImageCache(RecentImageCache.ZipType.PNG);
	private boolean offlineBackgroundSynchronousRenderingOnly=false;


	/**
	 * Draw an image.
	 * 
	 * @param centre
	 *            in world bitmap coordinates
	 * @param imageWidth
	 * @param imageHeight
	 * @param zoomLevel
	 * @return
	 */
	public synchronized Pair<BufferedImage, LatLongToScreen> drawAtBitmapCoordCentre(Point2D centre, int imageWidth, int imageHeight,
			final int zoomLevel, long renderflags, Iterable<? extends DrawableObject> drawables) {

		LatLongToScreen converter = createConverter(centre, imageWidth, imageHeight, zoomLevel);

		BufferedImage image = DatastoreRenderer.createBaseImage(imageWidth, imageHeight, renderflags);
		Graphics2D g = image.createGraphics();
		g.setClip(0, 0, imageWidth, imageHeight);
		try {

			if ((renderflags & RenderProperties.SHOW_BACKGROUND) == RenderProperties.SHOW_BACKGROUND) {
				Rectangle viewport = calculateViewportBounds(centre, imageWidth, imageHeight);
				drawMapTiles(g, zoomLevel, viewport);
			}

			if (drawables != null) {
				renderer.renderAll(g, drawables, converter, renderflags , null);
			}

		} finally {
			g.dispose();
		}

		return new Pair<BufferedImage, LatLongToScreen>(image, converter);
	}
	
	private TileFactoryInfo info(){
		return BackgroundTileFactorySingleton.getFactory().getInfo();
	}

	private LatLongToScreen createConverter(Point2D centre, int imageWidth, int imageHeight, final int zoomLevel) {
		final Rectangle viewport = calculateViewportBounds(centre, imageWidth, imageHeight);
		return new LatLongToScreenImpl() {

			@Override
			public Rectangle2D getViewportWorldBitmapScreenPosition() {
				return viewport;
			}

			@Override
			public Point2D getWorldBitmapPixelPosition(LatLong latLong) {
				Point2D point = GeoUtil.getBitmapCoordinate(new GeoPosition(latLong.getLatitude(), latLong.getLongitude()), zoomLevel, info());
				return point;
			}

			@Override
			public LatLong getLongLat(double pixelX, double pixelY) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Object getZoomHashmapKey() {
				return zoomLevel;
			}

			@Override
			public int getZoomForObjectFiltering() {
				return zoomLevel;
			}

		};
	}

	public synchronized BufferedImage drawAtLatLongCentre(View view, int imageWidth, int imageHeight, long renderFlags,
			Iterable<DrawableObjectImpl> pnts) {
		BitmapView bitmapView = calculateBitmapView(view, imageWidth, imageHeight);
		return drawAtBitmapCoordCentre(bitmapView.centre, imageWidth, imageHeight, bitmapView.zoom, renderFlags, pnts).getFirst();
	}

	private static class BitmapView {
		final Point2D centre;
		final int zoom;
		final double fitQuality;

		BitmapView(Point2D centre, int zoom, double fitQuality) {
			this.centre = centre;
			this.zoom = zoom;
			this.fitQuality = fitQuality;
		}

	}

	private BitmapView calculateBitmapView(View view, int imageWidth, int imageHeight) {
		// get bounding geopositions
		HashSet<GeoPosition> bounding = new HashSet<>();
		bounding.add(new GeoPosition(view.getMinLatitude(), view.getMinLongitude()));
		bounding.add(new GeoPosition(view.getMinLatitude(), view.getMaxLongitude()));
		bounding.add(new GeoPosition(view.getMaxLatitude(), view.getMinLongitude()));
		bounding.add(new GeoPosition(view.getMaxLatitude(), view.getMaxLongitude()));
		Pair<Integer, Double> zoomFit = JXMapUtils.getBestFitZoom(info(), bounding, imageWidth, imageHeight, 0.975);

		LatLong ll = view.getCentre();
		Point2D point = GeoUtil.getBitmapCoordinate(new GeoPosition(ll.getLatitude(), ll.getLongitude()), zoomFit.getFirst(), info());
		// Pair<Point2D, Integer> bitmapView = new Pair<Point2D, Integer>(point, zoom);
		return new BitmapView(point, zoomFit.getFirst(), zoomFit.getSecond());
	}

	// private synchronized BufferedImage drawAtLatLongCentre(LatLong pnt, int imageWidth, int imageHeight, int zoom,long renderFlags,
	// Iterable<DrawableLatLongImpl>pnts) {
	// Point2D point = GeoUtil.getBitmapCoordinate(new GeoPosition(pnt.getLatitude(), pnt.getLongitude()), zoom, info);
	// return drawAtBitmapCoordCentre(point, imageWidth, imageHeight, zoom, renderFlags,pnts);
	// }

	private Rectangle calculateViewportBounds(Point2D centre, int width, int height) {

		// calculate the "visible" viewport area in pixels
		double viewportX = (centre.getX() - width / 2);
		double viewportY = (centre.getY() - height / 2);
		return new Rectangle((int) viewportX, (int) viewportY, width, height);
	}

	protected void drawMapTiles(Graphics g, int zoom, Rectangle viewportBounds) {
		if(offlineBackgroundSynchronousRenderingOnly && !BackgroundTileFactorySingleton.getFactory().isRenderedOffline()){
			throw new RuntimeException("Cannot render background tiles as they are not produced by an offline source - e.g. Mapsforge");
		}
		
		int size = info().getTileSize(zoom);

		// calculate the "visible" viewport area in tiles
		int nbWide = viewportBounds.width / size + 2;
		int nbHigh = viewportBounds.height / size + 2;

		int tpx = (int) Math.floor(viewportBounds.getX() / info().getTileSize(0));
		int tpy = (int) Math.floor(viewportBounds.getY() / info().getTileSize(0));

		for (int x = 0; x <= nbWide; x++) {
			for (int y = 0; y <= nbHigh; y++) {
				int itpx = x + tpx;
				int itpy = y + tpy;
				Rectangle rect = new Rectangle(itpx * size - viewportBounds.x, itpy * size - viewportBounds.y, size, size);
				if (g.getClipBounds().intersects(rect)) {
					BufferedImage tile = BackgroundTileFactorySingleton.getFactory().renderSynchronously(itpx, itpy, zoom);
					if (tile != null) {
						int ox = ((itpx * info().getTileSize(zoom)) - viewportBounds.x);
						int oy = ((itpy * info().getTileSize(zoom)) - viewportBounds.y);
						g.drawImage(tile, ox, oy, null);
					}
				}
			}
		}
	}

	private static final SynchronousRenderer singleton;
	static {
		singleton = new SynchronousRenderer();
	}

	public static SynchronousRenderer singleton() {
		return singleton;
	}

	private static final double CM_IN_INCH = 2.54;

	/**
	 * Draw OSM tiles and input points but upscaling the OSM tiles to make them look better when printed out.
	 * 
	 * @param view
	 * @param physicalWidthCM
	 * @param physicalHeightCM
	 * @param dotsPerCM
	 * @param pnts
	 * @param renderFlags
	 * @return
	 */

	private static class UpscaledOSMImage {
		Dimension size;
		BufferedImage image;
		LatLongToScreen converter;
	}

	public Pair<BufferedImage, LatLongToScreen> drawPrintableAtLatLongCentre(View view, double physicalWidthCM, double physicalHeightCM,
			double dotsPerCM, Iterable<? extends DrawableObject> drawables, long renderFlags) {

		UpscaledOSMImage uposm = upscaleOSMImage(view, physicalWidthCM, physicalHeightCM, dotsPerCM, renderFlags);

		// Draw the points, assuming we want best rendering quality
		if (drawables != null) {
			Graphics2D g = uposm.image.createGraphics();
			g.setClip(0, 0, uposm.size.width, uposm.size.height);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			try {
				renderer.renderAll(g, drawables, uposm.converter, renderFlags, null);
			} finally {
				g.dispose();
			}
		}

		return new Pair<BufferedImage, LatLongToScreen>(uposm.image, uposm.converter);
	}

	private static class UpscaleOSMKey {
		final Point2D bitmapViewCentre;
		final Dimension osmImageRes;
		final int osmZoom;
		final Dimension finalImageRes;
		public UpscaleOSMKey(Point2D bitmapViewCentre, Dimension osmImageRes, int osmZoom, Dimension finalImageRes) {
			super();
			this.bitmapViewCentre = bitmapViewCentre;
			this.osmImageRes = osmImageRes;
			this.osmZoom = osmZoom;
			this.finalImageRes = finalImageRes;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((bitmapViewCentre == null) ? 0 : bitmapViewCentre.hashCode());
			result = prime * result + ((finalImageRes == null) ? 0 : finalImageRes.hashCode());
			result = prime * result + ((osmImageRes == null) ? 0 : osmImageRes.hashCode());
			result = prime * result + osmZoom;
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
			UpscaleOSMKey other = (UpscaleOSMKey) obj;
			if (bitmapViewCentre == null) {
				if (other.bitmapViewCentre != null)
					return false;
			} else if (!bitmapViewCentre.equals(other.bitmapViewCentre))
				return false;
			if (finalImageRes == null) {
				if (other.finalImageRes != null)
					return false;
			} else if (!finalImageRes.equals(other.finalImageRes))
				return false;
			if (osmImageRes == null) {
				if (other.osmImageRes != null)
					return false;
			} else if (!osmImageRes.equals(other.osmImageRes))
				return false;
			if (osmZoom != other.osmZoom)
				return false;
			return true;
		}
		
	}

	private class UpscaledOSMConfig {
		final double OSMDPI;
		final double OSMDotsPerCM;
		final Dimension osmImageRes;
		final BitmapView bitmapView;

		UpscaledOSMConfig(View view, double physicalWidthCM, double physicalHeightCM, double OSMDPI) {
			this.OSMDPI = OSMDPI;
			this.OSMDotsPerCM = this.OSMDPI / CM_IN_INCH;

			// Calculate the resolution (pixel width x pixel height) of an OSM image printed out at this physical size using the standard OSM dots per
			// cm.
			// Although this image would look too low resolution, its fonts, roads etc would appear the correct (readable) size on-paper.
			this.osmImageRes = new Dimension((int) Math.round(physicalWidthCM * OSMDotsPerCM), (int) Math.round(physicalHeightCM * OSMDotsPerCM));

			// calculate the OSM view - a centre and zoom level which will fit in the view
			this.bitmapView = calculateBitmapView(view, osmImageRes.width, osmImageRes.height);
		}

		double qualityOfFit() {
			return bitmapView.fitQuality;
		}
	}

	private synchronized UpscaledOSMImage upscaleOSMImage(View view, double physicalWidthCM, double physicalHeightCM, double dotsPerCM,
			long renderFlags) {

		// adjust the assumed OSM dpi from 90 to 110 until we get the best fit view
		UpscaledOSMConfig config = null;
		double osmDPI = 80;
		while (osmDPI < 110) {
			UpscaledOSMConfig test = new UpscaledOSMConfig(view, physicalWidthCM, physicalHeightCM, osmDPI);
			if (config == null || test.qualityOfFit() > config.qualityOfFit()) {
				config = test;
			}
			osmDPI += 1;
		}

		// Calculate final image size
		UpscaledOSMImage uposm = new UpscaledOSMImage();
		uposm.size = new Dimension((int) Math.round(physicalWidthCM * dotsPerCM), (int) Math.round(physicalHeightCM * dotsPerCM));

		// Should we render?
		boolean renderingOSM = (renderFlags & RenderProperties.SHOW_BACKGROUND) == RenderProperties.SHOW_BACKGROUND;
		if (renderingOSM) {

			// do we already have it?
			UpscaleOSMKey key = new UpscaleOSMKey(config.bitmapView.centre, config.osmImageRes, config.bitmapView.zoom, uposm.size);
			uposm.image = recentImageCache.getBufferedImage(key);

			if (uposm.image == null) {
				// render background and upscale
				Pair<BufferedImage, LatLongToScreen> osmImage = drawAtBitmapCoordCentre(config.bitmapView.centre, config.osmImageRes.width,
						config.osmImageRes.height, config.bitmapView.zoom, RenderProperties.SHOW_BACKGROUND, null);
				uposm.image = ImageUtils.scaleImage(osmImage.getFirst(), uposm.size);
				recentImageCache.put(key, uposm.image);
			}

		} else {
			// blank image
			uposm.image = DatastoreRenderer.createBaseImage(uposm.size.width, uposm.size.height,renderFlags);

		}

		// get original (unscaled) converter
		LatLongToScreen unscaledConverter = createConverter(config.bitmapView.centre, config.osmImageRes.width, config.osmImageRes.height,
				config.bitmapView.zoom);

		// Work out the scaling factor used and create a lat-long converter wrapping the original...
		double scalingFactor = dotsPerCM / config.OSMDotsPerCM;
		uposm.converter = new UpscalerLatLongToPixelPosition(unscaledConverter, scalingFactor);
		return uposm;
	}

	public boolean isOfflineBackgroundSynchronousRenderingOnly() {
		return offlineBackgroundSynchronousRenderingOnly;
	}

	public void setOfflineBackgroundSynchronousRenderingOnly(
			boolean offlineBackgroundSynchronousRenderingOnly) {
		this.offlineBackgroundSynchronousRenderingOnly = offlineBackgroundSynchronousRenderingOnly;
	}

}
