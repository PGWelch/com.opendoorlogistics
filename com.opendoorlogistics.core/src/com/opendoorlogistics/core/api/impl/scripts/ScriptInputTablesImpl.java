/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl.scripts;

import java.util.ArrayList;

import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

public class ScriptInputTablesImpl implements ScriptInputTables{
	private class Record{
		final String datastoreId;
		final ODLTableDefinition source;
		final ODLTableDefinition target;
		
		Record(String datastoreId, ODLTableDefinition source, ODLTableDefinition target) {
			this.datastoreId = datastoreId;
			this.source = source;
			this.target = target;
		}
		
	}
	final ArrayList<Record> records = new ArrayList<>();
	
	public void add(String datastoreId, ODLTableDefinition source, ODLTableDefinition target){
		records.add(new Record(datastoreId, source, target));
	}

	@Override
	public int size() {
		return records.size();
	}

	@Override
	public ODLTableDefinition getSourceTable(int i) {
		return records.get(i).source;
	}

	@Override
	public String getSourceDatastoreId(int i) {
		return records.get(i).datastoreId;
	}

	@Override
	public ODLTableDefinition getTargetTable(int i) {
		return records.get(i).target;
	}
}
