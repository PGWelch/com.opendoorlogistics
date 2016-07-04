/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.distances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.distances.ODLCostMatrix;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableQuery;
import com.opendoorlogistics.core.tables.memory.ODLTableDefinitionImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.iterators.IteratorUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;

public class ODLCostMatrixImpl extends ODLTableDefinitionImpl implements ODLTable,ODLCostMatrix {
	final private List<String> ids;
	protected final static String [] STANDARD_COST_FIELDNAMES =  new String[] { PredefinedTags.TRAVEL_COST, PredefinedTags.DISTANCE, PredefinedTags.TIME };
	final private double[][][] matrix;
	final private int n;
	final private int nSquared;
	final private int nbCosts;
	// final private int fromCol;
	// final private int toCol;
	// final private int costCol;
	final private StandardisedStringTreeMap<Integer> idsToIndices = new StandardisedStringTreeMap<>(false);

	public enum MatrixType {
		SINGLE_COST, DISTANCE_TIME
	}

	@Override
	public long getSizeInBytes() {
		return nbCosts * n * n * 8;
	}
	
	public ODLCostMatrixImpl(Iterable<String> ids,String[]costFieldNames) {
		setName(PredefinedTags.TRAVEL_COSTS);
		nbCosts = costFieldNames.length;
		
		// setup table definition
		addColumn(-1, PredefinedTags.FROM_LOCATION, ODLColumnType.STRING, 0);
		addColumn(-1, PredefinedTags.TO_LOCATION, ODLColumnType.STRING, 0);

		for(int i =0 ; i<costFieldNames.length ; i++){
			addColumn(-1, costFieldNames[i], ODLColumnType.DOUBLE, 0);			
		}

		this.ids = IteratorUtils.toList(ids);
		this.n = this.ids.size();
		this.nSquared = n * n;
		matrix = new double[costFieldNames.length][][];
		for (int i = 0; i < costFieldNames.length; i++) {
			matrix[i] = new double[n][];
			for (int j = 0; j < n; j++) {
				matrix[i][j] = new double[n];
			}
		}

		// setup id lookup
		for (int i = 0; i < n; i++) {
			String s = this.ids.get(i);
			if (idsToIndices.get(s) != null) {
				throw new RuntimeException("Duplicate location id: " + s);
			}
			idsToIndices.put(s, i);
		}
	}

	@Override
	public int getRowCount() {
		return nSquared;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		int from = rowIndex / n;
		int to = rowIndex % n;
		if (columnIndex == 0) {
			return ids.get(from);
		} else if (columnIndex == 1) {
			return ids.get(to);
		} else if (columnIndex > 1) {
			return (double) matrix[columnIndex - 2][from][to];
		}
		return null;
	}

	@Override
	public Object getValueById(long rowId, int columnIndex) {
		int index = TableUtils.getLocalRowId(rowId);
		return getValueAt(index, columnIndex);
	}

	@Override
	public long getRowId(int rowIndex) {
		return TableUtils.getGlobalId(getImmutableId(), rowIndex);
	}

	@Override
	public boolean containsRowId(long rowId) {
		int index = TableUtils.getLocalRowId(rowId);
		return index < nSquared;
	}

	@Override
	public long[] find(int col, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex >= 2) {
			int from = rowIndex / n;
			int to = rowIndex % n;
			Double val = Numbers.toDouble(aValue);
			float f;
			if (val != null) {
				f = (float) val.doubleValue();
			} else {
				f = 0;
			}
			matrix[columnIndex - 2][from][to] = f;
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public String getId(int i) {
		return ids.get(i);
	}

	@Override
	public double get(int fromIndex, int toIndex, int dim) {
		return matrix[dim][fromIndex][toIndex];
	}

	//
	// public void set(float val, String from, String to, int dim){
	// set(val, idsToIndices.get(from), idsToIndices.get(to),dim);
	// }

	public void set(double val, int fromIndex, int toIndex, int dim) {
		matrix[dim][fromIndex][toIndex] = val;
	}

	@Override
	public void setValueById(Object aValue, long rowid, int columnIndex) {
		int index = TableUtils.getLocalRowId(rowid);
		setValueAt(aValue, index, columnIndex);
	}

	@Override
	public int createEmptyRow(long rowId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void insertEmptyRow(int insertAtRowNb, long rowId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteRow(int rowNumber) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return TableUtils.convertToString(this);
	}

	@Override
	public long getRowFlags(long rowId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setRowFlags(long flags, long rowId) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getNbCosts() {
		return matrix.length;
	}

	@Override
	public int getIndex(String id){
		Integer ret = idsToIndices.get(id);
		if(ret==null){
			return -1;
		}
		return ret;
	}

	@Override
	public int getNbFroms() {
		return n;
	}

	@Override
	public int getNbTos() {
		return n;
	}

//	@Override
//	public int getNbConnectedSubsets() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public Iterable<String> getConnectedSubset(int i) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public boolean getIsConnected(int from, int to) {
		for(int i=0 ; i < matrix.length ; i++){
			if(matrix[i][from][to]==Double.POSITIVE_INFINITY || matrix[i][from][to]==Double.NaN || matrix[i][from][to]==Double.MAX_VALUE ){
				return false;
			}
		}
		return true;
	}

	@Override
	public long getRowLastModifiedTimeMillsecs(long rowId) {
		return 0;
	}

	@Override
	public ODLTableReadOnly query(TableQuery query) {
		return null;
	}


	public static ODLCostMatrixImpl createEmptyMatrix(List<Map.Entry<String, LatLong>> list) {
		ArrayList<String> idList = new ArrayList<>();
		for (Map.Entry<String, LatLong> entry : list) {
			idList.add(entry.getKey());
		}
		ODLCostMatrixImpl output = new ODLCostMatrixImpl(idList,STANDARD_COST_FIELDNAMES);
		return output;
	}

	@Override
	public boolean isStillValid() {
		return true;
	}

	@Override
	public String getFromId(int fromIndex) {
		return ids.get(fromIndex);
	}

	@Override
	public String getToId(int toIndex) {
		return ids.get(toIndex);
	}
	
	
}
