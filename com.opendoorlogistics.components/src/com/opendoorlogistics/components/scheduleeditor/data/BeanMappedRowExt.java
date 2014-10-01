/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor.data;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;

public class BeanMappedRowExt extends BeanMappedRowImpl{
	private ODLTableReadOnly table;

	public void setTable(ODLTableReadOnly table) {
		this.table = table;
	}

	public Object getValue(int col){
		return table.getValueById(getGlobalRowId(), col);
	}
}
