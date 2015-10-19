/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor.data.beans;

import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;

@ODLTableName(ResourceDescription.TABLE_NAME)
public class ResourceDescription extends BeanMappedRowImpl {
	public final static String TABLE_NAME = "ResourceDescriptions";
	private String id;
	private String description;
	
	public String getResourceId() {
		return id;
	}
	
	@ODLColumnName("resource-id")
	public void setResourceId(String id) {
		this.id = id;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
}

	
