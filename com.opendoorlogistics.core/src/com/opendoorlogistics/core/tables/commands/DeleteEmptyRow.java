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

final public class DeleteEmptyRow extends Command{
	private final int row;
	
	public DeleteEmptyRow(int tableId, int row) {
		super(tableId);
		this.row = row;
	}

	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {
		ODLTable table = (ODLTable)database.getTableByImmutableId(tableId);
		if(table==null){
			return null;
		}
		
		long rowid = table.getRowId(row);
		table.deleteRow(row);
		return new InsertEmptyRow(tableId, row,rowid);
	}


	@Override
	public long calculateEstimateSizeBytes() {
		return 12 + 4;
	}
	
}
