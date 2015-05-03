package com.opendoorlogistics.studio.components.map;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.event.MouseInputListener;

import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners;
import com.opendoorlogistics.api.standardcomponents.map.MapMode;
import com.opendoorlogistics.api.standardcomponents.map.MapToolbar;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.utils.Pair;

public class MapApiListenersImpl implements MapApiListeners, MouseInputListener, KeyListener{

	private class Listeners<T> implements Iterable<T>{
		ArrayList<Pair<T, Integer>> listeners = new ArrayList<Pair<T,Integer>>();
		
		synchronized void register(T listener, int priority){
			Pair<T, Integer> pair = new Pair<T, Integer>(listener, priority);
			listeners.add(pair);
			Collections.sort(listeners, new Comparator<Pair<T, Integer>>() {

				@Override
				public int compare(Pair<T, Integer> o1, Pair<T, Integer> o2) {
					return o1.getSecond().compareTo(o2.getSecond());
				}
			});
		}
		
		synchronized void remove(T listener){
			Iterator<Pair<T, Integer>> it = listeners.iterator();
			while(it.hasNext()){
				if(it.next().getFirst() == listener){
					it.remove();
				}
			}
		}
		
		public synchronized int size(){
			return listeners.size();
		}

		@Override
		public Iterator<T> iterator() {
			return new Iterator<T>() {
				int index=-1;
				
				@Override
				public boolean hasNext() {
					return (index+1) < listeners.size();
				}

				@Override
				public T next() {
					return listeners.get(++index).getFirst();
				}
			};
		}
	}
	
	private final Listeners<OnObjectsChanged> objsChangedListener = new Listeners<MapApiListeners.OnObjectsChanged>();
	private final Listeners<OnBuildToolbarListener> buildToolbarListeners = new Listeners<MapApiListeners.OnBuildToolbarListener>();
	private final Listeners<OnBuildContextMenu> buildContextMenuListeners = new Listeners<MapApiListeners.OnBuildContextMenu>();
	private final Listeners<MouseInputListener> mouseInputListeners = new Listeners<MouseInputListener>();
	private final Listeners<OnModeChangeListener> modeChangingListener = new Listeners<OnModeChangeListener>();
	private final Listeners<OnModeChangeListener> modeChangedListener = new Listeners<OnModeChangeListener>();
	private final Listeners<OnPaintListener> paintListeners = new Listeners<OnPaintListener>();
	private final Listeners<OnChangeListener> viewChangedListener = new Listeners<MapApiListeners.OnChangeListener>();
	private final Listeners<OnChangeListener> selectionChangedListener = new Listeners<MapApiListeners.OnChangeListener>();
	private final Listeners<OnDisposedListener> disposedListeners = new Listeners<MapApiListeners.OnDisposedListener>();
	private final Listeners<KeyListener> keyListeners = new Listeners<KeyListener>();
	private final Listeners<FilterVisibleObjects> filters = new Listeners<FilterVisibleObjects>();	
	private final Listeners<OnToolTipListener> tooltipListeners = new Listeners<OnToolTipListener>();
	private final Listeners<ModifyImageListener> modifyImageListeners = new Listeners<ModifyImageListener>();
	//private final Listeners<OnPreObjectsChanged> preObjectsChanged = new Listeners<OnPreObjectsChanged>();
//	private final Listeners<ObjectFilterFactory> objectFilterFactories = new Listeners<ObjectFilterFactory>();
	
	
	
	//private final Listeners<MouseInputListener> mouseInputListeners = new Listeners<MouseInputListener>();
	
	@Override
	public void registerObjectsChangedListener(OnObjectsChanged listener, int priority) {
		objsChangedListener.register(listener, priority);
	}

	@Override
	public void registerOnBuildToolbarListener(OnBuildToolbarListener listener, int priority) {
		buildToolbarListeners.register(listener, priority);
	}

	@Override
	public void registerOnBuildContextMenuListener(OnBuildContextMenu listener, int priority) {
		buildContextMenuListeners.register(listener, priority);
	}

	@Override
	public void registerMouseInputListener(MouseInputListener listener, int priority) {
		mouseInputListeners.register(listener, priority);
	}


	@Override
	public void registerModeChangingListener(OnModeChangeListener listener, int priority) {
		modeChangingListener.register(listener, priority);
	}

