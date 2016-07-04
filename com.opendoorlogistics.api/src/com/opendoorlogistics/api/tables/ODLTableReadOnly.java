/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public interface ODLTableReadOnly extends ODLTableDefinition, TableModel {
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

	
    /**
     * Needed to make interface compatible with swing TableModel
     */	
    @Override
    default void setValueAt(Object aValue, int rowIndex, int columnIndex){
    
    }

    /**
     * Needed to make interface compatible with swing TableModel
     */    
    @Override
    default boolean isCellEditable(int rowIndex, int columnIndex){
    	return false;
    }
    
    /**
     * Needed to make interface compatible with swing TableModel
     */    
    @Override
    default void addTableModelListener(TableModelListener l){
    	
    }

    /**
     * Needed to make interface compatible with swing TableModel
     */
    @Override
    default void removeTableModelListener(TableModelListener l){
    	
    }

    /**
     * Needed to make interface compatible with swing TableModel.
     * We reutrn string as we only use this for display purposes...
     */
    @Override
    default Class<?> getColumnClass(int columnIndex){
    	return String.class;
    }

	//ODLTableReadOnly findGeo(LatLong min, LatLong max, int zoom, int geomCol);
}
