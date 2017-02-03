package com.opendoorlogistics.api.scripts.parameters;

import java.util.UUID;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

/**
 * Interface for managing script parameters
 * @author Phil
 *
 */
public interface Parameters {
	ODLTableDefinition tableDefinition(boolean includeKeyColumn);
	ODLTableDefinition valuesTableDefinition(boolean includeKeyColumn);
	
	ODLDatastore<? extends ODLTableDefinition> dsDefinition(boolean includeKeyColumn);
	
	ODLDatastore<? extends ODLTableReadOnly> exampleDs();
	
	enum TableType{
		PARAMETERS,
		PARAMETER_VALUES,
	}
	ODLTable findTable(ODLDatastore<? extends ODLTable> ds,TableType type );

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
	void setByKey(ODLTable table, String key,ParamDefinitionField type, String newValue);
	boolean exists(ODLTableReadOnly parametersTable, String key);
	
	/**
	 * Get the parameters control if available
	 * @return
	 */
	ParametersControlFactory getControlFactory();
	
	/**
	 * Registers the app-global parameters control factory
	 * @param factory
	 */
	void registerControlFactory(ParametersControlFactory factory);
	
	public static final String FIELDNAME_KEY ="Key";
	public static final String FIELDNAME_VALUE_TYPE ="ValueType";
	//public static final String FIELDNAME_DEFAULT_VALUE ="DefaultValue";
//	public static final String FIELDNAME_EDITOR_TYPE ="EditorType";
	public static final String FIELDNAME_PROMPT_TYPE ="PromptType";
	public static final String FIELDNAME_VALUE ="Value";
	
	enum ParamDefinitionField{
		KEY,
		VALUE_TYPE,
	//	DEFAULT_VALUE,
//		EDITOR_TYPE,
		PROMPT_TYPE,
		VALUE;

	}
	
	String getParamDefinitionFieldName(ParamDefinitionField type);
	
	enum PromptType{
		ATTACH,
		POPUP,
		ATTACH_POPUP,
		HIDDEN,

	}
	
	/**
	 * Get and save last value (in-memory only, lifetime of the spreadsheet).
	 * Beware of parameter names clashing in different scripts!!!!
	 * @param scriptUUID
	 * @param parameterName
	 * @return
	 */
	public String getLastValue( String parameterName);
	
	/**
	 * Get and save last value (in-memory only, lifetime of the spreadsheet)
	 * Beware of parameter names clashing in different scripts!!!!
	 * @param parameterName
	 * @return
	 */
	public void saveLastValue( String parameterName, String value);
	
//	static final int COL_KEY=0;
//	static final int COL_VALUE_TYPE=COL_KEY+1;
//	static final int COL_DEFAULT_VALUE=COL_VALUE_TYPE+1;
//	static final int COL_EDITOR_TYPE=COL_DEFAULT_VALUE+1;
//	static final int COL_PROMPT_TYPE=COL_EDITOR_TYPE+1;
//	static final int COL_VALUE=COL_PROMPT_TYPE+1;
}
