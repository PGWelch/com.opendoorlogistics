package com.opendoorlogistics.core.scripts.parameters;

import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;

@ODLTableName(ParametersImpl.TABLE_NAME)
@SuppressWarnings("unused") class WithKeyParametersTable extends NoKeyParametersTable{
	static final int COL_KEY=0;
	static final int COL_VALUE_TYPE=COL_KEY+1;
//	static final int COL_DEFAULT_VALUE=COL_VALUE_TYPE+1;
	static final int COL_EDITOR_TYPE=COL_VALUE_TYPE+1;
	static final int COL_PROMPT_TYPE=COL_EDITOR_TYPE+1;
	static final int COL_VALUE=COL_PROMPT_TYPE+1;
	
	String key;

	
	public String getKey() {
		return key;
	}
	
	@ODLColumnOrder(COL_KEY)
	@ODLColumnName(Parameters.FIELDNAME_KEY)	
	public void setKey(String key) {
		this.key = key;
	}
}