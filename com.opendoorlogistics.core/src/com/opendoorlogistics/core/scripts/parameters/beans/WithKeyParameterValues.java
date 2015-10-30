package com.opendoorlogistics.core.scripts.parameters.beans;

import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.scripts.parameters.ParametersImpl;

import static com.opendoorlogistics.core.scripts.parameters.beans.WithKeyParametersTable.COL_KEY;;

@ODLTableName(ParametersImpl.PARAMETER_VALUES_TABLE_NAME)
public class WithKeyParameterValues extends NoKeyParameterValues{
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
