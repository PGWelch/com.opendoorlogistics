/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.commands;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

final public class Set extends Command{
	private final int row;
	private final int col;
	private final Object newValue;

	public Set(int tableId, int row, int col, Object newValue) {
		super(tableId);
		this.row = row;
		this.col = col;
		this.newValue = newValue;
	}

	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {			
		ODLTable table = (ODLTable)database.getTableByImmutableId(tableId);
		if(table==null){
			return null;
		}
		
		Object originalValue = table.getValueAt(row, col);
		table.setValueAt(newValue, row, col);
		return new Set(tableId, row, col, originalValue);
	}

	@Override
	public long calculateEstimateSizeBytes() {
		return 12 + 4 + 4 + getEstimatedObjectMemoryFootprintBytes(newValue);
	}


}
