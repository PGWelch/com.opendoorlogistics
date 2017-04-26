/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution;

import java.awt.Dimension;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.Func;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ModalDialogResult;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.ODLComponentProvider;
import com.opendoorlogistics.api.distances.DistancesConfiguration;
import com.opendoorlogistics.api.distances.ODLCostMatrix;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.scripts.ScriptAdapter.ScriptAdapterType;
import com.opendoorlogistics.api.scripts.ScriptOption.OutputType;
import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.scripts.parameters.Parameters.ParamDefinitionField;
import com.opendoorlogistics.api.scripts.parameters.Parameters.TableType;
import com.opendoorlogistics.api.scripts.parameters.ParametersControlFactory;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable.ODLDatastoreAlterableFactory;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.components.UpdateQueryComponent;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.TargetIODsInterpreter;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.ComponentConfig;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.OutputConfig;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutionBlackboardImpl.SavedDatastore;
import com.opendoorlogistics.core.scripts.execution.adapters.AdapterBuilder;
import com.opendoorlogistics.core.scripts.execution.adapters.AdapterBuilderUtils;
import com.opendoorlogistics.core.scripts.execution.adapters.BuiltAdapters;
import com.opendoorlogistics.core.scripts.execution.dependencyinjection.AbstractDependencyInjector;
import com.opendoorlogistics.core.scripts.execution.dependencyinjection.DependencyInjector;
import com.opendoorlogistics.core.scripts.formulae.TableParameters;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.scripts.parameters.ParametersImpl;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.ODLRowReadOnly;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator.AdapterMapping;
import com.opendoorlogistics.core.tables.decorators.datastores.RowFilterDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependencies;
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependenciesRecorder;
import com.opendoorlogistics.core.tables.decorators.datastores.undoredo.UndoRedoDecorator;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.UpdateTimer;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class ScriptExecutor {
	private final ODLDatastoreAlterableFactory<ODLTableAlterable> datastoreFactory;
	private final DependencyInjector internalExecutionApi;
	private final ODLComponentProvider components;
	private final ODLApi api;
	private boolean compileOnly = false;
	private ODLTableReadOnly initialParametersTable;

	private ScriptExecutor(ODLApi api, ODLDatastoreAlterableFactory<ODLTableAlterable> datastoreFactory, ODLComponentProvider components, DependencyInjector guiFascade, boolean compileOnly) {
		this.api = api;
		this.datastoreFactory = datastoreFactory;
		this.components = components;
		this.internalExecutionApi = guiFascade != null ? guiFascade : new AbstractDependencyInjector(api);
		this.compileOnly = compileOnly;
	}

	public ScriptExecutor(ODLApi api, boolean compileOnly, DependencyInjector reporter) {
		this(api, ODLDatastoreImpl.alterableFactory, ODLGlobalComponents.getProvider(), reporter, compileOnly);
	}

	private void fillParametersTable(Script script, ScriptExecutionBlackboardImpl result) {
		Parameters parameters = api.scripts().parameters();
		Tables tables = api.tables();
		ODLTable paramTable = parameters.findTable(findDatastoreOrAdapter(parameters.getDSId(), result), TableType.PARAMETERS);
		ODLTable valuesTable = parameters.findTable(findDatastoreOrAdapter(parameters.getDSId(), result), TableType.PARAMETER_VALUES);

		HashMap<ParamDefinitionField, Integer> withKeyCols = new HashMap<>();
		HashMap<ParamDefinitionField, Integer> noKeyCols = new HashMap<>();
		for (ParamDefinitionField type : ParamDefinitionField.values()) {
			noKeyCols.put(type, tables.findColumnIndex(parameters.tableDefinition(false), parameters.getParamDefinitionFieldName(type)));
			withKeyCols.put(type, tables.findColumnIndex(paramTable, parameters.getParamDefinitionFieldName(type)));
		}

		// get list of all existing parameters
		Set<String> preexistingIds = api.stringConventions().createStandardisedSet();
		int nr = paramTable.getRowCount();
		for (int i = 0; i < nr; i++) {
			String id = api.scripts().parameters().getByRow(paramTable, i, ParamDefinitionField.KEY);
			if (id != null) {
				preexistingIds.add(id);
			}
		}

		for (AdapterConfig config : script.getAdapters()) {
			if (config != null && config.getAdapterType() == ScriptAdapterType.PARAMETER) {

				// build the parameter adapter (always do this - something else
				// in the script could still reference it)
				ODLDatastore<? extends ODLTable> ds = findDatastoreOrAdapter(config.getId(), result);
				if (result.isFailed()) {
					return;
				}

				// Copy over the available values
				ODLTableReadOnly newValuesTable = parameters.findTable(ds, TableType.PARAMETER_VALUES);
				if (newValuesTable != null) {
					nr = newValuesTable.getRowCount();
					if (newValuesTable.getColumnCount() != 1) {
						throw new RuntimeException();
					}
					for (int i = 0; i < nr; i++) {
						int newRow = valuesTable.createEmptyRow(-1);
						valuesTable.setValueAt(config.getId(), newRow, 0);
						valuesTable.setValueAt(newValuesTable.getValueAt(i, 0), newRow, 1);
					}
				}

				// Don't do anything else if the parameter already exists, as
				// we're refreshing a report and want to use
				// the current version
				if (preexistingIds.contains(config.getId())) {
					continue;
				}

				// standardise / validate its fields - this will match to the
				// keyless parameter table as we take the key from
				// the config id
				ds = new TargetIODsInterpreter(api).buildScriptExecutionAdapter(ds, parameters.dsDefinition(false), result);
				if (result.isFailed()) {
					return;
				}

				// get the parameters table
				ODLTableReadOnly newParamTable = parameters.findTable(ds, TableType.PARAMETERS);
				nr = newParamTable.getRowCount();
				if (nr != 1) {
					result.setFailed("Found parameters table with either zero or more than one row: " + config.getId());
					return;
				}

				// now copy over, adding the dsid as key
				int newRow = paramTable.createEmptyRow(-1);
				for (ParamDefinitionField type : ParamDefinitionField.values()) {
					int fromCol = noKeyCols.get(type);
					int toCol = withKeyCols.get(type);
					Object val = type == ParamDefinitionField.KEY ? config.getId() : newParamTable.getValueAt(0, fromCol);
					
					if(type== ParamDefinitionField.VALUE){
						if(Strings.isEmptyWhenStandardised(val)){
							// We're opening the script new again and don't have a default value.
							// Check app properties for last value of this param type?
							val = parameters.getLastValue( config.getId());							
						}
					}
					
					paramTable.setValueAt(val, newRow, toCol);
				}

			}
		}

	}

	/**
	 * Execute the script. This method will not throw exceptions - failure is
	 * reported in the return object
	 * 
	 * @param script
	 * @param externalDS
	 * @return
	 */
	public ExecutionReport execute(Script script, ODLDatastoreAlterable<? extends ODLTableAlterable> externalDS) {
		
		// deep copy and add any global formulae to all table adapters...
		if(script.getUserFormulae()!=null && script.getUserFormulae().size()>0){
			script = ScriptIO.instance().deepCopy(script);
			for (AdapterConfig config : script.getAdapters()) {
				for(AdaptedTableConfig table: config.getTables()){
					if(table.getUserFormulae()==null){
						table.setUserFormulae(script.getUserFormulae());
					}else{
						table.getUserFormulae().addAll(script.getUserFormulae());
					}
				}
			}			
		}
		
		ScriptExecutionBlackboardImpl bb = new ScriptExecutionBlackboardImpl(compileOnly);
		
		internalExecutionApi.postStatusMessage("Starting script execution...");

		try {
			// execute main option
			buildDatastores(externalDS, script, bb);

			if (!bb.isFailed()) {
				initialiseAdapterRecords(script, bb);
			}

			if (!bb.isFailed()) {
				fillParametersTable(script, bb);
			}

			if (!bb.isFailed()) {
				if (!userPrompts(script, bb)) {
					return bb;
				}
			}

			if (!bb.isFailed()) {
				executeAllInstructions(script, bb);
			}

			if (!bb.isFailed()) {
				executeOutputs(script, bb);
			}

			checkForUserCancellation(bb);
		} catch (Throwable e) {
			bb.setFailed(e);
		}

		internalExecutionApi.postStatusMessage("Finished script execution...");

		return bb;
	}

	private boolean userPrompts(Script script, ScriptExecutionBlackboardImpl result) {
		ParametersImpl parameters = (ParametersImpl) api.scripts().parameters();
		ParametersControlFactory factory = parameters.getControlFactory();

		// only prompt on the first run and only if we have a control factory
		if (factory != null && internalExecutionApi != null && initialParametersTable == null) {
			ODLTable paramTable = parameters.findTable(findDatastoreOrAdapter(parameters.getDSId(), result), TableType.PARAMETERS);
			ODLTable valuesTable = parameters.findTable(findDatastoreOrAdapter(parameters.getDSId(), result), TableType.PARAMETER_VALUES);
			// if(factory.hasModalParameters(api, paramTable, valuesTable)){

			// test for overriding the visible parameters
			ODLTable overrideTable = null;
			if (script.isOverrideVisibleParameters()) {
				overrideTable = parameters.applyVisibleOverrides(script.getVisibleParametersOverride(), paramTable, result);
				if (result.isFailed()) {
					return true;
				}
			}

			// do the prompt
			ODLTable tableToUse = overrideTable != null ? overrideTable : paramTable;
			if (factory.hasModalParameters(api, tableToUse, valuesTable)) {
				JPanel panel = factory.createModalPanel(api, tableToUse, valuesTable);
				ModalDialogResult mdr = internalExecutionApi.showModalPanel(panel, "Select parameter(s)", new Dimension(400, 0),ModalDialogResult.OK, ModalDialogResult.CANCEL);
				if (mdr == ModalDialogResult.CANCEL) {
					return false;
				}
			}

			// save parameters last values
			for (int row = 0; row < tableToUse.getRowCount(); row++) {
				String key = parameters.getByRow(tableToUse, row, ParamDefinitionField.KEY);
				if (key != null) {
					String value =  parameters.getByRow(tableToUse, row, ParamDefinitionField.VALUE);
					parameters.saveLastValue( key, value);
				}
			}
			
			// if we used the override table to restrict visible parameters then copy the values over into the main table
			if (overrideTable != null) {
				for (int row = 0; row < overrideTable.getRowCount(); row++) {
					String key = parameters.getByRow(overrideTable, row, ParamDefinitionField.KEY);
					if (key != null) {
						parameters.setByKey(paramTable, key, ParamDefinitionField.VALUE, parameters.getByRow(overrideTable, row, ParamDefinitionField.VALUE));
					}
				}
			}
			// }

		}
		return true;
	}

	private boolean checkForUserCancellation(ExecutionReport ret) {
		if (internalExecutionApi.isCancelled()) {
			ret.setFailed("User stopped the script executing.");
			return false;
		}
		return true;
	}

	// private static String formatException(Throwable throwable) {
	// String ret = System.lineSeparator() +
	// Strings.getTabIndented(Exceptions.stackTraceToString(throwable), 1) +
	// System.lineSeparator();
	// return ret;
	// }

	private void executeAllInstructions(Script script, ScriptExecutionBlackboardImpl result) {
		// execute all instructions
		for (int i = 0; i < script.getInstructions().size(); i++) {
			// check for cancelled
			checkForUserCancellation(result);
			if (result.isFailed()) {
				return;
			}

			InstructionConfig instruction = script.getInstructions().get(i);
			try {
				// check if we're doing an update query... this has special
				// logic
				ODLComponent component = getComponent(instruction, result);
				if (result.isFailed()) {
					break;
				}
				if (UpdateQueryComponent.class.isInstance(component)) {
					executeUpdateQueryInstruction(script, instruction, result);

				} else {
					executeBatchedInstruction(script, instruction, result);
				}

			} catch (Throwable e) {
				result.setFailed(e);
				result.setFailed("Exception occurred executing instruction.");
			}

			if (result.isFailed() && script.getInstructions().size() > 1) {
				result.log("Failed on instruction line " + (i + 1) + ".");
				break;
			}
		}
	}

	private void executeUpdateQueryInstruction(Script root, InstructionConfig instruction, ScriptExecutionBlackboardImpl result) {
		// check for buildable adapter
		AdapterConfig adapterConfig = result.getAdapterConfig(instruction.getDatastore());
		if (adapterConfig == null) {
			result.setFailed("Could not find adapter config for update query: " + instruction.getDatastore());
		}

		// Loop over every input table
		for (int i = 0; i < adapterConfig.getTableCount() && !result.isFailed(); i++) {

			// convert to an update adapter
			AdaptedTableConfig tableConfig = null;
			if (!result.isFailed()) {
				tableConfig = AdapterBuilderUtils.convertToUpdateQueryTable(adapterConfig.getTable(i), result);
				if (tableConfig == null) {
					result.setFailed();
				}
			}

			// create dummy adapter config to store it...
			AdapterConfig tmpAdapterConfig = null;
			if (!result.isFailed()) {
				tmpAdapterConfig = new AdapterConfig(adapterConfig.getId());
				tmpAdapterConfig.getTables().add(tableConfig);
			}

			// build it
			ODLDatastore<? extends ODLTable> adapter = null;
			if (!result.isFailed()) {
				AdapterBuilder builder = new AdapterBuilder(tmpAdapterConfig, new StandardisedStringSet(false), result, internalExecutionApi, new BuiltAdapters());
				adapter = builder.build();
				if (adapter == null) {
					result.setFailed();
				}
			}

			// run component
			if (!result.isFailed()) {
				try {
					executeSingleInstruction(root, instruction, adapter, null, result);
				} catch (Throwable e) {
					result.setFailed(e);
					result.setFailed("Exception occurred executing update query.");
				}
			}

			if (internalExecutionApi != null) {
				internalExecutionApi.postStatusMessage("Updated " + (i + 1) + "/" + adapterConfig.getTableCount() + " tables");
			}
		}

		if (result.isFailed()) {
			result.log("Failed to execute update query.");
		}
	}

	private void executeOutputs(Option script, ScriptExecutionBlackboardImpl result) {

		// process outputs
		for (int i = 0; i < script.getOutputs().size(); i++) {
			// check for cancelled
			if (internalExecutionApi.isCancelled()) {
				result.setFailed("Execution was stopped");
				return;
			}

			OutputConfig output = script.getOutputs().get(i);
			try {
				executeOutput(output, result);

			} catch (Throwable e) {
				result.setFailed(e);
				result.setFailed("Exception occurred.");
			}

			if (result.isFailed()) {
				result.log("Failed on output line " + (i + 1) + ".");
				return;
			}
		}
	}

	private void initialiseAdapterRecords(Option script, ScriptExecutionBlackboardImpl result) {

		// get lookup of all named adapters to ensure they're unique
		for (AdapterConfig config : script.getAdapters()) {
			if (Strings.isEmpty(config.getId())) {
				result.setFailed("Adapter found with empty id.");
				return;
			}

			if (result.getAdapterConfig(config.getId()) != null) {
				result.setFailed("Adapter " + config.getId() + " is defined multiple times.");
				return;
			}
			if (result.getDatastore(config.getId()) != null) {
				result.setFailed("Adapter id " + config.getId() + " is also used for a datastore.");
				return;
			}

			result.addAdapterConfig(config);
		}

		// // Now build each adapter.
		// for (AdapterConfig adapterConfig : script.getAdapters()) {
		//
		// // This can recursively build other adapters. Adapters are registered
		// in the result object when built.
		// AdapterBuilder builder = new AdapterBuilder(adapterConfig.getId(),
		// new StandardisedStringSet(), result);
		// builder.build();
		// if (result.isFailed()) {
		// break;
		// }
		// }
	}

	/**
	 * Get the dependencies on the external datastore which have been recorded
	 * so far
	 * 
	 * @param result
	 * @return
	 */
	public DataDependencies extractDependencies(ScriptExecutionBlackboardImpl result) {
		DataDependencies ret = new DataDependencies();

		// loop over each saved datastore
		for (SavedDatastore sds : result.getDatastores()) {
			@SuppressWarnings("unchecked")
			DataDependenciesRecorder<ODLTableAlterable> recorder = (DataDependenciesRecorder<ODLTableAlterable>) sds.getDs();
			DataDependencies recordedDep = recorder.getDependencies();

			if (sds.isExternal()) {
				// if this is the external datastore, add the recorded
				// dependencies directly
				ret.add(recordedDep);
			} else if (recordedDep.isRead()) {
				// if its not the external but has been read, add its own
				// external dependencies
				ret.add(sds.getDependenciesOnExternal());
			}
		}

		return ret;
	}

	private void buildDatastores(ODLDatastoreAlterable<? extends ODLTableAlterable> externalDS, Option script, ScriptExecutionBlackboardImpl result) {

		// save external datastore wrapped in a data dependencies recorder
		result.addDatastore(ScriptConstants.EXTERNAL_DS_NAME, null, new DataDependenciesRecorder<>(ODLTableAlterable.class, externalDS));

		initialiseParametersTable(result);

		// Create output datastores
		for (InstructionConfig instruction : script.getInstructions()) {
			ODLComponent component = getComponent(instruction, result);
			if (result.isFailed()) {
				return;
			}

			// create output database, filling in the structure if provided and
			// giving no UI edit permissions
			ODLDatastore<? extends ODLTableDefinition> outputDfn = component.getOutputDsDefinition(api, instruction.getExecutionMode(), ScriptUtils.getComponentConfig(script, instruction));
			ODLDatastoreAlterable<ODLTableAlterable> outputDb = datastoreFactory.create();
			if (outputDfn != null) {
				outputDfn.setFlags(outputDfn.getFlags() & ~TableFlags.UI_EDIT_PERMISSION_FLAGS);
				DatastoreCopier.copyStructure(outputDfn, outputDb);

				// copy structure also copies table flags; ensure edit flags
				// turned off
				for (int i = 0; i < outputDb.getTableCount(); i++) {
					ODLTableDefinitionAlterable table = outputDb.getTableAt(i);
					table.setFlags(table.getFlags() & ~TableFlags.UI_EDIT_PERMISSION_FLAGS);
				}
			}

			// check output db if unique (if set)
			String outId = instruction.getOutputDatastore();
			if (Strings.isEmpty(outId) == false) {
				if (result.getDatastore(outId) != null) {
					result.setFailed("Component " + component.getId() + " output datastore " + outId + " already exists.");
				}
			}

			// some operations check that the source datastores can be rolled
			// back, wrap the datastore in an
			// undo-redo decorator to let this happen
			UndoRedoDecorator<ODLTableAlterable> undoRedoDecorator = new UndoRedoDecorator<>(ODLTableAlterable.class, outputDb);
			result.addDatastore(outId, instruction, new DataDependenciesRecorder<>(ODLTableAlterable.class, undoRedoDecorator));

		}
	}

	/**
	 * Initialise the internal DS that holds the parameters table + available
	 * values. This table holds the parameters with keys.
	 * 
	 * @param result
	 */
	private void initialiseParametersTable(ScriptExecutionBlackboardImpl result) {
		// build the internal parameters datastore, wrapped in an undo / redo
		// for operations requiring undoable datastore
		Tables tables = api.tables();
		Parameters parameters = api.scripts().parameters();
		ODLDatastoreAlterable<? extends ODLTableAlterable> internal = tables.createAlterableDs();
		internal = new UndoRedoDecorator<>(ODLTableAlterable.class, internal);
		if (initialParametersTable != null) {
			// copy existing parameters
			tables.copyTable(initialParametersTable, internal);
		} else {
			// create empty table
			tables.copyTableDefinition(parameters.tableDefinition(true), internal);
		}

		// copy other parameters tables over (e.g. available values)
		ODLDatastore<? extends ODLTableDefinition> dfn = parameters.dsDefinition(true);
		for (int i = 0; i < dfn.getTableCount(); i++) {
			ODLTableDefinition table = dfn.getTableAt(i);
			if (!api.stringConventions().equalStandardised(table.getName(), parameters.tableDefinition(true).getName())) {
				// copy empty table
				tables.copyTableDefinition(table, internal);
			}
		}

		result.addDatastore(api.scripts().parameters().getDSId(), null, new DataDependenciesRecorder<>(ODLTableAlterable.class, internal));
	}

	/**
	 * Execute the same instructions potentially many times if batch keys are
	 * set.
	 * 
	 * @param option
	 * @param instruction
	 * @param result
	 */
	private void executeBatchedInstruction(Script root, InstructionConfig instruction, final ScriptExecutionBlackboardImpl result) {

		// helper class to store information on the batch keys
		class BatchKeyInformation {
			final List<String> values;
			final int[] batchKeys;

			BatchKeyInformation(ODLDatastore<? extends ODLTableReadOnly> ds) {
				int nt = ds.getTableCount();
				batchKeys = new int[nt];
				Arrays.fill(batchKeys, -1);

				TreeSet<String> valueset = new TreeSet<>();

				for (int tbl = 0; tbl < nt; tbl++) {
					ODLTableReadOnly table = ds.getTableAt(tbl);
					int nc = table.getColumnCount();
					for (int col = 0; col < nc; col++) {
						if ((table.getColumnFlags(col) & TableFlags.FLAG_IS_BATCH_KEY) != 0) {
							if (batchKeys[tbl] != -1) {
								result.setFailed("Table \"" + table.getName() + "\" has more than one batch key column.");
								values = null;
								return;
							}

							ODLColumnType type = table.getColumnType(col);
							if (ColumnValueProcessor.isBatchKeyCompatible(type) == false) {
								result.setFailed(
										"Table \"" + table.getName() + "\" has batch key column \"" + table.getColumnName(col) + "\" of type " + type.name() + " which is not batch key compatible.");
								values = null;
								return;
							}

							batchKeys[tbl] = col;

							if (!compileOnly) {
								int nr = table.getRowCount();
								for (int row = 0; row < nr; row++) {
									Object val = table.getValueAt(row, col);
									if (val != null) {
										String s = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, val, type);
										s = Strings.std(s);
										valueset.add(s);
									}
								}

							}
						}
					}
				}

				values = new ArrayList<>(valueset);
			}
		}

		// check if we're doing an update query... this has special logic

		// get the input/output datastore or adapter (can be null)
		ODLDatastore<? extends ODLTable> availableIODS = null;
		if (Strings.isEmpty(instruction.getDatastore()) == false) {
			internalExecutionApi.postStatusMessage("Fetching datastore " + instruction.getDatastore());
			availableIODS = findDatastoreOrAdapter(instruction.getDatastore(), result);
			if (availableIODS == null) {
				return;
			}
		}

		// inspect the input data store for batch keys, getting values
		BatchKeyInformation batchKeys = null;
		if (availableIODS != null) {
			batchKeys = new BatchKeyInformation(availableIODS);
			if (result.isFailed()) {
				return;
			}
		}

		if (batchKeys != null && batchKeys.values.size() > 0) {

			// loop over each batch
			for (final String batchKey : batchKeys.values) {
				// create a filtering adapter...
				RowFilterDecorator<ODLTable> filterDecorator = new RowFilterDecorator<ODLTable>(availableIODS);
				for (int tableIndex = 0; tableIndex < availableIODS.getTableCount(); tableIndex++) {
					int keyCol = batchKeys.batchKeys[tableIndex];
					ODLTableReadOnly table = availableIODS.getTableAt(tableIndex);

					// loop over each row of the source table
					int nr = table.getRowCount();
					for (int row = 0; row < nr; row++) {

						// accept row if no key field
						boolean accept = keyCol == -1;

						// accept row if its keyfield value is null
						if (!accept) {
							accept = table.getValueAt(row, keyCol) == null;
						}

						// accept if strings the same
						if (!accept) {
							accept = Strings.equalsStd(batchKey, table.getValueAt(row, keyCol).toString());
						}

						if (accept) {
							filterDecorator.addRowToFilter(table.getImmutableId(), table.getRowId(row));
						}
					}
				}

				// execute with filtered data
				executeSingleInstruction(root, instruction, filterDecorator, batchKey, result);

				checkForUserCancellation(result);
				if (result.isFailed()) {
					break;
				}
			}
		} else {
			executeSingleInstruction(root, instruction, availableIODS, null, result);
		}

		// record the dependencies on the external datastore for this
		// instruction's output datastore
		SavedDatastore outputDb = result.getDsByInstruction(instruction);
		DataDependencies externalDependencies = extractDependencies(result);
		outputDb.getDependenciesOnExternal().add(externalDependencies);

	}

	/**
	 * Execute a single instruction once for a single batch key
	 * 
	 * @param option
	 * @param instruction
	 * @param availableIODS
	 * @param batchKey
	 * @param result
	 */
	private void executeSingleInstruction(Script root, final InstructionConfig instruction, final ODLDatastore<? extends ODLTable> availableIODS, final String batchKey,
			final ScriptExecutionBlackboardImpl result) {

		// get the component
		final ODLComponent component = getComponent(instruction, result);
		if (result.isFailed()) {
			return;
		}

		// get the component's expected datastore
		Serializable config = ScriptUtils.getComponentConfig(root, instruction);
		final ODLDatastore<? extends ODLTableDefinition> expectedIODS = component.getIODsDefinition(api, config);

		// adapt the available io datastore to the component's expected input or
		// take all available tables
		// if the expected iods has zero table count
		final ODLDatastore<? extends ODLTable> ioDS;
		if (availableIODS != null && expectedIODS != null) {
			ioDS = new TargetIODsInterpreter(api).buildScriptExecutionAdapter(availableIODS, expectedIODS, result);
		} else if (availableIODS == null && expectedIODS != null) {
			throw new RuntimeException("No input datastore provided.");
		} else {
			ioDS = null;
		}
		
		// if we have a formula for top label on any reports, execute it now
		String reportTopLabel;
		if(!Strings.isEmpty(instruction.getReportTopLabelFormula())){
			Func func = executeFunctionCompilationFromComponent(root,instruction.getReportTopLabelFormula(), null, availableIODS, result);
			if(result.isFailed()){
				return;
			}
			
			Object val=func.execute(-1);
			if(val== Functions.EXECUTION_ERROR){
				result.setFailed("Error executing report title formula: " + instruction.getReportTopLabelFormula());
				return;
			}
			reportTopLabel = val!=null? val.toString():null;
		}else{
			reportTopLabel = null;
		}


		// go from the internal api to the external one
		ComponentExecutionApi externalApi = new ComponentExecutionApi() {

			@Override
			public ODLApi getApi() {
				return internalExecutionApi.getApi();
			}

			@Override
			public boolean isFinishNow() {
				return internalExecutionApi.isFinishNow();
			}

			@Override
			public boolean isCancelled() {
				return internalExecutionApi.isCancelled();
			}

			@Override
			public <T extends JPanel & ClosedStatusObservable> void showModalPanel(T panel, String title) {
				internalExecutionApi.showModalPanel(panel, title);

			}

			@Override
			public ModalDialogResult showModalPanel(JPanel panel, String title, ModalDialogResult... buttons) {
				return internalExecutionApi.showModalPanel(panel, title, buttons);
			}

			@Override
			public void postStatusMessage(String s) {
				// prefix the batch key
				if (batchKey != null) {
					s = "Running batch key: " + batchKey + System.lineSeparator() + s;
				}
				internalExecutionApi.postStatusMessage(s);
			}

			@Override
			public void logWarning(String warning) {
				result.log(warning);
			}

			@Override
			public String getBatchKey() {
				return batchKey;
			}

			@Override
			public ODLCostMatrix calculateDistances(DistancesConfiguration request, ODLTableReadOnly... tables) {
				return internalExecutionApi.calculateDistances((DistancesConfiguration) request, tables);
			}

			@Override
			public void submitControlLauncher(ControlLauncherCallback cb) {
				// Take a copy of the parameters and parameters value tables
				// from the internal ds.
				// This ensures we pass an immutable snapshot to the GUI code.
				Tables tables = api.tables();
				Parameters parameters = api.scripts().parameters();
				ODLDatastore<? extends ODLTable> internalDs = result.getDatastore(parameters.getDSId());
				ODLTableReadOnly paramTable = parameters.findTable(internalDs, TableType.PARAMETERS);
				ODLTableReadOnly paramValuesTable = parameters.findTable(internalDs, TableType.PARAMETER_VALUES);
				ODLDatastoreAlterable<? extends ODLTableAlterable> copyDs = tables.createAlterableDs();
				tables.copyTable(paramTable, copyDs);
				tables.copyTable(paramValuesTable, copyDs);

				internalExecutionApi.submitControlLauncher(instruction.getUuid(), component, copyDs,reportTopLabel, cb);
			}

			@Override
			public ODLGeom calculateRouteGeom(DistancesConfiguration request, LatLong from, LatLong to) {
				return internalExecutionApi.calculateRouteGeom(request, from, to);
			}

			@Override
			public Func compileFunction(String formulaText, String sourceTableName) {
				return executeFunctionCompilationFromComponent(root,formulaText, sourceTableName, availableIODS, result);
			}
		};

		// execute the component
		if (!compileOnly) {
			// Read all input values to ensure that the data dependencies are
			// registered properly when the component
			// executes; for queries the input values can be read by the table
			// component later as well...
			// We also optionally pass these values to the componen here, if it
			// implements the correct interface,
			// to save on CPU time.
			if (ioDS != null) {
				long flags = component.getFlags(externalApi.getApi(), instruction.getExecutionMode());
				final long DisableFlag = ODLComponent.FLAG_DISABLE_FRAMEWORK_DATA_READ_FOR_DEPENDENCIES;
				if ((flags & DisableFlag) != DisableFlag) {
					externalApi.postStatusMessage("Validating input data" + (Strings.isEmpty(batchKey) ? "" : " (key=" + batchKey + ")"));
					UpdateTimer timer = new UpdateTimer(250);
					for (int i = 0; i < ioDS.getTableCount() && checkForUserCancellation(result); i++) {
						ODLTableReadOnly table = ioDS.getTableAt(i);
						int nrow = table.getRowCount();
						int ncol = table.getColumnCount();
						for (int row = 0; row < nrow && checkForUserCancellation(result); row++) {
							for (int col = 0; col < ncol; col++) {
								table.getValueAt(row, col);
							}

							if (timer.isUpdate()) {
								externalApi.postStatusMessage("Validating input data" + (Strings.isEmpty(batchKey) ? "" : " (key=" + batchKey + ")") + ", table " + (i + 1) + "/" + ioDS.getTableCount()
										+ ", row " + (row + 1) + "/" + nrow);
							}
						}
					}
				}

			}

			if (result.isFailed()) {
				return;
			}

			try {
				externalApi.postStatusMessage("Calling component: " + component.getName());
				ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb = result.getDsByInstruction(instruction).getDs();
				component.execute(externalApi, instruction.getExecutionMode(), config, ioDS, outputDb);
			} catch (Throwable e) {
				result.setFailed(e);
				result.setFailed("Component " + component.getId() + " threw an exception.");
				return;
			}

			// register or update the external datasource dependencies for any
			// UI components that were created or updated
			DataDependencies depends = extractDependencies(result);
			internalExecutionApi.addInstructionDependencies(instruction.getUuid(), depends);

		}
	}

	private ODLComponent getComponent(ComponentConfig instruction, ScriptExecutionBlackboardImpl result) {
		// get the component
		ODLComponent component = components.getComponent(instruction.getComponent());
		if (component == null) {
			result.setFailed("Could not find component \"" + instruction.getComponent() + "\".");
			return null;
		}
		return component;
	}

	/**
	 * Push the output tables to the datastore specified in the push config.
	 * This will either create the tables or append rows to existing tables, if
	 * already present. If the tables are already present then they *must* have
	 * the expected fieldnames. The push can have an adapter config set to
	 * change its output fieldnames and tablenames.
	 * 
	 * @param outputDb
	 * @param output
	 * @param result
	 */
	private void executeOutput(final OutputConfig output, final ScriptExecutionBlackboardImpl result) {
		if (output.getType() == OutputType.DO_NOT_OUTPUT) {
			return;
		}

		final ODLDatastoreAlterable<? extends ODLTableAlterable> externalDb = result.getDatastore(ScriptConstants.EXTERNAL_DS_NAME);

		// get the datastore
		ODLDatastore<? extends ODLTable> inputDs = findDatastoreOrAdapter(output.getDatastore(), result);
		if (inputDs == null) {
			result.setFailed("Failed to get input datastore '" + output.getDatastore() + "' for output command.");
			return;
		}

		class Helper {
			boolean create(ODLTableReadOnly inputTable, String destinationTable) {
				switch (output.getType()) {

				case REPLACE_CONTENTS_OF_EXISTING_TABLE:
				case APPEND_TO_EXISTING_TABLE:
				case APPEND_ALL_TO_EXISTING_TABLES: {
					// find table
					ODLTableAlterable outTable = TableUtils.findTable(externalDb, destinationTable, true);

					// create if missing
					if (outTable == null) {
						if (compileOnly) {
							return true;
						} else {
							outTable = createEmptyOutputTable(inputTable, destinationTable, externalDb, result);
							if (result.isFailed()) {
								return false;
							}
						}
					}

					// delete all rows if not empty
					if (output.getType() == OutputType.REPLACE_CONTENTS_OF_EXISTING_TABLE) {
						while (outTable.getRowCount() > 0) {
							outTable.deleteRow(0);
						}
					}

					// link fields by name
					AdaptedTableConfig tableAdapterConfig = AdapterConfig.createSameNameMapper(inputTable);
					AdapterMapping mapping = AdapterMapping.createUnassignedMapping(inputTable,false);
					mapping.setTableSourceId(inputTable.getImmutableId(), 0, outTable.getImmutableId());
					AdapterBuilderUtils.mapFields(outTable, inputTable.getImmutableId(), tableAdapterConfig, mapping, 0, result);
					if (result.isFailed()) {
						result.log("Failed to output to table.");
						result.log("If you are outputting to a table which already exists, it must already have all your output fields.");
						return false;
					}

					// create an adapted database with just this table; get new
					// output table from this
					ArrayList<ODLDatastore<? extends ODLTable>> dsList = new ArrayList<>();
					dsList.add(externalDb);
					AdaptedDecorator<ODLTable> adaptedPushToDS = new AdaptedDecorator<ODLTable>(mapping, dsList);
					ODLTable adaptedOutTable = adaptedPushToDS.getTableAt(0);

					// finally append the data
					if (!compileOnly) {
						DatastoreCopier.copyData(inputTable, adaptedOutTable);
					}
					break;
				}

				case COPY_ALL_TABLES:
				case COPY_TO_NEW_TABLE: {
					if (Strings.isEmpty(destinationTable)) {
						result.setFailed("Destination table is empty for output command.");
						return false;
					}

					// create unique name if table already exists
					if (TableUtils.findTable(externalDb, destinationTable, true) != null) {
						destinationTable = TableUtils.getUniqueNumberedTableName(destinationTable, externalDb);
					}

					if (compileOnly) {
						return false;
					}

					// create output table
					ODLTableAlterable outTable = createEmptyOutputTable(inputTable, destinationTable, externalDb, result);
					if (result.isFailed()) {
						return false;
					}

					// copy the data
					DatastoreCopier.copyData(inputTable, outTable);

					// ensure the external table has editable flags
					outTable.setFlags(outTable.getFlags() | TableFlags.UI_SET_INSERT_DELETE_PERMISSION_FLAGS);
					break;
				}

				default:
					break;
				}

				return true;
			}
		}
		Helper helper = new Helper();

		if (output.getType() == OutputType.COPY_ALL_TABLES || output.getType() == OutputType.APPEND_ALL_TO_EXISTING_TABLES) {
			// get all input tables from datastore
			for (int i = 0; i < inputDs.getTableCount(); i++) {
				ODLTableReadOnly copyFromTable = inputDs.getTableAt(i);
				if (!helper.create(copyFromTable, copyFromTable.getName())) {
					return;
				}
			}
		} else {
			// get specific input table
			ODLTableReadOnly copyFromTable = TableUtils.findTable(inputDs, output.getInputTable(), true);
			if (copyFromTable == null) {
				result.setFailed("Failed to get input table '" + output.getInputTable() + "' for output command.");
				return;
			}
			helper.create(copyFromTable, output.getDestinationTable());
		}

	}

	private static ODLTableAlterable createEmptyOutputTable(ODLTableReadOnly inputTable, String outputTableName, ODLDatastoreAlterable<? extends ODLTableAlterable> externalDb,
			ScriptExecutionBlackboardImpl result) {
		ODLTableAlterable outTable = externalDb.createTable(outputTableName, -1);
		if (outTable == null) {
			result.setFailed("Failed to create output table '" + outputTableName + "'.");
			return null;
		}
		DatastoreCopier.copyTableDefinition(inputTable, outTable);
		return outTable;
	}

	private ODLDatastore<? extends ODLTable> findDatastoreOrAdapter(String id, ScriptExecutionBlackboardImpl env) {
		// check for datastores with this id
		ODLDatastore<? extends ODLTable> ds = env.getDatastore(id);
		if (ds != null) {
			return ds;
		}

		// check for buildable adapter
		AdapterConfig adapterConfig = env.getAdapterConfig(id);
		if (adapterConfig != null) {
			internalExecutionApi.postStatusMessage("Building data adapter: " + (id != null ? id : "<no id>"));
			AdapterBuilder builder = new AdapterBuilder(adapterConfig, new StandardisedStringSet(false), env, internalExecutionApi, new BuiltAdapters());
			ds = builder.build();
			if (!env.isFailed() && ds != null) {
				return ds;
			}
		}

		env.setFailed("Could not find adapter or datastore with id " + id);
		return null;
	}

	/**
	 * Execute the compilation of a function which is called by a component
	 * 
	 * @param formulaText
	 * @param sourceTableName
	 *            Must be a source table in the component's input datastore
	 * @param availableIODS
	 *            The unadapted (i.e. raw) adapter for the component
	 * @param result
	 * @return
	 */
	private Func executeFunctionCompilationFromComponent(Script root,String formulaText, String sourceTableName, final ODLDatastore<? extends ODLTable> availableIODS, final ScriptExecutionBlackboardImpl result) {
		// find source table if set. use the availableiods?
		final ODLTableReadOnly sourceTable;
		if (!Strings.isEmpty(sourceTableName)) {
			sourceTable = TableUtils.findTable(availableIODS, sourceTableName);
			if (sourceTable == null) {
				throw new RuntimeException("Cannot find source table " + sourceTableName + " in function: " + formulaText);
			}
		} else {
			sourceTable = null;
		}

		final AdapterBuilder builder = new AdapterBuilder((AdapterConfig) null, new StandardisedStringSet(false), result, internalExecutionApi, new BuiltAdapters());

		// ensure we have the input datastore
		final int dsIndx;
		if (availableIODS != null) {
			dsIndx = builder.addDatasource(null, availableIODS);
		} else {
			dsIndx = -1;
		}

		//table.getUserFormulae().addAll(script.getUserFormulae());
		final Function function = builder.buildFormulaWithTableVariables(sourceTable, formulaText, -1, root.getUserFormulae(), null);

		final int sourceTableId = sourceTable != null ? sourceTable.getImmutableId() : -1;
		return new Func() {

			@Override
			public Object execute(int rowIndex) {
				long rowId = -1;
				if (sourceTable != null && rowIndex != -1) {
					rowId = sourceTable.getRowId(rowIndex);
				}

				// don't need the 'this row'
				ODLRowReadOnly thisRow = null;

				FunctionParameters parameters = new TableParameters(builder.getDatasources(), dsIndx, sourceTableId, rowId, rowIndex, thisRow);
				return function.execute(parameters);
			}
		};
	}

	public void setInitialParametersTable(ODLTableReadOnly initialParametersTable) {
		this.initialParametersTable = initialParametersTable;
	}

}
