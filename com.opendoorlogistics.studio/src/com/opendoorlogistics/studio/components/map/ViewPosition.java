package com.opendoorlogistics.studio.components.map;

import java.awt.geom.Point2D;

import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.core.gis.map.background.BackgroundTileFactorySingleton;

public class ViewPosition {
	/**
	 * The zoom level. Generally a value between 1 and 15 (TODO Is this true for
	 * all the mapping worlds? What does this mean if some mapping system
	 * doesn't support the zoom level?
	 */
	private int zoomLevel = 1;

	/**
	 * The position, in <I>map coordinates</I> of the center point. This is
	 * defined as the distance from the top and left edges of the map in pixels.
	 * Dragging the map component will change the center position. Zooming
	 * in/out will cause the center to be recalculated so as to remain in the
	 * center of the new "map".
	 */
	private Point2D center = new Point2D.Double(0, 0);
	
	public ViewPosition(){
		TileFactoryInfo info =BackgroundTileFactorySingleton.getFactory().getInfo(); 
		if (info != null) {
			setZoom(info.getMaximumZoomLevel());
			center = info.getMapCenterInPixelsAtZoom(zoomLevel);
		}		
	}
	
	public int getZoom() {
		return zoomLevel;
	}

	public void setZoom(int zoomLevel) {
		int [] range = zoomRange();
		if(zoomLevel < range[0]){
			zoomLevel = range[0];
		}
		else if(zoomLevel > range[1]){
			zoomLevel = range[1];
		}
		this.zoomLevel = zoomLevel;
	}

	public Point2D getCenter() {
		return center;
	}

	public void setCenter(Point2D center) {
		this.center = center;
	}
	
	private int [] zoomRange(){
		int low = BackgroundTileFactorySingleton.getFactory().getInfo().getMaximumZoomLevel();
		int high = BackgroundTileFactorySingleton.getFactory().getInfo().getMinimumZoomLevel();
		if(low > high){
			int tmp = high;
			high = low;
			low = tmp;
		}
		return new int[]{low,high};
	}
	
	public int getMinZoom(){
		return zoomRange()[0];
	}
	
	public int getMaxZoom(){
		return zoomRange()[1];
	}
}
