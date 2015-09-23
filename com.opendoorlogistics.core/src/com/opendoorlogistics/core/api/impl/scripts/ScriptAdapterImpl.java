/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl.scripts;

import java.util.ArrayList;
import java.util.List;

import com.lowagie.text.Table;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.api.impl.scripts.ScriptOptionImpl.FindMode;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.wizard.TableLinkerWizard;
import com.opendoorlogistics.core.utils.strings.Strings;

public class ScriptAdapterImpl extends ScriptElementImpl implements ScriptAdapter{
	private final AdapterConfig adapter;

	public ScriptAdapterImpl(ODLApi api, ScriptOptionImpl owner, AdapterConfig adapter) {
		super(api, owner, adapter);
		this.adapter = adapter;
	}

	
	@Override
	public List<ScriptAdapterTable> addSourcedTablesToAdapter(ScriptInputTables tables) {
		ArrayList<ScriptAdapterTable> ret = new ArrayList<>();
		for(int i = 0 ; i< tables.size();i++){
			ret.add(addSourcedTableToAdapter(tables.getSourceDatastoreId(i),tables.getSourceTable(i), tables.getTargetTable(i)));
		}
		return ret;
	}

	ScriptAdapterTable addAdaptedTable( AdaptedTableConfig newTableConfig) {
		adapter.getTables().add(newTableConfig);
		ScriptAdapterTableImpl ret = new ScriptAdapterTableImpl(newTableConfig);
		return ret;
	}

	@Override
	public ScriptAdapterTable addSourcedTableToAdapter(String datastoreId,ODLTableDefinition source, ODLTableDefinition destination) {
		AdaptedTableConfig table = TableLinkerWizard.createBestGuess(source, destination, TableLinkerWizard.FLAG_USE_ROWID_FOR_LOCATION_KEY);
		table.setName(destination.getName());
		table.setFromDatastore(datastoreId);
		return addAdaptedTable( table);	
	}


	@Override
	public ScriptAdapterTable addSourcelessTable(ODLTableDefinition table) {
		return addAdaptedTable(TableLinkerWizard.createBestGuess(null, table));
		// WizardUtils.createAdaptedTableConfig(table, table.getName())
	//	);
	}

	
	@Override
	public int getTableCount() {
		return adapter.getTableCount();
	}


	@Override
	public String getAdapterId() {
		return adapter.getId();
	}

	@Override
	public long getFlags() {
		return adapter.getFlags();
	}

	@Override
	public void setFlags(long flags) {
		adapter.setFlags(flags);
	}


	@Override
	public ScriptAdapterTable addEmptyTable(String tableName) {
		AdaptedTableConfig table = new AdaptedTableConfig();
		table.setName(tableName);
		return addAdaptedTable(table);
	}


	@Override
	public ScriptAdapterTable getTable(int i) {
		return new ScriptAdapterTableImpl(adapter.getTable(i));
	}


	@Override
	public ScriptAdapterType getAdapterType() {
		return adapter.getAdapterType();
	}


	@Override
	public void setAdapterType(ScriptAdapterType type) {
		adapter.setAdapterType(type);
	}



}
