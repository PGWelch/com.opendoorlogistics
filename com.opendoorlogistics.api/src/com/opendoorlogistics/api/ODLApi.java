/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api;

import java.util.List;

import com.opendoorlogistics.api.app.ODLApp;
import com.opendoorlogistics.api.app.ODLAppPreferences;
import com.opendoorlogistics.api.app.ODLAppProperties;
import com.opendoorlogistics.api.cache.ObjectCachePool;
import com.opendoorlogistics.api.components.ODLComponentProvider;
import com.opendoorlogistics.api.geometry.Geometry;
import com.opendoorlogistics.api.scripts.Scripts;
import com.opendoorlogistics.api.ui.UIFactory;

import net.xeoh.plugins.base.Plugin;


public interface ODLApi {
	ObjectCachePool cache();
	StringConventions stringConventions();
	Geometry geometry();
	StandardComponents standardComponents();
	ODLComponentProvider registeredComponents();
	Tables tables();
	Values values();
	UIFactory uiFactory();
	Functions functions();
	Scripts scripts();
	IO io();
	ODLAppProperties properties();
	ODLApp app();
	ODLAppPreferences preferences();
	
	/**
	 * Load all plugins of the input class from the plugins directory
	 * @param cls
	 * @return
	 */
	<T extends Plugin> List<T> loadPlugins(Class<T> cls);
	
		
	
	//ODLApp createODLStudio(boolean haltJVMOnDispose);
}
