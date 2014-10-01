/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl.scripts;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptComponentConfig;
import com.opendoorlogistics.api.scripts.ScriptElement;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.scripts.ScriptInstruction;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.TargetIODsInterpreter;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.ComponentConfig;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.OutputConfig;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.elements.ScriptBaseElement;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils.FindScriptElement;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.strings.Strings.DoesStringExist;

public class ScriptOptionImpl extends ScriptElementImpl implements ScriptOption {
	protected final ScriptOptionImpl parentOption;
	protected final Option option;
	protected final ScriptInputTables inputTables;

	/**
	 * Use this constructor when wrapping a pre-existing option
	 * @param api
	 * @param inputTables
	 * @param option
	 * @param parentOption
	 */
	public ScriptOptionImpl(ODLApi api,ScriptInputTables inputTables, Option option,ScriptOptionImpl parentOption){
		super(api,null,option);
		this.option = option;
		this.inputTables = inputTables;
		this.parentOption = parentOption;
	}
	
	/**
	 * Use this constructor when adding a new option to an existing script option object
	 * @param api
	 * @param inputTables
	 * @param parentOption
	 */
	private ScriptOptionImpl(ODLApi api,ScriptInputTables inputTables, ScriptOptionImpl parentOption) {		
		super(api,parentOption, null);
		this.option = (Option)getElement();
		this.inputTables = inputTables;
		this.parentOption = parentOption;
	}

	/**
	 * For the input script and option id, create a wrapped hierarchy and return the ScriptOption
	 * corresponding to the option id
	 * @param api
	 * @param script
	 * @param optionId
	 * @param inputTables
	 * @return
	 */
	public static ScriptOption createWrapperHierarchy(ODLApi api,Script script, String optionId, ScriptInputTables inputTables){ 
		List<Option> path = ScriptUtils.getOptionPath(script,optionId);
		if(path==null || path.size()==0){
			throw new RuntimeException("Corrupt script, option id unknown: " + optionId);
		}
		
		ScriptOption ret=null;
		for(Option option:path){
			if(ret==null){
				// top-level
				ret = new ScriptOptionImpl(api, inputTables,option, null);
			}else{
				// below top level
				ret = ret.getChildOption(option.getOptionId());
				if(ret==null){
					throw new RuntimeException("Corrupt script");		
				}
			}

			if(Strings.equalsStd(ret.getOptionId(), option.getOptionId())==false){
				throw new RuntimeException("Corrupt script");
			}			
		}
	
		return ret;
	}

	protected enum FindMode{
		CANNOT_EXIST_ANYWHERE,
		MUST_EXIST_IN_CURRENT_OPTION,
		MUST_EXIST_IN_AVAILABLE_OPTIONS,
		NO_RESTRICTION
	}
	
	protected Option findOption(final String optionId) {
		return ScriptUtils.getScriptElement(new FindScriptElement<Option>() {

			@Override
			public Option find(Option option) {
				if (Strings.equalsStd(optionId, option.getOptionId())) {
					return option;
				}
				return null;
			}
		}, root());
	}
	
	@Override
	public String createUniqueOptionId(String baseId) {
		return Strings.makeUnique(baseId, new DoesStringExist() {
			
			@Override
			public boolean isExisting(String s) {
				return findOption(s)!=null;
			}
		});	
	}

	@Override
	public String createUniqueDatastoreId(String baseId) {
		return ScriptUtils.createUniqueDatastoreId(root(), baseId);
	}

	@Override
	public String createUniqueComponentConfigId(String baseId) {
		return Strings.makeUnique(baseId, new DoesStringExist() {
			
			@Override
			public boolean isExisting(String s) {
				return findComponentConfig(s)!=null;
			}
		});	
	}

