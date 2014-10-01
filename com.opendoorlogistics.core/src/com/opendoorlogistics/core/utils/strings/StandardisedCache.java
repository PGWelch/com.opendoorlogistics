/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.strings;

import java.util.HashMap;

final public class StandardisedCache {
	private final HashMap<String, String> standardised = new HashMap<>();
	
	public String std(String s){
		String ret = standardised.get(s);
		if(ret==null){
			ret = Strings.std(s);
			standardised.put(s, ret);
		}
		return ret;
	}
}
