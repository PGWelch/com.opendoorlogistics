/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;


public class ArrayUtils {
	/**
	 * Check if the input array has the input object, using the ==
	 * operator (i.e. must be the same object).
	 * @param searchIn
	 * @param findValue
	 * @return
	 */
	public static <T> boolean contains(T[] searchIn , T findValue){
		for(T check:searchIn){
			if(check == findValue){
				return true;
			}
		}
		return false;
	}
}
