/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.elements;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.opendoorlogistics.core.tables.utils.HasShortDescription;
import com.opendoorlogistics.core.utils.JAXBUtils;

public abstract class ScriptBaseElement implements Serializable, HasShortDescription{
	private boolean userCanEdit = true;
	private String editorNote;
	private String name;
	
	@Override
	public String toString(){
		return JAXBUtils.toXMLString(this);
	}

	public boolean isUserCanEdit() {
		return userCanEdit;
	}

	@XmlAttribute
	public void setUserCanEdit(boolean userCanEdit) {
		this.userCanEdit = userCanEdit;
	}

	public String getEditorLabel() {
		return editorNote;
	}

	@XmlElement(name="EditorLabel")
	public void setEditorLabel(String note) {
		this.editorNote = note;
	}
	

	public String getName() {
		return name;
	}

	@XmlAttribute(name="Name")
	public void setName(String name) {
		this.name = name;
	}
		
}
