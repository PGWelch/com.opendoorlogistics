/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor.data.beans;

import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.components.scheduleeditor.data.AbstractResource;

@ODLTableName(ResourceType.TABLE_NAME)
public class ResourceType extends AbstractResource{
	public final static String TABLE_NAME = "ResourceTypes";

	private long number=1;

	public long getNumber() {
		return number;
	}
	
	@ODLColumnName("resource-count")
	public void setNumber(long number) {
		this.number = number;
	}
	
}
