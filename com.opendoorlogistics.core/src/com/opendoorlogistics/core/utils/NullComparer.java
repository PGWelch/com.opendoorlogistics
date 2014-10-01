/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

/**
 * Compare the null state only of two objects
 * @author Phil
 *
 */
public class NullComparer {
	public static int compare(Object a, Object b){
		boolean aNull = a==null;
		boolean bNull = b==null;
		return Boolean.compare(aNull, bNull);
	}
}
