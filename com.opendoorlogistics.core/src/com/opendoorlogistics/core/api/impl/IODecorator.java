package com.opendoorlogistics.core.api.impl;

import java.io.File;
import java.util.List;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.IO;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public class IODecorator implements IO{
	private final IO io;

	public IODecorator(IO io) {
		this.io = io;
	}

	public File getStandardDataDirectory() {
		return io.getStandardDataDirectory();
	}

	public File getStandardConfigDirectory() {
		return io.getStandardConfigDirectory();
	}

	public File getStandardScriptsDir() {
		return io.getStandardScriptsDir();
	}

	public boolean exportDatastore(ODLDatastore<? extends ODLTableReadOnly> ds, File file, boolean xlsx, ProcessingApi processing, ExecutionReport report) {
		return io.exportDatastore(ds, file, xlsx, processing, report);
	}

	public ODLDatastoreAlterable<ODLTableAlterable> importFile(File file, ImportFileType type, ProcessingApi processingApi, ExecutionReport report) {
		return io.importFile(file, type, processingApi, report);
	}

	public ODLDatastoreAlterable<ODLTableAlterable> importFile(File file, ProcessingApi processingApi, ExecutionReport report) {
		return io.importFile(file, processingApi, report);
	}

	public String normalisePath(String s) {
		return io.normalisePath(s);
	}

	@Override
	public File getLoadedExcelFile() {
		return io.getLoadedExcelFile();
	}

	@Override
	public List<String> getShapefileFieldnames(File shapefileOrODLRG) {
		return io.getShapefileFieldnames(shapefileOrODLRG);
	}

	@Override
	public ODLTable importShapefile(File file, int maxRows) {
		return io.importShapefile(file, maxRows);
	}

	@Override
	public File getAsRelativeIfWithinStandardShapefileDirectory(File file) {
		return io.getAsRelativeIfWithinStandardShapefileDirectory(file);
	}
	
	
}
