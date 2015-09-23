package com.opendoorlogistics.core.scripts.parameters.beans;

import static com.opendoorlogistics.core.scripts.parameters.beans.WithKeyParametersTable.COL_VALUE;

import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.scripts.parameters.ParametersImpl;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.core.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableFlags;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;

@ODLTableName(ParametersImpl.PARAMETER_VALUES_TABLE_NAME)
@ODLTableFlags(TableFlags.FLAG_IS_OPTIONAL)
public class NoKeyParameterValues extends BeanMappedRowImpl {
	private String value;

	public String getValue() {
		return value;
	}

	/**
	 * Column order is > 0 because we want it to come AFTER the key column in the derived sub-class
	 * @param value
	 */
	@ODLColumnOrder(COL_VALUE)
	@ODLNullAllowed
	public void setValue(String value) {
		this.value = value;
	}
	
}
