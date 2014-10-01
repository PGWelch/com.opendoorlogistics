/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;

import java.util.EventListener;

public interface ODLListener extends EventListener {
   void tableChanged(int tableId, int firstRow, int lastRow);
   void datastoreStructureChanged();
   
   enum ODLListenerType{
	   /**
	    * Listener is fired when one or more the tables registered to this 
	    * listener are modified (data or structure)
	    */
	   TABLE_CHANGED,
	   
	   /**
	    * Fired when the datastore structure has changed, but not when the 
	    * data has changed
	    */
	   DATASTORE_STRUCTURE_CHANGED
   }
   
   ODLListenerType getType();
}
