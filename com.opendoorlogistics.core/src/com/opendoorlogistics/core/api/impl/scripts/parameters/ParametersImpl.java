package com.opendoorlogistics.core.api.impl.scripts.parameters;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.Parameters;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMapping;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.core.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.utils.strings.EnumStdLookup;

public class ParametersImpl implements Parameters{
	public static final String TABLE_NAME = "Parameters";
	public static final String DS_ID = "internal";
	
	private static final BeanDatastoreMapping BEAN_DS_MAPPING = BeanMapping.buildDatastore(ScriptParametersTable.class);
	private static final BeanTableMapping BEAN_MAPPING = BEAN_DS_MAPPING.getTableMapping(0);
	private static final EnumStdLookup<ODLColumnType> COL_TYPE_LOOKUP = new EnumStdLookup<ODLColumnType>(ODLColumnType.class);
	static final int KEY_COL=0;
	static final int VALUE_TYPE_COL=KEY_COL+1;
	static final int DEFAULT_VALUE_COL=VALUE_TYPE_COL+1;
	static final int EDITOR_TYPE_COL=DEFAULT_VALUE_COL+1;
	static final int PROMPT_TYPE_COL=EDITOR_TYPE_COL+1;
	static final int VALUE_COL=PROMPT_TYPE_COL+1;
	
	private final ODLApi api;
	
	
	public ParametersImpl(ODLApi api) {
		super();
		this.api = api;
	}

	@ODLTableName(TABLE_NAME)
	@SuppressWarnings("unused")
	private static class ScriptParametersTable extends BeanMappedRowImpl{
		// key, value, description, value-type, ui-type, default value
		private String key;
		private String valuetype;
		private String defaultValue;
		private String editorType;
		private String promptType;
		private String value;
		
		public String getKey() {
			return key;
		}
		
		@ODLColumnOrder(KEY_COL)
		public void setKey(String key) {
			this.key = key;
		}
		public String getValuetype() {
			return valuetype;
		}
		
		@ODLColumnOrder(VALUE_TYPE_COL)
		public void setValuetype(String valuetype) {
			this.valuetype = valuetype;
		}
		public String getDefaultValue() {
			return defaultValue;
		}
		
		@ODLColumnOrder(DEFAULT_VALUE_COL)
		@ODLNullAllowed
		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}
		public String getPromptType() {
			return promptType;
		}
		
		@ODLColumnOrder(PROMPT_TYPE_COL)
		@ODLNullAllowed
		public void setPromptType(String promptType) {
			this.promptType = promptType;
		}
		
		public String getValue() {
			return value;
		}
		
		@ODLColumnOrder(VALUE_COL)
		@ODLNullAllowed
		public void setValue(String value) {
			this.value = value;
		}

		public String getEditorType() {
			return editorType;
		}

		@ODLColumnOrder(EDITOR_TYPE_COL)
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
	public Object getValue(String name, ODLTableReadOnly parametersTable) {
		String val = getRawColValue(name, parametersTable, VALUE_COL);
		ODLColumnType type = getType(name, parametersTable);
		if(type!=null){
			return api.values().convertValue(val, type);
		}
		return null;
	}

	@Override
	public ODLColumnType getType(String name, ODLTableReadOnly parametersTable) {
		return COL_TYPE_LOOKUP.get(getRawColValue(name, parametersTable, VALUE_TYPE_COL));
	}
	
	private long getRowId(String name, ODLTableReadOnly parametersTable){
		if(name!=null){
			long [] find = parametersTable.find(KEY_COL, name);
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
	public String getKey(ODLTableReadOnly parametersTable, int row) {
		return (String) parametersTable.getValueAt(row, KEY_COL);
	}

	@Override
	public boolean exists(String key, ODLTableReadOnly parametersTable) {
		return getRowId(key, parametersTable)!=-1;
	}
}
