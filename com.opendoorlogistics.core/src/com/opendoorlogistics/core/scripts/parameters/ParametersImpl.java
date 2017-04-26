package com.opendoorlogistics.core.scripts.parameters;

import static com.opendoorlogistics.core.scripts.parameters.beans.WithKeyParametersTable.COL_KEY;
import static com.opendoorlogistics.core.scripts.parameters.beans.WithKeyParametersTable.COL_PROMPT_TYPE;
import static com.opendoorlogistics.core.scripts.parameters.beans.WithKeyParametersTable.COL_VALUE;
import static com.opendoorlogistics.core.scripts.parameters.beans.WithKeyParametersTable.COL_VALUE_TYPE;

import java.util.List;
import java.util.UUID;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapter.ScriptAdapterType;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.scripts.parameters.ParametersControlFactory;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.api.impl.scripts.ScriptAdapterImpl;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.parameters.beans.NoKeyParameterValues;
import com.opendoorlogistics.core.scripts.parameters.beans.NoKeyParametersTable;
import com.opendoorlogistics.core.scripts.parameters.beans.WithKeyParameterValues;
import com.opendoorlogistics.core.scripts.parameters.beans.WithKeyParametersTable;
import com.opendoorlogistics.core.scripts.parameters.controls.ControlFactory;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.utils.strings.EnumStdLookup;

public class ParametersImpl implements Parameters {
	public static final String TABLE_NAME = "Parameter";
	public static final String PARAMETER_VALUES_TABLE_NAME = "ParameterValues";
	public static final String VALUES_TABLE_NAME = "ParameterValues";
	public static final String DS_ID = "internal";

	private static final BeanDatastoreMapping BEAN_NO_KEY_DS_MAPPING = BeanMapping.buildDatastore(NoKeyParametersTable.class, NoKeyParameterValues.class);
	private static final BeanDatastoreMapping BEAN_WITH_KEY_DS_MAPPING = BeanMapping.buildDatastore(WithKeyParametersTable.class, WithKeyParameterValues.class);
	// private static final BeanTableMapping NO_KEY_BEAN_MAPPING =
	// BEAN_NO_KEY_DS_MAPPING.getTableMapping(0);
	private static final EnumStdLookup<ODLColumnType> COL_TYPE_LOOKUP = new EnumStdLookup<ODLColumnType>(ODLColumnType.class);
	private static final EnumStdLookup<PromptType> PROMPT_TYPE_LOOKUP = new EnumStdLookup<PromptType>(PromptType.class);

	private final ODLApi api;

	private static volatile ParametersControlFactory PARAMETERS_CONTROL;
	
	/**
	 * Last used values cache for parameters is separate to main ODL Studio data cache as it doesn't store
	 * large data and we don't want it cleared when the user clears the rest of the cache.
	 * e.g. if the road network graph has been rebuilt the user might clear the main data cache to reload
	 * results based on this file, but they still want the UI to remember the location of the last road network file.
	 */
	private RecentlyUsedCache parameterLastUsedValuesCache = new RecentlyUsedCache("ParametersLastUsedValues", 1024*1024);
	
	private static class CachedParameterKey{
		final UUID scriptUUID;
		final String key;
		
		CachedParameterKey(UUID scriptUUID, String key) {
			this.scriptUUID = scriptUUID;
			this.key = key;
		}

		/**
		 * Estimate of size in bytes
		 */
		public int bytesSize(){
			return key.length() *2 + 5*8;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			result = prime * result + ((scriptUUID == null) ? 0 : scriptUUID.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CachedParameterKey other = (CachedParameterKey) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			if (scriptUUID == null) {
				if (other.scriptUUID != null)
					return false;
			} else if (!scriptUUID.equals(other.scriptUUID))
				return false;
			return true;
		}
		
	}

	static {
		List<ParametersControlFactory> ctrls = new ODLApiImpl().loadPlugins(ParametersControlFactory.class);
		if (ctrls.size() > 0) {
			System.out.println("Loaded parameter control factory plugin");
			PARAMETERS_CONTROL = ctrls.get(0);
		} else {
			PARAMETERS_CONTROL = new ControlFactory();
		}

	}

	public ParametersImpl(ODLApi api) {
		super();
		this.api = api;
	}

	@Override
	public ODLTableDefinition tableDefinition(boolean includeKey) {
		return getTableDefinition(includeKey, 0);
	}

	private ODLTableDefinition getTableDefinition(boolean includeKey, int indx) {
		return (includeKey ? BEAN_WITH_KEY_DS_MAPPING : BEAN_NO_KEY_DS_MAPPING).getDefinition().getTableAt(indx);
	}

	@Override
	public ODLTableDefinition valuesTableDefinition(boolean includeKeyColumn) {
		return getTableDefinition(includeKeyColumn, 1);
	}

	@Override
	public Object getValue(ODLTableReadOnly parametersTable, String name) {
		String val = getRawColValue(name, parametersTable, COL_VALUE);
		ODLColumnType type = getColumnType(parametersTable, name);
		if (type != null) {
			return api.values().convertValue(val, type);
		}
		return null;
	}

