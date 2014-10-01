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

final public class UndeleteRow extends Command{
	private final int row;
	private final long rowId;
	private final Object [] values;
	
	public UndeleteRow(int tableId, int row,long rowId, Object[] values) {
		super(tableId);
		this.row = row;
		this.rowId = rowId;
		this.values = values;
	}

	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {	
		ODLTable table = (ODLTable)database.getTableByImmutableId(tableId);
		if(table==null){
			return null;
		}
		
	//	System.out.println("Undeleting row " + rowId);
		
		table.insertEmptyRow(row, rowId);
		for(int i =0 ; i<values.length ; i++){
			table.setValueAt(values[i], row, i);
		}
		return new DeleteRow(tableId, row);
	}
	
}
