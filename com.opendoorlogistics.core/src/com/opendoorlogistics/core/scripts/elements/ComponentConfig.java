/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.elements;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.opendoorlogistics.api.components.ODLComponent;

@XmlRootElement(name = "ComponentConfig")
public class ComponentConfig extends ScriptBaseElementImpl {
	private String configId;
	private String component;
	private Serializable componentConfig;

	
	public ComponentConfig() {
		super();
	}

	public String getComponent() {
		return component;
	}

	@XmlAttribute
	public void setComponent(String componentId) {
		this.component = componentId;
	}

	/**
	 * Avoid using this method directly. Instead use the method in ScriptUtils
	 * as that checks if the component's configuration is actual a reference
	 * to data outside the class...
	 * @return
	 */
	public Serializable getComponentConfig() {
		return componentConfig;
	}

	@XmlTransient
	public void setComponentConfig(Serializable componentConfig) {
		this.componentConfig = componentConfig;
	}



	/**
	 * If this is a stand-alone component config (i.e. not in an instruction), this id uniquely
	 * identifies the configuration. If this is within an instruction, this id identifies an
	 * external configuration.
	 * @return
	 */
	public String getConfigId() {
		return configId;
	}

	@XmlAttribute	
	public void setConfigId(String configId) {
		this.configId = configId;
	}

	@Override
	public String getShortDescription() {
		return "Configuration data for a component";
	}

	
}
