package com.opendoorlogistics.studio.components.map.plugins;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

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

public class CreatePointPlugin implements MapPlugin, MapActionFactory{
	
	private final static long NEEDS_FLAGS = TableFlags.UI_INSERT_ALLOWED | TableFlags.UI_SET_ALLOWED;

	@Override
	public String getId(){
		return "com.opendoorlogistics.studio.components.map.plugins.CreatePointPlugin";
	}
	
	@Override
	public void initMap(MapApi api) {
		PluginUtils.registerActionFactory(api, this, StandardMapMenuOrdering.ADD_MODE, "mapmode",NEEDS_FLAGS);
	}

	@Override
	public Action create(final MapApi api) {
		AbstractAction action = new AbstractAction()
		{
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!PluginUtils.exitIfInMode(api, AddMode.class)){
					api.setMapMode(new AddMode(api));					
				}
			}
		};
		
		PluginUtils.initAction("Create point", "Create new points by clicking on the map.", "new_item_16x16.png", action);
		return action;
	}
	
	private static class AddMode extends AbstractMapMode {
		private final MapApi api;

		public AddMode(MapApi api) {
			this.api = api;
		}
		
		@Override
		public Cursor getCursor() {
			return PluginUtils.createCursor("new_item_32x32.png", 17, 16);
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);
			
			MapDataApi mdapi = api.getMapDataApi();
			mdapi.runTransactionOnGlobalDatastore(new Callable<Boolean>() {
				
				@Override
				public Boolean call() throws Exception {
					ODLTable table = mdapi.getUnfilteredActiveTable();
					if(table!=null){
						final int rowIndx = table.createEmptyRow(-1);
						if(rowIndx==-1){
							return false;
						}
						LatLong ll = api.createImmutableConverter().getLongLat(e.getX(), e.getY());
						table.setValueAt(ll.getLatitude(), rowIndx, mdapi.getLatitudeColumn());
						table.setValueAt(ll.getLongitude(), rowIndx, mdapi.getLongitudeColumn());
						SwingUtilities.invokeLater(new Runnable() {
							
							@Override
							public void run() {
								api.setSelectedIds(new long[]{table.getRowId(rowIndx)});
							}
						});
					}
					return true;
				}
			});

		}

		@Override
		public void onObjectsChanged(MapApi api) {
			PluginUtils.exitModeIfNeeded(api, AddMode.class, NEEDS_FLAGS,false);
		}
	}
}
