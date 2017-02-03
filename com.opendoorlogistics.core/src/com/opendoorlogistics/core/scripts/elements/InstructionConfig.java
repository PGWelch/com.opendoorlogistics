/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.elements;

import java.io.Serializable;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.utils.strings.Strings;

@XmlRootElement(name="Instruction")
final public class InstructionConfig extends ComponentConfig implements Serializable{
	private String datastore = ScriptConstants.EXTERNAL_DS_NAME;
	private String outputDatastore;
	private String uuid = UUID.randomUUID().toString();
	private String reportTopLabelFormula;
	private int executionMode = ODLComponent.MODE_DEFAULT;

	public InstructionConfig(){}
	
	public InstructionConfig(String dataStoreId,String outputDatastoreId, String componentId, Serializable componentConfig) {
		this.datastore = dataStoreId;
		this.outputDatastore = outputDatastoreId;
		setComponent(componentId);
		setComponentConfig(componentConfig);
	}

	
	
	//@XmlJavaTypeAdapter(MyMapAdapter.class)
	
	public String getDatastore() {
		return datastore;
	}
	
	@XmlAttribute
	public void setDatastore(String inputDataStoreId) {
		this.datastore = inputDataStoreId;
	}
	

	public String getOutputDatastore() {
		return outputDatastore;
	}

	@XmlAttribute
	public void setOutputDatastore(String outputDataStoreId) {
		this.outputDatastore = outputDataStoreId;
	}

	

	@Override
	public String getShortDescription() {
		String componentName = getComponentDisplayName();
		
		return "Run '" + componentName + "' on datastore '" + datastore +"'"+ (Strings.isEmpty(outputDatastore)?"":" outputting datastore '" + outputDatastore + "'");
	}

	public String getComponentDisplayName() {
		String componentName = "<unknown component>";
		if(Strings.isEmpty(getComponent())==false){
			componentName = getComponent();
			ODLComponent componentObj = ODLGlobalComponents.getProvider().getComponent(getComponent());
			if(componentObj!=null){
				componentName = componentObj.getName();
			}
		}
		return componentName;
	}

	
	public String getUuid() {
		return uuid;
	}

	@XmlAttribute
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	

	public int getExecutionMode() {
		return executionMode;
	}

	@XmlAttribute
	public void setExecutionMode(int mode) {
		this.executionMode = mode;
	}

	public String getReportTopLabelFormula() {
		return reportTopLabelFormula;
	}

	public void setReportTopLabelFormula(String reportTopLabelFormula) {
		this.reportTopLabelFormula = reportTopLabelFormula;
	}

	
}
