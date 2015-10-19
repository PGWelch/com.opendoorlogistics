package com.opendoorlogistics.api.app;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;

/**
 * A interface defining a modification to the datastore which gets run in the app.
 * @author Phil
 *
 */
public interface DatastoreModifier {
	/**
	 * Modify the datastore
	 * @param ds
	 * @param api
	 * @param report
	 */
	void modify(ODLDatastoreAlterable<? extends ODLTableAlterable> ds, ProcessingApi api,ExecutionReport report);
	
	/**
	 * Name of the task being run, appears in progress and failure window titles
	 * @return
	 */
	String name();
	
	/**
	 * Milliseconds delay until progress dialog appears
	 * @return
	 */
	int getMillisDelayUntilProgressAppears();
}