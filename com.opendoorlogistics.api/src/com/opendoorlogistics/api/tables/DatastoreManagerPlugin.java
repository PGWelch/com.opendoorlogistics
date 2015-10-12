package com.opendoorlogistics.api.tables;

import com.opendoorlogistics.api.ExecutionReport;

/**
 * Provides plugin functionality for managing datastores
 * @author Phil
 *
 */
public interface DatastoreManagerPlugin  extends net.xeoh.plugins.base.Plugin {
	int getPriority();
	
	ODLDatastoreAlterable<? extends ODLTableAlterable> processNewDatastore(ODLDatastoreAlterable<? extends ODLTableAlterable> ds, ExecutionReport report);
	
	/**
	 * For a datastore which is wrapped by the plugin, get the datastore which should be saved to a file
	 * @param ds Datastore wrapped by the plugin
	 * @return
	 */
	ODLDatastore<? extends ODLTableReadOnly> getDatastore2Save(ODLDatastore<? extends ODLTableReadOnly> ds);
}