	@Override
	public ODLColumnType getColumnType(ODLTableReadOnly parametersTable, String name) {
		return COL_TYPE_LOOKUP.get(getRawColValue(name, parametersTable, COL_VALUE_TYPE));
	}

	@Override
	public PromptType getPromptType(ODLTableReadOnly parametersTable, String name) {
		return PROMPT_TYPE_LOOKUP.get(getRawColValue(name, parametersTable, COL_PROMPT_TYPE));
	}

	private long getRowId(String name, ODLTableReadOnly parametersTable) {
		if (name != null) {
			long[] find = parametersTable.find(COL_KEY, name);
			if (find != null && find.length > 0) {
				return find[0];
			}
		}
		return -1;
	}

	private String getRawColValue(String name, ODLTableReadOnly parametersTable, int col) {
		long rowId = getRowId(name, parametersTable);
		if (rowId != -1) {
			return (String) parametersTable.getValueById(rowId, col);
		}
		return null;
	}

	@Override
	public String getDSId() {
		return DS_ID;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> dsDefinition(boolean includeKey) {
		return (includeKey ? BEAN_WITH_KEY_DS_MAPPING : BEAN_NO_KEY_DS_MAPPING).getDefinition();
	}

	@Override
	public String getByRow(ODLTableReadOnly parametersTable, int row, ParamDefinitionField type) {
		return (String) parametersTable.getValueAt(row, getWithKeyColIndx(type));
	}

	@Override
	public boolean exists(ODLTableReadOnly parametersTable, String key) {
		return getRowId(key, parametersTable) != -1;
	}

	@Override
	public ParametersControlFactory getControlFactory() {
		return PARAMETERS_CONTROL;
	}

	// @Override
	// public String getParameterControlComponentId() {
	// return "com.opendoorlogistics.core.parameters.control";
	// }

	private int getWithKeyColIndx(ParamDefinitionField type) {
		switch (type) {
		case KEY:
			return COL_KEY;
		case VALUE_TYPE:
			return COL_VALUE_TYPE;
		// case DEFAULT_VALUE:
		// return COL_DEFAULT_VALUE;
		// case EDITOR_TYPE:
		// return COL_EDITOR_TYPE;
		case PROMPT_TYPE:
			return COL_PROMPT_TYPE;
		case VALUE:
			return COL_VALUE;
		}
		return -1;
	}

	@Override
	public String getParametersTableName() {
		return TABLE_NAME;
	}

	@Override
	public String getByKey(ODLTableReadOnly parametersTable, String key, ParamDefinitionField type) {
		long rowId = getRowId(key, parametersTable);
		if (rowId != -1) {
			return (String) parametersTable.getValueById(rowId, getWithKeyColIndx(type));
		}
		return null;
	}

	@Override
	public void setByKey(ODLTable table, String key, ParamDefinitionField type, String newValue) {
		long rowId = getRowId(key, table);
		if (rowId != -1) {
			table.setValueById(newValue, rowId, getWithKeyColIndx(type));
		}
	}

	@Override
	public ODLDatastore<? extends ODLTableReadOnly> exampleDs() {
		ODLDatastoreAlterable<? extends ODLTableAlterable> ret = api.tables().createAlterableDs();
		api.tables().copyTableDefinition(tableDefinition(true), ret);

		for (int i = 0; i < 3; i++) {
			WithKeyParametersTable o = new WithKeyParametersTable();
			o.setKey("View" + (i + 1));
			o.setValue("View" + (i + 1));
			o.setPromptType(PromptType.ATTACH.name());
			BEAN_WITH_KEY_DS_MAPPING.getTableMapping(0).writeObjectToTable(o, ret.getTableAt(0));
		}

		return ret;
	}

	@Override
	public String getParamDefinitionFieldName(ParamDefinitionField type) {
		switch (type) {
		case KEY:
			return Parameters.FIELDNAME_KEY;
		case VALUE_TYPE:
			return Parameters.FIELDNAME_VALUE_TYPE;
		// case DEFAULT_VALUE:
		// return Parameters.FIELDNAME_DEFAULT_VALUE;
		// case EDITOR_TYPE:
		// return Parameters.FIELDNAME_EDITOR_TYPE;
		case PROMPT_TYPE:
			return Parameters.FIELDNAME_PROMPT_TYPE;
		case VALUE:
			return Parameters.FIELDNAME_VALUE;
		}
		return null;
	}

	/**
	 * Called from the script editor UI...
	 * 
	 * @return
	 */
	public AdapterConfig createParameterAdapter(String id) {
		AdapterConfig config = new AdapterConfig(id);
		config.setAdapterType(ScriptAdapterType.PARAMETER);

		// wrap the config in the api object so we can user our high-level api
		// code to configure it...
		ScriptAdapter adapter = new ScriptAdapterImpl(api, null, config);
		adapter.setName(adapter.getAdapterId());
		adapter.setAdapterType(ScriptAdapterType.PARAMETER);

		// add parameters table
		ScriptAdapterTable newParameter = adapter.addSourcelessTable(tableDefinition(false));
		ODLTable dataTable = (ODLTable) api.tables().copyTableDefinition(tableDefinition(false), api.tables().createAlterableDs());
		dataTable.createEmptyRow(-1);
		dataTable.setValueAt(PromptType.ATTACH_POPUP.name(), 0, api.tables().findColumnIndex(dataTable, Parameters.FIELDNAME_PROMPT_TYPE));
		dataTable.setValueAt(ODLColumnType.STRING.name(), 0, api.tables().findColumnIndex(dataTable, Parameters.FIELDNAME_VALUE_TYPE));
		newParameter.setDataTable(dataTable);

		// newParameter.setFormula(Parameters.FIELDNAME_VALUE_TYPE, "\"" +
		// ODLColumnType.STRING.name()+ "\"");
		// newParameter.setFormula(Parameters.FIELDNAME_EDITOR_TYPE, "\"\"");
		// newParameter.setFormula(Parameters.FIELDNAME_PROMPT_TYPE, "\"" +
		// PromptType.ATTACH.name() + "\"");
		// String inputTableName = tableDefinition(false).getName();
		// newParameter.setSourceTable(":=emptytable(\"" +inputTableName+
		// "\",1)", inputTableName);
		newParameter.setSourceTable(ScriptConstants.SCRIPT_EMBEDDED_TABLE_DATA_DS, "");
		// add available values table...
		ScriptAdapterTable availableValues = adapter.addSourcelessTable(valuesTableDefinition(false));
		// String valuesTableName = valuesTableDefinition(false).getName();
		// availableValues.setSourceTable(":=emptytable(\"" +valuesTableName+
		// "\",0)", valuesTableName);
		availableValues.setSourceTable(ScriptConstants.SCRIPT_EMBEDDED_TABLE_DATA_DS, "");
		// availableValues.setFormula(0, "\"\"");

		return config;
	}

	@Override
	public void registerControlFactory(ParametersControlFactory factory) {
		PARAMETERS_CONTROL = factory;
	}

	@Override
	public ODLTable findTable(ODLDatastore<? extends ODLTable> ds, TableType type) {
		return api.tables().findTable(ds, type == TableType.PARAMETERS ? TABLE_NAME : PARAMETER_VALUES_TABLE_NAME);
	}

	/**
	 * Apply the string defining the visible parameters override to the input
	 * parameters table, return the filtered and ordered result as a new table.
	 * 
	 * @param overrideCommand
	 * @param parametersTable
	 * @param report
	 * @return
	 */
	public ODLTable applyVisibleOverrides(String overrideCommand, ODLTableReadOnly parametersTable, ExecutionReport report) {
		String formatMsg = "Visible parameters override should be a comma-separated line in the format [PROMPT_TYPE] parametername, e.g. ATTACH Potential, Sales, POPUP Workload";
		ODLTable ret = (ODLTable) api.tables().copyTableDefinition(parametersTable, api.tables().createAlterableDs());
		if (overrideCommand != null) {
			String[] params = overrideCommand.split(",");
			for (int i = 0; i < params.length; i++) {
				String[] words = params[i].split("\\s+");
				if (words.length == 0 || words.length > 2) {
					report.setFailed("Incorrect format in visible parameters override. " + formatMsg);
					return null;
				}

				// Get and validate prompt type if we have one
				PromptType promptType = PromptType.ATTACH_POPUP;
				int paramWordIndx = 0;
				if (words.length == 2) {
					words[0] = api.stringConventions().standardise(words[0]);
					if(words[0].length()>0){
						promptType = PROMPT_TYPE_LOOKUP.get(words[0]);
						if (promptType == null) {
							report.setFailed("Unidentified prompt type in visible parameters override: " + words[0] + ".\n" + formatMsg);
							return null;
						}						
					}
					paramWordIndx++;
				}

				String key = words[paramWordIndx];
				key = api.stringConventions().standardise(key);
				if (key.length() > 0) {
					long rowId = getRowId(key, parametersTable);
					if (rowId == -1) {
						report.setFailed("Cannot find parameter \"" + key + "\" referenced in visible parameters override. " + formatMsg);
						return null;
					}

					// copy the parameter over
					api.tables().copyRowById(parametersTable, rowId, ret);

					// but set its new prompt type
					setByKey(ret, key, ParamDefinitionField.PROMPT_TYPE, promptType.name());
				}
			}
		}

		return ret;
	}

	@Override
	public String getLastValue(String parameterName) {
		return (String)parameterLastUsedValuesCache.get(new CachedParameterKey(null, parameterName));
	}

	@Override
	public void saveLastValue( String parameterName, String value) {
		CachedParameterKey key = new CachedParameterKey(null, parameterName);
		int bytes = key.bytesSize();
		if(value!=null){
			bytes += value.length()*2;
		}
		parameterLastUsedValuesCache.put(key, value, bytes);
	}

}
