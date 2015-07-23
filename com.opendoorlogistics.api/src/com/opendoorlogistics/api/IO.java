package com.opendoorlogistics.api;

import java.io.File;

import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public interface IO {
	/**
	 * Get the standard data directory in the ODL Studio installation
	 * @return
	 */
	File getStandardDataDirectory();
	
	File getStandardConfigDirectory();
	
	File getStandardScriptsDir();
	
	boolean exportDatastore(ODLDatastore<? extends ODLTableReadOnly> ds, File file, boolean xlsx,ProcessingApi processing, ExecutionReport report);

	ODLDatastoreAlterable<ODLTableAlterable> importFile(File file, ImportFileType type,ProcessingApi processingApi, ExecutionReport report);

}
