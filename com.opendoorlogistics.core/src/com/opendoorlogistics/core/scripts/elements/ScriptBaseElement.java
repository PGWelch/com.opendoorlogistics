package com.opendoorlogistics.core.scripts.elements;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.opendoorlogistics.core.tables.utils.HasShortDescription;

public interface ScriptBaseElement extends Serializable, HasShortDescription{

	boolean isUserCanEdit();

	void setUserCanEdit(boolean userCanEdit);

	String getEditorLabel();

	void setEditorLabel(String note);

	String getName();

	void setName(String name);

}