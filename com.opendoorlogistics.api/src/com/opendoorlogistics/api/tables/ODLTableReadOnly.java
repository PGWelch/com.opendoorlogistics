/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;



public interface ODLTableReadOnly extends ODLTableDefinition {
	int getRowCount();
	Object getValueAt(int rowIndex, int columnIndex);
	Object getValueById(long rowId, int columnIndex);
	long getRowId(int rowIndex);
	boolean containsRowId(long rowId);
	long getRowFlags(long rowId);
	long getRowLastModifiedTimeMillsecs(long rowId);
	
	/**
	 * Perform query against the table, returning a copy of the table's data
	 * at the time of the query. Wherever possible, the rowids in the returned table
	 * will be equal to those in this table (and can therefore be used to connect to the
	 * up-to-date, writable source data).
	 * @param query
	 * @return
	 */
	ODLTableReadOnly query(TableQuery query);
	
	/**
	 * Return the rowids of all matching values, using an index
	 * by default
	 * @param col
	 * @param value
	 * @return
	 */
	long[] find(int col, Object value);
	
	//ODLTableReadOnly findGeo(LatLong min, LatLong max, int zoom, int geomCol);
}
