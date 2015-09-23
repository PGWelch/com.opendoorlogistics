/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.tables.io;

import java.util.Arrays;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.strings.Strings;

class ColumnTypeEstimator {
	private int nbNonEmptyVals = 0;
	private boolean[] okByType = new boolean[ODLColumnType.values().length];
	
	ColumnTypeEstimator() {
		Arrays.fill(okByType, true);
		okByType[ODLColumnType.STRING.ordinal()] = false; // disable string as we select it by default
		
		// assume that nobody ever tries to save images to the datastore as checking if something as an image
		// is very slow - it actually creates a temporary file
		okByType[ODLColumnType.IMAGE.ordinal()]=false;
		
		// keep date auto-detect off as well, as different date format can create problems in different countries
		okByType[ODLColumnType.DATE.ordinal()]=false;
		
		// all engine types stay off
		for(ODLColumnType type : ODLColumnType.values()){
			if(type.isEngineType()){
				okByType[type.ordinal()]=false;
			}
		}
	}
	
	void processValue(String value){
		if (Strings.isEmpty(value) == false) {
			nbNonEmptyVals++;
			for (ODLColumnType otherType : ODLColumnType.standardTypes()) {
				if (okByType[otherType.ordinal()]) {
					okByType[otherType.ordinal()] = ColumnValueProcessor.convertToMe(otherType,value, ODLColumnType.STRING, true) != null;
				}
			}
		}
	}
	
	ODLColumnType getEstimatedType(){
		ODLColumnType selectedType = ODLColumnType.STRING;		
		if (nbNonEmptyVals > 0) {
			// if we had non empty values pick the first non-string type that converted for all
			for (ODLColumnType otherType : ODLColumnType.standardTypes()) {
				if (otherType != ODLColumnType.STRING && okByType[otherType.ordinal()]) {
					selectedType = otherType;
					break;
				}
			}
		}
		return selectedType;	
	}
}
