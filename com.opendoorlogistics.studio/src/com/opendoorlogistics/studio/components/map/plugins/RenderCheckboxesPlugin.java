package com.opendoorlogistics.studio.components.map.plugins;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnBuildToolbarListener;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.MapToolbar;
import com.opendoorlogistics.api.standardcomponents.map.StandardMapMenuOrdering;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.gis.map.RenderProperties;

public class RenderCheckboxesPlugin implements MapPlugin{

	@Override
	public String getId(){
		return "com.opendoorlogistics.studio.components.map.plugins.RenderCheckboxesPlugin";
	}

//	private class UnselectedVisibilityHandler implements MapApiListeners.FilterVisibleObjects, MapApiListeners.OnChangeListener{
//		final JCheckBox checkbox = new JCheckBox("Unsel", true);
//		final MapApi api;
//		
//		UnselectedVisibilityHandler(MapApi mapApi){
//			this.api = mapApi;
//			
//			checkbox.setToolTipText("Show unselected objects as well as selected ones?");
//			
//			api.registerFilterVisibleObjectsListener(this, 0);
//			
//			checkbox.addActionListener(new ActionListener() {
//				
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					api.updateObjectFiltering();
//				}
//			});
//			
//			api.registerSelectionChanged(this, 0);
//		}
//
//		@Override
//		public boolean acceptObject(ODLTableReadOnly table, int row) {
//			return checkbox.isSelected() || api.isSelectedId(table.getRowId(row));
//		}
//
//		@Override
//		public void onChanged(MapApi api) {
//			// selection has changed
//			if(!checkbox.isSelected()){
//				SwingUtilities.invokeLater(new Runnable() {
//					
//					@Override
//					public void run() {
//						api.updateObjectFiltering();
//					}
//				});
//			}
//		}
//	}
	
	@Override
	public void initMap(final MapApi api) {
		final JCheckBox text= new JCheckBox("Text",true);
		text.setToolTipText("Show labels?");
		
		addActionListener(text,api,RenderProperties.SHOW_TEXT);
		
		final JCheckBox map= new JCheckBox("Map",true);
		addActionListener(map,api,RenderProperties.SHOW_BACKGROUND);
		map.setToolTipText("Show background map?");
		
		//final UnselectedVisibilityHandler unselObjects = new UnselectedVisibilityHandler(api);
		
		api.registerOnBuildToolbarListener(new OnBuildToolbarListener() {
			
			@Override
			public void onBuildToolbar(MapApi api, MapToolbar toolBar) {
				toolBar.add(text, "renderoption");
				createSeparation(toolBar);
				toolBar.add(map, "renderoption");
			//	createSeparation(toolBar);
				//toolBar.add(unselObjects.checkbox, "renderoption");				
			}

			private void createSeparation(MapToolbar toolBar) {
				toolBar.add(Box.createRigidArea(new Dimension(4, 4)));
			}

		}, StandardMapMenuOrdering.RENDER_OPTIONS);
	}

	private void addActionListener(JCheckBox checkBox, final MapApi api, final long flag){
		checkBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				long current = api.getRenderFlags();
				if((current & flag)==flag){
					current = current & ~flag;
				}else{
					current = current | flag;
				}
				api.setRenderFlags(current);				
			}
		});

	}
}
