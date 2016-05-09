/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;


public class VRPUtils {

	/**
	 * Is the number an OK quantity or duration
	 * @param d
	 * @return
	 */
	public static boolean isOkQuantity(Long l){
		if(l==null){
			return true;
		}
		
		return l>=0 && l<=Integer.MAX_VALUE;
	}
}
