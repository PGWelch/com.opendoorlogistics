package com.opendoorlogistics.core.distances.external;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.distances.external.RoundingGrid.GridNeighboursResult;

import gnu.trove.impl.Constants;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;

public class LoadedMatrixFile {
	private final TObjectIntHashMap<LatLong> locationsToIndices = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
	private final List<LatLong> locations = new ArrayList<>();
	private final List<TDoubleArrayList> distancesKM = new ArrayList<>();
	private final List<TDoubleArrayList> timeSeconds = new ArrayList<>();
	private final File file;
	private final FileVersionId fileVersionId;
	
	public LoadedMatrixFile(File file) {
		this.file = file;
		fileVersionId = new FileVersionId(file);
	}

	public TObjectIntHashMap<LatLong> getLocationsToIndices() {
		return locationsToIndices;
	}
		

	public List<LatLong> getLocations() {
		return locations;
	}

	public List<TDoubleArrayList> getDistancesKM() {
		return distancesKM;
	}

	public List<TDoubleArrayList> getTimeSeconds() {
		return timeSeconds;
	}

	public enum ValueType{
		SECONDS,
		KM
	}
	
	public List<TDoubleArrayList> get(ValueType vt){
		return vt == ValueType.SECONDS ? timeSeconds:distancesKM;
	}
	
	public double get(int row, int col, ValueType vt){
		return get(vt).get(row).get(col);
	}
	
	public void set(int row, int col, ValueType vt, double value){
		get(vt).get(row).set(col, value);
	}



	public File getFile() {
		return file;
	}


	public FileVersionId getFileVersionId() {
		return fileVersionId;
	}
	
	
}
