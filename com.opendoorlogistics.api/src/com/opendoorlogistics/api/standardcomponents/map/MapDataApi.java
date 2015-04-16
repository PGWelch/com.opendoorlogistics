package com.opendoorlogistics.api.standardcomponents.map;

import java.util.concurrent.Callable;

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
	ODLTable getUnfilteredDrawableTable();
	ODLTable getUnfilteredInactiveForegroundTable();
	ODLTable getUnfilteredInactiveBackgroundTable();
	ODLTableReadOnly getActiveTableSelectedOnly();
	ODLTableReadOnly getFilteredAllLayersTable();
	ODLTableReadOnly getUnfilteredAllLayersTable();
}
