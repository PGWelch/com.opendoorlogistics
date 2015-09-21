package com.opendoorlogistics.studio.components.map;

import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder.BuildScriptCallback;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.ExtraFields;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.Layer;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.Style;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.VLSBuilder;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.View;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.utils.ui.Icons;

/**
 * A view-layer-style component is a dummy component which launches a map control fed by a view-layer-style adapter
 * @author Phil
 *
 */
public class ViewLayerStyleComponent implements ODLComponent {

	@Override
	public String getId() {
		return "com.opendoorlogistics.studio.components.map.view-layer-style";
	}

	@Override
	public String getName() {
		return "View, layer and style map";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		return VLSBuilder.getVLSTableDefinitions();
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		return null;
	}

	@Override
	public void execute(ComponentExecutionApi api, int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDs,
			ODLDatastoreAlterable<? extends ODLTableAlterable> outputDs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return null;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI api, int mode, Serializable config, boolean isFixedIO) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return 0;
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return Icons.loadFromStandardPath("view-layer-style.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return false;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		String name = "View, layer and style map";
		templatesApi.registerTemplate("View-layer-style map", name, "Create a map which is defined by view-layer-style tables",getIODsDefinition(null,null), new BuildScriptCallback() {

			@Override
			public void buildScript(ScriptOption builder) {
				ScriptInputTables inputTables = builder.getInputTables();
				
				// add only the vls table to the adapter
				ScriptAdapter mapAdapter = builder.addDataAdapter("VLSInput");
				mapAdapter.setAdapterType(ScriptAdapter.ScriptAdapterType.VLS);
				for(int i =0 ; i< inputTables.size() ; i++){
					ODLTableDefinition src = inputTables.getSourceTable(i);
					ODLTableDefinition dest = inputTables.getTargetTable(i);
					String dsid = inputTables.getSourceDatastoreId(i);
					if(!Strings.equalsStd(dest.getName(), View.TABLE_NAME)
					&& !Strings.equalsStd(dest.getName(), Layer.TABLE_NAME)
					&& !Strings.equalsStd(dest.getName(), Style.TABLE_NAME)
					){
						continue;
					}
					
					
					
					if(src!=null){
						mapAdapter.addSourcedTableToAdapter(dsid, src, dest);
					}else{
						mapAdapter.addSourcelessTable(dest);
					}
				}
				
				// add the extra fields afterwards as we want this to use embedded data...
				ODLTableDefinition extraFieldsDfn = TableUtils.findTable(VLSBuilder.getVLSTableDefinitions(), ExtraFields.TABLE_NAME);
				ScriptAdapterTable efAdapter = mapAdapter.addSourcelessTable(extraFieldsDfn);
				efAdapter.setSourceTable(ScriptConstants.SCRIPT_EMBEDDED_TABLE_DATA_DS, "");
				
				// now add the instruction which launches the map component, not this component
				builder.addInstruction(mapAdapter.getAdapterId(), AbstractMapViewerComponent.COMPONENT_ID, ODLComponent.MODE_DEFAULT,new MapConfig());
				
			}
			
		});
	}

}
