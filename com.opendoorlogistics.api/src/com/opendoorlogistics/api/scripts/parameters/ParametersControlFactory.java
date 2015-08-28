package com.opendoorlogistics.api.scripts.parameters;

import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;

public interface ParametersControlFactory extends  net.xeoh.plugins.base.Plugin{
	JPanel createHorizontalPanel(ODLApi api,ODLDatastore<? extends ODLTable> parameters);
	void updateHorizontalPanel(ODLApi api,ODLDatastore<? extends ODLTable> parameters);
//	boolean hasNonModalParameters(ODLTableReadOnly parameters);
}
