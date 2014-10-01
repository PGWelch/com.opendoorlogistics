/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;


public interface ODLTable extends ODLTableReadOnly{
	void setValueAt(Object aValue, int rowIndex, int columnIndex);

	void setValueById(Object aValue, long rowid, int columnIndex);
	
	/**
	 * Set the row's flags. Changes to a row's flags are not entered
	 * into the undo/redo buffer as they are used to convey states
	 * such as 'selected in map control'.
	 * @param flags
	 * @param rowId
	 */
	void setRowFlags(long flags, long rowId);
	
	/**
	 * Create an empty row with the input id and append to the end. 
	 * If the id is -1 an id will automatically assigned. If the id already
	 * exists an unchecked exception will be thrown.
	 * @param rowLocalId
	 * @return The index of the newly-created row
	 */
	int createEmptyRow(long rowId);
	
	/**
	 * Create an empty row with the input id and insert in the position.
	 * If the id is -1 an id will automatically assigned. If the id already
	 * exists an unchecked exception will be thrown.
	 * @param rowLocalId
	 * @return
	 */
	void insertEmptyRow(int insertAtRowNb, long rowId);
	
	void deleteRow(int rowNumber);
	
}
