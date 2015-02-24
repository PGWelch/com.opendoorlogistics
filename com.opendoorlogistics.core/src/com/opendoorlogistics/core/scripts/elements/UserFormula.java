package com.opendoorlogistics.core.scripts.elements;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.opendoorlogistics.core.scripts.ScriptConstants;

@XmlRootElement(name="UserFormulae")
public class UserFormula implements ScriptBaseElement{
	private String value = new String();
	
	public UserFormula() {
	}
	
	/**
	 * Copy constructor
	 * @param copyThis
	 */
	public UserFormula(UserFormula copyThis){
		this.value = copyThis.value;
	}
	
	public UserFormula(String value){
		this.value = value;
	}
	
	@Override
	public String getShortDescription() {
		// Return the value rather than "user defined formula" or similar as the formulae
		// box control appears to read getShortDescription rather than toString (and the
		// formula value need to be shown in here)
		return value; 
	}

	public String getValue() {
		return value;
	}

	@XmlValue
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString(){
		return value;
	}

	@Override
	public boolean isUserCanEdit() {
		return true;
	}

	@XmlAttribute
	@Override
	public void setUserCanEdit(boolean userCanEdit) {
	}

	@Override
	public String getEditorLabel() {
		return null;
	}

	@XmlAttribute
	@Override
	public void setEditorLabel(String note) {
	}

	@Override
	public String getName() {
		return null;
	}

	@XmlAttribute
	@Override
	public void setName(String name) {
	}
	
}
