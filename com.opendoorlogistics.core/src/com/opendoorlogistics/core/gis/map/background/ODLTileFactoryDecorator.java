package com.opendoorlogistics.core.gis.map.background;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;

/**
 * An ODLTileFactoryDecorator allows a fade to be applied to a tile factory
 * @author Phil
 *
 */
public class ODLTileFactoryDecorator implements ODLTileFactory{
	private final ODLTileFactory factory;
	private final FadeConfig fadeConfig;
	
	public ODLTileFactoryDecorator(ODLTileFactory factory, FadeConfig fadeConfig) {
		this.factory = factory;
		this.fadeConfig = fadeConfig;
	}

	public MapTile getMapTile(int x, int y, int zoom) {
		MapTile original= factory.getMapTile(x, y, zoom);
		if(fadeConfig!=null){
			return new MapTile() {
				
				@Override
				public boolean isLoaded() {
					return original.isLoaded();
				}
				
				@Override
				public int getZoom() {
					return original.getZoom();
				}
				
				@Override
				public int getY() {
					return original.getY();
				}
				
				@Override
				public int getX() {
					return original.getX();
				}
				
				@Override
				public BufferedImage getImage() {
					BufferedImage img = original.getImage();
					return img!=null ? BackgroundMapUtils.fadeWithGreyscale(img, fadeConfig):null;
				}
			};
		}
		return original;
	}

	public void addLoadedListener(MapTileLoadedListener listener) {
		factory.addLoadedListener(listener);
	}

	public void removeLoadedListener(MapTileLoadedListener listener) {
		factory.removeLoadedListener(listener);
	}

	public int getTileSize(int zoom) {
		return factory.getTileSize(zoom);
	}

	public Dimension getMapSize(int zoom) {
		return factory.getMapSize(zoom);
	}

	public GeoPosition pixelToGeo(Point2D pixelCoordinate, int zoom) {
		return factory.pixelToGeo(pixelCoordinate, zoom);
	}

	public Point2D geoToPixel(GeoPosition c, int zoomLevel) {
		return factory.geoToPixel(c, zoomLevel);
	}

	public TileFactoryInfo getInfo() {
		return factory.getInfo();
	}

	public void dispose() {
		factory.dispose();
	}

	public boolean isRenderedOffline() {
		return factory.isRenderedOffline();
	}

	public BufferedImage renderSynchronously(int x, int y, int zoom) {
		// get the image and apply the fade
		BufferedImage img= factory.renderSynchronously(x, y, zoom);
		return img!=null ? BackgroundMapUtils.fadeWithGreyscale(img, fadeConfig):null;
	}
	
}
