/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables;

public interface ODLRow extends ODLRowReadOnly {
	void add(Object o);
	
	void add(int indx, Object o);
	
	void remove(int indx);

	void set(int indx, Object obj);

}
