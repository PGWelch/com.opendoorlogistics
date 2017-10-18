/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl.scripts;

import java.io.Serializable;
import java.util.ArrayList;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.components.ODLWizardTemplateConfig;

public class ScriptTemplatesImpl implements ScriptTemplatesBuilder {
	private final ArrayList<ODLWizardTemplateConfig> templates = new ArrayList<>();
	private final ODLApi api ;
	
	public ScriptTemplatesImpl(ODLApi api) {
		this.api = api;
	}
	
	public static Iterable<ODLWizardTemplateConfig> getTemplates(ODLApi api,ODLComponent component){
		ScriptTemplatesImpl stapi = new ScriptTemplatesImpl(api);
		component.registerScriptTemplates(stapi);
		return stapi.getTemplates();
	}

	/**
	 * This method is called by a component to register its script templates.
	 * The script is then built automatically.
	 */
	@Override
	public void registerTemplate(String shortName, String name, String description,ODLDatastore<? extends ODLTableDefinition>expectedDatastore, Serializable config) {
		registerTemplate(shortName, name, description, expectedDatastore,config, ScriptTemplatesBuilder.STANDARD_FLAGS, ODLComponent.MODE_DEFAULT);
	}

	/**
	 * This method is called by a component to register its script templates.
	 * The script is then built automatically.
	 */
	@Override
	public void registerTemplate(String shortName, String name, String description, ODLDatastore<? extends ODLTableDefinition>expectedDatastore,Serializable config, long flags, int executionMode) {
		templates.add(new ODLWizardTemplateConfig( shortName, name, description,expectedDatastore, config, flags, executionMode,null));		
	}

	/**
	 * This method is called by a component to register its script templates
	 */
	@Override
	public void registerTemplate(String shortName, String name, String description,ODLDatastore<? extends ODLTableDefinition>expectedDatastore, BuildScriptCallback buildScriptCB) {
		registerTemplate(shortName, name, description,expectedDatastore, buildScriptCB, ScriptTemplatesBuilder.STANDARD_FLAGS);		
	}

	/**
	 * This method is called by a component to register its script templates 
	 */
	@Override
	public void registerTemplate(String shortName, String name, String description,ODLDatastore<? extends ODLTableDefinition>expectedDatastore,final BuildScriptCallback buildScriptCB, long flags) {
		templates.add(new ODLWizardTemplateConfig( shortName, name, description,expectedDatastore, null, flags, -1,buildScriptCB));		
	}
	
	public Iterable<ODLWizardTemplateConfig> getTemplates(){
		return templates;
	}

	@Override
	public ODLApi getApi() {
		return api;
	}

}
