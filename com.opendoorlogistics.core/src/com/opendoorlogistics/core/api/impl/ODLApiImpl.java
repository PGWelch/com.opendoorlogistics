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
import java.util.Properties;
import java.util.Set;

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
import com.opendoorlogistics.api.app.ODLApp;
import com.opendoorlogistics.api.app.ODLAppPreferences;
import com.opendoorlogistics.api.app.ODLAppProperties;
import com.opendoorlogistics.api.cache.ObjectCachePool;
import com.opendoorlogistics.api.components.ODLComponentProvider;
import com.opendoorlogistics.api.geometry.Geometry;
import com.opendoorlogistics.api.scripts.Scripts;
import com.opendoorlogistics.api.ui.UIFactory;
import com.opendoorlogistics.core.AppProperties;
import com.opendoorlogistics.core.api.impl.scripts.ScriptsImpl;
import com.opendoorlogistics.core.cache.ApplicationCache;
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
	private volatile ODLAppPreferences preferences;
	
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
			tables = new TablesImpl(this);			
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
			uiFactory = new UIFactoryImpl(this);
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

	@Override
	public ODLAppProperties properties() {
		return new ODLAppProperties(){


			@Override
			public Double getDouble(String key) {
				return AppProperties.getDouble(key);
			}

			@Override
			public Double getDouble(String key, double defaultValueIfKeyMissing) {
				return AppProperties.getDouble(key, defaultValueIfKeyMissing);
			}

			@Override
			public String getString(String key) {
				return AppProperties.getString(key);
			}

			@Override
			public void add(Properties properties) {
				AppProperties.add(properties);
			}

			@Override
			public Boolean getBool(String key) {
				return AppProperties.getBool(key);
			}

			@Override
			public Set<String> getKeys() {
				return AppProperties.getKeys();
			}

			@Override
			public void put(String key, Object value) {
				AppProperties.put(key,value);
			}

			@Override
			public boolean isTrue(String key) {
				Boolean b = getBool(key);
				return b!=null && b;
			}
			
		};
	}

	@Override
	public ObjectCachePool cache() {
		return ApplicationCache.singleton();
	}

	@Override
	public ODLApp app() {
		throw new UnsupportedOperationException("App object access not supported");
	}

	@Override
	public ODLAppPreferences preferences() {
		if(preferences == null){
			preferences = new ODLAppPreferencesImpl();
		}
		return preferences;
	}


	
}
