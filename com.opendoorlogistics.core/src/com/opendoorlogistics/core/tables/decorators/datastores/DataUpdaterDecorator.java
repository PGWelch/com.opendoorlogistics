/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.HasUndoStateListeners;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.scripts.ScriptsProvider.HasScriptsProvider;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.scripts.execution.OptionsSubpath;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutor;
import com.opendoorlogistics.core.scripts.execution.dependencyinjection.AbstractDependencyInjector;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;

/**
 * A data updater decorator automatically calls any data updater options in all loaded scripts
 * whenever the datastore is modified in a transaction.
 * @author Phil
 *
 */
public class DataUpdaterDecorator extends SimpleDecorator<ODLTableAlterable> implements ODLDatastoreUndoable<ODLTableAlterable> {
	private final HasScriptsProvider hasProvider;
	private final ODLApi api;
	
	public DataUpdaterDecorator(ODLApi api, ODLDatastoreUndoable<? extends ODLTableAlterable> decorated, HasScriptsProvider scriptsProvider) {
		super(ODLTableAlterable.class, decorated);
		this.hasProvider = scriptsProvider;
		this.api = api;
		update();
	}
	
	@Override
	public void endTransaction() {
		update();
		super.endTransaction();
	}
	
	private void update(){
		for(Script script : hasProvider.getScriptsProvider()){
			for(String optionId : ScriptUtils.getOptionIdsByInstructionExecutionMode(script, ODLComponent.MODE_DATA_UPDATER)){
				try {
					ExecutionReportImpl report = new ExecutionReportImpl();
					Script subpath = OptionsSubpath.getSubpathScript(script, new String[]{optionId}, report);
					if(!report.isFailed()){
						ScriptExecutor executor = new ScriptExecutor(api, false, new AbstractDependencyInjector(api));
						executor.execute(subpath, this);
					}
				} catch (Exception e) {
					// TODO: report this somewhere....?
				}
			}
		}
	}

	private ODLDatastoreUndoable<ODLTableAlterable> undoable() {
		return (ODLDatastoreUndoable<ODLTableAlterable>)decorated;
	}
	
	@Override
	public void undo() {
		undoable().undo();
	}

	@Override
	public void redo() {
		undoable().redo();
	}

	@Override
	public boolean hasRedo() {
		return undoable().hasRedo();
	}

	@Override
	public boolean hasUndo() {
		return undoable().hasUndo();
	}

	@Override
	public void addUndoStateListener(HasUndoStateListeners.UndoStateChangedListener< ODLTableAlterable> listener) {
		undoable().addUndoStateListener(listener);
	}

	@Override
	public void removeUndoStateListener(HasUndoStateListeners.UndoStateChangedListener< ODLTableAlterable> listener) {
		undoable().removeUndoStateListener(listener);
	}

	@Override
	public void clearUndoBuffer() {
		undoable().clearUndoBuffer();
	}

}
