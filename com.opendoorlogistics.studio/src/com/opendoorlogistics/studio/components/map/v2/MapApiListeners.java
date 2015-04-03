package com.opendoorlogistics.studio.components.map.v2;


import java.awt.Graphics2D;
import java.awt.event.KeyListener;

import javax.swing.event.MouseInputListener;

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
	
	interface FilterVisibleObjects{
		boolean acceptObject(ODLTableReadOnly table, int row);
	}
	
	void registerObjectsChangedListener(OnObjectsChanged listener, int priority);
	
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
}
