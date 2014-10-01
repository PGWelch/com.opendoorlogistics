/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import java.util.HashMap;
import java.util.Map;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * Calculates modal average using the canonical string representation of a value.
 * Modal average is returned as a string.
 * @author Phil
 *
 */
class CalculateStringModalAverage {
	private HashMap<String, Long> countByStringValue = new HashMap<>();
	private HashMap<String, Object> firstOriginalValue = new HashMap<>();
	
	void addEntry(Object otherVal) {
		// get canonical string value - null will give empty string
		String s = "";
		if(otherVal!=null){
			Object conv = ColumnValueProcessor.convertToMe(ODLColumnType.STRING, otherVal);
			if(conv!=null){
				s = conv.toString();
			}
		}
		s = Strings.std(s);
		
		// save the first original value encountered for the string
		if(!firstOriginalValue.containsKey(s)){
			firstOriginalValue.put(s, otherVal);
		}
		
		Long countForValue = countByStringValue.get(s);
		if(countForValue==null){
			countForValue = 0L;
		}
		countForValue++;
		countByStringValue.put(s, countForValue);
	}
	
	Object getModalAverage() {
		String maxValue = null;
		long maxCount=-1;
		for(Map.Entry<String,Long> entry: countByStringValue.entrySet()){
			if(entry.getValue()>maxCount){
				maxCount =entry.getValue();
				maxValue = entry.getKey();
			}
		}
		
		if(maxValue!=null){
			// return the non-standardised input value
			return firstOriginalValue.get(maxValue);
		}
		
		return null;
	}

}
