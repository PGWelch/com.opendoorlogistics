package com.opendoorlogistics.components.timecontrol;

import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;

public class TimeControlComponent implements ODLComponent {

	@Override
	public String getId() {
		return "com.opendoorlogistics.components.timecontrol";
	}

	@Override
	public String getName() {
		return "Time control";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds = api.tables().createAlterableDs();
		ODLTableDefinitionAlterable table = ds.createTable("Clock", -1);
		table.addColumn(-1, "Time", ODLColumnType.TIME, 0);
		return ds;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		return null;
	}

	@Override
	public void execute(ComponentExecutionApi api, int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDs,
			ODLDatastoreAlterable<? extends ODLTableAlterable> outputDs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI api, int mode, Serializable config, boolean isFixedIO) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return ODLComponent.FLAG_OUTPUT_WINDOWS_ALWAYS_SYNCHRONISED;
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		// TODO Auto-generated method stub
		
	}

}
