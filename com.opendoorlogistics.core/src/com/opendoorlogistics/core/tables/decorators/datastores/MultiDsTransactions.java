/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

/**
 * Helper class for using transactions with multiple datastores
 * @author Phil
 *
 * @param <T>
 */
public class MultiDsTransactions<T extends ODLTableDefinition>  {
	void rollbackTransaction(Iterable<ODLDatastore<? extends T>> stores) {
		for(ODLDatastore<? extends T> ds:stores){
			if(ds.isInTransaction()){
				ds.rollbackTransaction();
			}
		}
	}

	boolean isRollbackSupported(Iterable<ODLDatastore<? extends T>> stores) {
		for(ODLDatastore<? extends T> ds:stores){
			if(ds.isRollbackSupported()==false){
				return false;
			}
		}
		return true;
	}

	
	boolean isInTransaction(Iterable<ODLDatastore<? extends T>> stores) {
		for(ODLDatastore<? extends T> ds:stores){
			if(ds.isInTransaction()){
				return true;
			}
		}
		return false;
	}
	
	void startTransaction(Iterable<ODLDatastore<? extends T>> stores) {
		for(ODLDatastore<? extends T> ds:stores){
			if(!ds.isInTransaction()){
				ds.startTransaction();
			}
		}
	}
	
	void endTransaction(Iterable<ODLDatastore<? extends T>> stores) {
		for(ODLDatastore<? extends T> ds:stores){
			if(ds.isInTransaction()){
				ds.endTransaction();
			}
		}
	}
}
