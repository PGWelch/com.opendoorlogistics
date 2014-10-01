/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables;

import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

public interface ODLDatastoreUndoable<T extends ODLTableDefinition> extends ODLDatastoreAlterable<T>, Undoable{

	public interface UndoStateChangedListener<T extends ODLTableDefinition>{
		void undoStateChanged( ODLDatastoreUndoable<T>datastoreUndoable );
	}
	
	void addUndoStateListener(UndoStateChangedListener<T> listener);

	void removeUndoStateListener(UndoStateChangedListener<T> listener);

}
