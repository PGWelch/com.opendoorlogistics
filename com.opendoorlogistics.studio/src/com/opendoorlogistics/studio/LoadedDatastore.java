/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.tables.DatastoreManagerPlugin;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.tables.DatastoreManagerGlobalPlugin;
import com.opendoorlogistics.core.tables.decorators.datastores.DataUpdaterDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.ListenerDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.deepcopying.OptimisedDeepCopierDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.undoredo.UndoRedoDecorator;
import com.opendoorlogistics.core.tables.io.PoiIO;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableFlagUtils;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.LoggerUtils;
import com.opendoorlogistics.studio.appframe.AbstractAppFrame;
import com.opendoorlogistics.studio.scripts.execution.ReporterFrame;
import com.opendoorlogistics.studio.scripts.execution.ScriptsRunner;

public class LoadedDatastore extends GlobalMapSelectedRowsManager implements Disposable {
	private final static Logger LOGGER = Logger.getLogger(LoadedDatastore.class.getName());

	private final ODLDatastoreUndoable<ODLTableAlterable> ds;
	private final AbstractAppFrame appFrame;
	private File lastFile;
	private final ScriptsRunner runner;

	public LoadedDatastore(ODLDatastoreAlterable<? extends ODLTableAlterable> newDs, File file, AbstractAppFrame appFrame) {
		this.appFrame = appFrame;
		
		
		if (ODLDatastoreImpl.class.isInstance(newDs) == false) {
			throw new RuntimeException();
		}
		
		// todo... apply the datastore manager plugin if we have one... only allow one (throw an exception if more found)
		DatastoreManagerPlugin plugin = DatastoreManagerGlobalPlugin.getPlugin();
		if(plugin!=null){
			
		}
				
		// wrap in the decorator that allows lazy deep copying first of all
		OptimisedDeepCopierDecorator<ODLTableAlterable> odcd = new OptimisedDeepCopierDecorator<>(newDs);
		
		// wrap in listener decorator, then undo/redo decorator, then data updater
		ListenerDecorator<ODLTableAlterable> listeners = new ListenerDecorator<ODLTableAlterable>(ODLTableAlterable.class, odcd);
		ODLDatastoreUndoable<ODLTableAlterable> undoable = new UndoRedoDecorator<ODLTableAlterable>(ODLTableAlterable.class, listeners);
		ds = new DataUpdaterDecorator(appFrame.getApi(), undoable, appFrame);

		lastFile = file;
		
		runner = new ScriptsRunner(appFrame,ds);
		ds.addListener(tableChangeListener, -1);
		ds.addListener(tableSetChangeListener);			

	}


	public File getLastFile() {
		return lastFile;
	}

	void setLastFile(File lastFile) {
		this.lastFile = lastFile;
	}

	public ODLDatastoreUndoable<ODLTableAlterable> getDs() {
		return ds;
	}

	public void onSaved(File file) {
		lastFile = file;
	}

	public boolean save(File file, boolean xlsx,ProcessingApi processing, ExecutionReport report) {
		return PoiIO.exportDatastore(ds, file, xlsx, processing,report);
	}


	public boolean runTransaction(Callable<Boolean> callable) {
		return TableUtils.runTransaction(ds, callable);
	}

	/**
	 * This is called by a map selection list when the selection state changes. 
	 * It modifies the selected flags in the rows in the global datastore as needed,
	 * and then notifies all selection listeners that the selection state has changed.
	 */
	@Override
	public void onMapSelectedChanged() {
		// update selection state in the ds for everything
		long countSelected=0;
		long countChanged=0;
		for(int i = 0 ;i<ds.getTableCount() ; i++){
			ODLTable table = ds.getTableAt(i);
			int n = table.getRowCount();
			for(int row=0;row<n;row++){
				long id = table.getRowId(row);
				boolean selected = isRowSelectedInMap(id);
				if(selected){
					countSelected++;
				}
				long flags = table.getRowFlags(id);
				boolean selectedInDs = (flags & TableFlags.FLAG_ROW_SELECTED_IN_MAP)==TableFlags.FLAG_ROW_SELECTED_IN_MAP;
				if(selectedInDs!=selected){
					flags = TableFlagUtils.setFlag(flags, TableFlags.FLAG_ROW_SELECTED_IN_MAP, selected);
					table.setRowFlags(flags, id);
					countChanged++;
				}
			}
		}
		
		LOGGER.info(LoggerUtils.addPrefix(" - " + Long.toString(countSelected) + " obj(s) sel., " + Long.toString(countChanged) +" sel. state changes"));
		
		fireListeners();
	}
	
	public ScriptsRunner getRunner(){
		return runner;
	}
	
	private ODLListener tableChangeListener = new ODLListener() {

		@Override
		public void datastoreStructureChanged() {
			// TODO Auto-generated method stub

		}

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			appFrame.updateAppearance();
		}

		@Override
		public ODLListenerType getType() {
			return ODLListenerType.TABLE_CHANGED;
		}
	};

	private ODLListener tableSetChangeListener = new ODLListener() {

		@Override
		public void datastoreStructureChanged() {
			appFrame.updateAppearance();
		}

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			// TODO Auto-generated method stub

		}

		@Override
		public ODLListenerType getType() {
			return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
		}
	};

	@Override
	public void dispose() {
		runner.dispose();
		getDs().removeListener(tableChangeListener);
		getDs().removeListener(tableSetChangeListener);
	}

	public interface HasLoadedDatastore{

		LoadedDatastore getLoadedDatastore();
	}
}
