package com.opendoorlogistics.studio.components.map.plugins;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners;
import com.opendoorlogistics.api.standardcomponents.map.MapMode;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.MapPopupMenu;
import com.opendoorlogistics.api.standardcomponents.map.MapToolbar;
import com.opendoorlogistics.api.standardcomponents.map.StandardMapMenuOrdering;
import com.opendoorlogistics.studio.components.map.AbstractMapMode;
import com.opendoorlogistics.studio.components.map.plugins.utils.PluginUtils;
import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.utils.ui.SimpleAction;

public class PanMapPlugin implements MapPlugin{

	@Override
	public void initMap(MapApi api) {
		PanMapState ret = new PanMapState(api);
		api.registerOnBuildToolbarListener(ret, StandardMapMenuOrdering.NAVIGATE);
		api.registerOnBuildContextMenuListener(ret, 0);
		api.registerKeyListener(ret, 0);
		api.setMapMode(ret);
		api.setDefaultMapMode(ret);
	}
	

	@Override
	public String getId(){
		return "com.opendoorlogistics.studio.components.map.plugins.PanMapPlugin";
	}


	private static class PanMapState extends AbstractMapMode implements MapApiListeners.OnBuildToolbarListener,MapApiListeners.OnBuildContextMenu, MapMode{
		private final MapApi api;
		private final double pan = 0.25;	
		private final Cursor cursor;

		public PanMapState(MapApi api) {
			this.api = api;
			cursor = PluginUtils.createCursor("arrow-out-32x32.png", 17, 15);
		}

		@Override
		public void onBuildToolbar(final MapApi api, MapToolbar toolBar) {

			for(Action action : createActions()){
				toolBar.add(action, "navigate");				
			}
			
		}
		
		@Override
		public Cursor getCursor() {
			return cursor;
		}
		
		private class PanAction extends SimpleAction {
			double x;
			double y;
			int key;

			PanAction(String name, String tooltip, String smallIconPng, double x, double y, int keyEvent) {
				super(name, tooltip, smallIconPng);
				this.x = x;
				this.y = y;
				this.key = keyEvent;

				// register myself in action map
			//	registerActionKeyEvent(this, keyEvent);
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				Rectangle bounds = api.getWorldBitmapViewport();
				double dx = x * bounds.width;
				double dy = y * bounds.height;
				Point2D centre = api.getWorldBitmapMapCentre();
				centre = new Point2D.Double(centre.getX() + dx, centre.getY() + dy);
				api.setView(api.getZoom(), centre);
			}
		}

		public ArrayList<Action> createActions() {
			ArrayList<Action> ret = new ArrayList<Action>();
			
			ret.add(new SimpleAction("Zoom to all", "Zoom to all", "small_zoom_best_fit.png") {

				@Override
				public void actionPerformed(ActionEvent e) {
					api.setViewToBestFit(api.getMapDataApi().getFilteredAllLayersTable(true));
				}
			});
			
			Action zoomIn = new SimpleAction("Zoom in", "Zoom in", "zoom-in-3.png") {

				@Override
				public void actionPerformed(ActionEvent e) {
					api.setZoom(api.getZoom() - 1);
				}
			};
			// registerActionKeyEvent(zoomIn, KeyEvent.VK_PLUS);
			ret.add(zoomIn);

			Action zoomOut = new SimpleAction("Zoom out", "Zoom out", "zoom-out-3.png") {

				@Override
				public void actionPerformed(ActionEvent e) {
					api.setZoom(api.getZoom() + 1);
				}
			};
			ret.add(zoomOut);

			// create pan buttons
			ret.add(leftAction());
			ret.add(rightAction());
			ret.add(upAction());
			ret.add(downAction());
			
			// create move mode
			ImageIcon icon = Icons.loadFromStandardPath("arrow-out-16x16.png");
			AbstractAction action = new AbstractAction("Move mode",icon) {

				@Override
				public void actionPerformed(ActionEvent e) {
					api.setMapMode(PanMapState.this);
				}
			};
			
			PluginUtils.initAction("Move mode", "Drag the map to look around.", "arrow-out-16x16.png", action);
			ret.add(action);
			
			return ret;
		}

		private PanAction downAction() {
			return new PanAction("Pan right", "Pan right", "arrow-right-3-16x16.png", pan, 0, KeyEvent.VK_RIGHT);
		}

		private PanAction upAction() {
			return new PanAction("Pan down", "Pan down", "arrow-down-3-16x16.png", 0, pan, KeyEvent.VK_DOWN);
		}

		private PanAction rightAction() {
			return new PanAction("Pan up", "Pan up", "arrow-up-3-16x16.png", 0, -pan, KeyEvent.VK_UP);
		}

		private PanAction leftAction() {
			return new PanAction("Pan left", "Pan left", "arrow-left-3-16x16.png", -pan, 0, KeyEvent.VK_LEFT);
		}
		
		@Override
		public void mouseDragged(MouseEvent evt) {
			super.mouseDragged(evt);
			if(isDragging()){
				
				// do the move to account for the last drag
				Point2D centre = api.getWorldBitmapMapCentre();
				api.setView(api.getZoom(), new Point2D.Double(centre.getX() - getDragEndPoint().getX() + getDragStartPoint().getX(), 
						centre.getY()- getDragEndPoint().getY() + getDragStartPoint().getY()));
				
				// and update the drag start point as it is now effectively the current point
				setDragStartPoint(getDragEndPoint());
			}
		}
		
		@Override
		public void keyPressed(KeyEvent e) {
			for(PanAction panAction : new PanAction[]{leftAction(),rightAction(),upAction(),downAction()}){
				if(e.getKeyCode() == panAction.key){
					panAction.actionPerformed(null);
				}
			}
		}

		@Override
		public void onBuildContextMenu(MapApi api, MapPopupMenu menu) {

			for(Action action : createActions()){
				menu.add(action, "navigate");
			}
		}
	}
}
