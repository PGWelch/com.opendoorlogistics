/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;

import java.io.Serializable;

import com.opendoorlogistics.api.tables.ODLTableDefinition;


public interface ODLDatastore <T extends ODLTableDefinition> extends ODLHasTables<T>, Serializable, ODLHasListeners, SupportsTransactions,HasFlags{
	T getTableByImmutableId(int id);

	public static final int FLAG_FILE_CREATED_BY_ODL = 1<<0;
	
	void setFlags(long flags);
	
	public interface ODLDatastoreFactory<T extends ODLTableDefinition>{
		ODLDatastore<T> create();
	}

	ODLDatastore<T> deepCopyDataOnly();
}
