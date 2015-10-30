/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.beans;

import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.annotations.ODLIgnore;

public class BeanMappedRowImpl implements BeanMappedRow{
	private long globalRowId;

	@Override
	@ODLIgnore
	public long getGlobalRowId() {
		return globalRowId;
	}

	@Override
	@ODLIgnore
	public void setGlobalRowId(long globalRowId) {
		this.globalRowId = globalRowId;
	}
	
	
}
