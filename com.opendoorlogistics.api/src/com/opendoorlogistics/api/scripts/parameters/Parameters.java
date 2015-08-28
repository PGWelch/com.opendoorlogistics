package com.opendoorlogistics.api.scripts.parameters;

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
	
	ODLDatastore<? extends ODLTableReadOnly> exampleDs();

	//String getParameterControlComponentId();
	
	/**
	 * Get the ID of the internal datastore storing the parameters
	 * @return
	 */
	String getDSId();
	
	String getParametersTableName();
	
	Object getValue(ODLTableReadOnly parametersTable, String key);
	ODLColumnType getColumnType(ODLTableReadOnly parametersTable, String key);
	PromptType getPromptType(ODLTableReadOnly parametersTable, String key);
	//String getKey(ODLTableReadOnly parametersTable, int row);
	String getByRow(ODLTableReadOnly parametersTable, int row,FieldType type);
	String getByKey(ODLTableReadOnly parametersTable, String key,FieldType type);	
	boolean exists(ODLTableReadOnly parametersTable, String key);
	
	/**
	 * Get the parameters control if available
	 * @return
	 */
	ParametersControlFactory getControlFactory();
	
	enum FieldType{
		KEY,
		VALUE_TYPE,
		DEFAULT_VALUE,
		EDITOR_TYPE,
		PROMPT_TYPE,
		VALUE
	}
	
	enum PromptType{
		ATTACH,
		POPUP,
		ATTACH_POPUP,
	}
	
	
//	static final int COL_KEY=0;
//	static final int COL_VALUE_TYPE=COL_KEY+1;
//	static final int COL_DEFAULT_VALUE=COL_VALUE_TYPE+1;
//	static final int COL_EDITOR_TYPE=COL_DEFAULT_VALUE+1;
//	static final int COL_PROMPT_TYPE=COL_EDITOR_TYPE+1;
//	static final int COL_VALUE=COL_PROMPT_TYPE+1;
}
