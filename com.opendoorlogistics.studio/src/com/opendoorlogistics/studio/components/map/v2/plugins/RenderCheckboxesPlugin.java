package com.opendoorlogistics.studio.components.map.v2.plugins;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JToolBar;

import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.studio.components.map.v2.MapApi;
import com.opendoorlogistics.studio.components.map.v2.MapPlugin;
import com.opendoorlogistics.studio.components.map.v2.MapApiListeners.OnBuildToolbarListener;
import com.opendoorlogistics.studio.components.map.v2.MapToolbar;

public class RenderCheckboxesPlugin implements MapPlugin{

	@Override
	public void initMap(final MapApi api) {
		final JCheckBox text= new JCheckBox("Text",true);
		addActionListener(text,api,RenderProperties.SHOW_TEXT);
		
		final JCheckBox map= new JCheckBox("Map",true);
		addActionListener(map,api,RenderProperties.SHOW_BACKGROUND);
		
		api.registerOnBuildToolbarListener(new OnBuildToolbarListener() {
			
			@Override
			public void onBuildToolbar(MapApi api, MapToolbar toolBar) {
				toolBar.add(text, "renderoption");
				toolBar.add(Box.createRigidArea(new Dimension(8, 4)));
				toolBar.add(map, "renderoption");
			}

		}, StandardOrdering.RENDER_OPTIONS);
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
