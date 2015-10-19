/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor.data.beans;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.components.scheduleeditor.data.BeanMappedRowExt;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;

@ODLTableName(Task.TABLE_NAME)
public class Task extends BeanMappedRowExt{
	public final static String TABLE_NAME = "Tasks";

	private String id;
	private String name;
//	private String address;

//	public EditorStop deepCopy(){
//		EditorStop ret = new EditorStop();
//		ret.setGlobalRowId(getGlobalRowId());
//		ret.setId(getId());
//		ret.setName(getName());
//		//ret.setAddress(getAddress());
//		return ret;
//	}
	
	public String getId() {
		return id;
	}
	
	@ODLColumnName(PredefinedTags.ID)
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString(){
		return id;
	}
	
	public static StandardisedStringSet toTaskIds(Iterable<Task> stops){
		StandardisedStringSet ret = new StandardisedStringSet(true);
		for(Task stop:stops){
			ret.add(stop.getId());
		}
		return ret;
	}

	public String getName() {
		return name;
	}

	@ODLColumnName(PredefinedTags.NAME)
	public void setName(String name) {
		this.name = name;
	}

//	public String getAddress() {
//		return address;
//	}
//
//	@ODLNullAllowed
//	@ODLColumnName(PredefinedTags.ADDRESS)
//	public void setAddress(String address) {
//		this.address = address;
//	}
//	
	
}
