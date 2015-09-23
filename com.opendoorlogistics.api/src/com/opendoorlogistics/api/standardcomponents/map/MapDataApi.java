package com.opendoorlogistics.api.standardcomponents.map;

import java.util.concurrent.Callable;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public interface MapDataApi {
	void runTransactionOnGlobalDatastore(Callable<Boolean> toRun);
	ODLDatastoreUndoable<? extends ODLTableAlterable> getGlobalDatastore();
	ODLTable getGlobalTable(int tableId);
	int getLatitudeColumn();
	int getLongitudeColumn();
	int getGeomColumn();
	int getLegendKeyColumn();
	int getTooltipColumn();
	ODLTable getUnfilteredActiveTable();
	ODLTable getUnfilteredInactiveForegroundTable();
	ODLTable getUnfilteredInactiveBackgroundTable();
	ODLTableReadOnly getActiveTableSelectedOnly();
	ODLTableReadOnly getBackgroundImagesTable();
	
	/**
	 * Get a single table with all input tables (filtered)
	 * @param immutableSnapshot True if you want to the return data to be an immutable thread-safe
	 * snapshot (if false, the data may still be a snapshot, but this isn't guaranteed.
	 * @return
	 */
	ODLTableReadOnly getFilteredAllLayersTable(boolean immutableSnapshot);
	
	/**
	 * Get a single table with all input tables (unfiltered)
	 * @param immutableSnapshot True if you want to the return data to be an immutable thread-safe
	 * snapshot (if false, the data may still be a snapshot, but this isn't guaranteed.
	 * @return
	 */
	ODLTableReadOnly getUnfilteredAllLayersTable(boolean immutableSnapshot);
	Iterable<ODLTable> getDrawableTables(ODLDatastore<? extends ODLTable> mapDatastore);
	ODLDatastore<? extends ODLTable> getMapDatastore();
	
}
