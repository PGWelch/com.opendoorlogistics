/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.commands;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

final public class DeleteEmptyTable extends Command{ 

	public DeleteEmptyTable(int tableId) {
		super(tableId);
	}
	
	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {
		ODLDatastoreAlterable<? extends ODLTableDefinition> upcast = (ODLDatastoreAlterable<? extends ODLTableDefinition>)database;
		ODLTableDefinition table =upcast.getTableByImmutableId(tableId);
		if(table==null){
			return null;
		}
		
		if(table.getColumnCount()>0 || (ODLTableReadOnly.class.isInstance(table) && ((ODLTableReadOnly)table).getRowCount()>0)){
			throw new RuntimeException();
		}
		
	//	System.out.println("Deleting table with id " + tableId);

		long flags = table.getFlags();
		String name = table.getName();
		upcast.deleteTableById(tableId);
		return new CreateTable(name, tableId, flags);
	}

	@Override
	public long calculateEstimateSizeBytes() {
		return 12;
	}

}
