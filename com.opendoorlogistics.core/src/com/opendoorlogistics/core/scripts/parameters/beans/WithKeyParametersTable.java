package com.opendoorlogistics.core.scripts.parameters.beans;

import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.scripts.parameters.ParametersImpl;

@ODLTableName(ParametersImpl.TABLE_NAME)
public class WithKeyParametersTable extends NoKeyParametersTable{
	public static final int COL_KEY=0;
	public static final int COL_VALUE_TYPE=COL_KEY+1;
//	static final int COL_DEFAULT_VALUE=COL_VALUE_TYPE+1;
	//public static final int COL_EDITOR_TYPE=COL_VALUE_TYPE+1;
	public static final int COL_PROMPT_TYPE=COL_VALUE_TYPE+1;
	public static final int COL_VALUE=COL_PROMPT_TYPE+1;
	
	private String key;

	
	public String getKey() {
		return key;
	}
	
	@ODLColumnOrder(COL_KEY)
	@ODLColumnName(Parameters.FIELDNAME_KEY)	
	public void setKey(String key) {
		this.key = key;
	}
}