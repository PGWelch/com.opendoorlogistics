package com.opendoorlogistics.studio.components.map.plugins;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.standardcomponents.map.MapActionFactory;
import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapDataApi;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.StandardMapMenuOrdering;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.studio.components.map.AbstractMapMode;
import com.opendoorlogistics.studio.components.map.plugins.utils.PluginUtils;

public class MovePointPlugin implements MapPlugin, MapActionFactory{
	private static final long NEEDS_FLAGS = TableFlags.UI_SET_ALLOWED;

	@Override
	public void initMap(MapApi api) {
		PluginUtils.registerActionFactory(api, this, StandardMapMenuOrdering.MOVE_MODE, "mapmode",NEEDS_FLAGS);
	}

	@Override
	public String getId(){
		return "com.opendoorlogistics.studio.components.map.plugins.MovePointPlugin";
	}

	@Override
	public Action create(final MapApi api) {
		AbstractAction action = new AbstractAction()
		{
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!PluginUtils.exitIfInMode(api, MovePointMode.class)){
					api.setMapMode(new MovePointMode(api));					
				}
			}
		};
		
		PluginUtils.initAction("Move point","Move the selected point around by clicking on its new position", "tool-move-16x16.png", action);
		return action;
	}
	
	private static class MovePointMode extends AbstractMapMode {
		private final MapApi api;

		public MovePointMode(MapApi api) {
			this.api = api;
		}
		
		@Override
		public Cursor getCursor() {
			return PluginUtils.createCursor("tool-move.png", 17, 15);
		}
		

		@Override
		public void onObjectsChanged(MapApi api) {
			PluginUtils.exitModeIfNeeded(api, MovePointMode.class, NEEDS_FLAGS,false);
		}
		
		@Override
		public void mouseDragged(MouseEvent evt) {
			super.mouseDragged(evt);
			if(isDragging()){
				movePoint(evt);
				
			}
		}

		@Override
	    public void mouseClicked(MouseEvent e) {
			movePoint(e);
	    }
	    
		private void movePoint(MouseEvent evt) {
			final long [] selectedIds = api.getSelectedIds();
			if(selectedIds == null || selectedIds.length == 0 ){
				JOptionPane.showMessageDialog(api.getMapWindowComponent(), "No point selected to move");
			}
			else if (selectedIds.length>1){
				JOptionPane.showMessageDialog(api.getMapWindowComponent(), "Cannot move more than one point at a time.");					
			}
			else{
				final MapDataApi mdapi = api.getMapDataApi();
				mdapi.runTransactionOnGlobalDatastore(new Callable<Boolean>() {
					
					@Override
					public Boolean call() throws Exception {
						LatLong ll = api.createImmutableConverter().getLongLat(evt.getX(), evt.getY());
						ODLTable drawables = mdapi.getUnfilteredActiveTable();
						if(drawables!=null){
							long id = selectedIds[0];
							drawables.setValueById(ll.getLatitude(), id, mdapi.getLatitudeColumn());
							drawables.setValueById(ll.getLongitude(), id, mdapi.getLongitudeColumn());
						}
						return true;
					}
				});
			}
		}

		


	}
}
