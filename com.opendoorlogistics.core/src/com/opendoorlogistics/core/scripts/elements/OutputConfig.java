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

import com.opendoorlogistics.api.scripts.ScriptOption.OutputType;
import com.opendoorlogistics.core.utils.strings.Strings;

@XmlRootElement(name = "Output")
final public class OutputConfig extends ScriptBaseElementImpl implements Serializable{
	private String inputDatastore;
	private String inputTable;	
	private OutputType type = OutputType.REPLACE_CONTENTS_OF_EXISTING_TABLE;
	private String destinationTable = "";

	public String getDatastore() {
		return inputDatastore;
	}

	@XmlAttribute
	public void setDatastore(String datastore) {
		this.inputDatastore = datastore;
	}

	public OutputType getType() {
		return type;
	}

	@XmlAttribute
	public void setType(OutputType type) {
		this.type = type;
	}

	public String getDestinationTable() {
		return destinationTable;
	}

	@XmlAttribute
	public void setDestinationTable(String destinationTable) {
		this.destinationTable = destinationTable;
	}

	@Override
	public String getShortDescription() {
		return Strings.convertEnumToDisplayFriendly(getType().name());
	}

	public String getInputTable() {
		return inputTable;
	}

	@XmlAttribute	
	public void setInputTable(String inputTable) {
		this.inputTable = inputTable;
	}
	

}
