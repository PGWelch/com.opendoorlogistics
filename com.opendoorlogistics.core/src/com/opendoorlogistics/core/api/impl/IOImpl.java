package com.opendoorlogistics.core.api.impl;

import java.io.File;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.IO;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.api.impl.scripts.ScriptOptionImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.tables.io.PoiIO;
import com.opendoorlogistics.core.tables.io.TableIOUtils;

public class IOImpl implements IO{

	@Override
	public File getStandardDataDirectory() {
		return getAbsFile(AppConstants.DATA_DIRECTORY);
	}

	private File getAbsFile(String s) {
		File ret = new File(s);
		ret = ret.getAbsoluteFile();
		return ret;
	}

	@Override
	public File getStandardConfigDirectory() {
		return getAbsFile(AppConstants.ODL_CONFIG_DIR);
	}

	@Override
	public File getStandardScriptsDir() {
		return getAbsFile(AppConstants.SCRIPTS_DIRECTORY);
	}

	@Override
	public boolean exportDatastore(ODLDatastore<? extends ODLTableReadOnly> ds, File file, boolean xlsx, ProcessingApi processing, ExecutionReport report) {
		return PoiIO.exportDatastore(ds, file, xlsx, processing, report);
	}

	@Override
	public ODLDatastoreAlterable<ODLTableAlterable> importFile(File file, ImportFileType type, ProcessingApi processingApi, ExecutionReport report) {
		return TableIOUtils.importFile(file, type, processingApi, report);
	}

}
