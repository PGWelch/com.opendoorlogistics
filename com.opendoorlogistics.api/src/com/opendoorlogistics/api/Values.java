/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api;

import com.opendoorlogistics.api.tables.ODLColumnType;

/**
 * API dealing with operations on single values
 * @author Phil
 *
 */
public interface Values {
	Object convertValue(Object value,ODLColumnType from, ODLColumnType to );
	Object convertValue(Object value,ODLColumnType to );
	String canonicalStringRepresentation(Object value);
//	return (String)ColumnValueProcessor.convertToMe(ODLColumnType.STRING,table.getValueAt(row, col),table.getColumnType(col));
	
	boolean equalsStandardised(String a, String b);
	String standardise(String s);
	
	/**
	 * We currently use long for booleans, so this method encapsulates the convention
	 * @param l
	 * @return true if l ==1
	 */
	boolean isTrue(long l);
}
