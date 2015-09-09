/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.scripts;

import java.util.List;

import com.opendoorlogistics.api.tables.ODLTableDefinition;

public interface ScriptAdapter extends ScriptElement{
	/**
	 * Add the destination table to the adapter and link it to the source table
	 * using a best-guess based on column names.
	 * @param source
	 * @param destination
	 * @return The table index.
	 */
	ScriptAdapterTable addSourcedTableToAdapter(String sourceDatastoreId, ODLTableDefinition source, ODLTableDefinition destination);

	List<ScriptAdapterTable> addSourcedTablesToAdapter(ScriptInputTables tables);
	
	/**
	 * Add the table to the adapter without a source table. 
	 * Default column values will be filled in as formulae.
	 * @param table
	 * @return The table index.
	 */
	ScriptAdapterTable addSourcelessTable(ODLTableDefinition destination);
	
	int getTableCount();

	String getAdapterId();
	
	ScriptAdapterTable addEmptyTable(String tableName);
	
	long getFlags();
	
	void setFlags(long flags);
	
	ScriptAdapterTable getTable(int i);
	
	enum ScriptAdapterType{
		NORMAL,
		VLS,
		PARAMETER,
	}
	
	ScriptAdapterType getAdapterType();
	
	void setAdapterType(ScriptAdapterType type);
}
