package com.opendoorlogistics.core.scripts.parameters;

import static com.opendoorlogistics.core.scripts.parameters.WithKeyParametersTable.COL_EDITOR_TYPE;
import static com.opendoorlogistics.core.scripts.parameters.WithKeyParametersTable.COL_KEY;
import static com.opendoorlogistics.core.scripts.parameters.WithKeyParametersTable.COL_PROMPT_TYPE;
import static com.opendoorlogistics.core.scripts.parameters.WithKeyParametersTable.COL_VALUE;
import static com.opendoorlogistics.core.scripts.parameters.WithKeyParametersTable.COL_VALUE_TYPE;

import java.util.List;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapter.ScriptAdapterType;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.scripts.parameters.ParametersControlFactory;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.api.impl.scripts.ScriptAdapterImpl;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.utils.strings.EnumStdLookup;

public class ParametersImpl implements Parameters{
	public static final String TABLE_NAME = "Parameter";
	public static final String VALUES_TABLE_NAME = "ParameterValues";
	public static final String DS_ID = "internal";
	
	private static final BeanDatastoreMapping BEAN_NO_KEY_DS_MAPPING = BeanMapping.buildDatastore(NoKeyParametersTable.class);
	private static final BeanDatastoreMapping BEAN_WITH_KEY_DS_MAPPING = BeanMapping.buildDatastore(WithKeyParametersTable.class);
//	private static final BeanTableMapping NO_KEY_BEAN_MAPPING = BEAN_NO_KEY_DS_MAPPING.getTableMapping(0);
	private static final EnumStdLookup<ODLColumnType> COL_TYPE_LOOKUP = new EnumStdLookup<ODLColumnType>(ODLColumnType.class);
	private static final EnumStdLookup<PromptType> PROMPT_TYPE_LOOKUP = new EnumStdLookup<PromptType>(PromptType.class);

	
	private final ODLApi api;
	
	private static final ParametersControlFactory PARAMETERS_CONTROL;
	static{
		List<ParametersControlFactory> ctrls =new ODLApiImpl().loadPlugins(ParametersControlFactory.class);
		if(ctrls.size()>0){
			PARAMETERS_CONTROL = ctrls.get(0);
		}else{
			PARAMETERS_CONTROL=null;
		}

	}
	
	public ParametersImpl(ODLApi api) {
		super();
		this.api = api;
	}

	@Override
	public ODLTableDefinition tableDefinition(boolean includeKey) {
		return (includeKey ? BEAN_WITH_KEY_DS_MAPPING : BEAN_NO_KEY_DS_MAPPING).getDefinition().getTableAt(0);
	}

	@Override
	public Object getValue(ODLTableReadOnly parametersTable, String name) {
		String val = getRawColValue(name, parametersTable, COL_VALUE);
		ODLColumnType type = getColumnType(parametersTable, name);
		if(type!=null){
			return api.values().convertValue(val, type);
		}
		return null;
	}

	@Override
	public ODLColumnType getColumnType(ODLTableReadOnly parametersTable, String name) {
		return COL_TYPE_LOOKUP.get(getRawColValue(name, parametersTable, COL_VALUE_TYPE));
	}

	@Override
	public PromptType getPromptType(ODLTableReadOnly parametersTable, String name) {
		return PROMPT_TYPE_LOOKUP.get(getRawColValue(name, parametersTable, COL_PROMPT_TYPE));
	}
	
	private long getRowId(String name, ODLTableReadOnly parametersTable){
		if(name!=null){
			long [] find = parametersTable.find(COL_KEY, name);
			if(find!=null && find.length>0){
				return find[0];
			}
		}
		return -1;
	}
	
	private String getRawColValue(String name, ODLTableReadOnly parametersTable, int col) {
		long rowId = getRowId(name, parametersTable);
		if(rowId!=-1){
			return (String)parametersTable.getValueById(rowId, col);	
		}
		return null;
	}

