/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.commands;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

final public class DeleteEmptyCol extends Command {
	private final int col;
	
	public DeleteEmptyCol(int tableId, int col) {
		super(tableId);
		this.col = col;
	}

	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {
		ODLTableAlterable table = (ODLTableAlterable)database.getTableByImmutableId(tableId);
		if(table!=null){

			InsertEmptyCol undo = new InsertEmptyCol(tableId, table.getColumnImmutableId(col), col, table.getColumnName(col), table.getColumnType(col),
					table.getColumnFlags(col),true);
			table.deleteColumn(col);
			return undo;			
		}
		return null;
	}

	public int getColumnIndex(){
		return col;
	}

	@Override
	public long calculateEstimateSizeBytes() {
		return 12 + 4;
	}
}
