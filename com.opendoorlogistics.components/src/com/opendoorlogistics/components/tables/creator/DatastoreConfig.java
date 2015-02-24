/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.tables.creator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLHasTables;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.scripts.elements.ScriptBaseElementImpl;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.memory.ODLTableDefinitionImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.HasStringId;

@XmlRootElement(name = "Tables")
final public class DatastoreConfig extends ScriptBaseElementImpl implements Serializable, ODLHasTables<ODLTableDefinition>{
//	private String id;
	private List<ODLTableDefinitionImpl> tables = new ArrayList<>();
	
	public DatastoreConfig(){}
	
//	public DatastoreConfig(String id) {
//		this.id = id;
//
//	}

//	@Override
//	public String getId() {
//		return id;
//	}
//	
//	@XmlAttribute
//	public void setId(String id) {
//		this.id = id;
//	}
	
	public ODLDatastore<? extends ODLTableDefinition> createDefinition() {
		ODLDatastoreAlterable<ODLTableAlterable> tmp = ODLDatastoreImpl.alterableFactory.create();
		if(!DatastoreCopier.copyTableDefinitions(tables, tmp)){
			return null;
		}
		return tmp;
	}
	
	public List<ODLTableDefinitionImpl> getTables() {
		return tables;
	}

	@XmlElement(name = "Table")		
	public void setTables(List<ODLTableDefinitionImpl> tables) {
		this.tables = tables;
	}

	@Override
	public String getShortDescription() {
		StringBuilder ret = new StringBuilder();
		ret.append("Datastore '" + "'");
		if(getTables().size()>0){
			ret.append(", tables ");
			for(int i =0 ; i < getTables().size(); i++){
				if(i>0){
					ret.append(", ");
				}
				ret.append("'" + getTables().get(i).getName() + "'");
			}
		}
		return ret.toString();
	}

	@Override
	public int getTableCount() {
		return tables.size();
	}

	@Override
	public ODLTableDefinition getTableAt(int i) {
		return tables.get(i);
	}

	
}
