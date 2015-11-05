package com.opendoorlogistics.studio.components.geocoder;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.standardcomponents.map.MapActionFactory;
import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.StandardMapMenuOrdering;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModel;
import com.opendoorlogistics.studio.components.map.AbstractMapMode;
import com.opendoorlogistics.studio.components.map.MapApiImpl;
import com.opendoorlogistics.studio.components.map.plugins.PanMapPlugin;
import com.opendoorlogistics.studio.components.map.plugins.RenderCheckboxesPlugin;
import com.opendoorlogistics.studio.components.map.plugins.utils.PluginUtils;

public class NominatimMap implements Disposable{
	private final GeocodeModel model;
	private final MapApiImpl map;
	
	NominatimMap(GeocodeModel model){
		this.model = model;			
		ArrayList<MapPlugin> plugins = new ArrayList<MapPlugin>();
		plugins.add(new PanMapPlugin());
		plugins.add(new MyMovePlugin());
		plugins.add(new RenderCheckboxesPlugin());
		map = new MapApiImpl(plugins, null, null, GeocoderMapObjects.createDrawableDs(model));
		map.getPanel().setPreferredSize(new Dimension(600, 200));
	}
	
	Component getComponent(){
		return map.getPanel();
	}
	
	void update(){
		map.setObjects(GeocoderMapObjects.createDrawableDs(model));
		map.repaint(false);
	}
	
	void zoomBestFit(){
		map.setViewToBestFit(map.getMapDataApi().getUnfilteredActiveTable());
	}
	
	private class MyMovePlugin implements MapPlugin, MapActionFactory{

		@Override
		public String getId() {
			return "nominatim-move-geocode-plugin";
		}

		@Override
		public void initMap(MapApi api) {
			PluginUtils.registerActionFactory(api, this, StandardMapMenuOrdering.MOVE_MODE, "mapmode",0);
		}

		@Override
		public Action create(MapApi api) {
			AbstractAction action = new AbstractAction()
			{
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if(!PluginUtils.exitIfInMode(api, NominatimMovePoint.class)){
						api.setMapMode(new NominatimMovePoint());					
					}
				}
			};
			
			PluginUtils.initAction("Move point","Move the geocoded point around by clicking on its new position", "tool-move-16x16.png", action);
			return action;
		}
	}
	
	private class NominatimMovePoint extends AbstractMapMode {
		
		@Override
		public Cursor getCursor() {
			return PluginUtils.createCursor("tool-move.png", 17, 15);
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
			LatLong ll = map.createImmutableConverter().getLongLat(evt.getX(), evt.getY());
			model.setGeocode(ll);
		}

	}

	@Override
	public void dispose() {
		map.dispose();
	}
}