	/**
	 * Get the adapter. The whole script is searched, not just the current option.
	 * 
	 * @param adapterId
	 * @param shouldExist
	 * @return
	 */
	protected AdapterConfig findAdapter(final String adapterId, FindMode findOption) {

		// find root option node, getting all available options as we go...
		final HashSet<Option> availableOptions = new HashSet<>();
		ScriptOptionImpl root = findRoot(availableOptions);

		class RecurseFind {
			Pair<AdapterConfig,Option> recurse(Option node) {
				for (AdapterConfig config : node.getAdapters()) {
					if (Strings.equalsStd(adapterId, config.getId())) {
						return new Pair<AdapterConfig,Option>(config,node);
					}
				}

				for (Option child : node.getOptions()) {
					Pair<AdapterConfig,Option> config = recurse(child);
					if (config != null) {
						return config;
					}
				}
				return null;
			}
		}

		Pair<AdapterConfig,Option> ret = new RecurseFind().recurse(root.option);
		
		if(findOption!=null){
			switch(findOption){
			case CANNOT_EXIST_ANYWHERE:
				if (ret != null) {
					throw new RuntimeException("Adapter with input id already exists: " + adapterId);
				}
				break;
				
			case MUST_EXIST_IN_CURRENT_OPTION:
			case MUST_EXIST_IN_AVAILABLE_OPTIONS:
				if(ret==null){
					throw new RuntimeException("Data adapter with id not found: " + adapterId);		
				}
				
				if(findOption == FindMode.MUST_EXIST_IN_CURRENT_OPTION && ret.getSecond() != option){
					throw new RuntimeException("Data adapter exists but not within current script option: " + adapterId);							
				}
			
				if(findOption == FindMode.MUST_EXIST_IN_AVAILABLE_OPTIONS && availableOptions.contains(ret.getSecond())==false){
					throw new RuntimeException("Data adapter exists but not within current script option or its parent options: " + adapterId);												
				}
				break;
				
			default:
				break;
			}
	
		}
		

		if(ret!=null){
			return ret.getFirst();
		}
		return null;
	}



	@Override
	protected ScriptBaseElement createRootElement() {
		return new Option();
	}

	protected AdapterColumnConfig getAdaptedColumn(String adapterId, int tableIndex, String columnName, FindMode findMode) {
		AdaptedTableConfig table = getAdaptedTable(adapterId, tableIndex, findMode);
		if (table != null) {
			for (AdapterColumnConfig col : table.getColumns()) {
				if (Strings.equalsStd(col.getName(), columnName)) {
					return col;
				}
			}

			if (findMode!= FindMode.CANNOT_EXIST_ANYWHERE) {
				throw new RuntimeException("Column with name not found: " + columnName);
			}
		}
		return null;
	}

	protected AdaptedTableConfig getAdaptedTable(String adapterId, int tableIndex, FindMode findMode) {
		AdapterConfig config = findAdapter(adapterId, findMode);
		AdaptedTableConfig table = config.getTable(tableIndex);
		return table;
	}

	@Override
	public ScriptAdapter addDataAdapter(String adapterId) {
		if(Strings.isEmpty(adapterId) || api.values().equalsStandardised(adapterId, api.stringConventions().getSpreadsheetAdapterId())){
			throw new RuntimeException("Illegal adapter id: " + adapterId);
		}
		adapterId = createUniqueDatastoreId(adapterId);
		
		findAdapter(adapterId, FindMode.CANNOT_EXIST_ANYWHERE);

		AdapterConfig ac = new AdapterConfig(adapterId);
		option.getAdapters().add(ac);
		return new ScriptAdapterImpl(api,this, ac);
	}

	@Override
	public ScriptInstruction addInstruction(String inputDataAdapter, String componentId, int mode, Serializable config) {
		InstructionConfig instruction = new InstructionConfig();
		ScriptOptionImpl root = findRoot(null);
		instruction.setUuid(ScriptUtils.createUniqueInstructionId(root.option));
		instruction.setComponentConfig(config);
		instruction.setComponent(componentId);
		instruction.setDatastore(inputDataAdapter);
		instruction.setExecutionMode(mode);
		option.getInstructions().add(instruction);
	
		ScriptInstruction ret= new ScriptInstructionImpl(api,this, instruction);
		
		if(ret.getInstructionOutput()!=null){
			ODLComponent component = ODLGlobalComponents.getProvider().getComponent(componentId);
			ret.setOutputDatastoreId(createUniqueDatastoreId(component!=null ? "Output of " + component.getName(): "Output"));
		}
		return ret;
	}

