/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.components;

import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

public class ODLComponentDecorator implements ODLComponent{
	protected final ODLComponent decorated;

	public ODLComponentDecorator(ODLComponent decorated) {
		super();
		this.decorated = decorated;
	}

	@Override
	public String getId() {
		return decorated.getId();
	}

	@Override
	public String getName() {
		return decorated.getName();
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api,Serializable configuration) {
		return decorated.getIODsDefinition(api,configuration);
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api,int mode, Serializable configuration) {
		return decorated.getOutputDsDefinition(api,mode,configuration);
	}

	@Override
	public void execute(ComponentExecutionApi reporter,int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDb, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {
		decorated.execute(reporter,mode, configuration, ioDb, outputDb);
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return decorated.getConfigClass();
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory,int mode,Serializable config , boolean isFixedIO) {
		return decorated.createConfigEditorPanel(factory,mode,config, false);
	}

	@Override
	public long getFlags(ODLApi api,int mode) {
		return decorated.getFlags(api,mode);
	}

	@Override
	public Icon getIcon(ODLApi api,int mode) {
		return decorated.getIcon(api,mode);
	}
	
	@Override
	public boolean isModeSupported(ODLApi api,int mode) {
		return decorated.isModeSupported(api, mode);
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		decorated.registerScriptTemplates(templatesApi);
	}
}
