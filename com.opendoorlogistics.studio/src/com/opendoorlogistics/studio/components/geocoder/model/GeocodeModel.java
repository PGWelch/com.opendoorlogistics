/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder.model;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;

final public class GeocodeModel {
	private final ODLDatastore<? extends ODLTable> ds;
	private final TLongObjectHashMap<String> addressByRowId = new TLongObjectHashMap<>();
	private int row = -1;
	private List<SearchResultPoint> searchResults;
	private TreeSet<Integer> selectedResultIndices;
	private HashSet<GeocodeModelListener> listeners = new HashSet<>();
	public static final int LAT_COL = 1;
	public static final int LNG_COL = 2;

	public GeocodeModel(ODLDatastore<? extends ODLTable> ds) {
		this.ds = ds;
	}

	public void addListener(GeocodeModelListener listener) {
		listeners.add(listener);
	}

	public void fireListeners(boolean recordChanged, boolean searchResultsChanged) {
		for (GeocodeModelListener listener : listeners) {
			listener.modelChanged(recordChanged, searchResultsChanged);
		}
	}

	private ODLTable table() {
		return ds.getTableAt(0);
	}

	public Double getLatitude() {
		Double ret = (Double) getValueAt(LAT_COL);
		if (ret != null) {
			return ret;
		}
		return null;
	}

	public Double getLongitude() {
		Double ret = (Double) getValueAt(LNG_COL);
		if (ret != null) {
			return ret;
		}
		return null;
	}

	public String getAddress() {
		ODLTableReadOnly table = ds.getTableAt(0);
		if (table != null && row >= 0 && row < table.getRowCount()) {
			// take from the cache of addresses if present as we may have modified it
			// but don't want to modify in the actual datastore
			long rowId = table.getRowId(row);
			String address = addressByRowId.get(rowId);
			if (address != null) {
				return address;
			} else {
				address = cacheAddressValue(table, rowId);
			}
		}

		return "";
	}

	private String cacheAddressValue(ODLTableReadOnly table, long rowId) {
		Object val = table.getValueAt(row, 0);
		String ret= val != null ? val.toString() : "";
		addressByRowId.put(rowId, ret);
		return ret;
	}

	public void resetAddress(){
		ODLTableReadOnly table = ds.getTableAt(0);
		if (table != null && row >= 0 && row < table.getRowCount()) {
			// reset the cached value with the datastore value
			long rowId = table.getRowId(row);	
			cacheAddressValue(table,rowId);
			fireListeners(true, true);
		}
	}
	
	public boolean hasPoint() {
		ODLTable table = ds.getTableAt(0);
		return table != null && row >= 0 && row < table.getRowCount();
	}

	private Object getValueAt(int col) {
		ODLTableReadOnly table = ds.getTableAt(0);
		if (table != null && row >= 0 && row < table.getRowCount()) {
			return table.getValueAt(row, col);
		}
		return null;
	}

	private void setValueAt(Object val, int col) {
		ODLTable table = ds.getTableAt(0);
		if (table != null && row >= 0 && row < table.getRowCount()) {
			table.setValueAt(val, row, col);
			fireListeners(false, false);
		}
	}

	public void setAddress(String s) {
		ODLTableReadOnly table = ds.getTableAt(0);
		if (table != null && row >= 0 && row < table.getRowCount()) {
			long rowId = table.getRowId(row);
			
			// save locally, not to the datastore
			addressByRowId.put(rowId, s);
		}
	}

	public void setGeocode(LatLong ll) {
		setGeocode(ll.getLatitude(), ll.getLongitude());
	}

	public void setGeocode(Double latitude, Double longitude) {
		setValueAt(latitude, LAT_COL);
		setValueAt(longitude, LNG_COL);
	}

	public void gotoNextRecord() {
		if (!hasNextRecord()) {
			throw new IndexOutOfBoundsException();
		}
		row++;
		clearResultsOnRecordChange();
	}

	private void clearResultsOnRecordChange() {
		searchResults = new ArrayList<>();
		selectedResultIndices = new TreeSet<>();
		fireListeners(true, true);
	}

	public void gotoPreviousRecord() {
		if (!hasPreviousRecord()) {
			throw new IndexOutOfBoundsException();
		}
		row--;
		clearResultsOnRecordChange();
	}

	public boolean hasNextRecord() {
		ODLTable table = table();
		return table != null && row < table.getRowCount() - 1;
	}

	// private int nextRow(){
	// ODLTable table = table();
	// if(table!=null && row < table.getRowCount()-1){
	// if(skipAlreadyGeocoded==false){
	// return row+1;
	// }
	//
	// for(int i =row+1 ; i<table.getRowCount() ; i++){
	// if(table.getValueAt(i, LAT_COL)==null || table.getValueAt(i, LNG_COL)==null){
	// return i;
	// }
	// }
	// }
	// return -1;
	// }

	public boolean hasPreviousRecord() {
		return table() != null && row > 0;
	}

	public void setSearchResults(List<SearchResultPoint> list) {
		if (list != null) {
			searchResults = Collections.unmodifiableList(list);
		} else {
			searchResults = null;
		}
		fireListeners(false, true);
	}

	public List<SearchResultPoint> getSearchResults() {
		return searchResults;
	}

	public Iterable<Integer> getSelectedResultIndices() {
		return selectedResultIndices;
	}

	public int getSelectedResultsCount() {
		return selectedResultIndices.size();
	}

	public void setSelectedResultIndices(Iterable<Integer> selectedResultIndices) {
		this.selectedResultIndices.clear();
		for (int i : selectedResultIndices) {
			this.selectedResultIndices.add(i);
		}
		fireListeners(false, false);
	}

	public int getRowCount() {
		ODLTable table = table();
		if (table != null) {
			return table.getRowCount();
		}
		return 0;
	}

	public int getRow() {
		return row;
	}

	public static ODLDatastore<? extends ODLTableDefinition> getDsDefn() {
		ODLDatastoreAlterable<ODLTableAlterable> ret = createEmptyDs();
		return ret;
	}

	public static ODLDatastoreAlterable<ODLTableAlterable> createEmptyDs() {
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
		ODLTableDefinitionAlterable table = ret.createTable("Points", -1);
		table.addColumn(-1, "Address", ODLColumnType.STRING, 0);
		table.addColumn(-1, "Latitude", ODLColumnType.DOUBLE, 0);
		table.addColumn(-1, "Longitude", ODLColumnType.DOUBLE, 0);
		return ret;
	}
}
