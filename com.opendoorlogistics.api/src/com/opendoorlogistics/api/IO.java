package com.opendoorlogistics.api;

import java.io.File;
import java.util.List;

import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
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
	
	File getAsRelativeIfWithinStandardShapefileDirectory(File file);
	
	/**
	 * If we're in ODL Studio this returns the file that the current datastore was loaded from.
	 * Unlikely to work in ODL Connect...
	 * @return
	 */
	File getLoadedExcelFile();
	
	boolean exportDatastore(ODLDatastore<? extends ODLTableReadOnly> ds, File file, boolean xlsx,ProcessingApi processing, ExecutionReport report);

	ODLDatastoreAlterable<ODLTableAlterable> importFile(File file, ImportFileType type,ProcessingApi processingApi, ExecutionReport report);

	/**
	 * Import a supported file, using its extension to identify the type
	 * @param file
	 * @param processingApi
	 * @param report
	 * @return
	 */
	ODLDatastoreAlterable<ODLTableAlterable> importFile(File file ,ProcessingApi processingApi, ExecutionReport report);

	/**
	 * Normalise a file path, internally calling Apache commons-io FilenameUtils.Normalize
	 * @param s
	 * @return
	 */
	String normalisePath(String s);
	
	List<String> getShapefileFieldnames(File shapefileOrODLRG);
	
	/**
	 * Import a shapefile specifying a max number of rows
	 * @param file
	 * @param maxRows
	 * @return
	 */
	ODLTable importShapefile(File file, int maxRows);
}
