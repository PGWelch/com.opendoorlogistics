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
	ODLTableDefinition tableDefinition(boolean includeKeyColumn);
	
	ODLDatastore<? extends ODLTableDefinition> dsDefinition(boolean includeKeyColumn);
	
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
	String getByRow(ODLTableReadOnly parametersTable, int row,ParamDefinitionField type);
	String getByKey(ODLTableReadOnly parametersTable, String key,ParamDefinitionField type);	
	boolean exists(ODLTableReadOnly parametersTable, String key);
	
	/**
	 * Get the parameters control if available
	 * @return
	 */
	ParametersControlFactory getControlFactory();
	
	public static final String FIELDNAME_KEY ="Key";
	public static final String FIELDNAME_VALUE_TYPE ="ValueType";
	//public static final String FIELDNAME_DEFAULT_VALUE ="DefaultValue";
	public static final String FIELDNAME_EDITOR_TYPE ="EditorType";
	public static final String FIELDNAME_PROMPT_TYPE ="PromptType";
	public static final String FIELDNAME_VALUE ="Value";
	
	enum ParamDefinitionField{
		KEY,
		VALUE_TYPE,
	//	DEFAULT_VALUE,
		EDITOR_TYPE,
		PROMPT_TYPE,
		VALUE;

	}
	
	String getParamDefinitionFieldName(ParamDefinitionField type);
	
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
