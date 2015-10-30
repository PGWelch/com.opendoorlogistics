/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.app.ODLAppLoadedState;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.tables.DatastoreManagerPlugin;
import com.opendoorlogistics.api.tables.DatastoreManagerPlugin.DatastoreManagerPluginState;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.tables.DatastoreManagerGlobalPlugin;
import com.opendoorlogistics.core.tables.io.PoiIO;
import com.opendoorlogistics.core.tables.utils.TableFlagUtils;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.LoggerUtils;
import com.opendoorlogistics.studio.appframe.AbstractAppFrame;
import com.opendoorlogistics.studio.scripts.execution.ScriptsRunner;

public class LoadedState extends GlobalMapSelectedRowsManager implements Disposable,ODLAppLoadedState {
	private final static Logger LOGGER = Logger.getLogger(LoadedState.class.getName());

	private final ODLDatastoreUndoable<? extends ODLTableAlterable> ds;
	private final AbstractAppFrame appFrame;
	private File lastFile;
	private final ScriptsRunner runner;
	private final HashMap<DatastoreManagerPlugin, DatastoreManagerPluginState> pluginStates = new HashMap<>();

	public LoadedState(ODLDatastoreUndoable<? extends ODLTableAlterable> decoratedDs, File file, AbstractAppFrame appFrame) {
		this.appFrame = appFrame;
		
		this.ds = decoratedDs;
		
		lastFile = file;
		
		// create scripts runner - this has a thread-pool which exists for the lifetime of the open datastore
		runner = new ScriptsRunner(appFrame,ds);
		
		// add listeners which cause the app to update appearance when the data changes
		ds.addListener(tableChangeListener, -1);
		ds.addListener(tableSetChangeListener);			

	}

	public void putPluginState(DatastoreManagerPlugin plugin,DatastoreManagerPluginState state){
		pluginStates.put(plugin, state);
	}
	
	public ODLDatastoreUndoable<? extends ODLTableAlterable> getDs() {
		return ds;
	}

	public void onSaved(File file) {
		lastFile = file;
	}

	public boolean save(File file, boolean xlsx,ProcessingApi processing, ExecutionReport report) {
		
		// filter if we have a datastore manager plugin
		DatastoreManagerPlugin plugin = DatastoreManagerGlobalPlugin.getPlugin();
		if(plugin!=null && ds!=null){
			ODLDatastore<? extends ODLTableReadOnly> filtered = plugin.getDatastore2Save(appFrame.getApi(), this);
			return PoiIO.exportDatastore(filtered, file, xlsx, processing,report);
		}					
		
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

		LoadedState getLoadedDatastore();
	}

	@Override
	public File getFile() {
		return lastFile;
	}

	@Override
	public DatastoreManagerPluginState getDatastorePluginState(DatastoreManagerPlugin plugin) {
		return pluginStates.get(plugin);
	}
}
