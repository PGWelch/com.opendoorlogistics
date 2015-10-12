package com.opendoorlogistics.core.tables;

import java.util.List;

import com.opendoorlogistics.api.tables.DatastoreManagerPlugin;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;

public class DatastoreManagerGlobalPlugin {
	private static final DatastoreManagerPlugin PLUGIN;
	
	static{
		DatastoreManagerPlugin plugin=null;
		List<DatastoreManagerPlugin> plugins = new ODLApiImpl().loadPlugins(DatastoreManagerPlugin.class);
		if(plugins!=null){
			if(plugins.size()==1){
				
			}else{
				throw new RuntimeException("More than one " + DatastoreManagerPlugin.class.getName() + " loaded on startup.");
			}
		}
		
		PLUGIN = plugin;
	}
	
	public static DatastoreManagerPlugin getPlugin(){
		return PLUGIN;
	}
}
