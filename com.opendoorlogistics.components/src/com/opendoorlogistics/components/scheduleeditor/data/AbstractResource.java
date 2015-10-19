/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor.data;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;

public class AbstractResource extends BeanMappedRowExt {

	private String name;
	protected String id;

	public AbstractResource() {
		super();
	}

	public String getName() {
		return name;
	}

	@ODLColumnName(PredefinedTags.NAME)
	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	@ODLColumnName(PredefinedTags.ID)
	public void setId(String id) {
		this.id = id;
	}

}
