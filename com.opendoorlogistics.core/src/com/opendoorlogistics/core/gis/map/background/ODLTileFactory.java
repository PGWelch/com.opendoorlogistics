package com.opendoorlogistics.core.gis.map.background;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import com.opendoorlogistics.api.standardcomponents.map.MapTileProvider;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;

public interface ODLTileFactory extends MapTileProvider {


	public int getTileSize(int zoom);

	public Dimension getMapSize(int zoom);
	
	//public Tile getTile(int x, int y, int zoom);


	public GeoPosition pixelToGeo(Point2D pixelCoordinate, int zoom);
	
	public Point2D geoToPixel(GeoPosition c, int zoomLevel);

	public TileFactoryInfo getInfo();

//	public void addTileListener(TileListener listener);
	
//	public void removeTileListener(TileListener listener);

	public void dispose();

	public boolean isRenderedOffline();
	
	public abstract BufferedImage renderSynchronously(int x, int y, int zoom);

}
