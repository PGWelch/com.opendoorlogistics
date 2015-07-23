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
		File dir = new File("." + File.separator + "plugins");
		PluginManagerUtil pmu = registerPluginDirectory(dir);			
		
		// find libraries and init them; this can add further components or even remove them
		ODLApiImpl api = new ODLApiImpl();
		for(ODLPluginLibrary lib : pmu.getPlugins(ODLPluginLibrary.class)){
			lib.init(api);
		}
	}



	public static PluginManagerUtil registerPluginDirectory(File dir) {
		PluginManager pm = PluginManagerFactory.createPluginManager();
		pm.addPluginsFrom(dir.toURI());
		PluginManagerUtil pmu = new PluginManagerUtil(pm);		
		for(ODLComponent component : pmu.getPlugins(ODLComponent.class)){
			logger.info("Found component " + component.getId() + " in directory " + dir.getAbsolutePath());
			register(component);
		}
		return pmu;
	}
	

	
	public static synchronized void register(ODLComponent component){
		GLOBAL.register(component);
		logger.info("Registered component " + component.getId());
	}
	
	public static ODLComponentProvider getProvider(){
		return GLOBAL;
	}
	

}


