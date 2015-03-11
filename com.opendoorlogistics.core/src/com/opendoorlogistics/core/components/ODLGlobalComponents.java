/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.components;

import java.io.File;
import java.util.logging.Logger;

import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.ODLComponentProvider;
import com.opendoorlogistics.api.components.ODLPluginLibrary;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;

import net.xeoh.plugins.base.Plugin;
import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import net.xeoh.plugins.base.util.PluginManagerUtil;

/**
 * Class storing all loaded plugins, application-wide
 * @author Phil
 *
 */
public final class ODLGlobalComponents {
	private static final ODLComponentsList GLOBAL;
	private static final Logger logger = Logger.getLogger(ODLGlobalComponents.class.getName());
	static{
		// create global list
		GLOBAL = new ODLComponentsList();
		
		// register plugins from plugins directory
		PluginManager pm = PluginManagerFactory.createPluginManager();
		pm.addPluginsFrom(new File("." + File.separator + "plugins").toURI());
		PluginManagerUtil pmu = new PluginManagerUtil(pm);		
		for(ODLComponent component : pmu.getPlugins(ODLComponent.class)){
			register(component);
		}			
		
		// find libraries and init them; this can add further components or even remove them
		ODLApiImpl api = new ODLApiImpl();
		for(ODLPluginLibrary lib : pmu.getPlugins(ODLPluginLibrary.class)){
			lib.init(api);
		}
	}
	
	public static synchronized void register(ODLComponent component){
		if(GLOBAL.register(component)){
			logger.info("Registered component " + component.getId());
			//System.out.println("Registered component " + component.getId());
		}else{
			logger.severe("Failed to register component " + component.getId() + "; id is already registered");
			//System.out.println("Failed to register component " + component.getId() + "; id is already registered");			
		}
	}
	
	public static ODLComponentProvider getProvider(){
		return GLOBAL;
	}
	

}


