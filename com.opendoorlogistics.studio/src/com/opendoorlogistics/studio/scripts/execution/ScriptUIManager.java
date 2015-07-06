/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.execution;

import java.io.File;
import java.util.concurrent.Future;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.studio.appframe.AppPermissions;
import com.opendoorlogistics.studio.scripts.editor.adapters.QueryAvailableData;

public interface ScriptUIManager {

	ODLDatastore<? extends ODLTableDefinition> getDatastoreDefinition();

	void launchScriptEditor(Script script,String optionId, File file);

	void launchScriptEditor(File file,String optionId);

	Future<Void> executeScript(Script script, String []optionIds,String name);

	void testCompileScript(Script script, String []optionIds,String name);

	Future<Void> executeScript(File file,String []optionIds);

	void testCompileScript(File file,String []optionIds);
	
	void launchCreateTablesWizard(ODLDatastore<? extends ODLTableDefinition>ds);

	boolean hasLoadedData();

	QueryAvailableData getAvailableFieldsQuery();

	void registerDatastoreStructureChangedListener(ODLListener listener);
	
	void removerDatastoreStructureChangedListener(ODLListener listener);
	
	ODLApi getApi();
	
	AppPermissions getAppPermissions();
}
