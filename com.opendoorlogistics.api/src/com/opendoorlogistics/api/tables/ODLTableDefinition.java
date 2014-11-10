/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;

import java.io.Serializable;
import java.util.Set;

public interface ODLTableDefinition extends Serializable,HasTags, HasFlags{

	int getColumnCount();

	int getColumnImmutableId(int col);
	
	String getColumnDescription(int col);
	
	long getColumnFlags(int col);

	Object getColumnDefaultValue(int col);
	
	String getColumnName(int col);
	
//	ODLTableDefinition deepCopyDataOnly();

	/**
	 * Returns a read only version of the column's tags that will throw an exception if any modification is attempted.
	 * @param col
	 * @return
	 */
	Set<String> getColumnTags(int col);
	
	ODLColumnType getColumnType(int i);
	

	/**
	 * The id is unique and immutable; it is created when the table
	 * is created and never changes, even if other lower index tables are deleted 
	 * in the datastore.
	 * @return
	 */
	int getImmutableId();
	
	String getName();

	void setColumnDescription(int col, String description);
	
	ODLTableDefinition deepCopyWithShallowValueCopy();
}
