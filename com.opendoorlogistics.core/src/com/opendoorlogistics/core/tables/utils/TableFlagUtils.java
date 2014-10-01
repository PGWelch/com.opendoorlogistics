/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.utils;

final public class TableFlagUtils {
	private TableFlagUtils(){}
	
	public static long addFlags(long current , long flagsToAdd){
		return current | flagsToAdd;
	}
	
	public static long removeFlags(long current , long flagsToRemove){
		return current & (~flagsToRemove);
	}
	
	public static boolean hasFlag(long currentFlags, long flagToCheckFor){
		return (currentFlags & flagToCheckFor)==flagToCheckFor;
	}
	
	public static long setFlag(long currentFlags, long flagToSet, boolean on){
		if(on){
			return addFlags(currentFlags, flagToSet);
		}else{
			return removeFlags(currentFlags, flagToSet);
		}
	}
}
