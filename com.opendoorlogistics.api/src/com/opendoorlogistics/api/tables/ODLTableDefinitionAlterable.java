/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;

import java.util.Set;

public interface ODLTableDefinitionAlterable extends ODLTableDefinition{
	 int addColumn(int id, String name, ODLColumnType type, long flags);
	 void setFlags(long flags);
	 void setTags(Set<String> tags);
	 void setColumnTags(int col, Set<String> tags);
	 void setColumnFlags(int col,long flags);
	 void setColumnDefaultValue(int col, Object value);
	 void deleteColumn(int col);
	 boolean insertColumn(int id,int col,String name, ODLColumnType type, long flags, boolean allowDuplicateNames);
}
