/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.wizard;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.scripts.ScriptInstruction;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptOption.OutputType;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.api.impl.scripts.ScriptOptionImpl;
import com.opendoorlogistics.core.components.ODLWizardTemplateConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;

final public class ScriptGenerator {
//	public static AdaptedTableConfig createAdaptedTableConfig(ODLTableDefinition table, String adaptedName) {
//		AdaptedTableConfig tableConfig = new AdaptedTableConfig();
//		tableConfig.setFromTable(table != null ? table.getName() : "");
//		tableConfig.setName(adaptedName);
//		tableConfig.setFromDatastore(ScriptConstants.EXTERNAL_DS_NAME);
//
//		// copy table definition, setting from field and keeping flags 0
//		if (table != null) {
//			DatastoreCopier.copyTableDefinition(table, tableConfig);
//			for (int i = 0; i < tableConfig.getColumnCount(); i++) {
//				tableConfig.setColumnFlags(i, 0);
//				tableConfig.getColumn(i).setFrom(tableConfig.getColumnName(i));
//			}
//		}
//		return tableConfig;
//	}

	// public static int getNbInputTables(ODLComponent component, ODLWizardTemplateConfig config) {
	// ODLDatastore<? extends ODLTableDefinition> dfn = component.getIODbDefinition(config.getConfig());
	// if (dfn != null) {
	// return dfn.getTableCount();
	// }
	// return 0;
	// }


	private ScriptGenerator() {
	}

//	public static interface WizardOptionChooseCallback {
//
//		/**
//		 * Return the index of the chosen option or -1 if user cancelled.
//		 * 
//		 * @param message
//		 * @param options
//		 * @param tooltips
//		 *            Can be null.
//		 * @return
//		 */
//		int selectOption(String message, String[] options, String[] tooltips);
//	}

	private static void setComponentInformation(ODLComponent component, Script script){
		if(component!=null){
			script.setCreatedByComponentId(component.getId());
			script.setName(component.getName());
			script.setOptionId(component.getName());
		}
	}
	
	public static Option generate(ODLApi api,Script script, Option parent,ScriptGeneratorInput input ){
		Option ret=null;

		final ODLComponent component = input.getComponent();
		final ODLWizardTemplateConfig config = input.getConfig();

		// get the option builder object
		ScriptOption builder = null;
		if(parent==null){
			if(script!=null){
				throw new RuntimeException();
			}
					
			// we don't have an input script so create one
			script = new Script();
			script.setName(component.getName());
			script.setOptionId(component.getName());
			setComponentInformation(component,script);			
			ret = script;
			builder = new ScriptOptionImpl(api, input.getInputTables(), script, null);
		}else{
			// create a new node on the parent node
			ScriptOption parentBuilder = ScriptOptionImpl.createWrapperHierarchy(api, script, parent.getOptionId(), input.getInputTables());
			builder = parentBuilder.addOption(component.getName(), component.getName());
			ret = ((ScriptOptionImpl)builder).getOption();
		}
		
		if(config.getBuildScriptCB()!=null){
			// Build using the template's own callback
			config.getBuildScriptCB().buildScript(builder);
		}else{
			// Build a default one option...
			
			// Add the instruction first. An output datastore id will be automatically assigned
			ScriptInstruction instructionBuilder = builder.addInstruction(null, component.getId(), config.getExecutionMode(),config.getConfig());
			
			// Add input datastore if needed
			ODLDatastore<? extends ODLTableDefinition> ioDs = instructionBuilder.getInstructionRequiredIO();
			if(ioDs!=null){
				ScriptAdapter adapter = builder.addDataAdapterLinkedToInputTables("Input to \"" + component.getName() + "\"", ioDs);
				instructionBuilder.setInputDatastoreId(adapter.getAdapterId());
			}
			
			// Create copy tables for the output (if we have output)
			ODLDatastore<? extends ODLTableDefinition> outDs = instructionBuilder.getInstructionOutput();
			if(outDs!=null){
				if (config.hasFlag(ScriptTemplatesBuilder.FLAG_OUTPUT_DATASTORE_IS_FIXED)) {
					// This assumes output is fixed and allows table names to be edited
					for (int i = 0; i < outDs.getTableCount(); i++) {
						ODLTableDefinition outTable = outDs.getTableAt(i);
						builder.addCopyTable(instructionBuilder.getOutputDatastoreId(), outTable.getName(), OutputType.COPY_TO_NEW_TABLE, outTable.getName());
					}
				} else {
					// This assumes we have output but the table names are not fixed.
					builder.addCopyTable(instructionBuilder.getOutputDatastoreId(), null, OutputType.COPY_ALL_TABLES, null);
				}
			}
		
		}
		
		// Deep copy the new portion of the script to ensure we don't maintain references
		// to default configuration objects owned by the component - which could potentially be
		// modified later-on by the script editor.
		Script deepCopiedScript = ScriptIO.instance().deepCopy(script);
		if(parent==null){
			ret = deepCopiedScript;
		}else{
			Option deepCopiedOption = ScriptUtils.getOption(deepCopiedScript, ret.getOptionId());
			ret = deepCopiedOption;
			parent.getOptions().set(parent.getOptions().size()-1, deepCopiedOption);
		}
		
		return ret;
	}
	
