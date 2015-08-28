/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.xeoh.plugins.base.Plugin;
import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import net.xeoh.plugins.base.util.PluginManagerUtil;

import com.opendoorlogistics.api.Functions;
import com.opendoorlogistics.api.IO;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StandardComponents;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.Values;
import com.opendoorlogistics.api.components.ODLComponentProvider;
import com.opendoorlogistics.api.geometry.Geometry;
import com.opendoorlogistics.api.scripts.Scripts;
import com.opendoorlogistics.api.ui.UIFactory;
import com.opendoorlogistics.core.api.impl.scripts.ScriptsImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;

public class ODLApiImpl implements ODLApi{
	private volatile StringConventions conventions;
	private volatile StandardComponents standardComponents;
	private volatile Values conversionApi;
	private volatile Tables tables;
	private volatile Geometry geometry;
	private volatile UIFactory uiFactory;
	private volatile Functions functions;
	private volatile IO io;
	private volatile Scripts scripts;

	@Override
	public Values values() {
		if(conversionApi==null){
			conversionApi = new ValuesImpl();
		}
		return conversionApi;
	}

	@Override
	public Tables tables() {
		if(tables==null){
			tables = new TablesImpl();			
		}
		return tables;
	}

	@Override
	public StringConventions stringConventions() {
		if(conventions==null){
			conventions = new StringConventionsImpl();
		}
		return conventions;
	}

	@Override
	public Geometry geometry() {
		if(geometry==null){
			geometry = new GeometryImpl();
		}
		return geometry;
	}

	@Override
	public UIFactory uiFactory() {
		if(uiFactory==null){
			uiFactory = new UIFactoryImpl();
		}
		return uiFactory;
	}

	@Override
	public StandardComponents standardComponents() {
		if(standardComponents==null){
			standardComponents = new StandardComponentsImpl();
		}
		return standardComponents;
	}

	@Override
	public ODLComponentProvider registeredComponents() {
		return ODLGlobalComponents.getProvider();
	}

	@Override
	public Functions functions() {
		if(functions==null){
			functions = new Functions() {
				
				@Override
				public boolean isFunctionError(Object functionReturnValue) {
					return functionReturnValue == com.opendoorlogistics.core.formulae.Functions.EXECUTION_ERROR;
				}
			};
		}
		
		return functions;
	}

	@Override
	public IO io() {
		if(io == null){
			io = new IOImpl();
		}
		
		return io;
	}

	@Override
	public Scripts scripts() {
		if(scripts == null){
			scripts = new ScriptsImpl(this);
		}
		return scripts;
	}

	@Override
	public <T extends Plugin> List<T> loadPlugins(Class<T> cls) {
		// register plugins from plugins directory
		PluginManager pm = PluginManagerFactory.createPluginManager();
		pm.addPluginsFrom(new File("." + File.separator + "plugins").toURI());
		PluginManagerUtil pmu = new PluginManagerUtil(pm);	
		ArrayList<T> ret = new ArrayList<T>();
		Collection<T> plugins = pmu.getPlugins(cls);
		if(plugins!=null){
			ret.addAll(plugins);
		}
		return ret;
	}


	
}
