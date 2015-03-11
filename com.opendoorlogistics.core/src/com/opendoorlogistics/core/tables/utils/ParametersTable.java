package com.opendoorlogistics.core.tables.utils;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.core.api.impl.TablesImpl;

public class ParametersTable {
	public static final String NAME = "Parameters"; 
	public static final String KEY = "Key"; 
	public static final String VALUE = "Value"; 
	
	private final static ODLDatastore<? extends ODLTableDefinition> dfn;

	public static ODLDatastore<? extends ODLTableDefinition> dsDefinition(){
		return dfn;
	}

	public static ODLTableDefinition tableDefinition(){
		return dfn.getTableAt(0);
	}

	static{
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds = new TablesImpl().createDefinitionDs();
		ODLTableDefinitionAlterable table = ds.createTable(NAME, -1);
		table.addColumn(-1, KEY, ODLColumnType.STRING, 0);
		table.addColumn(-1, VALUE, ODLColumnType.STRING, 0);
		dfn = ds;
	}

}
