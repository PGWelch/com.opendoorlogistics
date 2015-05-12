package com.opendoorlogistics.studio.components.map.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnBuildContextMenu;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnBuildToolbarListener;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnChangeListener;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnDisposedListener;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.MapPopupMenu;
import com.opendoorlogistics.api.standardcomponents.map.MapToolbar;
import com.opendoorlogistics.api.standardcomponents.map.StandardMapMenuOrdering;
import com.opendoorlogistics.codefromweb.DropDownMenuButton;
import com.opendoorlogistics.utils.ui.Icons;

public class ViewSyncer implements MapPlugin, OnChangeListener{
	private HashMap<MapApi,SyncerState> states = new HashMap<MapApi,ViewSyncer.SyncerState>();
	private boolean syncOngoing;
	private static final ImageIcon SYNCED_ICON = Icons.loadFromStandardPath("map-synced.png");
	private static final ImageIcon UNSYNCED_ICON = Icons.loadFromStandardPath("map-unsynced.png");

	
	@Override
	public String getId() {
		return "com.opendoorlogistics.studio.components.plugins.viewsyncer";
	}

	@Override
	public void initMap(MapApi api) {
		// add the state to the list maintained by the plugin
		final SyncerState state = new SyncerState(api);
		states.put(api, state);
		
		// but remove it when the map is disposed
		api.registerDisposedListener(new OnDisposedListener() {
			
			@Override
			public void onDispose(MapApi api) {
				states.remove(api);
				api.removeViewChanged(ViewSyncer.this);
			}
		}, 0);
		
		// add the sync button (which has the popup menu) to the toolbar
		api.registerOnBuildToolbarListener(new OnBuildToolbarListener() {
			
			@Override
			public void onBuildToolbar(MapApi api, MapToolbar toolBar) {
				toolBar.add(state.button, "ViewSync");
			}
		}, StandardMapMenuOrdering.SYNC);
		
		// create the context menu
		api.registerOnBuildContextMenuListener(new OnBuildContextMenu() {
			
			@Override
			public void onBuildContextMenu(MapApi api, MapPopupMenu menu) {
				for(Action a : state.createActions()){
					menu.add(a, "ViewSync");
				}
			}
		}, StandardMapMenuOrdering.SYNC);
		
		// add this listener which does the syncing
		api.registerViewChanged(this, 0);
	}

	private class SyncerState{
		private final MapApi api;
		private boolean syncing;
		private final DropDownMenuButton button = new DropDownMenuButton(UNSYNCED_ICON,false) {
			
			@Override
			protected JPopupMenu getPopupMenu() {
				if(syncing){
					// no menu needed ... just stop syncing (which is done by the other listener)
					return null;
				}
				
				JPopupMenu popup = new JPopupMenu();
				for(Action a : createActions()){
					popup.add(a);
				}
				return popup;
			}
		};
		
		SyncerState(MapApi api) {
			this.api = api;
			
			button.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if(syncing){
						setSyncing(false);
					}else{
						button.processPopup();
					}
				}
			});
		}
		
		private void setSyncing(boolean on){
			if(on!=syncing){
				syncing = on;
				button.setIcon(on ? SYNCED_ICON : UNSYNCED_ICON);
			}
			
			
		}
		
		List<Action> createActions(){
			boolean wasSyncing = syncing;
			ArrayList<Action> ret = new ArrayList<Action>();
			if(syncing){
				ret.add(new AbstractAction("Stop syncing view", UNSYNCED_ICON) {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						setSyncing(false);
					}
				});
			}
			else{
				ret.add(new AbstractAction("Sync to other view", SYNCED_ICON) {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						setSyncing(true);
						for(SyncerState other : states.values()){
							if(other!=SyncerState.this){
								other.api.setView(other.api.getZoom(), other.api.getWorldBitmapMapCentre());
								break;
							}
						}
					}
				});
				
				ret.add(new AbstractAction("Sync others to my view", SYNCED_ICON) {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						setSyncing(true);
						api.setView(api.getZoom(), api.getWorldBitmapMapCentre());
					}
				});
			}
			
			return ret;
		}
	}

	@Override
	public void onChanged(MapApi api) {
		// are we already in an sync?
		if(syncOngoing){
			return;
		}
		
		// should this state trigger a sync?
		SyncerState state = states.get(api);
		if(state!=null && state.syncing){
			
			// then trigger a sync in all other ones
			syncOngoing = true;
			try {
				for(SyncerState other : states.values()){
					if(other!=state && other.syncing){
						other.api.setView(api.getZoom(), api.getWorldBitmapMapCentre());
					}
				}				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			finally{
				syncOngoing = false;			
			}
		}
		
	}

}
