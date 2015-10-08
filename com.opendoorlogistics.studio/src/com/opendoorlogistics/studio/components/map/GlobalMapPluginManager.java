package com.opendoorlogistics.studio.components.map;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.studio.components.map.plugins.CreatePointPlugin;
import com.opendoorlogistics.studio.components.map.plugins.FillPlugin;
import com.opendoorlogistics.studio.components.map.plugins.LegendPlugin;
import com.opendoorlogistics.studio.components.map.plugins.MovePointPlugin;
import com.opendoorlogistics.studio.components.map.plugins.PanMapPlugin;
import com.opendoorlogistics.studio.components.map.plugins.RenderCheckboxesPlugin;
import com.opendoorlogistics.studio.components.map.plugins.ViewSyncer;
import com.opendoorlogistics.studio.components.map.plugins.selection.SelectPlugin;
import com.opendoorlogistics.studio.components.map.plugins.snapshot.SnapshotPlugin;

public class GlobalMapPluginManager {
	private static final StandardisedStringTreeMap<MapPlugin> GLOBAL = new StandardisedStringTreeMap<MapPlugin>(false);
	
	private static final Logger logger = Logger.getLogger(GlobalMapPluginManager.class.getName());
	
	static{
		// add standard ones
		register(new PanMapPlugin());
		register(new LegendPlugin());
		register(new SelectPlugin());
		register(new FillPlugin());
		register(new RenderCheckboxesPlugin());
		register(new SnapshotPlugin());
		register(new CreatePointPlugin());
		register(new MovePointPlugin());
		register(new ViewSyncer());
		
		// register plugins from plugins directory	
		for(MapPlugin plugin :new ODLApiImpl().loadPlugins(MapPlugin.class)){
			register(plugin);
		}			

	}
	
	public static synchronized void register(MapPlugin plugin){
		if(!GLOBAL.containsKey(plugin.getId())){
			GLOBAL.put(plugin.getId(), plugin);
			logger.info("Registered map plugin " + plugin.getId());
		}
		else{
			logger.severe("Failed to register map plugin " + plugin.getId() + "; id is already registered");
		}
		
//		if(GLOBAL.register(component)){
//			logger.info("Registered component " + component.getId());
//			//System.out.println("Registered component " + component.getId());
//		}else{
//			logger.severe("Failed to register component " + component.getId() + "; id is already registered");
//			//System.out.println("Failed to register component " + component.getId() + "; id is already registered");			
//		}
	}
	
	public static synchronized Iterable<MapPlugin> getPlugins(){
		// return a copy
		return new ArrayList<MapPlugin>(GLOBAL.values());
	}
	
//	
//	public static ODLComponentProvider getProvider(){
//		return GLOBAL;
//	}
//	
}
