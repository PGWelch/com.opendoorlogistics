/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables;

import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.memory.ODLTableImpl;



public class ODLFactory {
	/**
	 * Create an empty datastore where the structure can be modified
	 * @return
	 */
	public static ODLDatastoreAlterable<ODLTableDefinitionAlterable> createDefinition(){
		ODLDatastoreImpl<ODLTableDefinitionAlterable> ret = new ODLDatastoreImpl<>(
				ODLTableImpl.ODLTableDefinitionAlterableFactory);
		return ret;
	}
	
	public static ODLDatastoreAlterable<ODLTableAlterable> createAlterable(){
		ODLDatastoreImpl<ODLTableAlterable> ret = new ODLDatastoreImpl<>(
				ODLTableImpl.ODLTableAlterableFactory);
		return ret;
	}
	
	public static ODLTableAlterable createAlterableTable(String tablename){
		return createAlterable().createTable(tablename, -1);
	}
	
	

	
//	/**
//	 * Shallow copy the tables in the database to another database,
//	 * casting them to the return type
//	 * @param from
//	 * @return
//	 */
//	public static <TFrom extends ODLTableDefinition>
//		ODLDatastore<ODLTableDefinition> castCopyToODLTableDefinition(ODLDatastore<TFrom> from){
//			return TableUtils.castCopyToODLTableDefinition(from);
//		
//	}

}
