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
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * Query acting against a single datastore
 * @author Phil
 *
 */
public abstract class QueryAvailableDataImpl implements QueryAvailableData{
	protected abstract ODLDatastore<? extends ODLTableDefinition> getDs();
	protected abstract String getDsName();
	
	@Override
	public String[] queryAvailableFields(String datastore, String tablename) {
		if (getDs()!= null && Strings.equalsStd(getDsName(), datastore)) {
			ODLTableDefinition table = TableUtils.findTable(getDs(), tablename, true);
			if (table != null) {
				return TableUtils.getColumnNames(table);
			}
		}
		return new String[] {};
	}

	@Override
	public String[] queryAvailableTables(String datastore) {
		if (getDs() != null && Strings.equalsStd(getDsName(), datastore)) {
			return TableUtils.getAlphabeticallySortedTableNames(getDs());
		}
		return new String[] {};
	}

	@Override
	public String[] queryAvailableDatastores() {
		if(getDsName()!=null){
			return new String[] { getDsName()};			
		}
		return new String[]{};
	}

	@Override
	public String[] queryAvailableFormula(ODLColumnType columnType) {
		return new String[]{};
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getDatastoreDefinition(String datastore){
		if(Strings.equalsStd(getDsName(), datastore) ){
			return getDs();
		}		
		return null;
	}

	@Override	
	public ODLTableDefinition getTableDefinition(String datastore, String tablename){
		ODLDatastore<? extends ODLTableDefinition> ds = getDatastoreDefinition(datastore);
		if(ds!=null){
			return TableUtils.findTable(ds, tablename);
		}
		return null;
	}

}
