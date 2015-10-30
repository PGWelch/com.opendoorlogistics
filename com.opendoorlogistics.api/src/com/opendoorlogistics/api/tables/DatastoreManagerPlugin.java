package com.opendoorlogistics.api.tables;

import java.io.File;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.app.ODLAppInitListener;
import com.opendoorlogistics.api.app.ODLAppLoadedState;
import com.opendoorlogistics.api.components.ProcessingApi;

/**
 * Provides plugin functionality for managing datastores.
 * @author Phil
 *
 */
public interface DatastoreManagerPlugin  extends net.xeoh.plugins.base.Plugin , ODLAppInitListener{
	int getPriority();
	
	ProcessDatastoreResult processNewDatastore(File datastoreFileLocation,ODLDatastoreUndoable<? extends ODLTableAlterable> datastore,ProcessingApi processingApi, ExecutionReport report);
	
	/**
	 * For a datastore which is wrapped by the plugin, get the datastore which should be saved to a file
	 * @param ds Datastore wrapped by the plugin
	 * @return
	 */
	ODLDatastore<? extends ODLTableReadOnly> getDatastore2Save(ODLApi api,ODLAppLoadedState state);
	
	
	public interface ProcessDatastoreResult{
		ODLDatastoreUndoable<? extends ODLTableAlterable> getDs();
		DatastoreManagerPluginState getState();
	}
	
	public interface DatastoreManagerPluginState{
		
	}
	
}
