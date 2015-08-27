package com.opendoorlogistics.core.api.impl.scripts.parameters;

import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.Parameters;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder.BuildScriptCallback;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public class ScriptParameterCreatorComponent implements ODLComponent{

	@Override
	public String getId() {
		return "com.opendoorlogistics.core.api.impl.scripts.parameters.creator";
	}

	@Override
	public String getName() {
		return "Script parameter creator";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		Tables tables = api.tables();
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = tables.createAlterableDs();
		ODLTableDefinition dfn = api.scripts().parameters().tableDefinition();
		ODLTableDefinition newParameters = tables.copyTableDefinition(dfn, ret);
		ret.setTableName(newParameters.getImmutableId(), "New-parameter");
		
		ODLTableDefinition paramTable = tables.copyTableDefinition(dfn, ret);
		ret.setTableName(paramTable.getImmutableId(), "Write-to-table");
		
		return ret;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		return null;
	}

	@Override
	public void execute(ComponentExecutionApi api, int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDs,
			ODLDatastoreAlterable<? extends ODLTableAlterable> outputDs) {
		
		// copy the new parameters over to the parameters table
		ODLTableReadOnly from= ioDs.getTableAt(0);
		ODLTable to = ioDs.getTableAt(1);
		Tables tables = api.getApi().tables();
		Parameters parameters = api.getApi().scripts().parameters();
		for(int i =0 ; i < from.getRowCount() ; i++){
			String key = parameters.getKey(from, i);
			if(key!=null){
				// don't replace if already present (initial parameters are used for refreshing reports etc)
				if(!parameters.exists(key, to)){
					tables.copyRow(from, i, to);					
				}
			}
		}
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return new ImageIcon(ScriptParameterCreatorComponent.class.getResource("/resources/icons/parameter.png"));
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate("Script parameter", "Script parameter", "Define a script parameter, accessible in the script using the sp(name) function.", new ScriptParameterCreatorComponent().getIODsDefinition(templatesApi.getApi(), null), new BuildScriptCallback() {
			
			@Override
			public void buildScript(ScriptOption builder) {
				ODLApi api = templatesApi.getApi();
				Parameters parameters = api.scripts().parameters();
				ODLDatastore<? extends ODLTableDefinition> inputDfn = getIODsDefinition(api, null);
				
				// create adapter
				ScriptAdapter adapter = builder.addDataAdapter("Parameter");
				adapter.setName(adapter.getAdapterId());
				ScriptAdapterTable newParameter = adapter.addSourcelessTable(inputDfn.getTableAt(0));
				newParameter.setFormula(ParametersImpl.KEY_COL, "\"name\"");
				newParameter.setFormula(ParametersImpl.VALUE_TYPE_COL, "\"" + ODLColumnType.STRING.name()+ "\"");
				newParameter.setFormula(ParametersImpl.DEFAULT_VALUE_COL, "");
				newParameter.setFormula(ParametersImpl.EDITOR_TYPE_COL, "");
				newParameter.setFormula(ParametersImpl.PROMPT_TYPE_COL, "");
				
				String inputTableName = inputDfn.getTableAt(0).getName();
				newParameter.setSourceTable(":=emptytable(\"" +inputTableName+ "\",1)", inputTableName);
				
				ScriptAdapterTable writeTo= adapter.addEmptyTable(inputDfn.getTableAt(1).getName());
				writeTo.setFetchSourceField(true);
				writeTo.setSourceTable(parameters.getDSId(), parameters.tableDefinition().getName());
				
				// add instruction
				builder.addInstruction(adapter.getAdapterId(), getId(), ODLComponent.MODE_DEFAULT);
			}
		});
	}

}
