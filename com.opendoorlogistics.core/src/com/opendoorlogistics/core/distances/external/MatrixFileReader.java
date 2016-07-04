package com.opendoorlogistics.core.distances.external;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.components.ContinueProcessingCB;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.distances.ExternalMatrixFileConfiguration;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.distances.DistancesSingleton;
import com.opendoorlogistics.core.distances.external.LoadedMatrixFile.ValueType;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.Exceptions;
import com.opendoorlogistics.core.utils.UpdateTimer;
import com.opendoorlogistics.core.utils.io.RelativeFiles;
import com.opendoorlogistics.core.utils.strings.Strings;

import gnu.trove.list.array.TDoubleArrayList;

public class MatrixFileReader {
	public static final RoundingGrid ROUNDING_GRID = new RoundingGrid();

	private static class LineRecord {
		double fromLat;
		double fromLng;
		double toLat;
		double toLng;
		double distanceKM;
		double timeSecs;
	}

	private static double readDouble(String filename, long lineNb, int zeroBasedColNb, String[] split) {

		try {
			// LineRecord lineRecord = new LineRecord();
			// lineRecord.fromLat = Double.parseDouble(s)
			return Double.parseDouble(split[zeroBasedColNb]);
		}
		catch (Exception e) {
			throw new RuntimeException(
					"Matrix file " + filename + " has a corrupt value which can't be read on line " + lineNb + " column " + (zeroBasedColNb + 1));
			// throw new RuntimeException("Matrix file " + file.getName() + " has one or more values with incorrect format on line " + line + ".");
		}
	}

	public static LoadedMatrixFile loadFile(File file, ProcessingApi continueProcessing) {
		return loadFile(file, continueProcessing, Long.MAX_VALUE,null);
	}

	public static ODLTable loadFileAsTable(File file,long maxLines, ProcessingApi continueProcessing) {
		ODLApiImpl api = new ODLApiImpl();
		ODLTableAlterable table = api.tables().createAlterableDs().createTable("MatrixFile",-1);
		table.addColumn(-1, "FromLatitude", ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, "FromLongitude", ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, "ToLatitude", ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, "ToLongitude", ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, "DistanceKM", ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, "Time", ODLColumnType.TIME, 0);		
		loadFile(file, continueProcessing, maxLines,table);
		return table;
	}

