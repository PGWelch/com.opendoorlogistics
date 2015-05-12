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

final public class SetTableProperty extends Command{
	final private PropertyType type;
	final private Object value;
	
	public enum PropertyType{
		FLAGS,
		TAGS,
	//	DESCRIPTION,
	//	DEFAULT_VALUE
	}
	
	public SetTableProperty(int tableId, PropertyType type, Object value) {
		super(tableId);
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
			current = table.getFlags();
			table.setFlags((Long)value);
			break;
			
		case TAGS:
			current = table.getTags();
			if(current!=null){
				// deep copy as safer...
				current = new TreeSet<String>( (java.util.Set<String>)current);
			}
			table.setTags(  (java.util.Set<String>)value);
			break;
			
//		case DESCRIPTION:
//			current = table.getColumnDescription(col);
//			table.setColumnDescription(col, (String)value);
//			break;
//			
//		case DEFAULT_VALUE:
//			current = table.getColumnDefaultValue(col);
//			table.setColumnDefaultValue(col, value);
//			break;
		}
		
		return new SetTableProperty(tableId,type,current);
	}

	@Override
	public long calculateEstimateSizeBytes() {
		return 12 + 4 + getEstimatedObjectMemoryFootprintBytes(value);
	}

}
