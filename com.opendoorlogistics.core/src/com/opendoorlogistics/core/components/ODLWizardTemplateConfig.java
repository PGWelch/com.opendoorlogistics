/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.components;

import java.io.Serializable;

import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder.BuildScriptCallback;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.scripts.elements.Script;


public class ODLWizardTemplateConfig {
	private final String shortName;
	private final String name;
	private final String description;
	private final Serializable config;
	private final long flags;
	private final int executionMode;
	private final ODLDatastore<? extends ODLTableDefinition> expectedIods;
	private final BuildScriptCallback buildScriptCB;
	
	//public static long FLAG_IO_DATASTORE_IS_FIXED = 1<<0;

//	public ODLWizardTemplateConfig(String shortName, String name, String description, Serializable config) {
//		this(shortName, name, description, config, STANDARD_FLAGS, ODLComponent.MODE_DEFAULT);
//	}
	
	public ODLWizardTemplateConfig(String shortName, String name, String description, ODLDatastore<? extends ODLTableDefinition> expectedIods, Serializable config, long flags, int executionMode, BuildScriptCallback buildScriptCB) {
		this.shortName = shortName;
		this.name = name;
		this.description = description;
		this.config = config;
		this.flags = flags;
		this.executionMode = executionMode;
		this.expectedIods = expectedIods;
		this.buildScriptCB = buildScriptCB;
	}

	public String getShortName() {
		return shortName;
	}
	
	public boolean hasFlag(long flag){
		return (flags & flag) == flag;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Serializable getConfig() {
		return config;
	}

	
//	/**
//	 * Override this to create your own template script
//	 * @param cb
//	 * @param selectedTables
//	 * @return
//	 */
//	public Script createScript(ScriptInputTables inputTables){
//		return null;
//	}
	
	public int getExecutionMode(){
		return executionMode;
	}

	public ODLDatastore<? extends ODLTableDefinition> getExpectedIods() {
		return expectedIods;
	}

	public BuildScriptCallback getBuildScriptCB() {
		return buildScriptCB;
	}
	

	
}
