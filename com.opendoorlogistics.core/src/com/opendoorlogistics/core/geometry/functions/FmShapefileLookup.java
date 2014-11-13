/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.functions;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;

public final class FmShapefileLookup extends FunctionImpl {
	public FmShapefileLookup(Function filename, Function searchvalue, Function type, Function searchfield) {
		super(filename, searchvalue, type, searchfield);
	}

	public FmShapefileLookup(Function filename, Function searchvalue, Function searchfield) {
		super(filename, searchvalue, searchfield);
	}
	
	@Override
	public Object execute(FunctionParameters parameters) {
		Object[] children = executeChildFormulae(parameters, true);
		if (children == null) {
			return Functions.EXECUTION_ERROR;
		}

		// ensure we use canonical string conversion
		String [] strs = new String[children.length];
		for(int i =0 ; i < strs.length ; i++){
			strs[i] = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, children[i]);
		}
		
		if(children.length==4){
			return Spatial.lookupshapefile(strs[0],strs[1],strs[2],strs[3]);	
		}
		else{
			return Spatial.lookupshapefile(strs[0],strs[1],null,strs[2]);			
		}
	}

	@Override
	public Function deepCopy() {
		return new FmShapefileLookup(child(0).deepCopy(), child(1).deepCopy(), child(2).deepCopy(), child(3).deepCopy());
	}

	@Override
	public String toString() {
		return toString("shapefilelookup");
	}
}