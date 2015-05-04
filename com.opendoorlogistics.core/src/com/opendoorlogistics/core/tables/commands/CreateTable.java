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
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;

final public class CreateTable extends Command {
	private final String name;
	private final long flags;
	
	public CreateTable(String tablename, int tableId, long flags) {
		super(tableId);
		this.name = tablename;
		this.flags= flags;
	}

	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {
		ODLDatastoreAlterable<? extends ODLTableDefinition> upcast = (ODLDatastoreAlterable<? extends ODLTableDefinition>)database;
		ODLTableDefinitionAlterable td =(ODLTableDefinitionAlterable) upcast.createTable(name,tableId);
		//System.out.println("Creating table with id " + tableId);
		if(td!=null){
			td.setFlags(flags);
			return new DeleteEmptyTable(td.getImmutableId());
		}
		return null;
	}

	@Override
	public long calculateEstimateSizeBytes() {
		long ret = 12;
		ret += getEstimatedObjectMemoryFootprintBytes(name);
		ret += 8;
		return ret;
	}

}