	public static Script generate(ODLApi api,ScriptGeneratorInput input) {

//		// create from template configuration
//		Script ret = config.createScript(inputTables);		
//		if(ret!=null){
//			setComponentInformation(component,ret);
//			return ret;
//		}
//		
//		ret = new Script();
//		setComponentInformation(component,ret);
//		ret.setScriptEditorUIType(ScriptEditorType.WIZARD_GENERATED_EDITOR);
//
//		ExecutionReportImpl report = new ExecutionReportImpl();
//		WizardHelper helper = new WizardHelper(api,ret, inputTables, report);
//		helper.setCreateOutputTables(true);
//		helper.addInstruction(component, config);
//
//		if (!report.isFailed()) {
//			return ret;
//		}
		return (Script)generate(api, null, null,input);
	}

//	private static class WizardHelper {
//		final private ODLApi api ;
//		final Script script;
//		final ScriptInputTables inputTables;;
//
//		final ExecutionReport executionReport;
//		boolean createOutputTables = true;
//
//		private WizardHelper(ODLApi api,Script script, ScriptInputTables inputTables,ExecutionReport executionReport) {
//			this.api = api;
//			this.script = script;
//			this.inputTables = inputTables;
//			this.executionReport = executionReport;
//		}
//
//		/**
//		 * Make the input datastore or adapter id unique
//		 * 
//		 * @param dsId
//		 * @return
//		 */
//		private String makeDsIDUnique(String dsId) {
//			String ret;
//			int i = 0;
//			while (true) {
//				if (i == 0) {
//					ret = dsId;
//				} else {
//					ret = dsId + Integer.toString(i);
//				}
//
//				if (ScriptUtils.getAdapterById(script, ret, true) != null) {
//					i++;
//					continue;
//				}
//
//				if (ScriptUtils.hasDatastore(script, ret)) {
//					i++;
//					continue;
//				}
//				break;
//			}
//			return ret;
//		}
//
//		void setCreateOutputTables(boolean createOutputTables) {
//			this.createOutputTables = createOutputTables;
//		}
//
//		ComponentConfig addInstruction(ODLComponent component, ODLWizardTemplateConfig config) {
//
//			script.setScriptEditorUIType(ScriptEditorType.WIZARD_GENERATED_EDITOR);
//
//			// get input and output datastore
//			ODLDatastore<? extends ODLTableDefinition> ioDs = component.getIODsDefinition(api,config.getConfig());
//			ODLDatastore<? extends ODLTableDefinition> outDs = component.getOutputDsDefinition(api,config.getExecutionMode(),config.getConfig());
//
//			// get name for input datastore adapter
//			String inputDsName = ioDs != null ? "Input to \"" + component.getName() + "\"" : null;
//			if (inputDsName != null) {
//				inputDsName = makeDsIDUnique(inputDsName);
//			}
//
//			// create input adapter
//			if (ioDs != null) {
//				AdapterConfig adapterConfig = new TargetIODsInterpreter(api).buildAdapterConfig( inputTables, ioDs);
//				if(adapterConfig!=null){
//					adapterConfig.setId(inputDsName);
//					script.getAdapters().add(adapterConfig);
//				}
//			}
//
//			// create instruction
//			String outputDsName = outDs != null ? "Output of \"" + component.getName() + "\"" : null;
//			if (outputDsName != null) {
//				outputDsName = makeDsIDUnique(outputDsName);
//			}
//			InstructionConfig instruction = new InstructionConfig(inputDsName, outputDsName, component.getId(), config.getConfig());
//			instruction.setUuid(ScriptUtils.createUniqueInstructionId(script));
//			script.getInstructions().add(instruction);
//
//			// create output tables
//			if (createOutputTables && outDs != null) {
//				createOutputTables(config, outDs, outputDsName);
//			}
//
//			return instruction;
//		}
//
//		private void createOutputTables(ODLWizardTemplateConfig config, ODLDatastore<? extends ODLTableDefinition> outDs, String outputDsName) {
//			if (config.hasFlag(ODLWizardTemplateConfig.FLAG_OUTPUT_DATASTORE_IS_FIXED)) {
//				// This assumes output is fixed and allows table names to be edited
//				for (int i = 0; i < outDs.getTableCount(); i++) {
//					ODLTableDefinition outTable = outDs.getTableAt(i);
//					OutputConfig output = new OutputConfig();
//					output.setDatastore(outputDsName);
//					output.setInputTable(outTable.getName());
//					output.setDestinationTable(outTable.getName());
//					output.setType(OutputType.COPY_TO_NEW_TABLE);
//					script.getOutputs().add(output);
//				}
//			} else {
//				// This assumes we have output but the table names are not fixed.
//				// They cannot be edited.
//				OutputConfig output = new OutputConfig();
//				output.setUserCanEdit(false);
//				output.setDatastore(outputDsName);
//				output.setType(OutputType.COPY_ALL_TABLES);
//				script.getOutputs().add(output);
//			}
//		}
//
//
//	}


	public static class ScriptGeneratorInput{
		private final ODLComponent component;
		private final ODLWizardTemplateConfig config;
		private final ScriptInputTables inputTables;
		
		public ScriptGeneratorInput(ODLComponent component, ODLWizardTemplateConfig config, ScriptInputTables inputTables) {
			this.component = component;
			this.config = config;
			this.inputTables = inputTables;
		}
		public ODLComponent getComponent() {
			return component;
		}
		public ODLWizardTemplateConfig getConfig() {
			return config;
		}
		public ScriptInputTables getInputTables() {
			return inputTables;
		}
		
	}

}
