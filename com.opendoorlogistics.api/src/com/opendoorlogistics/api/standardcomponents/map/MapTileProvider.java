package com.opendoorlogistics.api.standardcomponents.map;

import java.awt.image.BufferedImage;

import com.opendoorlogistics.api.ui.Disposable;

public interface MapTileProvider extends Disposable {
	public MapTile getMapTile(int x, int y, int zoom);
	public void addLoadedListener(MapTileLoadedListener listener);
	public void removeLoadedListener(MapTileLoadedListener listener);
	
	public interface MapTile{
		BufferedImage getImage();
		int getX();
		int getY();
		int getZoom();
		boolean isLoaded();
	}
	
	public interface MapTileLoadedListener{
		void tileLoaded(MapTile tile);
	}
}
