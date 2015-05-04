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

final public class SetTableName extends Command{
	private final String name;
	
	public SetTableName(int tableId, String name) {
		super(tableId);
		this.name = name;
	}

	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {
		String current = database.getTableByImmutableId(tableId).getName();
		ODLDatastoreAlterable<? extends ODLTableDefinition> upcast = (ODLDatastoreAlterable<? extends ODLTableDefinition>)database;
		if(upcast.setTableName(tableId, name)){
			return new SetTableName(tableId,current);
		}
		return null;
	}

	@Override
	public long calculateEstimateSizeBytes() {
		return 12 + getEstimatedObjectMemoryFootprintBytes(name);
	}

}
