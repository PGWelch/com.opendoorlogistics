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

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.tables.memory.ODLColumnDefinition;
import com.opendoorlogistics.core.utils.JAXBUtils;

@XmlRootElement(name = "AdaptedColumn")
final public class AdapterColumnConfig extends ODLColumnDefinition implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6969365768727995610L;
	private String from;
	private String formula;
	private boolean useFormula;
	private SortField sortField = SortField.NO;
	
	public enum SortField{
		NO,
		ASCENDING,
		DESCENDING
	}
	
	public AdapterColumnConfig(){
	}
	
	public AdapterColumnConfig(AdapterColumnConfig copyThis, int newImmutableId){
		super(copyThis, newImmutableId);
		this.from = copyThis.getFrom();
		this.formula = copyThis.getFormula();
		this.useFormula = copyThis.isUseFormula();
		this.sortField = copyThis.sortField;
	}
	
	public AdapterColumnConfig(int id,String from, String to, ODLColumnType toType, long toFlags) {
		super(id,to, toType, toFlags);
		// if TO is null take the from
		if(to==null){
			to = from;
		}
		
		this.from = from;

	}

	public String getFrom() {
		return from;
	}

	@XmlAttribute
	public void setFrom(String from) {
		this.from = from;
	}

	@Override
	public String toString() {
		return JAXBUtils.toXMLString(this);
	}


	public String getFormula() {
		return formula;
	}

	@XmlAttribute
	public void setFormula(String formula) {
		this.formula = formula;
	}

	public boolean isUseFormula() {
		return useFormula;
	}

	@XmlAttribute
	public void setUseFormula(boolean useFormula) {
		this.useFormula = useFormula;
	}

	public void setSourceFields(String from,String formula,boolean useFormula){
		this.from = from;
		this.formula = formula;
		this.useFormula = useFormula;
	}

	public SortField getSortField() {
		return sortField;
	}

	@XmlAttribute	
	public void setSortField(SortField sortField) {
		this.sortField = sortField;
	}
	
}
