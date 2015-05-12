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

final public class SetByRowId extends Command{
	private final long rowId;
	private final int col;
	private final Object newValue;

	public SetByRowId(int tableId, long rowId, int col, Object newValue) {
		super(tableId);
		this.rowId = rowId;
		this.col = col;
		this.newValue = newValue;
	}

	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {			
		ODLTable table = (ODLTable)database.getTableByImmutableId(tableId);
		if(table==null){
			return null;
		}
		
		Object originalValue = table.getValueById(rowId, col);
		table.setValueById(newValue, rowId, col);
		return new SetByRowId(tableId, rowId, col, originalValue);
	}

	@Override
	public long calculateEstimateSizeBytes() {
		return 12 + 8 + 4 + getEstimatedObjectMemoryFootprintBytes(newValue);
	}


}
