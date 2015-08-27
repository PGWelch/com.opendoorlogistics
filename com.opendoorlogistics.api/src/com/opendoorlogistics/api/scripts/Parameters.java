package com.opendoorlogistics.api.scripts;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

/**
 * Interface for managing script parameters
 * @author Phil
 *
 */
public interface Parameters {
	ODLTableDefinition tableDefinition();
	ODLDatastore<? extends ODLTableDefinition> dsDefinition();
	
	/**
	 * Get the ID of the internal datastore storing the parameters
	 * @return
	 */
	String getDSId();
	
	Object getValue(String key, ODLTableReadOnly parametersTable);
	ODLColumnType getType(String key, ODLTableReadOnly parametersTable);
	String getKey(ODLTableReadOnly parametersTable, int row);
	boolean exists(String key, ODLTableReadOnly parametersTable);
	
}
