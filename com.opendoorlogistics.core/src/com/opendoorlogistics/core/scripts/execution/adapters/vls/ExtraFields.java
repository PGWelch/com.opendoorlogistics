package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableFlags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;

@ODLTableName(ExtraFields.TABLE_NAME)
@ODLTableFlags(TableFlags.FLAG_IS_OPTIONAL)
public class ExtraFields extends BeanMappedRowImpl{
	public static final String TABLE_NAME = "ExtraFieldDefinitions";
	private String name;
	private String type;
	
	public String getName() {
		return name;
	}
	
	@ODLColumnOrder(0)
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	
	@ODLColumnOrder(1)
	public void setType(String type) {
		this.type = type;
	}
	
	
}
