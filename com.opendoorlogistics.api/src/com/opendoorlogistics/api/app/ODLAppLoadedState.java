package com.opendoorlogistics.api.app;

import java.io.File;

import com.opendoorlogistics.api.tables.DatastoreManagerPlugin;
import com.opendoorlogistics.api.tables.DatastoreManagerPlugin.DatastoreManagerPluginState;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;

public interface ODLAppLoadedState {
	ODLDatastoreUndoable<? extends ODLTableAlterable> getDs();
	File getFile();
	DatastoreManagerPluginState getDatastorePluginState(DatastoreManagerPlugin plugin);
	
}
