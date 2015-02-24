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

public abstract class ScriptBaseElementImpl implements ScriptBaseElement{
	private boolean userCanEdit = true;
	private String editorNote;
	private String name;
	
	@Override
	public String toString(){
		return JAXBUtils.toXMLString(this);
	}

	/* (non-Javadoc)
	 * @see com.opendoorlogistics.core.scripts.elements.ScriptBaseElement#isUserCanEdit()
	 */
	@Override
	public boolean isUserCanEdit() {
		return userCanEdit;
	}

	/* (non-Javadoc)
	 * @see com.opendoorlogistics.core.scripts.elements.ScriptBaseElement#setUserCanEdit(boolean)
	 */
	@Override
	@XmlAttribute
	public void setUserCanEdit(boolean userCanEdit) {
		this.userCanEdit = userCanEdit;
	}

	/* (non-Javadoc)
	 * @see com.opendoorlogistics.core.scripts.elements.ScriptBaseElement#getEditorLabel()
	 */
	@Override
	public String getEditorLabel() {
		return editorNote;
	}

	/* (non-Javadoc)
	 * @see com.opendoorlogistics.core.scripts.elements.ScriptBaseElement#setEditorLabel(java.lang.String)
	 */
	@Override
	@XmlElement(name="EditorLabel")
	public void setEditorLabel(String note) {
		this.editorNote = note;
	}
	

	/* (non-Javadoc)
	 * @see com.opendoorlogistics.core.scripts.elements.ScriptBaseElement#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.opendoorlogistics.core.scripts.elements.ScriptBaseElement#setName(java.lang.String)
	 */
	@Override
	@XmlAttribute(name="Name")
	public void setName(String name) {
		this.name = name;
	}
		
}
