package com.opendoorlogistics.core.api.impl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.opengis.feature.simple.SimpleFeatureType;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.IO;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.geometry.ImportShapefile;
import com.opendoorlogistics.core.geometry.rog.RogReaderUtils;
import com.opendoorlogistics.core.tables.io.PoiIO;
import com.opendoorlogistics.core.tables.io.TableIOUtils;
import com.opendoorlogistics.core.utils.Exceptions;
import com.opendoorlogistics.core.utils.io.RelativeFiles;
import com.opendoorlogistics.core.utils.strings.Strings;

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

	@Override
	public ODLDatastoreAlterable<ODLTableAlterable> importFile(File file, ProcessingApi processingApi, ExecutionReport report) {
		String fileExt = FilenameUtils.getExtension(file.getAbsolutePath());
		for(ImportFileType ift : ImportFileType.values()){
			for(String ext:ift.getFilter().getExtensions()){
				if(Strings.equalsStd(ext,fileExt )){
					return importFile(file, ift, processingApi, report);
				}
			}
		}
		return null;
	}

	@Override
	public String normalisePath(String s) {
		return FilenameUtils.normalize(s);
	}

	@Override
	public File getLoadedExcelFile() {
		// This gets overridden within the ODL Studio project
		return null;
	}

	@Override
	public List<String> getShapefileFieldnames(File shapefileOrODLRG) {
		// turn an .odlrg file reference into a shapefile reference 
		if(Strings.equalsStd(FilenameUtils.getExtension(shapefileOrODLRG.getName()), RogReaderUtils.RENDER_GEOMETRY_FILE_EXT)){
			String s = FilenameUtils.removeExtension(shapefileOrODLRG.getAbsolutePath());
			s += ".shp";
			shapefileOrODLRG = new File(s);
		}

		DataStore shapefile = null;
		try {
			Map<String, URL> map = new HashMap<String, URL>();
			map.put("url", shapefileOrODLRG.toURI().toURL());
			shapefile = DataStoreFinder.getDataStore(map);

			// check not corrupt
			if (shapefile.getTypeNames().length != 1) {
				throw new RuntimeException("Shapefile should only contain one type");
			}

			String typename = shapefile.getTypeNames()[0];
			SimpleFeatureType schema = shapefile.getSchema(typename);
			int nAttrib = schema.getAttributeCount();
			ArrayList<String> ret = new ArrayList<>();
			for (int i = 0; i < nAttrib; i++) {
				ret.add(schema.getDescriptor(i).getLocalName());
			}
			return ret;

		} catch (Throwable e) {
			throw Exceptions.asUnchecked(e);
		} finally {

			if (shapefile != null) {
				shapefile.dispose();
			}
		}
	}

	@Override
	public ODLTable importShapefile(File file, int maxRows) {
		ODLDatastoreAlterable<? extends ODLTableAlterable> ds=new ODLApiImpl().tables().createAlterableDs();
		ImportShapefile.importShapefile(file, false, ds, false, maxRows);
		return ds.getTableAt(0);
	}

	@Override
	public File getAsRelativeIfWithinStandardShapefileDirectory(File file) {
		return RelativeFiles.makeRelativeIfAbsoluteWithinDefaultDirectory(file.getAbsolutePath(), AppConstants.SHAPEFILES_DIRECTORY);
	}

}
