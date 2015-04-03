package com.opendoorlogistics.studio.components.map.v2.plugins;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMapping;
import com.opendoorlogistics.studio.components.map.v2.AbstractMapMode;
import com.opendoorlogistics.studio.components.map.v2.MapApi;
import com.opendoorlogistics.studio.components.map.v2.MapDataApi;
import com.opendoorlogistics.studio.components.map.v2.MapPlugin;
import com.opendoorlogistics.studio.components.map.v2.plugins.PluginUtils.ActionFactory;

public class MovePointPlugin implements MapPlugin, ActionFactory{

	@Override
	public void initMap(MapApi api) {
		PluginUtils.registerActionFactory(api, this, StandardOrdering.MOVE_MODE, "mapmode");
	}

	

	@Override
	public Action create(final MapApi api) {
		AbstractAction action = new AbstractAction()
		{
			
			@Override
			public void actionPerformed(ActionEvent e) {
				api.setMapMode(new MovePointMode(api));
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
		public void mouseDragged(MouseEvent evt) {
			super.mouseDragged(evt);
			if(isDragging()){
				final long [] selectedIds = api.getSelectedIds();
				if(selectedIds == null || selectedIds.length == 0 ){
					JOptionPane.showMessageDialog(api.getMapUIComponent(), "No point selected to move");
				}
				else if (selectedIds.length>1){
					JOptionPane.showMessageDialog(api.getMapUIComponent(), "Cannot move more than one point at a time.");					
				}
				else{
					final MapDataApi mdapi = api.getMapDataApi();
					mdapi.runTransactionOnGlobalDatastore(new Callable<Boolean>() {
						
						@Override
						public Boolean call() throws Exception {
							LatLong ll = api.createImmutableConverter().getLongLat(evt.getX(), evt.getY());
							ODLTable drawables = mdapi.getUnfilteredDrawableTable();
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
}
