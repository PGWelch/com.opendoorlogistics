/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.commands;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

final public class InsertEmptyCol extends Command{
	private final int col;
	private final int id;
	private final String name;
	private final ODLColumnType type;
	private final long flags;
	private final boolean allowDuplicateNames;
	//private final String description;
	
	public InsertEmptyCol(int tableId,int id, int col,String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		super(tableId);
		this.id = id;
		this.col = col;
		this.name = name;
		this.type = type;
		this.flags = flags;
		//this.description = description;
		this.allowDuplicateNames = allowDuplicateNames;
	}

	@Override
	public long calculateEstimateSizeBytes() {
		return 12 + 4 + 4 + getEstimatedObjectMemoryFootprintBytes(name) + 4 + 8 + 4 ;
	}
	
	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {	
		ODLTableAlterable table = (ODLTableAlterable)database.getTableByImmutableId(tableId);
		if(table==null){
			return null;
		}
		
		if(table.insertColumn(id,col, name, type, flags,allowDuplicateNames)){
			return new DeleteEmptyCol(tableId, col); 			
		}
		return null;
	}




}
