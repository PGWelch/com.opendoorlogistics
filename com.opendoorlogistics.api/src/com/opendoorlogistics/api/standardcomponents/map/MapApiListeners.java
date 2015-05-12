package com.opendoorlogistics.api.standardcomponents.map;


import java.awt.Graphics2D;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.event.MouseInputListener;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public interface MapApiListeners {
	interface OnChangeListener{
		void onChanged(MapApi api);
	}
	
	interface OnObjectsChanged{
		void onObjectsChanged(MapApi api);
	}
	
	interface OnBuildToolbarListener{
		void onBuildToolbar(MapApi api,MapToolbar toolBar);
	}

//	/**
//	 * Callback called just prior to the map objects changing
//	 * @author Phil
//	 *
//	 */
//	interface OnPreObjectsChanged{
//		void onPreObjectsChanged(MapApi api,ODLDatastore<? extends ODLTable> newMapDatastore);
//	}
	
	interface OnBuildContextMenu{
		void onBuildContextMenu(MapApi api,MapPopupMenu menu);
	}
	
	interface OnPaintListener{
		void onPaint(MapApi api,Graphics2D g);
	}

	interface OnModeChangeListener{
		void onModeChange(MapApi api,MapMode oldMode, MapMode newMode);
	}
	
	interface OnDisposedListener{
		void onDispose(MapApi api);
	}
	
	interface ModifyImageListener{
		BufferedImage modifyMapImage(MapApi api,BufferedImage img);
	}
	
	interface FilterVisibleObjects{
		void startFilter(MapApi api,ODLDatastore<? extends ODLTable> newMapDatastore);
		boolean acceptObject(ODLTableReadOnly table, int row);
		void endFilter(MapApi api);
	}

	
//	interface ObjectFilterFactory{
//		FilterVisibleObjects createObjectFilter(MapApi api,ODLDatastore<? extends ODLTable> newMapDatastore);
//	}
	
	interface OnToolTipListener{
		void onToolTip(MapApi api,MouseEvent evt,long [] objectIdsUnderMouse,StringBuilder currentTip);
	}
	
	void registerObjectsChangedListener(OnObjectsChanged listener, int priority);
	
	//void registerObjectFilterFactory(ObjectFilterFactory factory, int priority);
	
	void registerFilterVisibleObjectsListener(FilterVisibleObjects listener, int priority);
	
	void registerOnBuildToolbarListener(OnBuildToolbarListener listener, int priority);
	
	void registerOnBuildContextMenuListener(OnBuildContextMenu listener, int priority);
	
	void registerMouseInputListener(MouseInputListener listener, int priority);
	
	void registerModeChangingListener(OnModeChangeListener listener, int priority);
	
	void registerModeChangedListener(OnModeChangeListener listener, int priority);
	
	void registerOnPaintListener(OnPaintListener listener, int priority);
	
	void registerViewChanged(OnChangeListener listener, int priority);

	void registerSelectionChanged(OnChangeListener listener, int priority);

	void registerDisposedListener(OnDisposedListener listener, int priority);
	
	void registerKeyListener(KeyListener listener, int priority);
	
	void registerOnTooltipListener(OnToolTipListener listener, int priority);

	void registerModifyMapImage(ModifyImageListener listener, int priority);

//	void registerPreObjectsChangedListener(OnPreObjectsChanged listener, int priority);

	void removeObjectsChangedListener(OnObjectsChanged listener);
	
	void removeOnBuildToolbarListener(OnBuildToolbarListener listener);
	
	void removeOnBuildContextMenuListener(OnBuildContextMenu listener);
	
	void removeMouseInputListener(MouseInputListener listener);
	
	void removeModeChangingListener(OnModeChangeListener listener);
	
	void removeModeChangedListener(OnModeChangeListener listener);
	
	void removeOnPaintListener(OnPaintListener listener);
	
	void removeViewChanged(OnChangeListener listener);

	void removeSelectionChangedListener(OnChangeListener listener);

	void removeOnDisposedListener(OnDisposedListener listener);
	
	void removeKeyListener(KeyListener listener);
	
	void removeFilterVisibleObjectsListener(FilterVisibleObjects listener);
	
	void removeOnToolTipListener(OnToolTipListener listener);
	
	void removeModifyMapImage(ModifyImageListener listener);

	//void removePreObjectsChangedListener(OnPreObjectsChanged listener);

//void removeObjectFilterFactory(ObjectFilterFactory factory);
}
