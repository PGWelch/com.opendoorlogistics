/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl;

import com.opendoorlogistics.api.Values;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.strings.Strings;

public class ValuesImpl implements Values{
	
	@Override
	public Object convertValue(Object value, ODLColumnType from, ODLColumnType to) {
		return ColumnValueProcessor.convertToMe(to,value, from);
	}

	@Override
	public Object convertValue(Object value, ODLColumnType to) {
		return ColumnValueProcessor.convertToMe(to,value);
	}

	@Override
	public boolean equalsStandardised(String a, String b) {
		return Strings.equalsStd(a, b);
	}

	@Override
	public String standardise(String s) {
		return Strings.std(s);
	}

	@Override
	public String canonicalStringRepresentation(Object value) {
		return (String)ColumnValueProcessor.convertToMe(ODLColumnType.STRING, value);
	}

	@Override
	public boolean isTrue(long l) {
		return l==1;
	}

}
