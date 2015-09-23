package com.opendoorlogistics.core.scripts.elements;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Row")
public class EmbeddedDataRow {
	private ArrayList<String> values;

	public EmbeddedDataRow(){
		values = new ArrayList<>();
	}
	
	public EmbeddedDataRow(int initialCapacity){
		values = new ArrayList<>(initialCapacity);
	}
	
	public EmbeddedDataRow(Collection<String> values){
		if(values!=null){
			this.values = new ArrayList<>(values);			
		}
		else{
			this.values = new ArrayList<>();
		}
	}
	
	public ArrayList<String> getValues() {
		return values;
	}

	@XmlElement
	public void setValues(ArrayList<String> values) {
		this.values = values;
	}
	
	
}
