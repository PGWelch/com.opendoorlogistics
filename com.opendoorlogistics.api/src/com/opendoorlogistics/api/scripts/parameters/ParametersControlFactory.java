package com.opendoorlogistics.api.scripts.parameters;

import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public interface ParametersControlFactory extends  net.xeoh.plugins.base.Plugin{
	JPanel createHorizontalPanel(ODLApi api,ODLTable parameters, ODLTableReadOnly values);
	//void updateHorizontalPanel(ODLApi api,ODLDatastore<? extends ODLTable> parameters);
	JPanel createModalPanel(ODLApi api,ODLTable paramTable, ODLTableReadOnly valuesTable);
	
	boolean hasModalParameters(ODLApi api,ODLTable paramTable, ODLTableReadOnly valuesTable);
//	boolean hasNonModalParameters(ODLTableReadOnly parameters);
}
