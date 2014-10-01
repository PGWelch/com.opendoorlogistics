/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.adapters;

import com.opendoorlogistics.api.tables.ODLColumnType;

public interface QueryAvailableData extends QueryAvailableTables{
	String[] queryAvailableFields(String datastore, String tablename);
	String[] queryAvailableFormula(ODLColumnType columnType);
//	ODLDatastore<? extends ODLTableDefinition> getDatastoreDefinition(String datastore);
}