	private static LoadedMatrixFile loadFile(File file, ProcessingApi processingApi, long maxLinesToRead, ODLTable outputTable) {
		LoadedMatrixFile ret = new LoadedMatrixFile(file);

		try {
			try (BufferedReader br = new BufferedReader(new FileReader(file))) {
				long lineNb = 1;
				String line;
				UpdateTimer progressTimer = new UpdateTimer(250);
				while ((line = br.readLine()) != null) {
					// assume first line is header
					if (lineNb > 1) {
						String[] split = line.split("\t");
						if (split.length != 6) {
							throw new RuntimeException("Matrix file " + file.getName() + " has wrong number of values on line " + line + ".");
						}

						// FromLatitude - from point latitude in decimal degrees (e.g. 52.342)
						// FromLongitude- from point longitude in decimal degrees (e.g. 100.342)
						// ToLatitude - to point latitude in decimal degrees
						// ToLongitude - to point longitude in decimal degrees
						// DistanceKM - distance in kilometres
						// Time - time in standard ODL Studio time format, which is hours:minutes:seconds, so 01:43:23. If the travel time is greater than 24
						// hours, you should include a days component as well - e.g. 1d 01:23:12
						//
						try {
							
							int tableRow =-1;
							if(outputTable!=null){
								tableRow = outputTable.createEmptyRow(-1);
							}
							
							LineRecord lineRecord = new LineRecord();
							lineRecord.fromLat = readDouble(file.getName(), lineNb, 0, split);
							lineRecord.fromLng = readDouble(file.getName(), lineNb, 1, split);
							lineRecord.toLat = readDouble(file.getName(), lineNb, 2, split);
							lineRecord.toLng = readDouble(file.getName(), lineNb, 3, split);

							if (split[4].trim().length() == 0) {
								lineRecord.distanceKM = DistancesSingleton.UNCONNECTED_TRAVEL_COST;
							} else {
								lineRecord.distanceKM = readDouble(file.getName(), lineNb, 4, split);
							}
							
							if(outputTable!=null){
								outputTable.setValueAt(lineRecord.fromLat, tableRow, 0);
								outputTable.setValueAt(lineRecord.fromLng, tableRow, 1);
								outputTable.setValueAt(lineRecord.toLat, tableRow, 2);
								outputTable.setValueAt(lineRecord.toLng, tableRow, 3);
								outputTable.setValueAt(lineRecord.distanceKM, tableRow, 4);
								
							}

							if (split[5].trim().length() == 0) {
								lineRecord.timeSecs = DistancesSingleton.UNCONNECTED_TRAVEL_COST;
							} else {
								ODLTime time = (ODLTime) ColumnValueProcessor.convertToMe(ODLColumnType.TIME, split[5]);
								if (time == null) {
									throw new RuntimeException(
											"Matrix file " + file.getName() + ", line " + line + ", could not read time value \"" + split[5] + "\".");
								}
								lineRecord.timeSecs = ((double) time.getTotalMilliseconds()) / ODLTime.MILLIS_IN_SEC;
								
								if(outputTable!=null){
									outputTable.setValueAt(time, tableRow, 5);
								}
							}

							processLine(lineRecord, lineNb, ret);
						}
						catch (Exception e) {
							throw new RuntimeException("Matrix file " + file.getName() + " has one or more values with incorrect format(s) on line " + lineNb + "."
									+System.lineSeparator() 
									+ "The contents of the line is:"
									+ System.lineSeparator() + System.lineSeparator() + line);
						}
					}

					lineNb++;
					
					if(processingApi!=null && progressTimer.isUpdate()){
						processingApi.postStatusMessage("Read " + lineNb  + " lines from file " + file.getAbsolutePath() + ", found " + ret.getLocations().size() + " locations.");
					}
					
					if(processingApi!=null && processingApi.isCancelled()){
						throw new RuntimeException("User cancelled during matrix file load.");
					}
					
					if(lineNb>=maxLinesToRead){
						// Skip validation as we won't have all the entries
						return ret;
					}
				}
			}

		}
		catch (Exception e) {
			throw Exceptions.asUnchecked(e);
		}
		
		// Now validate the matrix - check for any entries which are still nan
		int n = ret.getLocationsToIndices().size();
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				
				for(ValueType vt : ValueType.values()){
					boolean isNaN = Double.isNaN(ret.get(i, j, vt));
					if(isNaN){
						if(i==j){
							// set to zero if not set
							ret.set(i, j, vt, 0);
						}else{
							throw new RuntimeException("Matrix file " + file.getName() + " is missing an entry for " + ret.getLocations().get(i) + " to " + ret.getLocations().get(j)
								+". A valid matrix file must contain rows for all FROM and TO combinations of locations in the file.");
						}	
					}
				}
				

			}
		}
		return ret;
	}

	private static void processLine(LineRecord record, long lineNb, LoadedMatrixFile bb) {
		LatLong from = processLL(lineNb, record.fromLat, record.fromLng);
		LatLong to = processLL(lineNb, record.toLat, record.toLng);

		int iFrom = allocateLocation(from, bb);
		int iTo = allocateLocation(to, bb);
		if (record.distanceKM < 0) {
			throw new RuntimeException("Found negative distance entry on line " + lineNb + ": " + record.distanceKM);
		}

		setToMin(iFrom, iTo, record.distanceKM, bb.getDistancesKM());
		setToMin(iFrom, iTo, record.timeSecs, bb.getTimeSeconds());

	}

	private static void setToMin(int row, int col, double newValue, List<TDoubleArrayList> matrix) {
		TDoubleArrayList array = matrix.get(row);
		double existing = array.get(col);
		if (Double.isNaN(existing)) {
			array.set(col, newValue);
		} else {
			array.set(col, Math.min(newValue, existing));
		}
	}

	private static LatLong processLL(long lineNb, double lat, double lng) {
		LatLong ll = new LatLongImpl(lat, lng);
		if (!ll.isValid()) {
			throw new RuntimeException("Invalid latitude-longitude found on matrix line " + lineNb + ": " + ll.toString());
		}

		ll = ROUNDING_GRID.snapToGrid(ll);
		return ll;
	}

	private static int allocateLocation(LatLong ll, LoadedMatrixFile bb) {
		int indx = bb.getLocationsToIndices().get(ll);
		if (indx != -1) {
			return indx;
		}
		indx = bb.getLocationsToIndices().size();
		bb.getLocationsToIndices().put(ll, indx);
		bb.getLocations().add(ll);
		expandMatrixBy1(bb.getDistancesKM());
		expandMatrixBy1(bb.getTimeSeconds());
		return indx;
	}

	private static void expandMatrixBy1(List<TDoubleArrayList> matrix) {
		int n = matrix.size();

		// add new row to the bottom
		TDoubleArrayList newRow = new TDoubleArrayList(n + 1);
		newRow.fill(0, n, Double.NaN);
		matrix.add(newRow);

		// add one element to each row including the new row
		for (int i = 0; i <= n; i++) {
			matrix.get(i).add(Double.NaN);
		}
	}
	
	/**
	 * Resolve the matrix file or throw an exception if it can't be
	 * @param conf
	 * @param refFile
	 * @return
	 */
	public static File resolveExternalMatrixFileOrThrowException(ExternalMatrixFileConfiguration conf, File refFile) {
		// resolve the file...
		File file = null;
		if(conf.isUseDefaultFile()){
			String msgStart="Distances are set to use the automatic external matrix file";
			if(refFile==null){
				throw new RuntimeException(msgStart + "."+ System.lineSeparator() + "The automatic file is defined by the location of the loaded Excel file but no Excel file is loaded."
						+ System.lineSeparator() + "If you've created a new Excel file in ODL Studio, try saving it.");
			}
			
			String refNoExt=FilenameUtils.removeExtension(refFile.getAbsolutePath());
			file = new File(refNoExt + AppConstants.EXTERNAL_MATRIX_TEXTFILE_EXTENSION);
			if(!file.exists()){
				throw new RuntimeException(msgStart + " but the automatically-defined file " + file.getAbsolutePath() + " is not found.");
			}
		}else{
			String msgStart = "Distances are set to use the external matrix file in non-automatic mode ";
			if(Strings.isEmptyWhenStandardised(conf.getNonDefaultFilename())){
				throw new RuntimeException(msgStart + " but the filename is empty.");
			}
			file = RelativeFiles.validateRelativeFiles(conf.getNonDefaultFilename(), AppConstants.EXTERNAL_MATRIX_DIRECTORY);
			if(file==null){
				throw new RuntimeException(msgStart + " but the file " + conf.getNonDefaultFilename() + " cannot be found. " + System.lineSeparator()
						+ "You should either use an absolute file (e.g. \"c:\\mymatrixfile.txt\") or a relative file (e.g. \"mymatrixfile.txt\") "
						+ "which is located in the " + AppConstants.EXTERNAL_MATRIX_DIRECTORY + " subdirectory of your ODL Studio installation directory.");		
			}
		}
		return file;
	}

}
