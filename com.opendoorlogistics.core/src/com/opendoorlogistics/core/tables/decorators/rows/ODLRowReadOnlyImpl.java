/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.rows;

import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.ODLRowReadOnly;
import com.opendoorlogistics.core.tables.memory.ODLTableImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;

final public class ODLRowReadOnlyImpl implements ODLRowReadOnly{
	private final ODLTableReadOnly table;
	private final int rowNb;
	
	public ODLRowReadOnlyImpl(ODLTableReadOnly table, int rowNb) {
		super();
		this.table = table;
		this.rowNb = rowNb;
	}

	@Override
	public int getColumnCount() {
		return table.getColumnCount();
	}

	@Override
	public Object get(int col) {
		return table.getValueAt(rowNb, col);
	}

	@Override
	public ODLTableDefinition getDefinition() {
		return table;
	}

	@Override
	public int getRowIndex() {
		return rowNb;
	}
	
	@Override
	public String toString(){
		ODLTableImpl tmp = new ODLTableImpl(0, table.getName());
		DatastoreCopier.copyTableDefinition(table, tmp);
		int row = tmp.createEmptyRow(-1);
		for(int i =0 ;i< tmp.getColumnCount() ;i++){
			DatastoreCopier.copyCell(table, rowNb, i, tmp, row, i);
		}
		return tmp.toString();
	}
}
