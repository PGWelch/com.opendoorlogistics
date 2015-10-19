/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor.data;

import com.opendoorlogistics.api.tables.beans.annotations.ODLIgnore;

public class Resource extends AbstractResource{
	
	private String description;
	
	@Override
	public String toString(){
		return id;
	}

	public String getDescription() {
		return description;
	}

	@ODLIgnore
	public void setDescription(String description) {
		this.description = description;
	}
	
	
}
