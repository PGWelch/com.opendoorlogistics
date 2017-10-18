package com.opendoorlogistics.core.api.impl;

import java.util.List;

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

import net.xeoh.plugins.base.Plugin;

public class ODLApiDecorator implements ODLApi{
	protected final ODLApi api;

	public ODLApiDecorator(ODLApi api) {
		this.api = api;
	}

	public ObjectCachePool cache() {
		return api.cache();
	}

	public StringConventions stringConventions() {
		return api.stringConventions();
	}

	public Geometry geometry() {
		return api.geometry();
	}

	public StandardComponents standardComponents() {
		return api.standardComponents();
	}

	public ODLComponentProvider registeredComponents() {
		return api.registeredComponents();
	}

	public Tables tables() {
		return api.tables();
	}

	public Values values() {
		return api.values();
	}

	public UIFactory uiFactory() {
		return api.uiFactory();
	}

	public Functions functions() {
		return api.functions();
	}

	public Scripts scripts() {
		return api.scripts();
	}

	public IO io() {
		return api.io();
	}

	public ODLAppProperties properties() {
		return api.properties();
	}

	public <T extends Plugin> List<T> loadPlugins(Class<T> cls) {
		return api.loadPlugins(cls);
	}

	@Override
	public ODLApp app() {
		return api.app();
	}

	@Override
	public ODLAppPreferences preferences() {
		return api.preferences();
	}
	
	
}
