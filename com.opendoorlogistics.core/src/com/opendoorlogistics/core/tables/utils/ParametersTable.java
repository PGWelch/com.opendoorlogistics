package com.opendoorlogistics.core.tables.utils;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.api.impl.TablesImpl;

public class ParametersTable {
	
	private final static ODLDatastore<? extends ODLTableDefinition> dfn;

	public static ODLDatastore<? extends ODLTableDefinition> dsDefinition(){
		return dfn;
	}

	public static ODLTableDefinition tableDefinition(){
		return dfn.getTableAt(0);
	}

	static{
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds =new ODLApiImpl().tables().createDefinitionDs();
		ODLTableDefinitionAlterable table = ds.createTable(PredefinedTags.PARAMETERS_TABLE_NAME, -1);
		table.addColumn(-1, PredefinedTags.PARAMETERS_TABLE_KEY, ODLColumnType.STRING, 0);
		table.addColumn(-1, PredefinedTags.PARAMETERS_TABLE_VALUE, ODLColumnType.STRING, 0);
		dfn = ds;
	}

}