	@Override
	public void registerModeChangedListener(OnModeChangeListener listener, int priority) {
		modeChangedListener.register(listener, priority);
	}

	@Override
	public void registerOnPaintListener(OnPaintListener listener, int priority) {
		paintListeners.register(listener, priority);
	}

	@Override
	public void removeObjectsChangedListener(OnObjectsChanged listener) {
		objsChangedListener.remove(listener);
	}

	@Override
	public void removeOnBuildToolbarListener(OnBuildToolbarListener listener) {
		buildToolbarListeners.remove(listener);
	}

	@Override
	public void removeOnBuildContextMenuListener(OnBuildContextMenu listener) {
		buildContextMenuListeners.remove(listener);
	}

	@Override
	public void removeMouseInputListener(MouseInputListener listener) {
		mouseInputListeners.remove(listener);
	}

	@Override
	public void removeModeChangingListener(OnModeChangeListener listener) {
		modeChangingListener.remove(listener);
	}

	@Override
	public void removeModeChangedListener(OnModeChangeListener listener) {
		modeChangedListener.remove(listener);
	}

	@Override
	public void removeOnPaintListener(OnPaintListener listener) {
		paintListeners.remove(listener);
	}

	public void fireObjectsChangedListeners(MapApi api){
		//LayeredDrawables filtered = original;
		for(OnObjectsChanged l : objsChangedListener){
		//	filtered = l.onObjectsChanged(api,original, filtered);
			l.onObjectsChanged(api);
		}
		//return filtered;
	}
	
	public boolean fireFilterObject(MapApi api, ODLTableReadOnly table, int row){
		for(FilterVisibleObjects l : filters){
			if(!l.acceptObject(table, row)){
				return false;
			}
		}
		return true;
	}
	
	public void fireSelectionChangedListeners(MapApi api){
		for(OnChangeListener l : selectionChangedListener){
			l.onChanged(api);
		}
	}
	
	public void fireViewChangedListeners(MapApi api){
		for(OnChangeListener l : viewChangedListener){
			l.onChanged(api);
		}
	}
	
	public void fireOnPaintListeners(MapApi api,Graphics2D g){
		for(OnPaintListener l : paintListeners){
			l.onPaint(api,g);
		}
	}
	
	public void fireBuildToolbarListeners(MapApi api,MapToolbar toolbar){
		for(OnBuildToolbarListener l : buildToolbarListeners){
			l.onBuildToolbar(api,toolbar);
		}
	}
	
	public void fireBuildContextMenuListeners(MapApi api,MapPopupMenuImpl menu){
		for(OnBuildContextMenu l : buildContextMenuListeners){
			l.onBuildContextMenu(api,menu);
		}
	}
	
	public void fireModeChangingListener(MapApi api,MapMode oldMode, MapMode newMode){
		for(OnModeChangeListener l : modeChangingListener){
			l.onModeChange(api,oldMode, newMode);
		}
	}
	
