/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.column;

import java.util.Set;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

/**
 * The internal column class is not exposed by the table interfaces
 * but this decorator can use the interfaces to provide what appears
 * to be a column object 
 * @author Phil
 *
 */
final public class ColumnDecorator {
	private final ODLTableDefinition table;
	private final int col;
	
	public ColumnDecorator(ODLTableDefinition table, int col) {
		super();
		this.table = table;
		this.col = col;
	}
	
	public String getName() {
		return table.getColumnName(col);
	}

	public ODLColumnType getType() {
		return table.getColumnType(col);
	}

	public long getFlags(){
		return table.getColumnFlags(col);
	}
	
	public String getDescription() {
		return table.getColumnDescription(col);
	}

	public Set<String> getTags() {
		return table.getColumnTags(col);
	}

	public Object getDefaultValue() {
		return table.getColumnDefaultValue(col);
	}

	public int getImmutableId(){
		return table.getColumnImmutableId(col);
	}

}
