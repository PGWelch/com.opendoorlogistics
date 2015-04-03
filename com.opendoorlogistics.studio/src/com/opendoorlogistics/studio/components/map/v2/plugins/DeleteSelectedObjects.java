package com.opendoorlogistics.studio.components.map.v2.plugins;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.studio.components.map.v2.MapApi;
import com.opendoorlogistics.studio.components.map.v2.MapPlugin;
import com.opendoorlogistics.studio.components.map.v2.plugins.PluginUtils.ActionFactory;

public class DeleteSelectedObjects implements MapPlugin, ActionFactory{

	@Override
	public void initMap(MapApi api) {
		PluginUtils.registerActionFactory(api, this, StandardOrdering.DELETE_SELECTED, "delete");
	}

	

	@Override
	public Action create(final MapApi api) {
		AbstractAction action = new AbstractAction()
		{
			
			@Override
			public void actionPerformed(ActionEvent e) {
				long [] ids = api.getSelectedIds();
				
				if (ids == null || ids.length == 0) {
					JOptionPane.showMessageDialog(api.getMapUIComponent(), "No objects are selected");
					return;
				}

				TableUtils.deleteByGlobalId(api.getMapDataApi().getGlobalDatastore(), true, ids);
			}
		};
		
		PluginUtils.initAction("Delete selected objects", "Delete selected objects", "edit-delete-6.png", action);
		return action;
	}
	

}
