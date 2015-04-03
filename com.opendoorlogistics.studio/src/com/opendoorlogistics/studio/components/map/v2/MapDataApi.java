package com.opendoorlogistics.studio.components.map.v2;

import java.util.concurrent.Callable;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.ODLDatastoreUndoable;

public interface MapDataApi {
	void runTransactionOnGlobalDatastore(Callable<Boolean> toRun);
	ODLDatastoreUndoable<? extends ODLTableAlterable> getGlobalDatastore();
	ODLTable getGlobalTable(int tableId);
	int getLatitudeColumn();
	int getLongitudeColumn();
	int getGeomColumn();
	int getLegendKeyColumn();
	ODLTable getUnfilteredDrawableTable();
	ODLTable getUnfilteredInactiveForegroundTable();
	ODLTable getUnfilteredInactiveBackgroundTable();
	ODLTableReadOnly getFilteredAllLayersTable();
	ODLTableReadOnly getUnfilteredAllLayersTable();
}
