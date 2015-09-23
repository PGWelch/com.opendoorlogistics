package com.opendoorlogistics.core.gis.map.background;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;

import com.opendoorlogistics.api.standardcomponents.map.MapTileProvider;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.Tile;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactory;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileListener;

public class TileFactory2ODL implements ODLTileFactory{
	private final TileFactory tileFactory;
	private final boolean disposeFactory;
	private final FadeConfig fade;
	private final HashSet<MapTileLoadedListener> listeners = new HashSet<MapTileProvider.MapTileLoadedListener>();
	private final TileListener baseListener = new TileListener() {
		
		@Override
		public void tileLoaded(Tile tile) {
			MapTile myTile = wrapTile(tile,fade);
			for(MapTileLoadedListener l : listeners){
				l.tileLoaded(myTile);
			}
		}
	};
	
	public TileFactory2ODL(TileFactory tileFactory, boolean disposeFactory, FadeConfig fade) {
		this.tileFactory = tileFactory;
		this.disposeFactory = disposeFactory;
		this.fade = fade;
	}

	@Override
	public void dispose() {
		if(disposeFactory && tileFactory!=null){
			tileFactory.dispose();
		}
		
		tileFactory.removeTileListener(baseListener);
	}

	@Override
	public MapTile getMapTile(int x, int y, int zoom) {
		Tile tile = tileFactory.getTile(x, y, zoom);
		if(tile!=null){
			return wrapTile(tile,fade);			
		}
		return null;
	}

	public static MapTile wrapTile(Tile tile, FadeConfig fade) {
		return new MapTile() {
			
			@Override
			public boolean isLoaded() {
				return tile.isLoaded();
			}
			
			@Override
			public int getZoom() {
				return tile.getZoom();
			}
			
			@Override
			public int getY() {
				return tile.getY();
			}
			
			@Override
			public int getX() {
				return tile.getX();
			}
			
			@Override
			public BufferedImage getImage() {
				BufferedImage ret = tile.getImage();
				if(ret!=null && fade!=null){
					ret = BackgroundMapUtils.fadeWithGreyscale(ret, fade);
				}
				return ret;
			}
		};
	}

	@Override
	public synchronized void addLoadedListener(MapTileLoadedListener listener) {
		// make sure I'm listening ... but not more than once (as underlying collection is a list not set)
		tileFactory.removeTileListener(baseListener);
		tileFactory.addTileListener(baseListener);
		
		// now add to my own listeners
		listeners.add(listener);
	}

	@Override
	public synchronized void removeLoadedListener(MapTileLoadedListener listener) {
		listeners.remove(listener);
		
		// stop listening to the base tile factory if i've got no listeners left
		if(listeners.size()==0){
			tileFactory.removeTileListener(baseListener);			
		}
	}

	@Override
	public int getTileSize(int zoom) {
		return tileFactory.getTileSize(zoom);
	}

	@Override
	public Dimension getMapSize(int zoom) {
		return tileFactory.getMapSize(zoom);
	}

	@Override
	public GeoPosition pixelToGeo(Point2D pixelCoordinate, int zoom) {
		return tileFactory.pixelToGeo(pixelCoordinate, zoom);
	}

	@Override
	public Point2D geoToPixel(GeoPosition c, int zoomLevel) {
		return tileFactory.geoToPixel(c, zoomLevel);
	}

	@Override
	public TileFactoryInfo getInfo() {
		return tileFactory.getInfo();
	}

	@Override
	public boolean isRenderedOffline() {
		return tileFactory.isRenderedOffline();
	}

	@Override
	public BufferedImage renderSynchronously(int x, int y, int zoom){
		return tileFactory.renderSynchronously(x, y, zoom);
	}
}
