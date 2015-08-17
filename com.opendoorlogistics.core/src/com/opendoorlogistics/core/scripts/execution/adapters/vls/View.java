package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;

@ODLTableName(View.TABLE_NAME)
public class View extends BeanMappedRowImpl{
	public static final String TABLE_NAME = "Views";
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}