	@Override
	public ODLApi getApi() {
		return api;
	}



	@Override
	public ScriptOption addOption(String id, String name) {
		ScriptOptionImpl ret = new ScriptOptionImpl(api,inputTables, this);
		if (Strings.isEmpty(id)) {
			throw new RuntimeException("Option id cannot be empty.");
		}

		id = createUniqueOptionId(id);
		
		if(findOption(id)!=null){
			throw new RuntimeException("Option id already exists in script: " + id);
		}
		
		if (Strings.isEmpty(name)) {
			throw new RuntimeException("Option name cannot be empty.");
		}

		ret.option.setOptionId(id);
		ret.option.setName(name);
		option.getOptions().add(ret.option);
		return ret;
	}

	@Override
	public ScriptInstruction addInstruction(String inputDataAdapter, String componentId, int mode) {
		ODLComponent component = ODLGlobalComponents.getProvider().getComponent(componentId);
		if (component == null) {
			throw new RuntimeException("Unknown component: " + componentId);
		}

		Serializable config = null;
		if (component.getConfigClass() != null) {
			try {
				config = component.getConfigClass().newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return addInstruction(inputDataAdapter, componentId, mode, config);
	}
	
	@Override
	public ScriptInstruction addInstruction(String inputDataAdapter, String componentId, int mode, String configId) {
		ScriptInstruction ret=  addInstruction(inputDataAdapter, componentId, mode,(Serializable) null);
		option.getInstructions().get(ret.getIndex()).setConfigId(configId);
		return ret;
	}
//
//	@Override
//	public ScriptAdapter addDataAdapter(String adapterId, String sourceAdapterId, ODLDatastore<? extends ODLTableDefinition> destination) {
//		ScriptAdapter ret = addDataAdapter(adapterId);
//		adapterId = ret.getAdapterId();
//		for (int i = 0; i < destination.getTableCount(); i++) {
//			ODLTableDefinition destTable = destination.getTableAt(i);
//
//			// try getting source table
//			ODLTableDefinition sourceTable = null;
//			
//			// try getting from the input tables first
//			if(inputTables!=null){
//				for(int j =0 ; j<inputTables.size();j++){
//					if(Strings.equalsStd(adapterId, inputTables.getSourceDatastoreId(j)) &&
//						Strings.equalsStd(destTable.getName(), inputTables.getSourceTable(j).getName())){
//						sourceTable = inputTables.getSourceTable(j);
//					}
//				}				
//			}
//
//			// then try getting internally to the script
//			if(sourceTable==null){
//				AdapterConfig adapterConfig = findAdapter(sourceAdapterId, FindMode.MUST_EXIST_IN_AVAILABLE_OPTIONS);
//				for(AdaptedTableConfig tableConfig:adapterConfig){
//					if(Strings.equalsStd(tableConfig.getName(), destTable.getName())){
//						sourceTable = tableConfig;
//						break;
//					}
//				}
//			}
//
//			int indx=-1;
//			if (sourceTable != null) {
//				indx = ret.addSourcedTableToAdapter(sourceTable, destTable);
//			} else {
//				indx = ret.addSourcelessTable(destTable);
//			}
//			
//			// set the source adapter on the table
//			getAdaptedTable(adapterId, indx, FindMode.MUST_EXIST_IN_CURRENT_OPTION).setFromDatastore(sourceAdapterId);
//		}
//		return ret;
//	}



//	@Override
//	public ODLDatastore<? extends ODLTableDefinition> getSelectedInputTables() {
//		return selectedInputTables;
//	}

//	@Override
//	public ODLDatastore<? extends ODLTableDefinition> getSpreadsheetDefinition() {
//		return spreadsheetDatastore;
//	}

//	@Override
//	public void setOptionEditorLabel( String note) {
//		option.setEditorLabel(note);
//	}
//	

//	@Override
//	public void setAdapterEditorLabel(String adapterId,String label) {
//		findAdapter(adapterId, FindMode.MUST_EXIST_IN_CURRENT_OPTION).setEditorLabel(label);
//	}
//	
//	@Override
//	public void setComponentConfigurationEditorLabel(String id,String html) {
//		for(ComponentConfig c:option.getComponentConfigs()){
//			if(Strings.equalsStd(c.getConfigId(), id)){
//				c.setEditorLabel(html);
//			}
//		}
//	}
//
//
//	@Override
//	public void setInstructionEditorLabel(int instructionIndex, String label) {
//		option.getInstructions().get(instructionIndex).setEditorLabel(label);
//	}

	@Override
	public ScriptElement addCopyTable(String sourceAdapterId, String sourceTableName, OutputType type, String destinationTableName) {
		OutputConfig outputConfig = new OutputConfig();
		outputConfig.setDatastore(sourceAdapterId);
		outputConfig.setInputTable(sourceTableName);
		outputConfig.setType(type);
		outputConfig.setDestinationTable(destinationTableName);
		option.getOutputs().add(outputConfig);
		//return option.getOutputs().size()-1;
		return new ScriptElementImpl(api,this, outputConfig);
	}
//
//	@Override
//	public void setOutputEditorLabel(int outputIndex, String label) {
//		option.getOutputs().get(outputIndex).setEditorLabel(label);
//	}

	@Override
	public ScriptComponentConfig addComponentConfig(String configId, String componentid, Serializable config) {
		configId = createUniqueComponentConfigId(configId);
		ComponentConfig conf = new ComponentConfig();
		conf.setComponent(componentid);
		conf.setComponentConfig(config);
		conf.setConfigId(configId);
		option.getComponentConfigs().add(conf);
		return new ScriptComponentConfigImpl(api,this, conf);
	}

	/**
	 * @param configId
	 * @return
	 */
	protected ComponentConfig findComponentConfig(String configId) {
		return ScriptUtils.getComponentConfig(root(), configId);
	}

	private Option root() {
		return findRoot(null).option;
	}

	@Override
	public void setSynced(boolean scriptIsSynced) {
		option.setSynchronised(scriptIsSynced);
	}

	@Override
	protected ScriptOptionImpl findRoot(final HashSet<Option> availableOptions) {
		if(availableOptions!=null){
			availableOptions.add(option);			
		}
		ScriptOptionImpl root = this;
		while (root.parentOption != null) {
			root = root.parentOption;
			
			if(availableOptions!=null){
				availableOptions.add(root.option);				
			}
		}
		return root;
	}

	@Override
	public String getOptionId() {
		return option.getOptionId();
	}

	@Override
	public ScriptInputTables getInputTables() {
		return inputTables;
	}

	@Override
	public ScriptOption getParent() {
		return parentOption;
	}

	@Override
	public int getChildOptionCount() {
		return option.getOptions().size();
	}

	@Override
	public ScriptOption getChildOption(int i) {
		return new ScriptOptionImpl(api, inputTables, option.getOptions().get(i), this);
	}

	@Override
	public ScriptOption getChildOption(String optionId) {
		for(int i =0 ; i<getChildOptionCount();i++){
			ScriptOption child = getChildOption(i);
			if(Strings.equalsStd(child.getOptionId(), optionId)){
				return child;
			}
		}
		return null;
	}

	public Option getOption(){
		return option;
	}

	@Override
	public ScriptAdapter addDataAdapterLinkedToInputTables(String baseId, ODLDatastore<? extends ODLTableDefinition> destination) {
		ScriptAdapter ret = addDataAdapter(baseId);
		AdapterConfig adapterConfig = new TargetIODsInterpreter(api).buildAdapterConfig(inputTables, destination);
		for(AdaptedTableConfig table:adapterConfig){
			((ScriptAdapterImpl)ret).addAdaptedTable(table);
		}
		return ret;
	}
}
