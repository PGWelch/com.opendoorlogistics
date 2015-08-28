package com.opendoorlogistics.core.scripts.parameters;

import java.util.List;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.scripts.parameters.ParametersControlFactory;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMapping;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.core.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.utils.strings.EnumStdLookup;
import com.sun.org.apache.bcel.internal.generic.RET;

public class ParametersImpl implements Parameters{
	public static final String TABLE_NAME = "Parameters";
	public static final String DS_ID = "internal";
	
	private static final BeanDatastoreMapping BEAN_DS_MAPPING = BeanMapping.buildDatastore(ScriptParametersTable.class);
	private static final BeanTableMapping BEAN_MAPPING = BEAN_DS_MAPPING.getTableMapping(0);
	private static final EnumStdLookup<ODLColumnType> COL_TYPE_LOOKUP = new EnumStdLookup<ODLColumnType>(ODLColumnType.class);
	private static final EnumStdLookup<PromptType> PROMPT_TYPE_LOOKUP = new EnumStdLookup<PromptType>(PromptType.class);
	static final int COL_KEY=0;
	static final int COL_VALUE_TYPE=COL_KEY+1;
	static final int COL_DEFAULT_VALUE=COL_VALUE_TYPE+1;
	static final int COL_EDITOR_TYPE=COL_DEFAULT_VALUE+1;
	static final int COL_PROMPT_TYPE=COL_EDITOR_TYPE+1;
	static final int COL_VALUE=COL_PROMPT_TYPE+1;
	
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

	@ODLTableName(TABLE_NAME)
	@SuppressWarnings("unused")
	public static class ScriptParametersTable extends BeanMappedRowImpl{
		// key, value, description, value-type, ui-type, default value
		private String key;
		private String valuetype = ODLColumnType.STRING.name();
		private String defaultValue;
		private String editorType;
		private String promptType = PromptType.ATTACH.name();
		private String value;
		
		public String getKey() {
			return key;
		}
		
		@ODLColumnOrder(COL_KEY)
		public void setKey(String key) {
			this.key = key;
		}
		public String getValuetype() {
			return valuetype;
		}
		
		@ODLColumnOrder(COL_VALUE_TYPE)
		public void setValuetype(String valuetype) {
			this.valuetype = valuetype;
		}
		public String getDefaultValue() {
			return defaultValue;
		}
		
		@ODLColumnOrder(COL_DEFAULT_VALUE)
		@ODLNullAllowed
		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}
		public String getPromptType() {
			return promptType;
		}
		
		@ODLColumnOrder(COL_PROMPT_TYPE)
		@ODLNullAllowed
		public void setPromptType(String promptType) {
			this.promptType = promptType;
		}
		
		public String getValue() {
			return value;
		}
		
		@ODLColumnOrder(COL_VALUE)
		@ODLNullAllowed
		public void setValue(String value) {
			this.value = value;
		}

		public String getEditorType() {
			return editorType;
		}

		@ODLColumnOrder(COL_EDITOR_TYPE)
		@ODLNullAllowed
		public void setEditorType(String editorType) {
			this.editorType = editorType;
		}
		
		
		
	}

	@Override
	public ODLTableDefinition tableDefinition() {
		return BEAN_MAPPING.getTableDefinition();
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
	public ODLDatastore<? extends ODLTableDefinition> dsDefinition() {
		return BEAN_DS_MAPPING.getDefinition();
	}

	@Override
	public String getByRow(ODLTableReadOnly parametersTable, int row, FieldType type) {
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
	
	private int getColIndx(FieldType type){
		switch(type){
		case KEY:
			return COL_KEY;
		case VALUE_TYPE:
			return COL_VALUE_TYPE;
		case DEFAULT_VALUE:
			return COL_DEFAULT_VALUE;
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
	public String getByKey(ODLTableReadOnly parametersTable, String key, FieldType type) {
		long rowId = getRowId(key, parametersTable);
		if(rowId!=-1){
			return (String)parametersTable.getValueById(rowId, getColIndx(type));
		}
		return null;
	}

	@Override
	public ODLDatastore<? extends ODLTableReadOnly> exampleDs() {
		ODLDatastoreAlterable<? extends ODLTableAlterable>  ret = api.tables().createAlterableDs();
		api.tables().copyTableDefinition(tableDefinition(), ret);
		
		for(int i =0 ; i < 3 ; i++){
			ScriptParametersTable o = new ScriptParametersTable();
			o.key = "View" + (i+1);
			o.value = "View" + (i+1);
			o.promptType = PromptType.ATTACH.name();
			BEAN_MAPPING.writeObjectToTable(o, ret.getTableAt(0));
		}
		
		return ret;
	}
}
