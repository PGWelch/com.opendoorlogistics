/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.memory;

import com.opendoorlogistics.api.tables.ODLColumnType;

final public class ODLIndexableColumn extends ODLColumnDefinition {
	final ColumnIndex index = new ColumnIndex();
	
	public ODLIndexableColumn(int id, String name, ODLColumnType type, long flags) {
		super(id, name, type, flags);
	}

	private ODLIndexableColumn(ODLColumnDefinition copyThis) {
		super(copyThis);
	}

	@Override
	public ODLColumnDefinition deepCopy() {
		return new ODLIndexableColumn(this);
	}

}
