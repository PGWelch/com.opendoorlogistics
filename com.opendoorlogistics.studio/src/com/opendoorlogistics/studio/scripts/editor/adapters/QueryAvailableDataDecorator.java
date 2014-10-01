/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.adapters;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

public class QueryAvailableDataDecorator implements QueryAvailableData{
	private final QueryAvailableData decorated;

	public QueryAvailableDataDecorator(QueryAvailableData decorated) {
		this.decorated = decorated;
	}

	@Override
	public String[] queryAvailableFields(String datastore, String tablename) {
		return decorated.queryAvailableFields(datastore, tablename);
	}

	@Override
	public String[] queryAvailableTables(String datastore) {
		return decorated.queryAvailableTables(datastore);
	}

	@Override
	public String[] queryAvailableDatastores () {
		return decorated.queryAvailableDatastores();
	}

	@Override
	public String[] queryAvailableFormula(ODLColumnType columnType) {
		return decorated.queryAvailableFormula(columnType);
	}

	@Override
	public ODLTableDefinition getTableDefinition(String datastore, String tablename) {
		return decorated.getTableDefinition(datastore, tablename);
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getDatastoreDefinition(String datastore) {
		return decorated.getDatastoreDefinition(datastore);
	}

//	@Override
//	public ODLDatastore<? extends ODLTableDefinition> getDatastoreDefinition() {
//		return decorated.getDatastoreDefinition();
//	}
	
	
}
