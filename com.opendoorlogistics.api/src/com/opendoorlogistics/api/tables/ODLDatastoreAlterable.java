/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

public interface ODLDatastoreAlterable<T extends ODLTableDefinition> extends ODLDatastore<T>, TableDeleter,TableNameSetter {
	/**
	 * Create a new table
	 * @param tablename
	 * @param id if -1 a unique id will be assigned. Creation will fail if id is non-unique
	 * @return null if the table wasn't created (usually if the name is not unique).
	 */
	T createTable(String tablename, int id);
	
	public interface ODLDatastoreAlterableFactory<T extends ODLTableAlterable>{
		ODLDatastoreAlterable<T > create();
	}
}
