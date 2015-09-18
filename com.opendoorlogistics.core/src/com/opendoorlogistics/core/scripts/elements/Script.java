/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.utils.Version;

@XmlRootElement(name=ScriptConstants.SCRIPT_XML_NODE_NAME)
final public class Script extends Option implements Serializable{
	private Version version = new Version(ScriptConstants.SCRIPT_VERSION_MAJOR, ScriptConstants.SCRIPT_VERSION_MINOR, ScriptConstants.SCRIPT_VERSION_REVISION);
	private ScriptEditorType scriptEditorUIType = ScriptEditorType.WIZARD_GENERATED_EDITOR;
	private String createdByComponentId;
	private List<UserFormula> userFormulae = new ArrayList<UserFormula>();

	
	@XmlTransient
	private UUID uuid = UUID.randomUUID();
	

	public Script() {
		setOptionId("root");
	}
	
	public Version getVersion() {
		return version;
	}

	@XmlElement(name = "ScriptFormatVersion")
	public void setVersion(Version version) {
		this.version = version;
	}

	public UUID getUuid() {
		return uuid;
	}

	@XmlTransient
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public ScriptEditorType getScriptEditorUIType() {
		return scriptEditorUIType;
	}


	@XmlAttribute(name="ScriptEditorType")
	public void setScriptEditorUIType(ScriptEditorType scriptType) {
		this.scriptEditorUIType = scriptType;
	}

	public String getCreatedByComponentId() {
		return createdByComponentId;
	}

	@XmlElement(name = "CreatedByComponent")
	public void setCreatedByComponentId(String createdByComponentId) {
		this.createdByComponentId = createdByComponentId;
	}

	public List<UserFormula> getUserFormulae() {
		return userFormulae;
	}

	@XmlElement(name = "UserFormulae")	
	public void setUserFormulae(List<UserFormula> userFormulae) {
		this.userFormulae = userFormulae;
	}


	

//	public TemplateConfig getTemplate() {
//		return template;
//	}
//
//	@XmlElement(name = "Template")
//	public void setTemplate(TemplateConfig template) {
//		this.template = template;
//	}
//	
	
	
}
