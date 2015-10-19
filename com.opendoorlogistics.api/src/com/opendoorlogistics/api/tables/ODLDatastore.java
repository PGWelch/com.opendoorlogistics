/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;

import java.io.Serializable;

import com.opendoorlogistics.api.tables.ODLTableDefinition;


public interface ODLDatastore <T extends ODLTableDefinition> extends ODLHasTables<T>, Serializable, ODLHasListeners, SupportsTransactions,HasFlags, FlagSetter{
	T getTableByImmutableId(int id);

	public static final int FLAG_FILE_CREATED_BY_ODL = 1<<0;
		
	public interface ODLDatastoreFactory<T extends ODLTableDefinition>{
		ODLDatastore<T> create();
	}

	/**
	 * Deep copy the datastore. 
	 * @param createLazyCopy
	 * @return Do a lazy copy which avoids deep copying the tables until needed.
	 * Laxy copies have restrictions - e.g. tables cannot be renamed, cannot be deep copied themselves
	 */
	ODLDatastoreAlterable<? extends T> deepCopyWithShallowValueCopy(boolean createLazyCopy);
}
