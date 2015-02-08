/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.formulae;

import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.scripts.formulae.FmLocalElement;

public final class FunctionUtils {
	/**
	 * Extract the constant string from the formula. The method will return null
	 * if the formula does not contain a constant string.
	 * 
	 * @param formula
	 * @return
	 */
	public static String getConstantString(Function formula) {
		String ret = null;
		if (formula != null) {
			if (FmConst.class.isInstance(formula)) {
				FmConst cnst = (FmConst) formula;
				// allow a non-string object to be treated as a const string (e.g. '1')
				if (cnst.value() != null) {
					ret = cnst.value().toString();
				}
			} else if (FmLocalElement.class.isInstance(formula)) {
				// We can have a local variable with the same name as remote one... just grab the name
				ret = ((FmLocalElement) formula).getName();
			}
		}

		return ret;
	}

	public static Function [] toSingleArray(Function a, Function ...arr){
		Function [] ret = new Function[arr.length+1];
		ret[0] = a;
		for(int i =0 ; i < arr.length ; i++){
			ret[i+1] = arr[i];
		}
		return ret;
	}
	
}
