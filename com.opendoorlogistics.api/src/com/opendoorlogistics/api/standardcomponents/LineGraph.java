package com.opendoorlogistics.api.standardcomponents;

import java.io.Serializable;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

public interface LineGraph extends ODLComponent{
	void setTitle(String title, Serializable config);
	void setXLabel(String title, Serializable config);
	void setYLabel(String title, Serializable config);
	ODLTableDefinition getInputTableDefinition(ODLApi api);
	
	
	enum LGColumn{
		Key,
		X,
		Y
	}
	
	String getColumnName(LGColumn col);
}
