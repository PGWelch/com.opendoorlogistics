/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

/**
 * Helper functions for when we store two ints in a single long
 * @author Phil
 *
 */
final public class Long2Ints {
	private Long2Ints(){}
	
	public static int getFirst(long combined){
		long high =  (0xFFFFFFFF00000000L & combined);
		high = high >> 32;
		return (int)high;
	}

	public static int getSecond(long combined){
		long extractLow = (0x00000000FFFFFFFFL & combined);
		return (int)extractLow;	
	}

	public static long get(int first, int second){
		return (((long) first) << 32) + second;		
	}
	
	
}
