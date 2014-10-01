/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.utils;

/**
 * Defines a column that's being sorted
 * @author Phil
 *
 */
final public class SortColumn {
	private final int indx;
	private final boolean ascending;
	
	public SortColumn(int indx, boolean ascending) {
		super();
		this.indx = indx;
		this.ascending = ascending;
	}
	
	public int getIndx() {
		return indx;
	}
	public boolean isAscending() {
		return ascending;
	}
	
}
