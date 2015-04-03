package com.opendoorlogistics.studio.components.map.v2;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.concurrent.Callable;

import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;


/**
 * An API for the map to be used by the modes
 * @author Phil
 *
 */
public interface MapApi extends MapApiListeners, SelectedIdChecker{

	enum PanelPosition{
		//TOP,
		BOTTOM,
		LEFT,
		RIGHT
	}
	
	void clearSelection();
	/**
	 * Create a converter representing the current view which doesn't change if the view changes
	 * @return
	 */
	LatLongToScreen createImmutableConverter();
	ComponentControlLauncherApi getControlLauncherApi();
	MapDataApi getMapDataApi();
	Component getMapUIComponent();
	int getMaxZoom();
	int getMinZoom();
	long getRenderFlags();
	long [] getSelectableIdsWithinPixelRectangle(Rectangle screenCoordinatesRectangle);
	long [] getSelectedIds();
	Point2D getWorldBitmapMapCentre();
	Dimension getWorldBitmapMapSize(int zoom);
	Point getWorldBitmapPosition(LatLong ll, int zoom);
	Rectangle getWorldBitmapViewport();
	int getZoom();
	void repaint(boolean repaintPluginOverlapOnly);
	void setCursor(Cursor cursor);
	void setMapMode(MapMode mapMode);
	void setRenderFlags(long flags);
	void setSelectedIds(long... ids);
	<T extends JPanel & Disposable> void setSidePanel(T panel, PanelPosition pos);
	<T extends JPanel & Disposable> T getSidePanel( PanelPosition pos);
	
	void setView(int zoom, Point2D worldBitmapMapCentre);
	
	void setViewToBestFit(ODLTableReadOnly drawables);
	
	/**
	 * Keep the same centre but change the zoom
	 * @param zoom
	 */
	void setZoom(int zoom);


	void updateObjectFiltering();

}

