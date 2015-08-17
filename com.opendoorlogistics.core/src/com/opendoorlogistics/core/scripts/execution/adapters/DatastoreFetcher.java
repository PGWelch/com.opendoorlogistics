package com.opendoorlogistics.core.scripts.execution.adapters;

import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;

public interface DatastoreFetcher {
	ODLDatastoreAlterable<? extends ODLTableAlterable> getDatastore(String id);
}
