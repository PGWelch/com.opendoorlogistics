/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.memory;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.utils.HasShortDescription;
import com.opendoorlogistics.core.tables.utils.TableUtils;


@XmlRootElement(name = "TableDefinition")
@XmlAccessorType(XmlAccessType.FIELD)
public class ODLTableDefinitionImpl extends ODLAbstractTableDefinition<ODLColumnDefinition> implements ODLTableDefinitionAlterable, Serializable, HasShortDescription{
	
	public ODLTableDefinitionImpl(int id, String name){
		super(id, name);
		initFlags();
	}
	
	public ODLTableDefinitionImpl(ODLTableDefinitionImpl copyThis){
		super(copyThis);
	}
	
	/**
	 * No-args constructor is used by JAXB
	 */
	public ODLTableDefinitionImpl(){
		initFlags();
	}
	
	private void initFlags(){
		TableUtils.addTableFlags(this, TableFlags.UI_EDIT_PERMISSION_FLAGS);
	}
	
	@Override
	protected ODLColumnDefinition createColObj(int id, String name, ODLColumnType type, long flags) {
		id = validateNewColumnId(id);	
		return new ODLColumnDefinition(id,name,type,flags);
	}


	@Override
	public synchronized String getShortDescription() {
		return TableUtils.getShortTableDescription(this);
	}

	/**
	 * Used by JAXB
	 * @param columns
	 */
	@XmlElement(name = "Column")	
	void setColumns(List<ODLColumnDefinition> columns){
		this.columns = columns;
	}
	
	List<ODLColumnDefinition> getColumns(){
		return columns;
	}

	@Override
	public synchronized Object getColumnDefaultValue(int col) {
		if(col>=columns.size()){
			return null;
		}
		
		return columns.get(col).getDefaultValue();
	}

	@Override
	public synchronized void setColumnDefaultValue(int col, Object value){
		if(col>=columns.size()){
			return;
		}
		columns.get(col).setDefaultValue(value);
	}

	@Override
	public ODLTableDefinition deepCopyWithShallowValueCopy() {
		throw new UnsupportedOperationException();
	}

}
