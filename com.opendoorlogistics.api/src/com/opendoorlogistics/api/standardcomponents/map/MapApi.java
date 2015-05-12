package com.opendoorlogistics.api.standardcomponents.map;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;


/**
 * An API for the map to be used by the modes
 * @author Phil
 *
 */
public interface MapApi extends MapApiListeners{

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
	Component getMapWindowComponent();
	int getMaxZoom();
	int getMinZoom();
	MapMode getMapMode();
	MapMode getDefaultMapMode();
	ODLApi getApi();
	long getRenderFlags();
	long [] getSelectableIdsWithinPixelRectangle(Rectangle screenCoordinatesRectangle);
	long [] getSelectedIds();
	boolean isSelectedId(long rowId);	
	Point2D getWorldBitmapMapCentre();
	Dimension getWorldBitmapMapSize(int zoom);
	Point getWorldBitmapPosition(LatLong ll, int zoom);
	Rectangle getWorldBitmapViewport();
	MapToolbar getMapToolbar();
	int getZoom();
	void repaint(boolean repaintPluginOverlapOnly);
	void setCursor(Cursor cursor);
	void setMapMode(MapMode mapMode);
	void setDefaultMapMode(MapMode mode);
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

	/**
	 * The map api maintains a single worker thread which can have tasks added to it and
	 * executed in-order.
	 * @param runnable
	 */
	void submitWork(Runnable runnable);
	
	/**
	 * Has the api been disposed (and should no longer be used?)
	 * @return
	 */
	boolean isDisposed();
}

