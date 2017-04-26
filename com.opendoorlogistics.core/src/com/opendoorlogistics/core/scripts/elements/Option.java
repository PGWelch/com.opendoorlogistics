/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.elements;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.opendoorlogistics.core.utils.strings.Strings;

@XmlType(propOrder = { "componentConfigs",  "adapters", "instructions" , "outputs", "options"})
@XmlRootElement(name="Option")
public class Option extends ScriptBaseElementImpl{
	protected List<AdapterConfig> adapters = new ArrayList<>();
	protected List<InstructionConfig> instructions = new ArrayList<>();
	protected List<OutputConfig> outputs = new ArrayList<>();
	protected List<Option> childOptions = new ArrayList<>();
	private List<ComponentConfig> componentConfig = new ArrayList<ComponentConfig>();
	private String optionId;
	private boolean synchronised;
	private boolean launchMultiple;
	private boolean overrideVisibleParameters=false;
	private boolean refreshButtonAlwaysEnabled=false;
	private boolean showLastRefreshedTime=false;
	private String visibleParametersOverride;
	
	public String getOptionId() {
		return optionId;
	}

	@XmlAttribute(name="OptionId")	
	public void setOptionId(String optionId) {
		if(Strings.isEmpty(optionId)){
			throw new RuntimeException("Script option id cannot be empty.");
		}
//		if(Strings.isEmptyAlphaNumericWhitespaceOrDash(optionId)==false){
//			throw new RuntimeException("Invalid script option id: " + optionId);
//		}
		this.optionId = optionId;
	}

	@Override
	public String getShortDescription() {
		return "An optional execution path within the script";
	}

	public List<AdapterConfig> getAdapters() {
		return adapters;
	}
	
	public List<InstructionConfig> getInstructions() {
		return instructions;
	}
	
	/**
	 * Add the input option to this option.
	 * Assumes ids are already unique
	 * @param other
	 */
	public void mergeIntoMe(Option other){
		getAdapters().addAll(other.getAdapters());
		getInstructions().addAll(other.getInstructions());
		getOutputs().addAll(other.getOutputs());
		getOptions().addAll(other.getOptions());
		getComponentConfigs().addAll(other.getComponentConfigs());
		
	}
	
	@XmlTransient
	public InstructionConfig getLastInstruction(){
		if(instructions!=null && instructions.size()>0){
			return instructions.get(instructions.size()-1);
		}
		return null;
	}
	

	@XmlElement(name="Adapter")
	public void setAdapters(List<AdapterConfig> adapters) {
		this.adapters = adapters;
	}

	@XmlElement(name="Instruction")
	public void setInstructions(List<InstructionConfig> instructions) {
		this.instructions = instructions;
	}

	public List<OutputConfig> getOutputs() {
		return outputs;
	}
	

	@XmlElement(name="Output")
	public void setOutputs(List<OutputConfig> reports) {
		this.outputs = reports;
	}
	
	public AdapterConfig createAdapter(String id){
		AdapterConfig ret = new AdapterConfig(id);
		adapters.add(ret);
		return ret;
	}

	public AdaptedTableConfig createSingleTableAdapter(String adapterId,String fromDatastoreid, String fromTable, String toTable){
		AdapterConfig adapter = createAdapter(adapterId);
		AdaptedTableConfig ret =  adapter.createTable(fromTable, toTable);
		ret.setFromDatastore(fromDatastoreid);
		return ret;
	
	}
	
//	public ComponentConfig createInstruction(String dataStoreId,String outputDatastoreId, String componentId, Serializable componentConfig){
//		InstructionConfig ret = new InstructionConfig(dataStoreId, outputDatastoreId,componentId, componentConfig);
//		instructions.add(ret);
//		return ret;
//	}	
//	
	public List<Option> getOptions(){
		return childOptions;
	}
	
	@XmlElement(name="Option")	
	public void setOptions(List<Option> childOptions){
		this.childOptions = childOptions;
	}

	public boolean isSynchronised() {
		return synchronised;
	}

	@XmlAttribute
	public void setSynchronised(boolean sychronised) {
		this.synchronised = sychronised;
	}

	public List<ComponentConfig> getComponentConfigs() {
		return componentConfig;
	}

	@XmlElement(name="ComponentConfig")		
	public void setComponentConfigs(List<ComponentConfig> componentConfig) {
		this.componentConfig = componentConfig;
	}

	public boolean isOverrideVisibleParameters() {
		return overrideVisibleParameters;
	}

	@XmlAttribute
	public void setOverrideVisibleParameters(boolean overrideVisibleParameters) {
		this.overrideVisibleParameters = overrideVisibleParameters;
	}

	public String getVisibleParametersOverride() {
		return visibleParametersOverride;
	}

	@XmlAttribute
	public void setVisibleParametersOverride(String visibleParametersOverride) {
		this.visibleParametersOverride = visibleParametersOverride;
	}

	public boolean isLaunchMultiple() {
		return launchMultiple;
	}

	@XmlAttribute
	public void setLaunchMultiple(boolean launchMultiple) {
		this.launchMultiple = launchMultiple;
	}

	public boolean isRefreshButtonAlwaysEnabled() {
		return refreshButtonAlwaysEnabled;
	}

	@XmlAttribute
	public void setRefreshButtonAlwaysEnabled(boolean refreshButtonAlwaysEnabled) {
		this.refreshButtonAlwaysEnabled = refreshButtonAlwaysEnabled;
	}

	public boolean isShowLastRefreshedTime() {
		return showLastRefreshedTime;
	}

	@XmlAttribute
	public void setShowLastRefreshedTime(boolean showLastRefreshedTime) {
		this.showLastRefreshedTime = showLastRefreshedTime;
	}


}
