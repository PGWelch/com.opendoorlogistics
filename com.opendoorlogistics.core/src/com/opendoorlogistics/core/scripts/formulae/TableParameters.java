/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import java.util.List;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.tables.ODLRow;
import com.opendoorlogistics.core.tables.ODLRowReadOnly;

final public class TableParameters implements FunctionParameters {
	private final List<?> datastores;
	private final int dsIndx;
	private final int tableId;
	private final long rowId;
	private final int rowNbIfKnown;
	private final ODLRowReadOnly thisRow;
	
	/**
	 * @param datastores
	 * @param dsIndx
	 * @param tableId
	 * @param rowId
	 * @param rowNbIfKnown Set to -1 if row number is unknown and it will be calculated
	 * from the default table.
	 */
	public TableParameters(List<?> datastores, int dsIndx, int tableId, long rowId, int rowNbIfKnown, ODLRowReadOnly thisRow) {
		this.datastores = datastores;
		this.dsIndx = dsIndx;
		this.tableId = tableId;
		this.rowId = rowId;
		this.rowNbIfKnown = rowNbIfKnown;
		this.thisRow = thisRow;
	}

	public int getDatasourceIndx() {
		return dsIndx;
	}

	public int getTableId() {
		return tableId;
	}

	public long getRowId() {
		return rowId;
	}

	public List<?> getDatastores() {
		return datastores;
	}

	public ODLTableReadOnly getTableById(int datastoreIndx, int tableId) {
		if (datastoreIndx < datastores.size() && datastoreIndx >= 0) {
			ODLDatastore<?> ds = (ODLDatastore<?>) datastores.get(datastoreIndx);
			return (ODLTableReadOnly) ds.getTableByImmutableId(tableId);
		}
		return null;
	}

	public ODLTableReadOnly getDefaultTable() {
		return getTableById(dsIndx, tableId);
	}
	
	public int getRowNb(){
		if(rowNbIfKnown!=-1){
			return rowNbIfKnown;
		}
		ODLTableReadOnly table = getDefaultTable();
		if(table!=null){
			int n = table.getRowCount();
			for(int i =0;i<n && rowId!=-1;i++){
				if(table.getRowId(i)==rowId){
					return i;
				}
			}
		}
		
		throw new RuntimeException("Row number is not available.");
	}
	
	public ODLRowReadOnly getThisRow(){
		return thisRow;
	}
}
