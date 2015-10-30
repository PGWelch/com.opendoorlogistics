/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLFlatDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.tables.decorators.tables.FlatDs2TableObject;
import com.opendoorlogistics.core.tables.utils.TableUtils;

/**
 * A decorator which defines a table decorator where all calls to table methods are
 * redirected to the datastore decorator instance. This class is the base class for many others.
 * @author Phil
 *
 * @param <T>
 */
public abstract class AbstractDecorator<T extends ODLTableDefinition> implements ODLDatastoreAlterable<T>, ODLFlatDatastore {
	private final FlatDs2TableObject.Flat2DsTableCache tableCache = new FlatDs2TableObject.Flat2DsTableCache(this);

	@SuppressWarnings("unchecked")
	@Override
	public T getTableByImmutableId(int tableId) {
		return (T)tableCache.getTable(tableId);
	}

	
	@Override
	public String toString(){
		return TableUtils.convertToString(this);
	}
}
