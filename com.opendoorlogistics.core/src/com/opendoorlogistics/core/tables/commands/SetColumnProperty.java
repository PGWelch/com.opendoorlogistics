/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.commands;

import java.util.TreeSet;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;

final public class SetColumnProperty extends Command{
	final private int col ; 
	final private PropertyType type;
	final private Object value;
	
	public enum PropertyType{
		FLAGS,
		TAGS,
		DESCRIPTION,
		DEFAULT_VALUE
	}
	
	public SetColumnProperty(int tableId, int col , PropertyType type, Object value) {
		super(tableId);
		this.col = col;
		this.type = type;
		this.value = value;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {
		ODLTableDefinitionAlterable table = (ODLTableDefinitionAlterable)database.getTableByImmutableId(tableId);
		if(table==null){
			return null;
		}
		
		Object current=null;
		switch(type){
		
		case FLAGS:
			current = table.getColumnFlags(col);
			table.setColumnFlags(col, (Long)value);
			break;
			
		case TAGS:
			current = table.getColumnTags(col);
			if(current!=null){
				// deep copy as safer...
				current = new TreeSet<String>( (java.util.Set<String>)current);
			}
			table.setColumnTags(col,  (java.util.Set<String>)value);
			break;
			
		case DESCRIPTION:
			current = table.getColumnDescription(col);
			table.setColumnDescription(col, (String)value);
			break;
			
		case DEFAULT_VALUE:
			current = table.getColumnDefaultValue(col);
			table.setColumnDefaultValue(col, value);
			break;
		}
		
		return new SetColumnProperty(tableId,col,type,current);
	}

	@Override
	public long calculateEstimateSizeBytes() {
		return 12 + 4 + 4 + getEstimatedObjectMemoryFootprintBytes(value);
	}

}
