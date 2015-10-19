package com.opendoorlogistics.core.scripts.parameters.beans;

import static com.opendoorlogistics.core.scripts.parameters.beans.WithKeyParametersTable.*;

import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.scripts.parameters.Parameters.ParamDefinitionField;
import com.opendoorlogistics.api.scripts.parameters.Parameters.PromptType;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.scripts.parameters.ParametersImpl;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;

@ODLTableName(ParametersImpl.TABLE_NAME)
@SuppressWarnings("unused") 
public class NoKeyParametersTable extends BeanMappedRowImpl{
	// key, value, description, value-type, ui-type, default value
	private String valuetype = ODLColumnType.STRING.name();
//	private String defaultValue;
//	private String editorType;
	private String promptType = PromptType.ATTACH.name();
	private String value;

	public String getValuetype() {
		return valuetype;
	}
	
	@ODLColumnOrder(COL_VALUE_TYPE)
	@ODLColumnName(Parameters.FIELDNAME_VALUE_TYPE)
	public void setValuetype(String valuetype) {
		this.valuetype = valuetype;
	}
//	public String getDefaultValue() {
//		return defaultValue;
//	}
//	
//	@ODLColumnName(Parameters.FIELDNAME_DEFAULT_VALUE)
//	@ODLColumnOrder(COL_DEFAULT_VALUE)
//	@ODLNullAllowed
//	public void setDefaultValue(String defaultValue) {
//		this.defaultValue = defaultValue;
//	}
	public String getPromptType() {
		return promptType;
	}
	
	@ODLColumnName(Parameters.FIELDNAME_PROMPT_TYPE)
	@ODLColumnOrder(COL_PROMPT_TYPE)
	@ODLNullAllowed
	public void setPromptType(String promptType) {
		this.promptType = promptType;
	}
	
	public String getValue() {
		return value;
	}
	
	@ODLColumnName(Parameters.FIELDNAME_VALUE)	
	@ODLColumnOrder(COL_VALUE)
	@ODLNullAllowed
	public void setValue(String value) {
		this.value = value;
	}
//
//	public String getEditorType() {
//		return editorType;
//	}
//
//	@ODLColumnName(Parameters.FIELDNAME_EDITOR_TYPE)
//	@ODLColumnOrder(COL_EDITOR_TYPE)
//	@ODLNullAllowed
//	public void setEditorType(String editorType) {
//		this.editorType = editorType;
//	}
//	
	
	
}