	public void fireModeChangedListener(MapApi api,MapMode oldMode, MapMode newMode){
		for(OnModeChangeListener l : modeChangedListener){
			l.onModeChange(api,oldMode, newMode);
		}
	}

	
	public void fireTooltipListeners(MapApi api,MouseEvent evt,long [] objectIdsUnderMouse,StringBuilder currentTip){
		for(OnToolTipListener l : tooltipListeners){
			l.onToolTip(api, evt, objectIdsUnderMouse, currentTip);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		for(MouseInputListener l : mouseInputListeners){
			l.mouseClicked(e);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		for(MouseInputListener l : mouseInputListeners){
			l.mousePressed(e);
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		for(MouseInputListener l : mouseInputListeners){
			l.mouseDragged(e);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		for(MouseInputListener l : mouseInputListeners){
			l.mouseMoved(e);
		}
	}

	
	@Override
	public void mouseEntered(MouseEvent e) {
		for(MouseInputListener l : mouseInputListeners){
			l.mouseEntered(e);
		}
	}


	@Override
	public void mouseReleased(MouseEvent e) {
		for(MouseInputListener l : mouseInputListeners){
			l.mouseReleased(e);
		}
	}


	@Override
	public void mouseExited(MouseEvent e) {
		for(MouseInputListener l : mouseInputListeners){
			l.mouseExited(e);
		}
	}

	@Override
	public void registerViewChanged(OnChangeListener listener, int priority) {
		viewChangedListener.register(listener, priority);
	}

	@Override
	public void removeViewChanged(OnChangeListener listener) {
		viewChangedListener.remove(listener);
	}

	@Override
	public void registerSelectionChanged(OnChangeListener listener, int priority) {
		selectionChangedListener.register(listener, priority);
	}

	@Override
	public void removeSelectionChangedListener(OnChangeListener listener) {
		selectionChangedListener.remove(listener);
	}

	@Override
	public void registerDisposedListener(OnDisposedListener listener, int priority) {
		disposedListeners.register(listener, priority);
	}

	@Override
	public void removeOnDisposedListener(OnDisposedListener listener) {
		disposedListeners.remove(listener);
	}
	
	public void fireDisposedListeners(MapApi api){
		for(OnDisposedListener l : disposedListeners){
			l.onDispose(api);
		}
	}

//	public List<FilterVisibleObjects> fireCreateObjectFilters(MapApi api,ODLDatastore<? extends ODLTable> newMapDatastore){
//		ArrayList<FilterVisibleObjects> ret = new ArrayList<MapApiListeners.FilterVisibleObjects>(objectFilterFactories.size());
//		for(ObjectFilterFactory factory : objectFilterFactories){
//			FilterVisibleObjects filter = factory.createObjectFilter(api,newMapDatastore);
//			if(filter!=null){
//				ret.add(filter);
//			}
//		}
//		return ret;
//	}
	
	@Override
	public void registerKeyListener(KeyListener listener, int priority) {
		keyListeners.register(listener, priority);
	}

	@Override
	public void removeKeyListener(KeyListener listener) {
		keyListeners.remove(listener);
	}

	@Override
	public void keyTyped(KeyEvent e) {
		for(KeyListener l : keyListeners){
			l.keyTyped(e);
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		for(KeyListener l : keyListeners){
			l.keyPressed(e);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		for(KeyListener l : keyListeners){
			l.keyReleased(e);
		}
	}

	@Override
	public void registerFilterVisibleObjectsListener(FilterVisibleObjects listener, int priority) {
		filters.register(listener, priority);
	}

	@Override
	public void removeFilterVisibleObjectsListener(FilterVisibleObjects listener) {
		filters.remove(listener);
	}

	@Override
	public void registerOnTooltipListener(OnToolTipListener listener, int priority) {
		tooltipListeners.register(listener, priority);
	}

	@Override
	public void removeOnToolTipListener(OnToolTipListener listener) {
		tooltipListeners.remove(listener);
	}

	@Override
	public void registerModifyMapImage(ModifyImageListener listener, int priority) {
		modifyImageListeners.register(listener, priority);
	}

	@Override
	public void removeModifyMapImage(ModifyImageListener listener) {
		modifyImageListeners.remove(listener);
	}
	
	public BufferedImage fireModifyMapImageListeners(MapApi api,BufferedImage mapImage){
		for(ModifyImageListener l : modifyImageListeners){
			mapImage = l.modifyMapImage(api,mapImage);
		}
		return mapImage;
	}

//	@Override
//	public void registerPreObjectsChangedListener(OnPreObjectsChanged listener, int priority) {
//		preObjectsChanged.register(listener, priority);
//	}
//
//	@Override
//	public void removePreObjectsChangedListener(OnPreObjectsChanged listener) {
//		preObjectsChanged.remove(listener);
//	}
//	
//	public void firePreObjectsChangedListener(MapApi api, ODLDatastore<? extends ODLTable> newMapDatastore){
//		for(OnPreObjectsChanged l : preObjectsChanged){
//			l.onPreObjectsChanged(api, newMapDatastore);
//		}
//	}
	
	public void fireStartObjectFiltering(MapApi api, ODLDatastore<? extends ODLTable> newMapDatastore){
		for(FilterVisibleObjects filter : filters){
			filter.startFilter(api, newMapDatastore);
		}
	}
	
	public void fireEndObjectFiltering(MapApi api){
		for(FilterVisibleObjects filter : filters){
			filter.endFilter(api);
		}
	}


//	@Override
//	public void registerObjectFilterFactory(ObjectFilterFactory factory, int priority) {
//		objectFilterFactories.register(factory, priority);
//	}
//
//	@Override
//	public void removeObjectFilterFactory(ObjectFilterFactory factory) {
//		objectFilterFactories.remove(factory);
//	}
}