	@Override
	public String getDSId() {
		return DS_ID;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> dsDefinition(boolean includeKey) {
		return (includeKey?BEAN_WITH_KEY_DS_MAPPING:  BEAN_NO_KEY_DS_MAPPING).getDefinition();
	}

	@Override
	public String getByRow(ODLTableReadOnly parametersTable, int row, ParamDefinitionField type) {
		return (String) parametersTable.getValueAt(row, getColIndx(type));
	}

	@Override
	public boolean exists(ODLTableReadOnly parametersTable, String key) {
		return getRowId(key, parametersTable)!=-1;
	}

	@Override
	public ParametersControlFactory getControlFactory() {
		return PARAMETERS_CONTROL;
	}

//	@Override
//	public String getParameterControlComponentId() {
//		return "com.opendoorlogistics.core.parameters.control";
//	}
	
	private int getColIndx(ParamDefinitionField type){
		switch(type){
		case KEY:
			return COL_KEY;
		case VALUE_TYPE:
			return COL_VALUE_TYPE;
	//	case DEFAULT_VALUE:
	//		return COL_DEFAULT_VALUE;
		case EDITOR_TYPE:
			return COL_EDITOR_TYPE;
		case PROMPT_TYPE:
			return COL_PROMPT_TYPE;
		case VALUE:
			return COL_VALUE;
		}
		return -1;
	}

	@Override
	public String getParametersTableName() {
		return TABLE_NAME;
	}

	@Override
	public String getByKey(ODLTableReadOnly parametersTable, String key, ParamDefinitionField type) {
		long rowId = getRowId(key, parametersTable);
		if(rowId!=-1){
			return (String)parametersTable.getValueById(rowId, getColIndx(type));
		}
		return null;
	}

	@Override
	public ODLDatastore<? extends ODLTableReadOnly> exampleDs() {
		ODLDatastoreAlterable<? extends ODLTableAlterable>  ret = api.tables().createAlterableDs();
		api.tables().copyTableDefinition(tableDefinition(true), ret);
		
		for(int i =0 ; i < 3 ; i++){
			WithKeyParametersTable o = new WithKeyParametersTable();
			o.key = "View" + (i+1);
			o.setValue("View" + (i+1));
			o.setPromptType( PromptType.ATTACH.name());
			BEAN_WITH_KEY_DS_MAPPING.getTableMapping(0).writeObjectToTable(o, ret.getTableAt(0));
		}
		
		return ret;
	}

	@Override
	public String getParamDefinitionFieldName(ParamDefinitionField type) {
		switch(type){
		case KEY:
			return Parameters.FIELDNAME_KEY;
		case VALUE_TYPE:
			return Parameters.FIELDNAME_VALUE_TYPE;
//		case DEFAULT_VALUE:
//			return Parameters.FIELDNAME_DEFAULT_VALUE;
		case EDITOR_TYPE:
			return Parameters.FIELDNAME_EDITOR_TYPE;
		case PROMPT_TYPE:
			return Parameters.FIELDNAME_PROMPT_TYPE;
		case VALUE:
			return Parameters.FIELDNAME_VALUE;
		}
		return null;
	}

	/**
	 * Called from the script editor UI...
	 * @return
	 */
	public AdapterConfig createParameterAdapter(String id){
		AdapterConfig config = new AdapterConfig(id);
		config.setAdapterType(ScriptAdapterType.PARAMETER);
		
		
		// wrap the config in the api object so we can user our high-level api code to configure it...
		ScriptAdapter adapter = new ScriptAdapterImpl(api,null,config);
		
		adapter.setName(adapter.getAdapterId());
		adapter.setAdapterType(ScriptAdapterType.PARAMETER);
		ScriptAdapterTable newParameter = adapter.addSourcelessTable(tableDefinition(false));
		//newParameter.setFormula(WithKeyParametersTable.COL_KEY, "\"name\"");
		newParameter.setFormula(Parameters.FIELDNAME_VALUE_TYPE, "\"" + ODLColumnType.STRING.name()+ "\"");
	//	newParameter.setFormula(Parameters.FIELDNAME_DEFAULT_VALUE, "\"\"");
		newParameter.setFormula(Parameters.FIELDNAME_EDITOR_TYPE, "\"\"");
		newParameter.setFormula(Parameters.FIELDNAME_PROMPT_TYPE, "\"" + PromptType.ATTACH.name() + "\"");
		
		String inputTableName = tableDefinition(false).getName();
		newParameter.setSourceTable(":=emptytable(\"" +inputTableName+ "\",1)", inputTableName);
	
		return config;
	}
}
