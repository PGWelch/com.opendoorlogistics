package com.opendoorlogistics.core.tables;

import java.util.List;

import com.opendoorlogistics.api.tables.DatastoreManagerPlugin;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;

public class DatastoreManagerGlobalPlugin {
	private static DatastoreManagerPlugin PLUGIN;
	
	static{

		List<DatastoreManagerPlugin> plugins = new ODLApiImpl().loadPlugins(DatastoreManagerPlugin.class);
		if(plugins!=null){
			if(plugins.size()==1){
				PLUGIN = plugins.get(0);
			}else if(plugins.size()>1){
				throw new RuntimeException("More than one " + DatastoreManagerPlugin.class.getName() + " loaded on startup.");
			}
		}

	}
	
	public static DatastoreManagerPlugin getPlugin(){
		return PLUGIN;
	}
	
	public static void setPlugin(DatastoreManagerPlugin plugin){
		PLUGIN = plugin;
	}
}
